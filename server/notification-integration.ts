/**
 * Notification System Integration for ModForge
 * 
 * This module integrates the notification system into the main application,
 * connecting it with error tracking, health checks, and backups.
 */

import { initializeNotificationSystem, sendTrackedErrorNotification, sendSystemStatusNotification, sendBackupStatusNotification } from './notification-manager';
import { getLogger } from './logging';
import { ErrorSeverity, ErrorCategory } from './error-types';

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
  
  try {
    // Send the notification
    await sendSystemStatusNotification(status, notificationMessage, {
      previousState,
      currentState,
      ...details
    });
    logger.debug('Health state change notification sent', {
      status,
      previousState,
      currentState
    });
  } catch (error) {
    logger.error('Failed to send health state change notification', {
      error,
      status,
      previousState,
      currentState
    });
    // Don't throw the error as we don't want health check notifications
    // to break the main application flow
  }
}

/**
 * Send a notification about a critical error
 */
export async function notifyCriticalError(
  error: Error,
  context: Record<string, any> = {}
): Promise<void> {
  try {
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
    logger.debug('Critical error notification sent', {
      message: error.message
    });
  } catch (notificationError) {
    // Use console.error as a last resort if the logger itself might be broken
    console.error('Failed to send critical error notification:', notificationError);
    logger.error('Failed to send critical error notification', {
      originalError: error.message,
      notificationError
    });
    // We don't throw here to prevent cascading failures
  }
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
  
  try {
    // Send the notification
    await sendBackupStatusNotification(status, message, details);
    logger.debug('Backup result notification sent', {
      status,
      success,
      partial
    });
  } catch (error) {
    logger.error('Failed to send backup result notification', {
      error,
      status,
      success,
      partial,
      message
    });
    // Don't throw to avoid breaking backup process if notification fails
  }
}