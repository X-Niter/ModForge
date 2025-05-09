import { Request, Response, NextFunction } from 'express';
import winston from 'winston';
import { z } from 'zod';
import { 
  ErrorCategory,
  ErrorSeverity,
  StructuredError,
  categoryFromString,
  severityFromString
} from './error-types';

// Configure structured logger for error tracking
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  defaultMeta: { service: 'modforge-error-handler' },
  transports: [
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.simple()
      )
    }),
    // Add file transport in production
    ...(process.env.NODE_ENV === 'production' 
      ? [new winston.transports.File({ filename: 'logs/errors.log', level: 'error' })]
      : [])
  ]
});

// Using centralized error type definitions from error-types.ts

/**
 * Records a structured error in the error tracking system
 * 
 * @param error The error to record
 * @param category Error category for classification
 * @param severity Error severity level
 * @param retryable Whether this error can be automatically retried
 * @param context Additional context information
 * @returns The original error to allow for chaining
 */
export function recordError(
  error: Error | string,
  category: ErrorCategory = ErrorCategory.UNKNOWN,
  severity: ErrorSeverity = ErrorSeverity.MEDIUM,
  retryable: boolean = false,
  context: Record<string, any> = {}
): Error {
  // Convert string errors to Error objects for consistency
  const actualError = typeof error === 'string' ? new Error(error) : error;
  
  // Create structured error
  const structuredError: StructuredError = {
    message: actualError.message,
    category,
    severity,
    retryable,
    context,
    originalError: actualError,
    timestamp: new Date().toISOString()
  };
  
  // Log to appropriate level based on severity
  switch (severity) {
    case ErrorSeverity.CRITICAL:
    case ErrorSeverity.HIGH:
      logger.error(structuredError);
      break;
    case ErrorSeverity.MEDIUM:
      logger.warn(structuredError);
      break;
    case ErrorSeverity.LOW:
    case ErrorSeverity.INFO:
      logger.info(structuredError);
      break;
  }
  
  // Track error statistics
  updateErrorStats(structuredError);
  
  // Return original error for chaining
  return actualError;
}

/**
 * Global Express error handler middleware
 * Captures and logs all errors that occur in the request pipeline
 */
export function errorHandlerMiddleware(err: any, req: Request, res: Response, next: NextFunction) {
  // Handle Zod validation errors
  if (err instanceof z.ZodError) {
    const validationError = recordError(
      new Error('Validation error'),
      ErrorCategory.VALIDATION,
      ErrorSeverity.LOW,
      false,
      { zodErrors: err.errors, path: req.path }
    );
    
    return res.status(400).json({
      success: false,
      message: 'Validation error',
      errors: err.errors.map(e => ({
        path: e.path.join('.'),
        message: e.message
      }))
    });
  }
  
  // Handle JWT errors
  if (err.name === 'JsonWebTokenError' || err.name === 'TokenExpiredError') {
    recordError(
      err,
      ErrorCategory.AUTHENTICATION,
      ErrorSeverity.LOW,
      false,
      { path: req.path }
    );
    
    return res.status(401).json({
      success: false,
      message: 'Authentication error',
      detail: err.message
    });
  }
  
  // Create contextual metadata
  const errorContext = {
    path: req.path,
    method: req.method,
    query: req.query,
    ip: req.ip,
    userId: req.user?.id,
    userAgent: req.headers['user-agent']
  };
  
  // Record the error with the error tracker
  recordError(
    err,
    ErrorCategory.API,
    ErrorSeverity.MEDIUM,
    false,
    errorContext
  );
  
  // Sanitize error message for production
  const message = process.env.NODE_ENV === 'production'
    ? 'An unexpected error occurred'
    : err.message || 'Unknown error';
  
  // Send error response
  res.status(err.statusCode || 500).json({
    success: false,
    message,
    ...(process.env.NODE_ENV !== 'production' && { stack: err.stack })
  });
}

// In-memory error tracking for recent errors
// This would be replaced with a database in a full implementation
interface ErrorRecord {
  message: string;
  category: ErrorCategory;
  severity: ErrorSeverity;
  timestamp: string;
  count: number;
}

// Store recent errors in memory
const recentErrors: ErrorRecord[] = [];
const errorCountsByCategory: Record<string, number> = {};
const errorCountsBySeverity: Record<string, number> = {};
let totalErrorCount = 0;

// Update error tracking stats when recording an error
function updateErrorStats(error: StructuredError): void {
  totalErrorCount++;
  
  // Update category counts
  const category = error.category;
  errorCountsByCategory[category] = (errorCountsByCategory[category] || 0) + 1;
  
  // Update severity counts
  const severity = error.severity;
  errorCountsBySeverity[severity] = (errorCountsBySeverity[severity] || 0) + 1;
  
  // Check if this is a duplicate of a recent error
  const existingErrorIndex = recentErrors.findIndex(e => 
    e.message === error.message && e.category === error.category
  );
  
  if (existingErrorIndex >= 0) {
    // Update existing error
    recentErrors[existingErrorIndex].count++;
    recentErrors[existingErrorIndex].timestamp = error.timestamp;
  } else {
    // Add new error
    recentErrors.unshift({
      message: error.message,
      category: error.category,
      severity: error.severity,
      timestamp: error.timestamp,
      count: 1
    });
    
    // Keep only the most recent errors
    if (recentErrors.length > 100) {
      recentErrors.pop();
    }
  }
}

/**
 * Get a summary of recent errors
 * @returns Summary of errors by category and severity
 */
export function getErrorSummary() {
  return {
    totalErrors: totalErrorCount,
    byCategory: errorCountsByCategory,
    bySeverity: errorCountsBySeverity,
    recentErrors: recentErrors.slice(0, 10) // Return only the 10 most recent errors
  };
}