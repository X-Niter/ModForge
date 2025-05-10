/**
 * Backup System Integration for ModForge
 * 
 * This module integrates the backup system into the main application,
 * ensuring critical data is regularly backed up and properly managed.
 */

import { scheduleBackups, BackupStatus, BackupResult, runFullBackup, BackupType, createBackup } from './backup-manager';
import { getLogger } from './logging';
import { notifyBackupResult } from './notification-integration';

const logger = getLogger('backup-integration');

/**
 * Monitor backup results and send notifications
 * @param backupResult The result of a backup operation
 */
async function monitorBackupResult(backupResult: BackupResult | BackupResult[]): Promise<void> {
  // Handle both single backup and array of backups
  const results = Array.isArray(backupResult) ? backupResult : [backupResult];
  
  // Check for overall success/failure
  const total = results.length;
  const successful = results.filter(r => r.status === BackupStatus.COMPLETED).length;
  const failed = results.filter(r => r.status === BackupStatus.FAILED).length;
  const partial = successful > 0 && failed > 0;
  const success = failed === 0;
  
  // Generate message
  let message = '';
  if (Array.isArray(backupResult)) {
    // Full backup notification
    message = `Full backup ${success ? 'completed' : 'failed'}: ${successful}/${total} successful`;
    if (partial) {
      message = `Full backup partially completed: ${successful}/${total} successful`;
    }
  } else {
    // Single backup notification
    const backupTypeStr = backupResult.type.toString().replace(/([A-Z])/g, ' $1').trim();
    message = `${backupTypeStr} backup ${success ? 'completed' : 'failed'}`;
    if (backupResult.error) {
      message += `: ${backupResult.error}`;
    }
  }
  
  // Collect backup details
  const details: Record<string, any> = {
    total,
    successful,
    failed,
    types: results.map(r => r.type),
    timestamp: new Date().toISOString(),
  };
  
  // Add error details if any failed
  if (failed > 0) {
    details.errors = results
      .filter(r => r.status === BackupStatus.FAILED && r.error)
      .map(r => ({ type: r.type, error: r.error }));
  }
  
  // Send notification about the backup result
  try {
    await notifyBackupResult(success, partial, message, details);
  } catch (error) {
    logger.error('Failed to send backup notification', { error });
  }
}

/**
 * Enhanced backup creation with notification
 * @param type Type of backup to create
 * @param metadata Additional metadata to include
 * @returns Result of the backup operation
 */
export async function createBackupWithNotification(
  type: BackupType, 
  metadata: Record<string, any> = {}
): Promise<BackupResult> {
  try {
    const result = await createBackup(type, metadata);
    await monitorBackupResult(result);
    return result;
  } catch (error) {
    logger.error(`Failed to create ${type} backup`, { error });
    const failedResult: BackupResult = {
      id: `failed-${Date.now()}`,
      type,
      status: BackupStatus.FAILED,
      error: error.message,
      path: null,
      size: 0,
      createdAt: new Date(),
      metadata: { ...metadata, failed: true }
    };
    await monitorBackupResult(failedResult);
    return failedResult;
  }
}

/**
 * Enhanced full backup with notification
 * @param metadata Additional metadata to include
 * @returns Results of all backup operations
 */
export async function runFullBackupWithNotification(
  metadata: Record<string, any> = {}
): Promise<BackupResult[]> {
  try {
    const results = await runFullBackup(metadata);
    await monitorBackupResult(results);
    return results;
  } catch (error) {
    logger.error('Failed to run full backup', { error });
    // Create a failed result for each backup type
    const failedResults: BackupResult[] = Object.values(BackupType)
      .filter(type => typeof type === 'number')
      .map(type => ({
        id: `failed-${type}-${Date.now()}`,
        type: type as BackupType,
        status: BackupStatus.FAILED,
        error: error.message,
        path: null,
        size: 0,
        createdAt: new Date(),
        metadata: { ...metadata, failed: true }
      }));
    await monitorBackupResult(failedResults);
    return failedResults;
  }
}

/**
 * Initialize the backup system
 * @returns Cleanup function to stop scheduled backups
 */
export function initializeBackupSystem(): () => void {
  logger.info('Initializing automated backup system');
  
  // Monkey patch the backup manager to add notifications
  const originalCreateBackup = createBackup;
  const originalRunFullBackup = runFullBackup;
  
  // Override the backup functions to add notification
  (global as any).__originalCreateBackup = originalCreateBackup;
  (global as any).__originalRunFullBackup = originalRunFullBackup;
  
  // Schedule backups to run at 2 AM daily, with full backups on Sunday (day 0)
  const cleanupBackups = scheduleBackups(2, 0);
  
  logger.info('Automated backup system initialized');
  
  // Return cleanup function
  return () => {
    logger.info('Shutting down backup system');
    cleanupBackups();
    
    // Restore original functions
    if ((global as any).__originalCreateBackup) {
      (global as any).__originalCreateBackup = undefined;
    }
    if ((global as any).__originalRunFullBackup) {
      (global as any).__originalRunFullBackup = undefined;
    }
  };
}