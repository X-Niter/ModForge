/**
 * Centralized logging system for ModForge
 * 
 * This module provides structured logging with support for different log levels,
 * timestamps, context tracking, and potential integration with external logging services.
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

// ES Module equivalent for __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Log levels in order of severity
export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error',
  CRITICAL = 'critical'
}

// ANSI color codes for console output
const colors = {
  reset: '\x1b[0m',
  debug: '\x1b[90m', // Gray
  info: '\x1b[36m',  // Cyan
  warn: '\x1b[33m',  // Yellow
  error: '\x1b[31m', // Red
  critical: '\x1b[41m\x1b[37m' // White on red background
};

// Log entry interface
export interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: Record<string, any>;
  component?: string;
  sessionId?: string;
}

// Configure log file paths
const LOG_DIR = path.join(__dirname, '../logs');
const ERROR_LOG_FILE = path.join(LOG_DIR, 'error.log');
const COMBINED_LOG_FILE = path.join(LOG_DIR, 'combined.log');

// Configure retention (7 days default)
const MAX_LOG_AGE_MS = 7 * 24 * 60 * 60 * 1000;

// Make sure log directory exists
async function ensureLogDir() {
  try {
    await fs.access(LOG_DIR);
  } catch {
    await fs.mkdir(LOG_DIR, { recursive: true });
  }
}

// Rotate logs that are too old
async function rotateLogs() {
  try {
    const now = Date.now();
    const files = await fs.readdir(LOG_DIR);
    
    for (const file of files) {
      if (!file.endsWith('.log')) continue;
      
      const filePath = path.join(LOG_DIR, file);
      const stats = await fs.stat(filePath);
      
      // If file is older than retention period, archive it
      if (now - stats.mtimeMs > MAX_LOG_AGE_MS) {
        const archiveName = `${file}.${new Date(stats.mtimeMs).toISOString().split('T')[0]}.old`;
        const archivePath = path.join(LOG_DIR, archiveName);
        
        await fs.rename(filePath, archivePath);
        console.log(`Rotated old log file: ${file} -> ${archiveName}`);
      }
    }
  } catch (error) {
    console.error('Error rotating logs:', error);
  }
}

// Write log to file
async function writeLogToFile(entry: LogEntry, errorOnly: boolean = false) {
  try {
    const logString = JSON.stringify(entry) + '\n';
    
    // Ensure log directory exists
    await ensureLogDir();
    
    // Always write to combined log
    await fs.appendFile(COMBINED_LOG_FILE, logString);
    
    // Write to error log if it's an error or critical level
    if (errorOnly || entry.level === LogLevel.ERROR || entry.level === LogLevel.CRITICAL) {
      await fs.appendFile(ERROR_LOG_FILE, logString);
    }
  } catch (error) {
    console.error(`Failed to write to log file: ${error}`);
  }
}

// Default logger implementation
class Logger {
  private component: string;
  private sessionId?: string;
  
  constructor(component: string, sessionId?: string) {
    this.component = component;
    this.sessionId = sessionId;
  }
  
  /**
   * Set or update the session ID for request tracking
   */
  setSessionId(sessionId: string) {
    this.sessionId = sessionId;
  }
  
  /**
   * Log a message with the specified level and optional context
   */
  log(level: LogLevel, message: string, context?: Record<string, any>) {
    const timestamp = new Date().toISOString();
    
    // Create log entry
    const entry: LogEntry = {
      timestamp,
      level,
      message,
      component: this.component,
      sessionId: this.sessionId
    };
    
    if (context) {
      entry.context = context;
    }
    
    // Console output with color
    const color = colors[level] || colors.reset;
    const reset = colors.reset;
    const consoleTimestamp = timestamp.replace('T', ' ').replace('Z', '');
    const componentInfo = this.component ? `[${this.component}] ` : '';
    const sessionInfo = this.sessionId ? `(${this.sessionId.substring(0, 8)}) ` : '';
    
    console.log(`${color}${consoleTimestamp} ${level.toUpperCase()} ${componentInfo}${sessionInfo}${message}${reset}`);
    
    // Write to log file asynchronously (don't await)
    writeLogToFile(entry);
  }
  
  // Convenience methods for different log levels
  debug(message: string, context?: Record<string, any>) {
    this.log(LogLevel.DEBUG, message, context);
  }
  
  info(message: string, context?: Record<string, any>) {
    this.log(LogLevel.INFO, message, context);
  }
  
  warn(message: string, context?: Record<string, any>) {
    this.log(LogLevel.WARN, message, context);
  }
  
  error(message: string, context?: Record<string, any>) {
    this.log(LogLevel.ERROR, message, context);
  }
  
  critical(message: string, context?: Record<string, any>) {
    this.log(LogLevel.CRITICAL, message, context);
  }
}

// Create root logger
const rootLogger = new Logger('system');

// Create logger factory
export function getLogger(component: string, sessionId?: string) {
  return new Logger(component, sessionId);
}

// Schedule daily log rotation
setInterval(rotateLogs, 24 * 60 * 60 * 1000);

// Initial log setup
ensureLogDir().then(() => {
  rootLogger.info('Logging system initialized');
  rotateLogs().catch(err => {
    rootLogger.error('Failed to rotate logs on startup', { error: err.message });
  });
}).catch(err => {
  console.error('Failed to initialize logging system:', err);
});

export { rootLogger };