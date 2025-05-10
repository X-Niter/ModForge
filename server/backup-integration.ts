/**
 * Backup System Integration for ModForge
 * 
 * This module integrates the backup system into the main application,
 * ensuring critical data is regularly backed up and properly managed.
 */

import { scheduleBackups } from './backup-manager';
import { getLogger } from './logging';

const logger = getLogger('backup-integration');

/**
 * Initialize the backup system
 * @returns Cleanup function to stop scheduled backups
 */
export function initializeBackupSystem(): () => void {
  logger.info('Initializing automated backup system');
  
  // Schedule backups to run at 2 AM daily, with full backups on Sunday (day 0)
  const cleanupBackups = scheduleBackups(2, 0);
  
  logger.info('Automated backup system initialized');
  
  // Return cleanup function
  return () => {
    logger.info('Shutting down backup system');
    cleanupBackups();
  };
}