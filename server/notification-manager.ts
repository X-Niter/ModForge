/**
 * Notification Manager for ModForge
 * 
 * This module provides alerting capabilities for critical system events,
 * ensuring administrators are promptly notified of important issues.
 */

import { getLogger } from './logging';
import nodemailer from 'nodemailer';
import axios from 'axios';
import { ErrorSeverity, ErrorCategory } from './error-types';

// Setup logger
const logger = getLogger('notification');

// Notification types
export enum NotificationType {
  SYSTEM_STATUS = 'system_status',
  ERROR_ALERT = 'error_alert',
  BACKUP_STATUS = 'backup_status',
  SECURITY_ALERT = 'security_alert',
  CONTINUOUS_SERVICE = 'continuous_service',
  SCHEDULED_JOB = 'scheduled_job'
}

// Notification severity
export enum NotificationSeverity {
  INFO = 'info',
  WARNING = 'warning',
  ERROR = 'error',
  CRITICAL = 'critical'
}

// Notification channels
export enum NotificationChannel {
  EMAIL = 'email',
  SLACK = 'slack',
  WEBHOOK = 'webhook',
  SMS = 'sms',
  LOG = 'log'
}

// Notification message
export interface NotificationMessage {
  type: NotificationType;
  severity: NotificationSeverity;
  title: string;
  message: string;
  details?: Record<string, any>;
  timestamp: Date;
}

// Notification channel configuration
// Type definition for email notification config
export interface EmailNotificationConfig {
  recipients: string[];
  from: string;
  smtp: {
    host: string;
    port: number;
    secure: boolean;
    auth: {
      user: string;
      pass: string;
    };
  };
}

// Type definition for Slack notification config
export interface SlackNotificationConfig {
  webhookUrl: string;
  channel?: string;
  username?: string;
}

// Type definition for webhook notification config
export interface WebhookNotificationConfig {
  url: string;
  method?: 'GET' | 'POST';
  headers?: Record<string, string>;
}

// Type definition for SMS notification config
export interface SmsNotificationConfig {
  to: string[];
  serviceProvider: string;
  apiKey: string;
}

// Type definition for log notification config
export interface LogNotificationConfig {
  logLevel?: string;
  includeDetails?: boolean;
  format?: string;
}

// Union type for all notification configs
export type NotificationConfigType = 
  | EmailNotificationConfig 
  | SlackNotificationConfig 
  | WebhookNotificationConfig 
  | SmsNotificationConfig
  | LogNotificationConfig
  | Record<string, unknown>;

export interface NotificationChannelConfig {
  enabled: boolean;
  channel: NotificationChannel;
  minSeverity: NotificationSeverity;
  config: NotificationConfigType;
}

// Notification settings
export interface NotificationSettings {
  channels: NotificationChannelConfig[];
  throttling: {
    enabled: boolean;
    maxPerHour: number;
    maxPerDay: number;
  };
  batchingSeconds: number;
}

// Default notification settings
const defaultSettings: NotificationSettings = {
  channels: [
    {
      enabled: process.env.SMTP_HOST !== undefined,
      channel: NotificationChannel.EMAIL,
      minSeverity: NotificationSeverity.ERROR,
      config: {
        recipients: (process.env.ADMIN_EMAILS || '').split(','),
        from: process.env.SMTP_FROM || 'modforge@example.com',
        smtp: {
          host: process.env.SMTP_HOST,
          port: process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT, 10) : 587,
          secure: process.env.SMTP_SECURE === 'true',
          auth: {
            user: process.env.SMTP_USER,
            pass: process.env.SMTP_PASS
          }
        }
      }
    },
    {
      enabled: process.env.SLACK_WEBHOOK_URL !== undefined,
      channel: NotificationChannel.SLACK,
      minSeverity: NotificationSeverity.WARNING,
      config: {
        webhookUrl: process.env.SLACK_WEBHOOK_URL,
        channel: process.env.SLACK_CHANNEL
      }
    },
    {
      enabled: process.env.WEBHOOK_URL !== undefined,
      channel: NotificationChannel.WEBHOOK,
      minSeverity: NotificationSeverity.ERROR,
      config: {
        url: process.env.WEBHOOK_URL,
        method: process.env.WEBHOOK_METHOD || 'POST',
        headers: { 
          'Authorization': process.env.WEBHOOK_AUTH_TOKEN ? 
            `Bearer ${process.env.WEBHOOK_AUTH_TOKEN}` : undefined
        }
      }
    },
    {
      enabled: process.env.TWILIO_ACCOUNT_SID !== undefined && 
               process.env.TWILIO_AUTH_TOKEN !== undefined &&
               process.env.TWILIO_PHONE_NUMBER !== undefined,
      channel: NotificationChannel.SMS,
      minSeverity: NotificationSeverity.CRITICAL,
      config: {
        to: (process.env.ADMIN_PHONE_NUMBERS || '').split(','),
        serviceProvider: 'twilio',
        apiKey: process.env.TWILIO_AUTH_TOKEN
      }
    },
    {
      enabled: true,
      channel: NotificationChannel.LOG,
      minSeverity: NotificationSeverity.INFO,
      config: {}
    }
  ],
  throttling: {
    enabled: true,
    maxPerHour: 10,
    maxPerDay: 50
  },
  batchingSeconds: 60 // Batch notifications for 60 seconds
};

// In-memory store for notification counters and batching
interface NotificationState {
  counters: {
    hourly: {
      timestamp: Date;
      count: number;
    };
    daily: {
      timestamp: Date;
      count: number;
    };
  };
  batchedNotifications: {
    [key: string]: { // Key is NotificationType:NotificationSeverity
      notifications: NotificationMessage[];
      lastScheduled: Date | null;
    };
  };
}

// Initialize notification state
const notificationState: NotificationState = {
  counters: {
    hourly: {
      timestamp: new Date(),
      count: 0
    },
    daily: {
      timestamp: new Date(),
      count: 0
    }
  },
  batchedNotifications: {}
};

// Current notification settings
let currentSettings: NotificationSettings = { ...defaultSettings };

/**
 * Update notification settings
 */
export function updateNotificationSettings(settings: Partial<NotificationSettings>): void {
  currentSettings = {
    ...currentSettings,
    ...settings,
    channels: settings.channels || currentSettings.channels,
    throttling: {
      ...currentSettings.throttling,
      ...(settings.throttling || {})
    }
  };
  
  logger.info('Notification settings updated', { 
    channelCount: currentSettings.channels.length,
    throttling: currentSettings.throttling.enabled
  });
}

/**
 * Get current notification settings
 */
export function getNotificationSettings(): NotificationSettings {
  return { ...currentSettings };
}

/**
 * Send an email notification
 */
async function sendEmailNotification(
  message: NotificationMessage,
  config: NotificationConfigType
): Promise<boolean> {
  // Type guard to check if config is an EmailNotificationConfig
  function isEmailConfig(cfg: NotificationConfigType): cfg is EmailNotificationConfig {
    return 'recipients' in cfg && 'from' in cfg && 'smtp' in cfg;
  }
  
  // Validate config is email config
  if (!isEmailConfig(config)) {
    logger.error('Invalid email notification config', { config });
    return false;
  }
  try {
    const { recipients, from, smtp } = config;
    
    if (!recipients || recipients.length === 0 || !smtp.host) {
      logger.warn('Missing email configuration', { config });
      return false;
    }
    
    // Create transporter
    const transporter = nodemailer.createTransport(smtp);
    
    // Severity emoji
    const severityEmoji = {
      [NotificationSeverity.INFO]: 'ℹ️',
      [NotificationSeverity.WARNING]: '⚠️',
      [NotificationSeverity.ERROR]: '❌',
      [NotificationSeverity.CRITICAL]: '🔥'
    };
    
    // Format details as HTML if available
    let detailsHtml = '';
    if (message.details) {
      detailsHtml = '<h3>Details:</h3><ul>';
      for (const [key, value] of Object.entries(message.details)) {
        detailsHtml += `<li><strong>${key}:</strong> ${typeof value === 'object' ? JSON.stringify(value) : value}</li>`;
      }
      detailsHtml += '</ul>';
    }
    
    // Send email
    const info = await transporter.sendMail({
      from,
      to: recipients.join(','),
      subject: `${severityEmoji[message.severity]} ModForge Alert: ${message.title}`,
      html: `
        <h2>${severityEmoji[message.severity]} ${message.title}</h2>
        <p>${message.message}</p>
        ${detailsHtml}
        <p style="color: #666; font-size: 0.8em;">
          Notification type: ${message.type}<br>
          Severity: ${message.severity}<br>
          Time: ${message.timestamp.toISOString()}
        </p>
      `
    });
    
    logger.info('Email notification sent', { 
      messageId: info.messageId,
      recipients: recipients.length
    });
    
    return true;
  } catch (error) {
    logger.error('Failed to send email notification', { error });
    return false;
  }
}

/**
 * Send a Slack notification
 */
async function sendSlackNotification(
  message: NotificationMessage,
  config: NotificationConfigType
): Promise<boolean> {
  // Type guard to check if config is a SlackNotificationConfig
  function isSlackConfig(cfg: NotificationConfigType): cfg is SlackNotificationConfig {
    return 'webhookUrl' in cfg;
  }
  
  // Validate config is Slack config
  if (!isSlackConfig(config)) {
    logger.error('Invalid Slack notification config', { config });
    return false;
  }
  try {
    const { webhookUrl, channel } = config;
    
    if (!webhookUrl) {
      logger.warn('Missing Slack webhook URL', { config });
      return false;
    }
    
    // Severity emoji and color
    const severityInfo = {
      [NotificationSeverity.INFO]: { emoji: 'ℹ️', color: '#36a64f' },
      [NotificationSeverity.WARNING]: { emoji: '⚠️', color: '#ffcc00' },
      [NotificationSeverity.ERROR]: { emoji: '❌', color: '#ff9900' },
      [NotificationSeverity.CRITICAL]: { emoji: '🔥', color: '#ff0000' }
    };
    
    // Format details as fields if available
    const fields = [];
    if (message.details) {
      for (const [key, value] of Object.entries(message.details)) {
        fields.push({
          title: key,
          value: typeof value === 'object' ? JSON.stringify(value) : String(value),
          short: String(value).length < 20
        });
      }
    }
    
    // Prepare Slack message
    const slackMessage: any = {
      attachments: [
        {
          color: severityInfo[message.severity].color,
          pretext: `${severityInfo[message.severity].emoji} *ModForge Alert*`,
          title: message.title,
          text: message.message,
          fields: [
            {
              title: 'Type',
              value: message.type,
              short: true
            },
            {
              title: 'Severity',
              value: message.severity,
              short: true
            },
            ...fields
          ],
          footer: `Time: ${message.timestamp.toISOString()}`,
          ts: Math.floor(message.timestamp.getTime() / 1000)
        }
      ]
    };
    
    // Add channel if specified
    if (channel) {
      slackMessage.channel = channel;
    }
    
    // Send to Slack
    await axios.post(webhookUrl, slackMessage, {
      headers: { 'Content-Type': 'application/json' }
    });
    
    logger.info('Slack notification sent', { channel });
    
    return true;
  } catch (error) {
    logger.error('Failed to send Slack notification', { error });
    return false;
  }
}

/**
 * Send a webhook notification
 */
async function sendWebhookNotification(
  message: NotificationMessage,
  config: NotificationConfigType
): Promise<boolean> {
  // Type guard to check if config is a WebhookNotificationConfig
  function isWebhookConfig(cfg: NotificationConfigType): cfg is WebhookNotificationConfig {
    return 'url' in cfg;
  }
  
  // Validate config is Webhook config
  if (!isWebhookConfig(config)) {
    logger.error('Invalid webhook notification config', { config });
    return false;
  }
  
  try {
    const { url, method = 'POST', headers = {} } = config;
    
    if (!url) {
      logger.warn('Missing webhook URL', { config });
      return false;
    }
    
    // Create a webhook payload from the notification message
    const payload = {
      title: message.title,
      message: message.message,
      severity: message.severity,
      type: message.type,
      timestamp: message.timestamp.toISOString(),
      details: message.details || {}
    };

    // Send to webhook endpoint
    await axios({
      method: method,
      url: url,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      },
      data: payload
    });
    
    logger.info('Webhook notification sent', { url });
    
    return true;
  } catch (error) {
    logger.error('Failed to send webhook notification', { error });
    return false;
  }
}

/**
 * Send an SMS notification
 */
async function sendSmsNotification(
  message: NotificationMessage,
  config: NotificationConfigType
): Promise<boolean> {
  // Type guard to check if config is an SmsNotificationConfig
  function isSmsConfig(cfg: NotificationConfigType): cfg is SmsNotificationConfig {
    return 'to' in cfg && 'serviceProvider' in cfg;
  }
  
  // Validate config is SMS config
  if (!isSmsConfig(config)) {
    logger.error('Invalid SMS notification config', { config });
    return false;
  }
  
  try {
    const { to, serviceProvider, apiKey } = config;
    
    if (!to || !to.length) {
      logger.warn('No recipients for SMS notification', { config });
      return false;
    }
    
    if (!apiKey) {
      logger.warn('Missing SMS API key', { serviceProvider });
      return false;
    }
    
    // Format a concise message for SMS (keeping it short)
    const smsContent = `${message.severity.toUpperCase()}: ${message.title}`;
    
    // This is a generalized implementation that would need to be customized
    // based on the specific SMS service provider being used
    switch (serviceProvider.toLowerCase()) {
      case 'twilio':
        // Example implementation for Twilio
        if (!process.env.TWILIO_ACCOUNT_SID) {
          logger.error('Missing TWILIO_ACCOUNT_SID environment variable');
          return false;
        }
        
        if (!process.env.TWILIO_PHONE_NUMBER) {
          logger.error('Missing TWILIO_PHONE_NUMBER environment variable');
          return false;
        }
        
        const accountSid = process.env.TWILIO_ACCOUNT_SID;
        
        for (const recipient of to) {
          try {
            await axios.post(
              `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Messages.json`,
              new URLSearchParams({
                'To': recipient,
                'From': process.env.TWILIO_PHONE_NUMBER,
                'Body': smsContent
              }),
              {
                auth: {
                  username: accountSid,
                  password: apiKey
                }
              }
            );
            logger.debug('SMS sent successfully', { to: recipient });
          } catch (err) {
            logger.error('Failed to send SMS to recipient', { recipient, error: err });
            // Continue with other recipients even if one fails
          }
        }
        break;
        
      default:
        logger.warn(`Unsupported SMS provider: ${serviceProvider}`);
        return false;
    }
    
    logger.info('SMS notifications sent', { recipients: to.length, provider: serviceProvider });
    return true;
  } catch (error) {
    logger.error('Failed to send SMS notification', { error });
    return false;
  }
}

/**
 * Send a log notification
 */
function sendLogNotification(
  message: NotificationMessage,
  config: NotificationConfigType
): boolean {
  // Type guard for log config
  function isLogConfig(cfg: NotificationConfigType): cfg is LogNotificationConfig {
    // Log config can be empty, so just check it's an object
    return typeof cfg === 'object' && cfg !== null;
  }
  
  // Even if config isn't a proper LogNotificationConfig, we can still log
  const logConfig = isLogConfig(config) ? config : {};
  try {
    // Log the notification based on severity
    const { title, severity, type } = message;
    // Use the includeDetails config option if available
    const includeDetails = logConfig.includeDetails !== false;
    const details = includeDetails && message.details ? JSON.stringify(message.details) : '';
    
    // Use the format config option if available
    const format = logConfig.format || `[${type}] ${title}: ${message.message} ${details}`;
    const logMessage = logConfig.format ? 
      format.replace('{type}', type)
            .replace('{title}', title)
            .replace('{message}', message.message)
            .replace('{details}', details) 
      : `[${type}] ${title}: ${message.message} ${details}`;
    
    switch (severity) {
      case NotificationSeverity.CRITICAL:
        logger.critical(logMessage);
        break;
      case NotificationSeverity.ERROR:
        logger.error(logMessage);
        break;
      case NotificationSeverity.WARNING:
        logger.warn(logMessage);
        break;
      case NotificationSeverity.INFO:
        logger.info(logMessage);
        break;
    }
    
    return true;
  } catch (error) {
    // This should never fail, but just in case
    console.error('Failed to log notification:', error);
    return false;
  }
}

/**
 * Check if notification should be throttled
 */
function shouldThrottle(): boolean {
  if (!currentSettings.throttling.enabled) {
    return false;
  }
  
  const now = new Date();
  
  // Reset hourly counter if needed
  if (now.getTime() - notificationState.counters.hourly.timestamp.getTime() > 60 * 60 * 1000) {
    notificationState.counters.hourly = {
      timestamp: now,
      count: 0
    };
  }
  
  // Reset daily counter if needed
  if (now.getTime() - notificationState.counters.daily.timestamp.getTime() > 24 * 60 * 60 * 1000) {
    notificationState.counters.daily = {
      timestamp: now,
      count: 0
    };
  }
  
  // Check if we've exceeded the limits
  return (
    notificationState.counters.hourly.count >= currentSettings.throttling.maxPerHour ||
    notificationState.counters.daily.count >= currentSettings.throttling.maxPerDay
  );
}

/**
 * Update notification counters
 */
function updateNotificationCounters(): void {
  notificationState.counters.hourly.count++;
  notificationState.counters.daily.count++;
}

/**
 * Get batch key for a notification
 */
function getBatchKey(message: NotificationMessage): string {
  return `${message.type}:${message.severity}`;
}

/**
 * Add notification to batch
 */
function addToBatch(message: NotificationMessage): void {
  const key = getBatchKey(message);
  
  if (!notificationState.batchedNotifications[key]) {
    notificationState.batchedNotifications[key] = {
      notifications: [],
      lastScheduled: null
    };
  }
  
  notificationState.batchedNotifications[key].notifications.push(message);
  
  // Schedule batch processing if not already scheduled
  if (!notificationState.batchedNotifications[key].lastScheduled) {
    notificationState.batchedNotifications[key].lastScheduled = new Date();
    
    setTimeout(() => {
      processBatch(key);
    }, currentSettings.batchingSeconds * 1000);
  }
}

/**
 * Process a batch of notifications
 */
async function processBatch(key: string): Promise<void> {
  const batch = notificationState.batchedNotifications[key];
  
  if (!batch || batch.notifications.length === 0) {
    return;
  }
  
  const [type, severityStr] = key.split(':');
  const severity = severityStr as NotificationSeverity;
  
  // Create a combined notification
  const firstNotification = batch.notifications[0];
  const notificationCount = batch.notifications.length;
  
  // Skip if throttled
  if (shouldThrottle()) {
    logger.warn('Notifications throttled, skipping batch', { 
      key, 
      count: notificationCount,
      hourlyCount: notificationState.counters.hourly.count,
      dailyLimit: currentSettings.throttling.maxPerDay
    });
    
    // Clear the batch
    delete notificationState.batchedNotifications[key];
    return;
  }
  
  // Create a combined message
  const combinedMessage: NotificationMessage = {
    type: firstNotification.type as NotificationType,
    severity: severity,
    title: notificationCount > 1 
      ? `${firstNotification.title} (+${notificationCount - 1} more)`
      : firstNotification.title,
    message: notificationCount > 1
      ? `${firstNotification.message}\n\nThis is a combined alert including ${notificationCount} similar notifications.`
      : firstNotification.message,
    details: {
      ...firstNotification.details,
      batchSize: notificationCount,
      additionalMessages: notificationCount > 1 
        ? batch.notifications.slice(1, 4).map(n => n.message) 
        : undefined
    },
    timestamp: new Date()
  };
  
  // Send the notification to all enabled channels
  await sendNotificationToAllChannels(combinedMessage);
  
  // Update counters
  updateNotificationCounters();
  
  // Clear the batch
  delete notificationState.batchedNotifications[key];
}

/**
 * Send notification to all enabled channels
 */
async function sendNotificationToAllChannels(message: NotificationMessage): Promise<void> {
  const enabledChannels = currentSettings.channels.filter(channel => 
    channel.enabled && 
    shouldSendNotificationToChannel(message.severity, channel.minSeverity)
  );
  
  if (enabledChannels.length === 0) {
    logger.debug('No enabled channels for notification', { 
      type: message.type,
      severity: message.severity 
    });
    return;
  }
  
  // Send to each channel
  for (const channelConfig of enabledChannels) {
    try {
      let success = false;
      
      switch (channelConfig.channel) {
        case NotificationChannel.EMAIL:
          success = await sendEmailNotification(message, channelConfig.config);
          break;
        case NotificationChannel.SLACK:
          success = await sendSlackNotification(message, channelConfig.config);
          break;
        case NotificationChannel.WEBHOOK:
          success = await sendWebhookNotification(message, channelConfig.config);
          break;
        case NotificationChannel.SMS:
          success = await sendSmsNotification(message, channelConfig.config);
          break;
        case NotificationChannel.LOG:
          success = sendLogNotification(message, channelConfig.config);
          break;
        default:
          logger.warn(`Unsupported notification channel: ${channelConfig.channel}`);
      }
      
      if (!success) {
        logger.warn(`Failed to send notification via ${channelConfig.channel}`);
      }
    } catch (error) {
      logger.error(`Error sending notification via ${channelConfig.channel}`, { error });
    }
  }
}

/**
 * Check if notification should be sent to a channel based on severity
 */
function shouldSendNotificationToChannel(
  notificationSeverity: NotificationSeverity,
  channelMinSeverity: NotificationSeverity
): boolean {
  const severityOrder = [
    NotificationSeverity.INFO,
    NotificationSeverity.WARNING,
    NotificationSeverity.ERROR,
    NotificationSeverity.CRITICAL
  ];
  
  const notificationLevel = severityOrder.indexOf(notificationSeverity);
  const minLevel = severityOrder.indexOf(channelMinSeverity);
  
  return notificationLevel >= minLevel;
}

/**
 * Send a notification
 */
export async function sendNotification(
  message: Omit<NotificationMessage, 'timestamp'>
): Promise<void> {
  const fullMessage: NotificationMessage = {
    ...message,
    timestamp: new Date()
  };
  
  // Always log the notification regardless of other settings
  sendLogNotification(fullMessage, {});
  
  // If batching is enabled, add to batch
  if (currentSettings.batchingSeconds > 0) {
    addToBatch(fullMessage);
    return;
  }
  
  // Skip if throttled
  if (shouldThrottle()) {
    logger.warn('Notifications throttled, skipping', { 
      type: message.type,
      severity: message.severity,
      hourlyCount: notificationState.counters.hourly.count,
      hourlyLimit: currentSettings.throttling.maxPerHour
    });
    return;
  }
  
  // Send to all channels
  await sendNotificationToAllChannels(fullMessage);
  
  // Update counters
  updateNotificationCounters();
}

/**
 * Send an error notification
 */
export async function sendErrorNotification(
  error: Error,
  severity: NotificationSeverity = NotificationSeverity.ERROR,
  context: Record<string, any> = {}
): Promise<void> {
  await sendNotification({
    type: NotificationType.ERROR_ALERT,
    severity,
    title: `Error: ${error.name || 'Application Error'}`,
    message: error.message,
    details: {
      stack: error.stack,
      ...context
    }
  });
}

/**
 * Send a system status notification
 */
export async function sendSystemStatusNotification(
  status: 'up' | 'down' | 'degraded',
  message: string,
  details: Record<string, any> = {}
): Promise<void> {
  // Determine severity based on status
  let severity: NotificationSeverity;
  switch (status) {
    case 'up':
      severity = NotificationSeverity.INFO;
      break;
    case 'degraded':
      severity = NotificationSeverity.WARNING;
      break;
    case 'down':
      severity = NotificationSeverity.CRITICAL;
      break;
    default:
      severity = NotificationSeverity.INFO;
  }
  
  await sendNotification({
    type: NotificationType.SYSTEM_STATUS,
    severity,
    title: `System Status: ${status.toUpperCase()}`,
    message,
    details
  });
}

/**
 * Send a backup status notification
 */
export async function sendBackupStatusNotification(
  status: 'success' | 'partial' | 'failed',
  message: string,
  details: Record<string, any> = {}
): Promise<void> {
  // Determine severity based on status
  let severity: NotificationSeverity;
  switch (status) {
    case 'success':
      severity = NotificationSeverity.INFO;
      break;
    case 'partial':
      severity = NotificationSeverity.WARNING;
      break;
    case 'failed':
      severity = NotificationSeverity.ERROR;
      break;
    default:
      severity = NotificationSeverity.INFO;
  }
  
  await sendNotification({
    type: NotificationType.BACKUP_STATUS,
    severity,
    title: `Backup Status: ${status.toUpperCase()}`,
    message,
    details
  });
}

/**
 * Map error severity to notification severity
 */
function mapErrorSeverity(errorSeverity: ErrorSeverity): NotificationSeverity {
  switch (errorSeverity) {
    case ErrorSeverity.CRITICAL:
      return NotificationSeverity.CRITICAL;
    case ErrorSeverity.HIGH:
      return NotificationSeverity.ERROR;
    case ErrorSeverity.MEDIUM:
      return NotificationSeverity.WARNING;
    case ErrorSeverity.LOW:
      return NotificationSeverity.INFO;
    default:
      return NotificationSeverity.INFO;
  }
}

/**
 * Send a notification for a tracked error
 */
export async function sendTrackedErrorNotification(
  errorId: string,
  message: string,
  errorSeverity: ErrorSeverity,
  category: ErrorCategory,
  count: number,
  details: Record<string, any> = {}
): Promise<void> {
  const notificationSeverity = mapErrorSeverity(errorSeverity);
  
  await sendNotification({
    type: NotificationType.ERROR_ALERT,
    severity: notificationSeverity,
    title: `${errorSeverity.toUpperCase()} Error [${category}]`,
    message: `${message}${count > 1 ? ` (occurred ${count} times)` : ''}`,
    details: {
      errorId,
      category,
      count,
      ...details
    }
  });
}

/**
 * Initialize the notification system
 */
// Track pending notification attempts for retry
interface PendingNotification {
  message: NotificationMessage;
  attempts: number;
  lastAttempt: Date;
  maxAttempts: number;
  retryIntervalMs: number;
}

// Queue of failed notifications for retry
const failedNotificationsQueue: PendingNotification[] = [];

// Retry intervals with exponential backoff (in ms)
const RETRY_INTERVALS = [
  30 * 1000,      // 30 seconds
  2 * 60 * 1000,  // 2 minutes
  10 * 60 * 1000, // 10 minutes
  30 * 60 * 1000  // 30 minutes
];

// Maximum retry attempts for notifications
const MAX_RETRY_ATTEMPTS = 4;

/**
 * Process the failed notifications queue, retrying with backoff
 */
async function processFailedNotificationsQueue(): Promise<void> {
  if (failedNotificationsQueue.length === 0) {
    return;
  }
  
  logger.debug(`Processing failed notifications queue (${failedNotificationsQueue.length} items)`);
  
  const now = new Date();
  const notificationsToRetry = failedNotificationsQueue.filter(item => {
    const timeSinceLastAttempt = now.getTime() - item.lastAttempt.getTime();
    return timeSinceLastAttempt >= item.retryIntervalMs;
  });
  
  if (notificationsToRetry.length === 0) {
    return;
  }
  
  logger.info(`Retrying ${notificationsToRetry.length} failed notifications`);
  
  // Process each notification ready for retry
  for (const item of notificationsToRetry) {
    try {
      // Update attempt tracking
      item.attempts++;
      item.lastAttempt = new Date();
      
      // Calculate next retry interval (with exponential backoff)
      const nextRetryIndex = Math.min(item.attempts, RETRY_INTERVALS.length - 1);
      item.retryIntervalMs = RETRY_INTERVALS[nextRetryIndex];
      
      logger.info(`Retry attempt ${item.attempts}/${item.maxAttempts} for notification`, {
        type: item.message.type,
        severity: item.message.severity,
        title: item.message.title
      });
      
      // Attempt to send the notification
      await sendNotification(item.message, true);
      
      // If successful, remove from the queue
      const index = failedNotificationsQueue.indexOf(item);
      if (index !== -1) {
        failedNotificationsQueue.splice(index, 1);
      }
      
      logger.info('Successfully resent notification on retry');
    } catch (error) {
      logger.warn(`Failed to resend notification (attempt ${item.attempts}/${item.maxAttempts})`, {
        error,
        type: item.message.type,
        severity: item.message.severity
      });
      
      // If we've reached max attempts, log and remove from queue
      if (item.attempts >= item.maxAttempts) {
        logger.error(`Giving up on notification after ${item.maxAttempts} failed attempts`, {
          title: item.message.title,
          type: item.message.type,
          severity: item.message.severity
        });
        
        const index = failedNotificationsQueue.indexOf(item);
        if (index !== -1) {
          failedNotificationsQueue.splice(index, 1);
        }
      }
    }
  }
}

export function initializeNotificationSystem(): () => void {
  logger.info('Initializing notification system');
  
  // Analyze environment to determine available channels
  const availableChannels = [];
  
  if (process.env.SMTP_HOST) {
    availableChannels.push('email');
    logger.info('Email notifications enabled', { 
      host: process.env.SMTP_HOST,
      from: process.env.SMTP_FROM
    });
  }
  
  if (process.env.SLACK_WEBHOOK_URL) {
    availableChannels.push('slack');
    logger.info('Slack notifications enabled', { 
      channel: process.env.SLACK_CHANNEL
    });
  }
  
  if (process.env.WEBHOOK_URL) {
    availableChannels.push('webhook');
    logger.info('Webhook notifications enabled', {
      url: process.env.WEBHOOK_URL
    });
  }
  
  if (process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN && process.env.TWILIO_PHONE_NUMBER) {
    availableChannels.push('sms');
    logger.info('SMS notifications enabled');
  }
  
  // Always have log notifications
  availableChannels.push('log');
  
  logger.info('Notification system ready', {
    channels: availableChannels.join(', '),
    batchingSeconds: currentSettings.batchingSeconds,
    throttlingEnabled: currentSettings.throttling.enabled
  });
  
  // Set up retry mechanism for failed notifications
  const retryInterval = setInterval(async () => {
    try {
      await processFailedNotificationsQueue();
    } catch (error) {
      logger.error('Error processing failed notifications queue', { error });
    }
  }, 60 * 1000); // Check every minute
  
  // Send a startup notification
  sendSystemStatusNotification('up', 'ModForge system has started successfully', {
    startupTime: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development'
  }).catch(error => {
    logger.error('Failed to send startup notification', { error });
    
    // Even if this fails, it will be added to the retry queue by the sendNotification function
  });
  
  // Return cleanup function
  return () => {
    logger.info('Shutting down notification system');
    
    // Clear retry interval
    clearInterval(retryInterval);
    
    // Process any remaining batched notifications
    Object.keys(notificationState.batchedNotifications).forEach(key => {
      processBatch(key).catch(error => {
        logger.error('Failed to process notification batch during shutdown', { 
          key, 
          error 
        });
      });
    });
    
    // Process any critical notifications in the retry queue one last time
    const criticalNotifications = failedNotificationsQueue.filter(
      item => item.message.severity === NotificationSeverity.CRITICAL
    );
    
    if (criticalNotifications.length > 0) {
      logger.info(`Attempting to send ${criticalNotifications.length} critical notifications before shutdown`);
      
      for (const item of criticalNotifications) {
        try {
          // Try to send synchronously as we're shutting down
          // We're intentionally not using await here as we don't want to block shutdown
          sendNotification(item.message, true)
            .then(() => logger.info('Successfully sent critical notification during shutdown'))
            .catch(e => logger.error('Failed to send critical notification during shutdown', { error: e }));
        } catch (error) {
          logger.error('Error attempting to send critical notification during shutdown', { error });
        }
      }
    }
  };
}