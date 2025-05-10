/**
 * Backup Manager for ModForge
 * 
 * This module provides automated backup capabilities for critical data,
 * ensuring that data can be recovered in case of failures or corruption.
 */

import { getLogger } from './logging';
import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import { exec } from 'child_process';
import { promisify } from 'util';
import { scheduleJob } from './scheduled-jobs';
import { pool } from './db';
import crypto from 'crypto';
import zlib from 'zlib';
import { pipeline } from 'stream/promises';
import { createReadStream, createWriteStream } from 'fs';

// Setup logger
const logger = getLogger('backup-manager');

// File paths
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const BACKUP_DIR = path.join(__dirname, '../backups');

// Promisify exec
const execAsync = promisify(exec);
const gzip = promisify(zlib.gzip);

// Backup types
export enum BackupType {
  DATABASE = 'database',
  MOD_FILES = 'mod_files',
  CONFIG = 'config',
  LOGS = 'logs'
}

// Backup status
export enum BackupStatus {
  PENDING = 'pending',
  IN_PROGRESS = 'in_progress',
  COMPLETED = 'completed',
  FAILED = 'failed'
}

// Backup result
export interface BackupResult {
  id: string;
  type: BackupType;
  status: BackupStatus;
  timestamp: Date;
  path?: string;
  size?: number;
  error?: string;
  metadata: Record<string, any>;
}

// In-memory storage of backup metadata
// In a production system, this would be stored in a database
const backupStore: Map<string, BackupResult> = new Map();

/**
 * Ensure backup directory exists
 */
async function ensureBackupDir(): Promise<void> {
  try {
    await fs.mkdir(BACKUP_DIR, { recursive: true });
    logger.info(`Backup directory ready: ${BACKUP_DIR}`);
  } catch (error) {
    logger.error(`Failed to create backup directory: ${BACKUP_DIR}`, { error });
    throw error;
  }
}

/**
 * Generate a unique backup ID
 */
function generateBackupId(type: BackupType): string {
  return `${type}-${Date.now()}-${crypto.randomBytes(4).toString('hex')}`;
}

/**
 * Create a database backup
 */
async function createDatabaseBackup(metadata: Record<string, any> = {}): Promise<BackupResult> {
  const backupId = generateBackupId(BackupType.DATABASE);
  const timestamp = new Date();
  const backupPath = path.join(BACKUP_DIR, `${backupId}.sql.gz`);
  
  // Create initial backup record
  const backupRecord: BackupResult = {
    id: backupId,
    type: BackupType.DATABASE,
    status: BackupStatus.PENDING,
    timestamp,
    metadata
  };
  
  backupStore.set(backupId, backupRecord);
  
  try {
    logger.info(`Starting database backup: ${backupId}`);
    backupRecord.status = BackupStatus.IN_PROGRESS;
    
    // Get database connection info from environment
    const {
      PGHOST = 'localhost',
      PGPORT = '5432',
      PGUSER = 'postgres',
      PGPASSWORD,
      PGDATABASE = 'postgres'
    } = process.env;
    
    // Create pg_dump command
    // We'll pipe to gzip directly for compression
    const cmd = `PGPASSWORD=${PGPASSWORD} pg_dump -h ${PGHOST} -p ${PGPORT} -U ${PGUSER} -d ${PGDATABASE} -F p`;
    
    // Execute pg_dump and pipe to gzip to compress the output
    const { stdout, stderr } = await execAsync(cmd);
    
    // Compress and save the output
    await fs.writeFile(backupPath, await gzip(stdout));
    
    // Get file size
    const stats = await fs.stat(backupPath);
    
    // Update backup record
    backupRecord.status = BackupStatus.COMPLETED;
    backupRecord.path = backupPath;
    backupRecord.size = stats.size;
    
    logger.info(`Database backup completed: ${backupId}`, {
      size: stats.size,
      path: backupPath
    });
    
    return backupRecord;
  } catch (error) {
    logger.error(`Database backup failed: ${backupId}`, { error });
    
    backupRecord.status = BackupStatus.FAILED;
    backupRecord.error = error instanceof Error ? error.message : String(error);
    
    return backupRecord;
  }
}

/**
 * Create a backup of all mod files
 */
async function createModFilesBackup(metadata: Record<string, any> = {}): Promise<BackupResult> {
  const backupId = generateBackupId(BackupType.MOD_FILES);
  const timestamp = new Date();
  const backupPath = path.join(BACKUP_DIR, `${backupId}.tar.gz`);
  
  // Create initial backup record
  const backupRecord: BackupResult = {
    id: backupId,
    type: BackupType.MOD_FILES,
    status: BackupStatus.PENDING,
    timestamp,
    metadata
  };
  
  backupStore.set(backupId, backupRecord);
  
  try {
    logger.info(`Starting mod files backup: ${backupId}`);
    backupRecord.status = BackupStatus.IN_PROGRESS;
    
    // Get mod files directory from the database using the DB connection
    // In a real system, we would query the database for mod file paths
    // For now, we'll just use a common directory structure
    const modFilesDir = path.join(__dirname, '../mod-files');
    
    // Check if directory exists
    try {
      await fs.access(modFilesDir);
    } catch (error) {
      // If directory doesn't exist, create a placeholder entry for testing
      await fs.mkdir(modFilesDir, { recursive: true });
      await fs.writeFile(path.join(modFilesDir, 'README.txt'), 'Placeholder for mod files');
    }
    
    // Create tar.gz backup of the mod files directory
    const cmd = `tar -czf "${backupPath}" -C "${path.dirname(modFilesDir)}" "${path.basename(modFilesDir)}"`;
    await execAsync(cmd);
    
    // Get file size
    const stats = await fs.stat(backupPath);
    
    // Update backup record
    backupRecord.status = BackupStatus.COMPLETED;
    backupRecord.path = backupPath;
    backupRecord.size = stats.size;
    
    logger.info(`Mod files backup completed: ${backupId}`, {
      size: stats.size,
      path: backupPath
    });
    
    return backupRecord;
  } catch (error) {
    logger.error(`Mod files backup failed: ${backupId}`, { error });
    
    backupRecord.status = BackupStatus.FAILED;
    backupRecord.error = error instanceof Error ? error.message : String(error);
    
    return backupRecord;
  }
}

/**
 * Create a backup of configuration files
 */
async function createConfigBackup(metadata: Record<string, any> = {}): Promise<BackupResult> {
  const backupId = generateBackupId(BackupType.CONFIG);
  const timestamp = new Date();
  const backupPath = path.join(BACKUP_DIR, `${backupId}.tar.gz`);
  
  // Create initial backup record
  const backupRecord: BackupResult = {
    id: backupId,
    type: BackupType.CONFIG,
    status: BackupStatus.PENDING,
    timestamp,
    metadata
  };
  
  backupStore.set(backupId, backupRecord);
  
  try {
    logger.info(`Starting config backup: ${backupId}`);
    backupRecord.status = BackupStatus.IN_PROGRESS;
    
    // For config backup, we'll include .env and other configuration files
    const configDir = path.join(__dirname, '..');
    const configFiles = ['.env', 'package.json', 'tsconfig.json', 'vite.config.ts'];
    
    // Create a temporary directory to store config files
    const tempDir = path.join(BACKUP_DIR, 'temp', backupId);
    await fs.mkdir(tempDir, { recursive: true });
    
    // Copy config files to temp directory
    for (const file of configFiles) {
      try {
        const filePath = path.join(configDir, file);
        await fs.access(filePath);
        await fs.copyFile(filePath, path.join(tempDir, file));
      } catch (error) {
        logger.warn(`Could not copy config file: ${file}`, { error });
        // Continue with other files
      }
    }
    
    // Create tar.gz backup of the temp directory
    const cmd = `tar -czf "${backupPath}" -C "${path.dirname(tempDir)}" "${path.basename(tempDir)}"`;
    await execAsync(cmd);
    
    // Clean up temp directory
    await fs.rm(tempDir, { recursive: true, force: true });
    
    // Get file size
    const stats = await fs.stat(backupPath);
    
    // Update backup record
    backupRecord.status = BackupStatus.COMPLETED;
    backupRecord.path = backupPath;
    backupRecord.size = stats.size;
    
    logger.info(`Config backup completed: ${backupId}`, {
      size: stats.size,
      path: backupPath
    });
    
    return backupRecord;
  } catch (error) {
    logger.error(`Config backup failed: ${backupId}`, { error });
    
    backupRecord.status = BackupStatus.FAILED;
    backupRecord.error = error instanceof Error ? error.message : String(error);
    
    return backupRecord;
  }
}

/**
 * Create a backup of log files
 */
async function createLogsBackup(metadata: Record<string, any> = {}): Promise<BackupResult> {
  const backupId = generateBackupId(BackupType.LOGS);
  const timestamp = new Date();
  const backupPath = path.join(BACKUP_DIR, `${backupId}.tar.gz`);
  
  // Create initial backup record
  const backupRecord: BackupResult = {
    id: backupId,
    type: BackupType.LOGS,
    status: BackupStatus.PENDING,
    timestamp,
    metadata
  };
  
  backupStore.set(backupId, backupRecord);
  
  try {
    logger.info(`Starting logs backup: ${backupId}`);
    backupRecord.status = BackupStatus.IN_PROGRESS;
    
    // Get logs directory
    const logsDir = path.join(__dirname, '../logs');
    
    // Check if directory exists
    try {
      await fs.access(logsDir);
    } catch (error) {
      logger.warn(`Logs directory does not exist: ${logsDir}`, { error });
      
      // Create a placeholder log file for testing
      await fs.mkdir(logsDir, { recursive: true });
      await fs.writeFile(path.join(logsDir, 'placeholder.log'), 'Placeholder log file');
    }
    
    // Create tar.gz backup of the logs directory
    const cmd = `tar -czf "${backupPath}" -C "${path.dirname(logsDir)}" "${path.basename(logsDir)}"`;
    await execAsync(cmd);
    
    // Get file size
    const stats = await fs.stat(backupPath);
    
    // Update backup record
    backupRecord.status = BackupStatus.COMPLETED;
    backupRecord.path = backupPath;
    backupRecord.size = stats.size;
    
    logger.info(`Logs backup completed: ${backupId}`, {
      size: stats.size,
      path: backupPath
    });
    
    return backupRecord;
  } catch (error) {
    logger.error(`Logs backup failed: ${backupId}`, { error });
    
    backupRecord.status = BackupStatus.FAILED;
    backupRecord.error = error instanceof Error ? error.message : String(error);
    
    return backupRecord;
  }
}

/**
 * Create a backup of the specified type
 */
export async function createBackup(type: BackupType, metadata: Record<string, any> = {}): Promise<BackupResult> {
  // Ensure backup directory exists
  await ensureBackupDir();
  
  // Create backup based on type
  switch (type) {
    case BackupType.DATABASE:
      return createDatabaseBackup(metadata);
    case BackupType.MOD_FILES:
      return createModFilesBackup(metadata);
    case BackupType.CONFIG:
      return createConfigBackup(metadata);
    case BackupType.LOGS:
      return createLogsBackup(metadata);
    default:
      throw new Error(`Unsupported backup type: ${type}`);
  }
}

/**
 * Run a full backup of all types
 */
export async function runFullBackup(metadata: Record<string, any> = {}): Promise<BackupResult[]> {
  logger.info('Starting full backup of all data');
  
  // Run all backup types in parallel
  const results = await Promise.all([
    createBackup(BackupType.DATABASE, metadata),
    createBackup(BackupType.MOD_FILES, metadata),
    createBackup(BackupType.CONFIG, metadata),
    createBackup(BackupType.LOGS, metadata)
  ]);
  
  const successful = results.filter(r => r.status === BackupStatus.COMPLETED).length;
  const failed = results.filter(r => r.status === BackupStatus.FAILED).length;
  
  logger.info(`Full backup completed: ${successful} successful, ${failed} failed`);
  
  return results;
}

/**
 * Get all backup results
 */
export function getBackups(type?: BackupType): BackupResult[] {
  const backups = Array.from(backupStore.values());
  
  if (type) {
    return backups.filter(b => b.type === type);
  }
  
  return backups;
}

/**
 * Get a specific backup by ID
 */
export function getBackup(id: string): BackupResult | undefined {
  return backupStore.get(id);
}

/**
 * Delete a backup by ID
 */
export async function deleteBackup(id: string): Promise<boolean> {
  const backup = backupStore.get(id);
  
  if (!backup) {
    return false;
  }
  
  // Delete backup file if it exists
  if (backup.path) {
    try {
      await fs.unlink(backup.path);
    } catch (error) {
      logger.error(`Failed to delete backup file: ${backup.path}`, { error });
      // Continue anyway to delete the record
    }
  }
  
  // Delete backup record
  backupStore.delete(id);
  
  logger.info(`Backup deleted: ${id}`);
  
  return true;
}

/**
 * Clean up old backups
 * @param maxAge Maximum age of backups to keep in milliseconds
 * @param maxCount Maximum number of backups to keep per type
 */
export async function cleanupBackups(maxAge: number = 7 * 24 * 60 * 60 * 1000, maxCount: number = 10): Promise<{ deleted: number, errors: number }> {
  logger.info(`Cleaning up backups older than ${maxAge}ms or exceeding ${maxCount} per type`);
  
  let deleted = 0;
  let errors = 0;
  
  // Group backups by type
  const backupsByType: Record<string, BackupResult[]> = {};
  
  for (const backup of backupStore.values()) {
    if (!backupsByType[backup.type]) {
      backupsByType[backup.type] = [];
    }
    
    backupsByType[backup.type].push(backup);
  }
  
  // Process each backup type
  for (const type in backupsByType) {
    const backups = backupsByType[type];
    
    // Sort by timestamp (newest first)
    backups.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
    
    // Delete old backups or if exceeding max count
    for (let i = 0; i < backups.length; i++) {
      const backup = backups[i];
      const age = Date.now() - backup.timestamp.getTime();
      
      if (i >= maxCount || age > maxAge) {
        try {
          await deleteBackup(backup.id);
          deleted++;
        } catch (error) {
          logger.error(`Failed to delete backup: ${backup.id}`, { error });
          errors++;
        }
      }
    }
  }
  
  logger.info(`Backup cleanup completed: ${deleted} deleted, ${errors} errors`);
  
  return { deleted, errors };
}

/**
 * Schedule regular backups
 * @param dailyBackupHour Hour of the day to run daily backups (0-23)
 * @param weeklyBackupDay Day of the week to run weekly backups (0-6, 0 is Sunday)
 * @returns Cleanup function to stop scheduled backups
 */
export function scheduleBackups(dailyBackupHour: number = 2, weeklyBackupDay: number = 0): () => void {
  logger.info(`Scheduling automatic backups: daily at ${dailyBackupHour}:00, weekly on day ${weeklyBackupDay}`);
  
  // Schedule daily database backups
  const dailyBackupJob = scheduleJob('daily-database-backup', 24 * 60 * 60 * 1000, async () => {
    const now = new Date();
    if (now.getHours() === dailyBackupHour) {
      try {
        logger.info('Running scheduled daily database backup');
        await createBackup(BackupType.DATABASE, { scheduled: true, type: 'daily' });
      } catch (error) {
        logger.error('Scheduled daily database backup failed', { error });
      }
    }
  });
  
  // Schedule weekly full backups
  const weeklyBackupJob = scheduleJob('weekly-full-backup', 24 * 60 * 60 * 1000, async () => {
    const now = new Date();
    if (now.getDay() === weeklyBackupDay && now.getHours() === dailyBackupHour) {
      try {
        logger.info('Running scheduled weekly full backup');
        await runFullBackup({ scheduled: true, type: 'weekly' });
        
        // Clean up old backups
        await cleanupBackups();
      } catch (error) {
        logger.error('Scheduled weekly full backup failed', { error });
      }
    }
  });
  
  // Return cleanup function
  return () => {
    dailyBackupJob();
    weeklyBackupJob();
    logger.info('Stopped scheduled backups');
  };
}