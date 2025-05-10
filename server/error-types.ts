/**
 * Centralized error type definitions for ModForge
 * 
 * This file contains all error-related type definitions to ensure
 * consistency across different components of the application.
 */

// Error categories for better organization and alerting
export enum ErrorCategory {
  API = 'api',                    // API-related errors
  DATABASE = 'database',          // Database errors
  AUTHENTICATION = 'auth',        // Authentication/authorization errors
  GITHUB = 'github',              // GitHub integration errors
  COMPILATION = 'compilation',    // Mod compilation errors
  AI_SERVICE = 'ai_service',      // AI service errors
  CONTINUOUS_DEVELOPMENT = 'continuous', // Continuous development errors
  VALIDATION = 'validation',      // Input validation errors
  SYSTEM = 'system',              // System/infrastructure errors
  UNKNOWN = 'unknown'             // Uncategorized errors
}

// Severity levels for prioritizing errors
export enum ErrorSeverity {
  CRITICAL = 'critical',   // System-wide failure, requires immediate attention
  HIGH = 'high',           // Affects multiple users or core functionality
  MEDIUM = 'medium',       // Affects individual user experience
  LOW = 'low',             // Minor issues, non-blocking
  INFO = 'info'            // Informational errors
}

// Common error context to standardize error reporting
export interface ErrorContext {
  [key: string]: any;
  operation?: string;      // Operation that caused the error
  userId?: number;         // User ID if applicable
  modId?: number;          // Mod ID if applicable
  timestamp?: string;      // When the error occurred
  requestId?: string;      // For tracking errors across microservices
  path?: string;           // Path/route where error occurred
  input?: any;             // Sanitized input that caused the error
}

// Structured error interface for consistent reporting
export interface StructuredError {
  message: string;
  category: ErrorCategory;
  severity: ErrorSeverity;
  retryable: boolean;
  context?: ErrorContext;
  originalError?: Error;
  timestamp: string;
}

// Maps from string values to enum values for compatibility
export const categoryFromString = (category: string): ErrorCategory => {
  if (Object.values(ErrorCategory).includes(category as ErrorCategory)) {
    return category as ErrorCategory;
  }
  
  // Handle legacy category names
  const mappings: Record<string, ErrorCategory> = {
    'api_error': ErrorCategory.API,
    'database_error': ErrorCategory.DATABASE,
    'auth_error': ErrorCategory.AUTHENTICATION,
    'github_error': ErrorCategory.GITHUB,
    'compilation_error': ErrorCategory.COMPILATION,
    'ai_service_error': ErrorCategory.AI_SERVICE,
    'continuous_dev_error': ErrorCategory.CONTINUOUS_DEVELOPMENT,
    'validation_error': ErrorCategory.VALIDATION,
    'system_error': ErrorCategory.SYSTEM,
    'unknown_error': ErrorCategory.UNKNOWN
  };
  
  return mappings[category] || ErrorCategory.UNKNOWN;
};

export const severityFromString = (severity: string): ErrorSeverity => {
  if (Object.values(ErrorSeverity).includes(severity as ErrorSeverity)) {
    return severity as ErrorSeverity;
  }
  return ErrorSeverity.MEDIUM; // Default to medium severity
};