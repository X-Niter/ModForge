/**
 * Notification System Routes for ModForge
 * 
 * This module provides API endpoints for configuring the notification system,
 * allowing administrators to customize notification preferences.
 */

import { Router, Request, Response, NextFunction } from 'express';
import { getLogger } from '../logging';

// Import Express user type
declare global {
  namespace Express {
    interface User {
      id: number;
      username: string;
      isAdmin: boolean;
      [key: string]: any;
    }
  }
}
import { 
  updateNotificationSettings, 
  getNotificationSettings, 
  NotificationChannel, 
  NotificationSeverity,
  NotificationType,
  NotificationSettings,
  sendNotification
} from '../notification-manager';
import { z } from 'zod';

const logger = getLogger('notification-routes');
const router = Router();

/**
 * Middleware to ensure only admin users can access protected endpoints
 */
function requireAdmin(req: Request, res: Response, next: NextFunction) {
  // Check if user exists and is authenticated
  if (!req.user) {
    return res.status(401).json({ error: 'Authentication required' });
  }
  
  // Check if user has admin privileges
  if (!req.user.isAdmin) {
    logger.warn('Unauthorized access attempt to admin endpoint', { 
      userId: req.user.id,
      username: req.user.username,
      endpoint: req.originalUrl
    });
    return res.status(403).json({ error: 'Admin privileges required' });
  }
  
  // User is authenticated and has admin privileges
  next();
}

/**
 * Sanitize config object by removing sensitive information
 * @param config The configuration object to sanitize
 * @returns Sanitized configuration object
 */
function sanitizeConfigObject(config: Record<string, any>): Record<string, any> {
  // Handle empty/null configs
  if (!config || typeof config !== 'object') {
    return {};
  }
  
  return Object.fromEntries(
    Object.entries(config).filter(([key]) => 
      // Filter out sensitive keys
      !key.toLowerCase().includes('key') && 
      !key.toLowerCase().includes('token') && 
      !key.toLowerCase().includes('pass') && 
      !key.toLowerCase().includes('secret')
    ).map(([key, value]) => {
      // Handle different value types
      if (typeof value === 'string') {
        // Redact URLs and endpoints
        if (key.toLowerCase().includes('url') || key.toLowerCase().includes('endpoint')) {
          return [key, '[REDACTED URL]'];
        } 
        return [key, value];
      } else if (value && typeof value === 'object' && !Array.isArray(value)) {
        // Recursively sanitize nested objects
        return [key, sanitizeConfigObject(value)];
      } else if (Array.isArray(value)) {
        // Handle arrays by sanitizing each object in the array if needed
        return [key, value.map(item => 
          item && typeof item === 'object' ? sanitizeConfigObject(item) : item
        )];
      } else {
        // Pass through other value types unchanged
        return [key, value];
      }
    })
  );
}

// Validation schema for notification settings
const notificationChannelConfigSchema = z.object({
  enabled: z.boolean(),
  channel: z.enum([
    NotificationChannel.EMAIL,
    NotificationChannel.SLACK,
    NotificationChannel.WEBHOOK,
    NotificationChannel.SMS,
    NotificationChannel.LOG
  ]),
  minSeverity: z.enum([
    NotificationSeverity.INFO,
    NotificationSeverity.WARNING,
    NotificationSeverity.ERROR,
    NotificationSeverity.CRITICAL
  ]),
  config: z.record(z.string(), z.union([
    z.string(), 
    z.number(), 
    z.boolean(), 
    z.null(),
    z.record(z.string(), z.union([z.string(), z.number(), z.boolean(), z.null()])),
    z.array(z.union([z.string(), z.number(), z.boolean(), z.null()]))
  ]))
});

const updateNotificationSettingsSchema = z.object({
  channels: z.array(notificationChannelConfigSchema).optional(),
  throttling: z.object({
    enabled: z.boolean().optional(),
    maxPerHour: z.number().min(1).optional(),
    maxPerDay: z.number().min(1).optional()
  }).optional(),
  batchingSeconds: z.number().min(0).optional()
});

// Get current notification settings
router.get('/settings', requireAdmin, (req: Request, res: Response) => {
  try {
    const settings = getNotificationSettings();
    
    // Don't expose sensitive information like API keys or passwords
    const sanitizedSettings = {
      ...settings,
      channels: settings.channels.map(channel => ({
        ...channel,
        config: sanitizeConfigObject(channel.config)
      }))
    };
    
    res.json(sanitizedSettings);
  } catch (error) {
    logger.error('Failed to get notification settings', { error });
    res.status(500).json({ error: 'Failed to get notification settings' });
  }
});

// Update notification settings
router.post('/settings', requireAdmin, (req: Request, res: Response) => {
  try {
    // Validate request body
    const result = updateNotificationSettingsSchema.safeParse(req.body);
    
    if (!result.success) {
      logger.warn('Invalid notification settings update request', { 
        issues: result.error.issues 
      });
      return res.status(400).json({ error: 'Invalid notification settings', details: result.error.issues });
    }
    
    // Transform to proper format for updateNotificationSettings
    const settingsToUpdate: Partial<NotificationSettings> = {};
    
    if (result.data.channels) {
      settingsToUpdate.channels = result.data.channels;
    }
    
    if (result.data.batchingSeconds !== undefined) {
      settingsToUpdate.batchingSeconds = result.data.batchingSeconds;
    }
    
    if (result.data.throttling) {
      // Convert partial throttling settings to required format
      const currentThrottling = getNotificationSettings().throttling;
      const throttlingEntries = Object.entries(result.data.throttling)
        .filter(([_, v]) => v !== undefined);
      
      // Ensure all required fields are present with proper types
      settingsToUpdate.throttling = {
        enabled: 'enabled' in result.data.throttling && result.data.throttling.enabled !== undefined 
          ? !!result.data.throttling.enabled 
          : currentThrottling.enabled,
        maxPerHour: 'maxPerHour' in result.data.throttling && result.data.throttling.maxPerHour !== undefined 
          ? result.data.throttling.maxPerHour 
          : currentThrottling.maxPerHour,
        maxPerDay: 'maxPerDay' in result.data.throttling && result.data.throttling.maxPerDay !== undefined 
          ? result.data.throttling.maxPerDay 
          : currentThrottling.maxPerDay
      };
    }
    
    // Update settings
    updateNotificationSettings(settingsToUpdate);
    
    // Return updated settings
    const settings = getNotificationSettings();
    res.json({ 
      message: 'Notification settings updated successfully',
      settings
    });
  } catch (error) {
    logger.error('Failed to update notification settings', { error });
    res.status(500).json({ error: 'Failed to update notification settings' });
  }
});

// Test notification endpoint
router.post('/test', requireAdmin, (req: Request, res: Response) => {
  try {
    // Validate request
    const testSchema = z.object({
      channel: z.enum([
        NotificationChannel.EMAIL,
        NotificationChannel.SLACK,
        NotificationChannel.WEBHOOK,
        NotificationChannel.SMS,
        NotificationChannel.LOG
      ]).optional(),
      severity: z.enum([
        NotificationSeverity.INFO,
        NotificationSeverity.WARNING,
        NotificationSeverity.ERROR,
        NotificationSeverity.CRITICAL
      ]).optional()
    });
    
    const result = testSchema.safeParse(req.body);
    if (!result.success) {
      return res.status(400).json({ 
        error: 'Invalid test notification parameters', 
        details: result.error.issues 
      });
    }
    
    // Default to LOG channel and INFO severity if not specified
    const channel = result.data.channel || NotificationChannel.LOG;
    const severity = result.data.severity || NotificationSeverity.INFO;
    
    // No need to re-import as we already imported at the top
    
    // Send a test notification
    sendNotification({
      type: NotificationType.SYSTEM_STATUS,
      severity: severity,
      title: 'Test Notification',
      message: `This is a test notification sent to the ${channel} channel with ${severity} severity.`,
      details: {
        isTest: true,
        requestedBy: req.user?.username || 'anonymous',
        timestamp: new Date().toISOString()
      }
    });
    
    res.json({ 
      message: `Test notification sent to ${channel} channel with ${severity} severity`,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to send test notification', { error });
    res.status(500).json({ error: 'Failed to send test notification' });
  }
});

export default router;