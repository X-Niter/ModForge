import { Request, Response, NextFunction } from 'express';
import winston from 'winston';
import { z } from 'zod';

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

// Error categories for better organization and alerting
export enum ErrorCategory {
  API = 'api_error',
  DATABASE = 'database_error',
  AUTHENTICATION = 'auth_error',
  GITHUB = 'github_error',
  COMPILATION = 'compilation_error',
  AI_SERVICE = 'ai_service_error',
  CONTINUOUS_DEVELOPMENT = 'continuous_dev_error',
  VALIDATION = 'validation_error',
  SYSTEM = 'system_error',
  UNKNOWN = 'unknown_error'
}

// Severity levels for prioritizing errors
export enum ErrorSeverity {
  CRITICAL = 'critical',   // System-wide failure, requires immediate attention
  HIGH = 'high',           // Affects multiple users or core functionality
  MEDIUM = 'medium',       // Affects individual user experience
  LOW = 'low',             // Minor issues, non-blocking
  INFO = 'info'            // Informational errors
}

// Structured error type for consistent error handling
export interface StructuredError {
  message: string;
  category: ErrorCategory;
  severity: ErrorSeverity;
  retryable: boolean;
  context?: Record<string, any>;
  originalError?: Error;
  timestamp: string;
}

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

/**
 * Get a summary of recent errors
 * @returns Summary of errors by category and severity
 */
export function getErrorSummary() {
  // In a real implementation, this would query the error logs
  // For now, just return placeholder information
  return {
    totalErrors: 0,
    byCategory: {},
    bySeverity: {},
    recentErrors: []
  };
}