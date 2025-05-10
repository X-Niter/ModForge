import winston from 'winston';
import { format } from 'winston';
import fs from 'fs';
import path from 'path';

// Create logs directory if it doesn't exist
const logsDir = path.join(process.cwd(), 'logs');
if (!fs.existsSync(logsDir)) {
  fs.mkdirSync(logsDir, { recursive: true });
}

// Define log format
const logFormat = format.combine(
  format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  format.errors({ stack: true }),
  format.splat(),
  format.json()
);

// Create logger instance
const logger = winston.createLogger({
  level: process.env.NODE_ENV === 'production' ? 'info' : 'debug',
  format: logFormat,
  defaultMeta: { service: 'mod-forge' },
  transports: [
    // Write logs to console
    new winston.transports.Console({
      format: format.combine(
        format.colorize(),
        format.printf(({ timestamp, level, message, ...meta }) => {
          return `${timestamp} ${level}: ${message} ${Object.keys(meta).length ? JSON.stringify(meta, null, 2) : ''}`;
        })
      ),
    }),
    // Write to error log
    new winston.transports.File({ 
      filename: path.join(logsDir, 'error.log'),
      level: 'error',
    }),
    // Write all logs to combined log
    new winston.transports.File({ 
      filename: path.join(logsDir, 'combined.log'),
      maxsize: 10485760, // 10MB
      maxFiles: 5
    }),
  ],
});

// Create specialized loggers for different services
export const githubLogger = logger.child({ module: 'github' });
export const aiLogger = logger.child({ module: 'ai-service' });
export const compilerLogger = logger.child({ module: 'compiler' });
export const appLogger = logger.child({ module: 'app' });
export const patternLearningLogger = logger.child({ module: 'pattern-learning' });

// Export default logger
export default logger;