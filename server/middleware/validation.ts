import { Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { AppError } from './error-handler';
import logger from '../logger';

/**
 * Middleware for validating request bodies against Zod schemas
 * @param schema Zod schema to validate against
 */
export const validateBody = (schema: z.ZodType<any>) => {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      const result = schema.safeParse(req.body);
      
      if (!result.success) {
        // Format Zod errors for better readability
        const formattedErrors = result.error.errors.map(err => ({
          path: err.path.join('.'),
          message: err.message
        }));
        
        logger.warn(`Validation error: ${JSON.stringify(formattedErrors)}`);
        
        return next(
          new AppError('Validation failed', 400, true)
        );
      }
      
      // Replace request body with validated and sanitized data
      req.body = result.data;
      next();
    } catch (error) {
      next(
        new AppError(
          'Validation error',
          400,
          true
        )
      );
    }
  };
};

/**
 * Middleware for validating URL parameters against Zod schemas
 * @param schema Zod schema to validate against
 */
export const validateParams = (schema: z.ZodType<any>) => {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      const result = schema.safeParse(req.params);
      
      if (!result.success) {
        const formattedErrors = result.error.errors.map(err => ({
          path: err.path.join('.'),
          message: err.message
        }));
        
        logger.warn(`Parameter validation error: ${JSON.stringify(formattedErrors)}`);
        
        return next(
          new AppError('Invalid parameters', 400, true)
        );
      }
      
      // Replace request params with validated data
      req.params = result.data;
      next();
    } catch (error) {
      next(
        new AppError(
          'Parameter validation error',
          400,
          true
        )
      );
    }
  };
};

/**
 * Middleware for validating query parameters against Zod schemas
 * @param schema Zod schema to validate against
 */
export const validateQuery = (schema: z.ZodType<any>) => {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      const result = schema.safeParse(req.query);
      
      if (!result.success) {
        const formattedErrors = result.error.errors.map(err => ({
          path: err.path.join('.'),
          message: err.message
        }));
        
        logger.warn(`Query validation error: ${JSON.stringify(formattedErrors)}`);
        
        return next(
          new AppError('Invalid query parameters', 400, true)
        );
      }
      
      // Replace request query with validated data
      req.query = result.data;
      next();
    } catch (error) {
      next(
        new AppError(
          'Query validation error',
          400,
          true
        )
      );
    }
  };
};