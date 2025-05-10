/**
 * Test Script for Error Tracking System
 * 
 * This module provides a simple way to test the error tracking system
 * by generating various types of errors and checking their tracking.
 */

import { 
  trackError, 
  ErrorCategory, 
  ErrorSeverity,
  getTrackedErrors,
  getErrorStatistics,
  resolveError,
  purgeResolvedErrors
} from './error-tracker';
import { getLogger } from './logging';

const logger = getLogger('error-tracking-test');

/**
 * Generate a simulated database connection error
 */
function simulateDatabaseError(): Error {
  return new Error('ECONNREFUSED: Database connection refused at 127.0.0.1:5432');
}

/**
 * Generate a simulated file system error
 */
function simulateFileSystemError(): Error {
  return new Error('ENOENT: no such file or directory, open "/tmp/modforge/mod-123/build/outputs/mod.jar"');
}

/**
 * Generate a simulated out of memory error
 */
function simulateOutOfMemoryError(): Error {
  const error = new Error('JavaScript heap out of memory');
  error.name = 'RangeError';
  return error;
}

/**
 * Generate a simulated validation error
 */
function simulateValidationError(): Error {
  return new Error('Invalid input: "minecraftVersion" must be a valid Minecraft version');
}

/**
 * Generate a simulated GitHub API error
 */
function simulateGitHubError(): Error {
  return new Error('GitHub API rate limit exceeded. Try again in 60 minutes.');
}

/**
 * Run a series of tests for the error tracking system
 */
export async function testErrorTracking(): Promise<void> {
  logger.info('Running error tracking system tests');
  
  // Track various types of errors
  const dbError = trackError(
    simulateDatabaseError(),
    { source: 'test', connectionAttempt: 3 },
    { category: ErrorCategory.DATABASE }
  );
  
  const fsError = trackError(
    simulateFileSystemError(),
    { source: 'test', modId: 123 },
    { category: ErrorCategory.FILESYSTEM }
  );
  
  const memoryError = trackError(
    simulateOutOfMemoryError(),
    { source: 'test', heapUsed: '2.1GB', heapLimit: '2GB' },
    { category: ErrorCategory.MEMORY, severity: ErrorSeverity.CRITICAL }
  );
  
  const validationError = trackError(
    simulateValidationError(),
    { source: 'test', input: { minecraftVersion: 'invalid' } },
    { category: ErrorCategory.VALIDATION }
  );
  
  const gitHubError = trackError(
    simulateGitHubError(),
    { source: 'test', repo: 'user/mod-repo' },
    { category: ErrorCategory.GITHUB }
  );
  
  // Track the same error twice to test grouping
  trackError(
    simulateDatabaseError(),
    { source: 'test', connectionAttempt: 4 },
    { category: ErrorCategory.DATABASE }
  );
  
  logger.info('Tracked various test errors:');
  logger.info(`- Database error: ${dbError.id}`);
  logger.info(`- File system error: ${fsError.id}`);
  logger.info(`- Memory error: ${memoryError.id}`);
  logger.info(`- Validation error: ${validationError.id}`);
  logger.info(`- GitHub error: ${gitHubError.id}`);
  
  // Get error statistics
  const stats = getErrorStatistics();
  logger.info(`Error tracking statistics: ${stats.total} total errors, ${stats.unresolved} unresolved`);
  
  // Resolve one error
  const memoryErrorResolved = resolveError(memoryError.id);
  if (memoryErrorResolved) {
    logger.info(`Resolved memory error: ${memoryError.id}`);
  }
  
  // Get all critical errors
  const criticalErrors = getTrackedErrors({ severity: ErrorSeverity.CRITICAL });
  logger.info(`Found ${criticalErrors.length} critical errors`);
  
  // Purge resolved errors older than 1 minute (which is all of them in this case)
  const oneMinuteAgo = new Date(Date.now() - 60 * 1000);
  const purgedCount = purgeResolvedErrors(oneMinuteAgo);
  logger.info(`Purged ${purgedCount} resolved errors`);
  
  logger.info('Error tracking system test completed successfully');
}