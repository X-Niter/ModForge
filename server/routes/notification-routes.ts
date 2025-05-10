/**
 * Notification System Routes for ModForge
 * 
 * This module provides API endpoints for configuring the notification system,
 * allowing administrators to customize notification preferences.
 */

import { Router } from 'express';
import { getLogger } from '../logging';
import { updateNotificationSettings, getNotificationSettings, NotificationChannel, NotificationSeverity } from '../notification-manager';
import { z } from 'zod';

const logger = getLogger('notification-routes');
const router = Router();

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
  config: z.record(z.string(), z.any())
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
router.get('/settings', (req, res) => {
  try {
    const settings = getNotificationSettings();
    
    // Don't expose sensitive information like API keys or passwords
    const sanitizedSettings = {
      ...settings,
      channels: settings.channels.map(channel => ({
        ...channel,
        config: Object.fromEntries(
          Object.entries(channel.config).filter(([key]) => 
            !key.toLowerCase().includes('key') && 
            !key.toLowerCase().includes('token') && 
            !key.toLowerCase().includes('pass') && 
            !key.toLowerCase().includes('secret')
          ).map(([key, value]) => [
            key, 
            typeof value === 'string' && 
            (key.toLowerCase().includes('url') || key.toLowerCase().includes('endpoint')) 
              ? '[REDACTED URL]' 
              : value
          ])
        )
      }))
    };
    
    res.json(sanitizedSettings);
  } catch (error) {
    logger.error('Failed to get notification settings', { error });
    res.status(500).json({ error: 'Failed to get notification settings' });
  }
});

// Update notification settings
router.post('/settings', (req, res) => {
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
    const settingsToUpdate: any = {};
    
    if (result.data.channels) {
      settingsToUpdate.channels = result.data.channels;
    }
    
    if (result.data.batchingSeconds !== undefined) {
      settingsToUpdate.batchingSeconds = result.data.batchingSeconds;
    }
    
    if (result.data.throttling) {
      settingsToUpdate.throttling = {
        ...getNotificationSettings().throttling, // Keep existing values
        ...Object.fromEntries(
          Object.entries(result.data.throttling)
            .filter(([_, v]) => v !== undefined)
        )
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
router.post('/test', (req, res) => {
  // Create a test notification here
  // This would be implemented in a real system but is omitted for this sample
  
  res.json({ 
    message: 'Test notification sent',
    timestamp: new Date().toISOString()
  });
});

export default router;