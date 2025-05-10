/**
 * Error Tracking System for ModForge
 * 
 * This module provides comprehensive error tracking capabilities including:
 * - Error categorization and severity assessment
 * - Aggregation of similar errors to avoid noise
 * - Rate limiting for frequent errors
 * - Analysis of error patterns
 * - Integration with the logging system
 */

import { getLogger } from "./logging";
import { performance } from "perf_hooks";
import crypto from "crypto";
import { sendTrackedErrorNotification } from "./notification-manager";

// Set up logger
const logger = getLogger("error-tracker");

import { 
  ErrorSeverity,
  ErrorCategory,
  categoryFromString,
  severityFromString
} from './error-types';

// Error with additional metadata
export interface TrackedError {
  id: string;               // Unique identifier for this error
  message: string;          // Error message
  stack?: string;           // Stack trace
  category: ErrorCategory;  // Error category
  severity: ErrorSeverity;  // Error severity
  context: Record<string, any>; // Additional context
  count: number;            // Number of times this error has occurred
  firstSeen: Date;          // When this error was first seen
  lastSeen: Date;           // When this error was last seen
  isResolved: boolean;      // Whether this error has been marked as resolved
  resolvedAt?: Date;        // When this error was resolved
  fingerprint: string;      // Error fingerprint for grouping similar errors
}

// In-memory storage for tracked errors
// In a production system, these would be stored in a database
const errorStore: Map<string, TrackedError> = new Map();

// Error fingerprinting settings
interface FingerprintOptions {
  considerPath: boolean;
  considerMessage: boolean;
  considerLineNumber: boolean;
  ignoreNumbers: boolean;
  ignoreTimestamps: boolean;
}

const defaultFingerprintOptions: FingerprintOptions = {
  considerPath: true,
  considerMessage: true,
  considerLineNumber: true,
  ignoreNumbers: false,
  ignoreTimestamps: true
};

/**
 * Generate a fingerprint for an error to group similar errors
 */
function generateErrorFingerprint(
  error: Error | string,
  category: ErrorCategory,
  options: Partial<FingerprintOptions> = {}
): string {
  const opts = { ...defaultFingerprintOptions, ...options };
  const errorMessage = typeof error === 'string' ? error : error.message;
  const errorStack = typeof error === 'string' ? '' : error.stack || '';
  
  // Extract the most relevant parts of the stack trace
  let relevantStack = '';
  if (errorStack) {
    // Get the first 3 lines of the stack trace
    const stackLines = errorStack.split('\n').slice(0, 3);
    
    // Process each line based on options
    relevantStack = stackLines
      .map(line => {
        let processed = line.trim();
        
        // Remove specific file paths if needed
        if (!opts.considerPath) {
          processed = processed.replace(/at\s+.+\s+\(.+\)/, 'at [path]');
        }
        
        // Remove line numbers if not considering them
        if (!opts.considerLineNumber) {
          processed = processed.replace(/:\d+:\d+/g, ':[line]:[column]');
        }
        
        // Remove all numbers if ignoring them
        if (opts.ignoreNumbers) {
          processed = processed.replace(/\d+/g, '[num]');
        }
        
        // Remove timestamps
        if (opts.ignoreTimestamps) {
          processed = processed.replace(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/g, '[timestamp]');
          processed = processed.replace(/\d{2}:\d{2}:\d{2}/g, '[time]');
        }
        
        return processed;
      })
      .join(' | ');
  }
  
  // Process message based on options
  let processedMessage = opts.considerMessage ? errorMessage : '[message ignored]';
  
  // Apply the same processing to the message as we did to the stack
  if (opts.ignoreNumbers) {
    processedMessage = processedMessage.replace(/\d+/g, '[num]');
  }
  
  if (opts.ignoreTimestamps) {
    processedMessage = processedMessage.replace(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/g, '[timestamp]');
    processedMessage = processedMessage.replace(/\d{2}:\d{2}:\d{2}/g, '[time]');
  }
  
  // Combine all parts to create a fingerprint string
  const fingerprintSource = `${category}:${processedMessage}:${relevantStack}`;
  
  // Hash the fingerprint source to get a consistent string
  return crypto.createHash('md5').update(fingerprintSource).digest('hex');
}

/**
 * Determine the severity of an error based on its characteristics
 */
function determineErrorSeverity(
  error: Error | string,
  category: ErrorCategory,
  context: Record<string, any> = {}
): ErrorSeverity {
  const errorMessage = typeof error === 'string' ? error : error.message;
  const errorStack = typeof error === 'string' ? '' : error.stack || '';
  
  // Check for known critical patterns
  if (
    errorMessage.includes('FATAL') ||
    errorMessage.includes('CRITICAL') ||
    errorMessage.includes('OUT OF MEMORY') ||
    errorMessage.includes('ENOSPC') || // No space left on device
    errorMessage.includes('EMFILE') || // Too many open files
    (category === ErrorCategory.DATABASE && errorMessage.includes('ECONNREFUSED'))
  ) {
    return ErrorSeverity.CRITICAL;
  }
  
  // Check for high severity patterns
  if (
    errorMessage.includes('EACCES') || // Permission denied
    errorMessage.includes('EPERM') ||  // Operation not permitted
    (category === ErrorCategory.DATABASE && 
     (errorMessage.includes('Connection') || errorMessage.includes('constraint'))) ||
    (category === ErrorCategory.AUTHENTICATION && 
     (errorMessage.includes('JWT') || errorMessage.includes('Token'))) ||
    (category === ErrorCategory.GITHUB && errorMessage.includes('rate limit'))
  ) {
    return ErrorSeverity.HIGH;
  }
  
  // Check for medium severity patterns
  if (
    errorMessage.includes('WARN') ||
    errorMessage.includes('Warning') ||
    errorMessage.includes('timeout') ||
    errorMessage.includes('deprecated') ||
    (context.statusCode && context.statusCode >= 400 && context.statusCode < 500) ||
    (category === ErrorCategory.VALIDATION)
  ) {
    return ErrorSeverity.MEDIUM;
  }
  
  // Default to low severity
  return ErrorSeverity.LOW;
}

/**
 * Determine the most likely category for an error
 */
function determineErrorCategory(error: Error | string, context: Record<string, any> = {}): ErrorCategory {
  const errorMessage = typeof error === 'string' ? error.toLowerCase() : error.message.toLowerCase();
  const errorStack = typeof error === 'string' ? '' : (error.stack || '').toLowerCase();
  
  // Check context hints first
  if (context.category && Object.values(ErrorCategory).includes(context.category)) {
    return context.category as ErrorCategory;
  }
  
  // Check error message and stack for category hints
  if (errorMessage.includes('db') || 
      errorMessage.includes('database') || 
      errorMessage.includes('sql') || 
      errorMessage.includes('query') ||
      errorStack.includes('database') ||
      errorStack.includes('/db/') ||
      errorStack.includes('postgres')) {
    return ErrorCategory.DATABASE;
  }
  
  if (errorMessage.includes('auth') || 
      errorMessage.includes('login') || 
      errorMessage.includes('password') || 
      errorMessage.includes('jwt') || 
      errorMessage.includes('token') ||
      errorStack.includes('auth')) {
    return ErrorCategory.AUTHENTICATION;
  }
  
  if (errorMessage.includes('validate') || 
      errorMessage.includes('validation') || 
      errorMessage.includes('schema') || 
      errorMessage.includes('invalid') || 
      errorMessage.includes('required field') ||
      errorStack.includes('validation') ||
      errorStack.includes('validator')) {
    return ErrorCategory.VALIDATION;
  }
  
  if (errorMessage.includes('compile') || 
      errorMessage.includes('compilation') || 
      errorMessage.includes('javac') || 
      errorMessage.includes('gradle') || 
      errorMessage.includes('maven') ||
      errorStack.includes('compiler')) {
    return ErrorCategory.COMPILATION;
  }
  
  if (errorMessage.includes('github') || 
      errorMessage.includes('git') || 
      errorMessage.includes('repo') || 
      errorMessage.includes('pull request') || 
      errorMessage.includes('commit') ||
      errorStack.includes('github')) {
    return ErrorCategory.GITHUB;
  }
  
  if (errorMessage.includes('fs') || 
      errorMessage.includes('file') || 
      errorMessage.includes('directory') || 
      errorMessage.includes('path') || 
      errorMessage.includes('enoent') ||
      errorStack.includes('fs/') ||
      errorStack.includes('file')) {
    return ErrorCategory.SYSTEM; // Changed from FILESYSTEM to SYSTEM
  }
  
  if (errorMessage.includes('network') || 
      errorMessage.includes('connection') || 
      errorMessage.includes('http') || 
      errorMessage.includes('request') || 
      errorMessage.includes('response') ||
      errorStack.includes('http') ||
      errorStack.includes('request')) {
    return ErrorCategory.SYSTEM; // Changed from NETWORK to SYSTEM
  }
  
  if (errorMessage.includes('memory') || 
      errorMessage.includes('allocation') || 
      errorMessage.includes('heap') || 
      errorMessage.includes('out of memory') ||
      errorStack.includes('memory')) {
    return ErrorCategory.SYSTEM; // Changed from MEMORY to SYSTEM
  }
  
  if (errorMessage.includes('api') || 
      context.path?.includes('/api/') || 
      errorStack.includes('api')) {
    return ErrorCategory.API;
  }
  
  // Default to unknown category
  return ErrorCategory.UNKNOWN;
}

/**
 * Track an error in the error tracking system
 */
export function trackError(
  error: Error | string,
  context: Record<string, any> = {},
  options: { 
    category?: ErrorCategory,
    severity?: ErrorSeverity,
    fingerprint?: string 
  } = {}
): TrackedError {
  const start = performance.now();
  
  // Get error details
  const errorMessage = typeof error === 'string' ? error : error.message;
  const errorStack = typeof error === 'string' ? undefined : error.stack;
  
  // Determine category and severity if not provided
  const category = options.category || determineErrorCategory(error, context);
  const severity = options.severity || determineErrorSeverity(error, category, context);
  
  // Generate fingerprint if not provided
  const fingerprint = options.fingerprint || generateErrorFingerprint(error, category);
  
  // Check if we already have this error
  let trackedError = errorStore.get(fingerprint);
  
  if (trackedError) {
    // Update existing error
    trackedError.count += 1;
    trackedError.lastSeen = new Date();
    trackedError.context = { ...trackedError.context, ...context };
    
    // Update severity if the new occurrence is more severe
    const severityOrder = Object.values(ErrorSeverity);
    if (severityOrder.indexOf(severity) > severityOrder.indexOf(trackedError.severity)) {
      trackedError.severity = severity;
    }
    
    // If marked as resolved, mark it as unresolved again
    if (trackedError.isResolved) {
      trackedError.isResolved = false;
      trackedError.resolvedAt = undefined;
      logger.warn(`Error ${trackedError.id} marked as unresolved again after reoccurrence`, {
        errorId: trackedError.id,
        fingerprint,
        category,
        severity,
        reappeared: true
      });
    }
  } else {
    // Create new tracked error
    const id = crypto.randomBytes(8).toString('hex');
    trackedError = {
      id,
      message: errorMessage,
      stack: errorStack,
      category,
      severity,
      context,
      count: 1,
      firstSeen: new Date(),
      lastSeen: new Date(),
      isResolved: false,
      fingerprint
    };
    
    // Store the error
    errorStore.set(fingerprint, trackedError);
  }
  
  // Log based on severity
  const perfDuration = performance.now() - start;
  
  switch (severity) {
    case ErrorSeverity.CRITICAL:
      logger.critical(`[ERROR-TRACKER] CRITICAL error tracked: ${errorMessage}`, {
        errorId: trackedError.id,
        fingerprint,
        category,
        count: trackedError.count,
        tracking_ms: perfDuration.toFixed(2)
      });
      
      // Send notification for critical errors
      try {
        sendTrackedErrorNotification(
          trackedError.id,
          errorMessage,
          ErrorSeverity.CRITICAL,
          category,
          trackedError.count,
          {
            stack: errorStack,
            fingerprint,
            ...context
          }
        ).catch(notificationError => {
          logger.error("Failed to send critical error notification", { 
            error: notificationError,
            originalErrorId: trackedError.id
          });
        });
      } catch (notificationError) {
        logger.error("Failed to send critical error notification", { 
          error: notificationError,
          originalErrorId: trackedError.id
        });
      }
      
      break;
      
    case ErrorSeverity.HIGH:
      logger.error(`[ERROR-TRACKER] HIGH severity error tracked: ${errorMessage}`, {
        errorId: trackedError.id,
        fingerprint,
        category,
        count: trackedError.count,
        tracking_ms: perfDuration.toFixed(2)
      });
      
      // Send notification for high severity errors
      try {
        sendTrackedErrorNotification(
          trackedError.id,
          errorMessage,
          ErrorSeverity.HIGH,
          category,
          trackedError.count,
          {
            stack: errorStack,
            fingerprint,
            ...context
          }
        ).catch(notificationError => {
          logger.error("Failed to send high severity error notification", { 
            error: notificationError,
            originalErrorId: trackedError.id
          });
        });
      } catch (notificationError) {
        logger.error("Failed to send high severity error notification", { 
          error: notificationError,
          originalErrorId: trackedError.id
        });
      }
      
      break;
      
    case ErrorSeverity.MEDIUM:
      logger.warn(`[ERROR-TRACKER] MEDIUM severity error tracked: ${errorMessage}`, {
        errorId: trackedError.id,
        fingerprint,
        category,
        count: trackedError.count,
        tracking_ms: perfDuration.toFixed(2)
      });
      break;
      
    case ErrorSeverity.LOW:
      logger.info(`[ERROR-TRACKER] LOW severity error tracked: ${errorMessage}`, {
        errorId: trackedError.id,
        fingerprint,
        category,
        count: trackedError.count,
        tracking_ms: perfDuration.toFixed(2)
      });
      break;
  }
  
  return trackedError;
}

/**
 * Mark an error as resolved
 */
export function resolveError(errorId: string): boolean {
  // Find the error by ID
  let found = false;
  
  errorStore.forEach((error, fingerprint) => {
    if (error.id === errorId) {
      error.isResolved = true;
      error.resolvedAt = new Date();
      
      logger.info(`Error ${errorId} marked as resolved`, {
        errorId,
        fingerprint,
        category: error.category,
        severity: error.severity,
        occurences: error.count
      });
      
      found = true;
    }
  });
  
  if (!found) {
    logger.warn(`Attempted to resolve non-existent error ${errorId}`);
  }
  
  return found;
}

/**
 * Get all tracked errors, optionally filtered by category, severity, or resolution status
 */
export function getTrackedErrors(filters: {
  category?: ErrorCategory,
  severity?: ErrorSeverity,
  resolved?: boolean,
  minCount?: number,
  since?: Date
} = {}): TrackedError[] {
  const errors = Array.from(errorStore.values());
  
  // Apply filters
  return errors.filter(error => {
    if (filters.category && error.category !== filters.category) {
      return false;
    }
    
    if (filters.severity && error.severity !== filters.severity) {
      return false;
    }
    
    if (filters.resolved !== undefined && error.isResolved !== filters.resolved) {
      return false;
    }
    
    if (filters.minCount && error.count < filters.minCount) {
      return false;
    }
    
    if (filters.since && error.lastSeen < filters.since) {
      return false;
    }
    
    return true;
  });
}

/**
 * Get error statistics grouped by category
 */
export function getErrorStatistics(): {
  total: number;
  resolved: number;
  unresolved: number;
  bySeverity: Record<ErrorSeverity, number>;
  byCategory: Record<ErrorCategory, number>;
  topErrors: Array<{ id: string; message: string; count: number; category: string; }>;
} {
  const errors = Array.from(errorStore.values());
  const total = errors.length;
  const resolved = errors.filter(e => e.isResolved).length;
  const unresolved = total - resolved;
  
  // Count by severity
  const bySeverity: Record<ErrorSeverity, number> = {
    [ErrorSeverity.CRITICAL]: 0,
    [ErrorSeverity.HIGH]: 0,
    [ErrorSeverity.MEDIUM]: 0,
    [ErrorSeverity.LOW]: 0,
    [ErrorSeverity.INFO]: 0
  };
  
  // Count by category
  const byCategory: Record<ErrorCategory, number> = {
    [ErrorCategory.API]: 0,
    [ErrorCategory.DATABASE]: 0,
    [ErrorCategory.AUTHENTICATION]: 0,
    [ErrorCategory.VALIDATION]: 0,
    [ErrorCategory.COMPILATION]: 0,
    [ErrorCategory.GITHUB]: 0,
    [ErrorCategory.SYSTEM]: 0, // Replaced FILESYSTEM, NETWORK, MEMORY
    [ErrorCategory.AI_SERVICE]: 0,
    [ErrorCategory.CONTINUOUS_DEVELOPMENT]: 0,
    [ErrorCategory.UNKNOWN]: 0
  };
  
  // Fill in the counts
  for (const error of errors) {
    bySeverity[error.severity]++;
    byCategory[error.category]++;
  }
  
  // Get top errors by count
  const topErrors = errors
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
    .map(e => ({
      id: e.id,
      message: e.message,
      count: e.count,
      category: e.category
    }));
  
  return {
    total,
    resolved,
    unresolved,
    bySeverity,
    byCategory,
    topErrors
  };
}

/**
 * Clear all resolved errors that haven't been seen in a while
 */
export function purgeResolvedErrors(olderThan: Date = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)): number {
  let purgedCount = 0;
  
  // Use an array to store fingerprints to delete to avoid modification during iteration
  const fingerprintsToDelete: string[] = [];
  
  errorStore.forEach((error, fingerprint) => {
    if (error.isResolved && error.lastSeen < olderThan) {
      fingerprintsToDelete.push(fingerprint);
    }
  });
  
  // Now delete the errors
  fingerprintsToDelete.forEach(fingerprint => {
    errorStore.delete(fingerprint);
    purgedCount++;
  });
  
  if (purgedCount > 0) {
    logger.info(`Purged ${purgedCount} resolved errors`);
  }
  
  return purgedCount;
}

/**
 * Purge all errors (resolved or unresolved) that are older than the specified date
 * to prevent uncontrolled memory growth in long-running processes
 */
export function purgeAllOldErrors(olderThan: Date = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)): number {
  let purgedCount = 0;
  const fingerprintsToDelete: string[] = [];
  
  // First identify the errors to delete
  errorStore.forEach((error, fingerprint) => {
    // Delete any error that hasn't been seen in the specified time period
    if (error.lastSeen < olderThan) {
      fingerprintsToDelete.push(fingerprint);
    }
  });
  
  // Then delete them
  fingerprintsToDelete.forEach(fingerprint => {
    errorStore.delete(fingerprint);
    purgedCount++;
  });
  
  if (purgedCount > 0) {
    logger.info(`Purged ${purgedCount} old errors for memory management`);
  }
  
  return purgedCount;
}

/**
 * Limit the total number of errors stored to prevent memory issues
 * This keeps the most recent errors and errors with the highest severity
 */
export function limitErrorStore(maxErrors: number = 1000): number {
  const currentCount = errorStore.size;
  
  // If we're under the limit, no action needed
  if (currentCount <= maxErrors) {
    return 0;
  }
  
  // Calculate how many errors to remove
  const excessCount = currentCount - maxErrors;
  let purgedCount = 0;
  
  // Convert to array for sorting
  const errors = Array.from(errorStore.entries()).map(([fingerprint, error]) => ({
    fingerprint, 
    error,
    // Create a score for sorting (higher = more important to keep)
    score: calculateErrorImportance(error)
  }));
  
  // Sort by importance score (ascending so least important are first)
  errors.sort((a, b) => a.score - b.score);
  
  // Delete the least important errors
  for (let i = 0; i < excessCount; i++) {
    if (i < errors.length) {
      errorStore.delete(errors[i].fingerprint);
      purgedCount++;
    }
  }
  
  if (purgedCount > 0) {
    logger.info(`Purged ${purgedCount} least important errors to stay under memory limit`);
  }
  
  return purgedCount;
}

/**
 * Calculate an importance score for an error to determine which ones to keep
 * Higher score = more important to keep
 */
function calculateErrorImportance(error: TrackedError): number {
  // Start with a base score
  let score = 0;
  
  // Severity is the most important factor (0-3 for LOW to CRITICAL)
  const severityValues = {
    [ErrorSeverity.LOW]: 0,
    [ErrorSeverity.MEDIUM]: 25,
    [ErrorSeverity.HIGH]: 50,
    [ErrorSeverity.CRITICAL]: 100
  };
  score += severityValues[error.severity] || 0;
  
  // Recent errors are more important (1-10 based on recency)
  // Newer errors get higher scores
  const ageInDays = (Date.now() - error.lastSeen.getTime()) / (1000 * 60 * 60 * 24);
  score += Math.max(0, 10 - Math.min(10, ageInDays));
  
  // Frequency is important (max 20 points for very frequent errors)
  score += Math.min(20, error.count / 5);
  
  // Unresolved errors are more important than resolved ones
  if (!error.isResolved) {
    score += 15;
  }
  
  return score;
}

/**
 * Schedule automatic cleanup of errors
 */
export function scheduleErrorStoreCleanup(
  resolvedInterval: number = 24 * 60 * 60 * 1000,  // Daily cleanup of resolved errors
  oldInterval: number = 7 * 24 * 60 * 60 * 1000,   // Weekly cleanup of very old errors
  limitInterval: number = 60 * 60 * 1000          // Hourly check of total error count
): () => void {
  logger.info('Scheduling automatic error store cleanup');
  
  // Schedule cleanup of resolved errors
  const resolvedTimer = setInterval(() => {
    try {
      const olderThan = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000); // 7 days
      const purged = purgeResolvedErrors(olderThan);
      logger.debug(`Scheduled cleanup of resolved errors complete`, { purged });
    } catch (err) {
      logger.error('Error in scheduled resolved errors cleanup', { error: err });
    }
  }, resolvedInterval);
  
  // Schedule cleanup of all old errors
  const oldTimer = setInterval(() => {
    try {
      const olderThan = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000); // 30 days
      const purged = purgeAllOldErrors(olderThan);
      logger.debug(`Scheduled cleanup of old errors complete`, { purged });
    } catch (err) {
      logger.error('Error in scheduled old errors cleanup', { error: err });
    }
  }, oldInterval);
  
  // Schedule check of total error count
  const limitTimer = setInterval(() => {
    try {
      const purged = limitErrorStore(1000); // Limit to 1000 errors
      if (purged > 0) {
        logger.debug(`Error store size limited`, { purged, newSize: errorStore.size });
      }
    } catch (err) {
      logger.error('Error in error store size limiting', { error: err });
    }
  }, limitInterval);
  
  // Return cleanup function
  return () => {
    clearInterval(resolvedTimer);
    clearInterval(oldTimer);
    clearInterval(limitTimer);
    logger.info('Error store cleanup schedules cancelled');
  };
}

/**
 * Create a middleware function for Express to track errors
 */
export function errorTrackerMiddleware() {
  return (err: Error, req: any, res: any, next: any) => {
    // Track the error
    const context = {
      path: req.path,
      method: req.method,
      statusCode: res.statusCode,
      ip: req.ip,
      userId: req.user?.id,
      requestId: req.id
    };
    
    trackError(err, context);
    
    // Continue to the next error handler
    next(err);
  };
}