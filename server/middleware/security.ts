import { Request, Response, NextFunction } from 'express';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import logger from '../logger';

/**
 * Configure helmet for securing HTTP headers
 */
export const helmetMiddleware = helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
      styleSrc: ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
      fontSrc: ["'self'", "https://fonts.gstatic.com"],
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'", "https://api.openai.com"],
    }
  },
  crossOriginEmbedderPolicy: false // Required for some features
});

/**
 * Standard rate limiter for most API endpoints
 */
export const standardRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  limit: 100, // Limit each IP to 100 requests per windowMs
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  message: {
    status: 429,
    message: 'Too many requests, please try again later.'
  },
  handler: (req: Request, res: Response, next: NextFunction, options: any) => {
    logger.warn(`Rate limit exceeded: ${req.ip} - ${req.method} ${req.originalUrl}`);
    res.status(options.statusCode).json(options.message);
  }
});

/**
 * Stricter rate limiter for AI-related endpoints
 */
export const aiRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  limit: 30, // Limit each IP to 30 requests per windowMs
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  message: {
    status: 429,
    message: 'Too many AI requests, please try again later.'
  },
  handler: (req: Request, res: Response, next: NextFunction, options: any) => {
    logger.warn(`AI rate limit exceeded: ${req.ip} - ${req.method} ${req.originalUrl}`);
    res.status(options.statusCode).json(options.message);
  }
});

/**
 * Middleware to set security-related headers not covered by helmet
 */
export const additionalSecurityHeaders = (req: Request, res: Response, next: NextFunction) => {
  // Prevent browsers from detecting the mimetype if not sent in headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  
  // Prevent clickjacking
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  
  // Prevent XSS attacks
  res.setHeader('X-XSS-Protection', '1; mode=block');
  
  next();
};

/**
 * Request logger middleware
 */
export const requestLogger = (req: Request, res: Response, next: NextFunction) => {
  const start = Date.now();
  
  // Log request
  logger.http(`${req.method} ${req.originalUrl} [${req.ip}]`);
  
  // Log response time when finished
  res.on('finish', () => {
    const duration = Date.now() - start;
    const statusCode = res.statusCode;
    
    if (statusCode >= 400) {
      logger.warn(`${req.method} ${req.originalUrl} ${statusCode} - ${duration}ms`);
    } else {
      logger.http(`${req.method} ${req.originalUrl} ${statusCode} - ${duration}ms`);
    }
  });
  
  next();
};