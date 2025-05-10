/**
 * Error Recovery Integration for ModForge
 * 
 * This module integrates the error recovery system into the main application,
 * ensuring errors are automatically fixed and resources are maintained.
 */

import { scheduleErrorRecovery } from './error-recovery';
import { stopAllJobs } from './scheduled-jobs';
import { getLogger } from './logging';

const logger = getLogger('error-recovery-integration');

/**
 * Initialize error recovery system
 * @returns Cleanup function to stop scheduled recovery
 */
export function initializeErrorRecovery(): () => void {
  logger.info('Initializing error recovery system');
  
  // Schedule error recovery every 30 minutes
  const cleanupErrorRecovery = scheduleErrorRecovery(30 * 60 * 1000);
  
  // Return cleanup function
  return () => {
    logger.info('Shutting down error recovery system');
    cleanupErrorRecovery();
  };
}

/**
 * Cleanup all scheduled jobs before shutdown
 */
export function cleanupScheduledJobs(): void {
  logger.info('Cleaning up all scheduled jobs');
  stopAllJobs();
}