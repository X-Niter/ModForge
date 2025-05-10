/**
 * Backup Routes for ModForge
 * 
 * These routes provide access to the backup system, allowing administrators
 * to create, manage, and restore backups of critical data.
 */

import express from 'express';
import {
  createBackup,
  runFullBackup,
  getBackups,
  getBackup,
  deleteBackup,
  cleanupBackups,
  scheduleBackups,
  BackupType
} from '../backup-manager';
import { getLogger } from '../logging';
import fs from 'fs/promises';
import path from 'path';
import { createReadStream } from 'fs';

const router = express.Router();
const logger = getLogger('backup-api');

// Get all backups
router.get('/', async (req, res) => {
  try {
    // Get backup type from query param
    let type: BackupType | undefined = undefined;
    if (req.query.type && Object.values(BackupType).includes(req.query.type as BackupType)) {
      type = req.query.type as BackupType;
    }
    
    const backups = getBackups(type);
    
    // Sort by timestamp (newest first)
    backups.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
    
    res.json({
      success: true,
      count: backups.length,
      data: backups,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to get backups', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to get backups',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Get a specific backup by ID
router.get('/:id', async (req, res) => {
  try {
    const backupId = req.params.id;
    const backup = getBackup(backupId);
    
    if (!backup) {
      return res.status(404).json({
        success: false,
        message: `Backup with ID ${backupId} not found`,
        timestamp: new Date().toISOString()
      });
    }
    
    res.json({
      success: true,
      data: backup,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to get backup', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to get backup',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Download a backup file
router.get('/:id/download', async (req, res) => {
  try {
    const backupId = req.params.id;
    const backup = getBackup(backupId);
    
    if (!backup) {
      return res.status(404).json({
        success: false,
        message: `Backup with ID ${backupId} not found`,
        timestamp: new Date().toISOString()
      });
    }
    
    if (!backup.path) {
      return res.status(404).json({
        success: false,
        message: `Backup ${backupId} has no file path`,
        timestamp: new Date().toISOString()
      });
    }
    
    // Check if file exists
    try {
      await fs.access(backup.path);
    } catch (error) {
      return res.status(404).json({
        success: false,
        message: `Backup file not found: ${path.basename(backup.path)}`,
        timestamp: new Date().toISOString()
      });
    }
    
    // Set download headers
    const filename = path.basename(backup.path);
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    res.setHeader('Content-Type', 'application/octet-stream');
    
    // Stream the file
    const fileStream = createReadStream(backup.path);
    fileStream.pipe(res);
  } catch (error) {
    logger.error('Failed to download backup', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to download backup',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Create a new backup
router.post('/create', async (req, res) => {
  try {
    const { type, metadata } = req.body;
    
    if (!type || !Object.values(BackupType).includes(type)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid backup type',
        validTypes: Object.values(BackupType),
        timestamp: new Date().toISOString()
      });
    }
    
    // Add user info to metadata
    const backupMetadata = {
      ...metadata,
      userId: req.user?.id,
      username: req.user?.username,
      manual: true
    };
    
    const backup = await createBackup(type, backupMetadata);
    
    res.json({
      success: true,
      message: `Backup created: ${backup.id}`,
      data: backup,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to create backup', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to create backup',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Create a full backup
router.post('/full-backup', async (req, res) => {
  try {
    const { metadata } = req.body;
    
    // Add user info to metadata
    const backupMetadata = {
      ...metadata,
      userId: req.user?.id,
      username: req.user?.username,
      manual: true
    };
    
    const backups = await runFullBackup(backupMetadata);
    
    const successful = backups.filter(b => b.status === 'completed').length;
    const failed = backups.filter(b => b.status === 'failed').length;
    
    res.json({
      success: true,
      message: `Full backup completed: ${successful} successful, ${failed} failed`,
      data: backups,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to create full backup', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to create full backup',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Delete a backup
router.delete('/:id', async (req, res) => {
  try {
    const backupId = req.params.id;
    const deleted = await deleteBackup(backupId);
    
    if (!deleted) {
      return res.status(404).json({
        success: false,
        message: `Backup with ID ${backupId} not found`,
        timestamp: new Date().toISOString()
      });
    }
    
    res.json({
      success: true,
      message: `Backup ${backupId} deleted`,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to delete backup', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to delete backup',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

// Clean up old backups
router.post('/cleanup', async (req, res) => {
  try {
    const { maxAge, maxCount } = req.body;
    
    // Convert maxAge from days to milliseconds
    const maxAgeMs = maxAge ? maxAge * 24 * 60 * 60 * 1000 : undefined;
    
    const result = await cleanupBackups(maxAgeMs, maxCount);
    
    res.json({
      success: true,
      message: `Backup cleanup completed: ${result.deleted} deleted, ${result.errors} errors`,
      data: result,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to clean up backups', { error });
    
    res.status(500).json({
      success: false,
      message: 'Failed to clean up backups',
      error: error instanceof Error ? error.message : String(error),
      timestamp: new Date().toISOString()
    });
  }
});

export default router;