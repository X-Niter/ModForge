/**
 * Notification System Integration for ModForge
 * 
 * This module integrates the notification system into the main application,
 * connecting it with error tracking, health checks, and backups.
 */

import { initializeNotificationSystem, sendTrackedErrorNotification, sendSystemStatusNotification, sendBackupStatusNotification } from './notification-manager';
import { getLogger } from './logging';
import { ErrorSeverity, ErrorCategory } from './error-tracker';

const logger = getLogger('notification-integration');

/**
 * Initialize and connect the notification system to other modules
 * @returns Cleanup function to stop notification system
 */
export function initializeNotifications(): () => void {
  logger.info('Initializing notification integrations');
  
  // Initialize the notification system
  const cleanupNotifications = initializeNotificationSystem();
  
  // Return cleanup function
  return () => {
    logger.info('Shutting down notification integrations');
    cleanupNotifications();
  };
}

/**
 * Send a notification about system health state change
 */
export async function notifyHealthStateChange(
  previousState: string,
  currentState: string,
  message: string,
  details: Record<string, any> = {}
): Promise<void> {
  // Determine the system status
  let status: 'up' | 'down' | 'degraded';
  
  switch (currentState) {
    case 'healthy':
      status = 'up';
      break;
    case 'degraded':
      status = 'degraded';
      break;
    case 'unhealthy':
    case 'error':
      status = 'down';
      break;
    default:
      status = 'degraded';
  }
  
  // Build a better message
  let notificationMessage = message;
  if (previousState && previousState !== currentState) {
    notificationMessage = `System health changed from ${previousState.toUpperCase()} to ${currentState.toUpperCase()}: ${message}`;
  }
  
  // Send the notification
  await sendSystemStatusNotification(status, notificationMessage, {
    previousState,
    currentState,
    ...details
  });
}

/**
 * Send a notification about a critical error
 */
export async function notifyCriticalError(
  error: Error,
  context: Record<string, any> = {}
): Promise<void> {
  await sendTrackedErrorNotification(
    'untracked-' + Date.now(),
    error.message,
    ErrorSeverity.CRITICAL,
    ErrorCategory.UNKNOWN,
    1,
    {
      stack: error.stack,
      ...context
    }
  );
}

/**
 * Send a notification about a backup result
 */
export async function notifyBackupResult(
  success: boolean,
  partial: boolean,
  message: string,
  details: Record<string, any> = {}
): Promise<void> {
  // Determine the status
  let status: 'success' | 'partial' | 'failed';
  
  if (success && !partial) {
    status = 'success';
  } else if (success && partial) {
    status = 'partial';
  } else {
    status = 'failed';
  }
  
  // Send the notification
  await sendBackupStatusNotification(status, message, details);
}