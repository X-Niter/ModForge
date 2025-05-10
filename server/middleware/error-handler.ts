import { Request, Response, NextFunction } from 'express';
import logger from '../logger';

// Custom error class with status code
export class AppError extends Error {
  statusCode: number;
  isOperational: boolean;

  constructor(message: string, statusCode: number, isOperational = true) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = isOperational;
    
    // Capture stack trace
    Error.captureStackTrace(this, this.constructor);
  }
}

// Error handler middleware
export const errorHandler = (
  err: Error | AppError,
  req: Request,
  res: Response,
  _next: NextFunction
) => {
  // Default values for non-AppError errors
  let statusCode = 500;
  let message = 'Internal Server Error';
  let isOperational = false;
  
  // If it's our custom AppError, use its properties
  if ('statusCode' in err) {
    statusCode = err.statusCode;
    message = err.message;
    isOperational = err.isOperational;
  }
  
  // Log the error
  if (statusCode >= 500) {
    logger.error(`[${statusCode}] ${req.method} ${req.path}: ${err.message}\n${err.stack}`);
  } else {
    logger.warn(`[${statusCode}] ${req.method} ${req.path}: ${err.message}`);
  }
  
  // Send response based on environment
  const isDev = process.env.NODE_ENV === 'development';
  
  res.status(statusCode).json({
    status: 'error',
    message,
    ...(isDev && { 
      stack: err.stack,
      isOperational
    })
  });
};

// 404 Not Found handler
export const notFoundHandler = (
  req: Request,
  res: Response,
  _next: NextFunction
) => {
  logger.warn(`Not Found: ${req.method} ${req.path}`);
  
  res.status(404).json({
    status: 'error',
    message: `Route not found: ${req.method} ${req.path}`
  });
};

// Async handler to wrap controllers and avoid try/catch blocks
export const asyncHandler = (fn: Function) => {
  return (req: Request, res: Response, next: NextFunction) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
};