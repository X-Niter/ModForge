/**
 * Error Tracking Routes for ModForge
 * 
 * These routes provide access to the error tracking system, allowing administrators
 * to view, manage, and analyze errors that occur in the system.
 */

import express from 'express';
import {
  trackError,
  resolveError,
  getTrackedErrors,
  getErrorStatistics,
  purgeResolvedErrors,
  ErrorCategory,
  ErrorSeverity
} from '../error-tracker';
import { getLogger } from '../logging';

const router = express.Router();
const logger = getLogger('error-tracking-api');

// Get error statistics (counts by category, severity, etc.)
router.get('/stats', async (req, res) => {
  try {
    const stats = getErrorStatistics();
    res.json({
      success: true,
      data: stats,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to get error statistics', { error });
    res.status(500).json({
      success: false,
      message: 'Failed to get error statistics',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get all tracked errors with optional filtering
router.get('/errors', async (req, res) => {
  try {
    // Parse filters from query parameters
    const filters: {
      category?: ErrorCategory;
      severity?: ErrorSeverity;
      resolved?: boolean;
      minCount?: number;
      since?: Date;
    } = {};
    
    if (req.query.category && Object.values(ErrorCategory).includes(req.query.category as ErrorCategory)) {
      filters.category = req.query.category as ErrorCategory;
    }
    
    if (req.query.severity && Object.values(ErrorSeverity).includes(req.query.severity as ErrorSeverity)) {
      filters.severity = req.query.severity as ErrorSeverity;
    }
    
    if (req.query.resolved !== undefined) {
      filters.resolved = req.query.resolved === 'true';
    }
    
    if (req.query.minCount !== undefined) {
      const minCount = parseInt(req.query.minCount as string, 10);
      if (!isNaN(minCount)) {
        filters.minCount = minCount;
      }
    }
    
    if (req.query.since !== undefined) {
      try {
        filters.since = new Date(req.query.since as string);
      } catch (e) {
        // Ignore invalid date
      }
    }
    
    const errors = getTrackedErrors(filters);
    
    res.json({
      success: true,
      count: errors.length,
      data: errors,
      filters,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to get tracked errors', { error });
    res.status(500).json({
      success: false,
      message: 'Failed to get tracked errors',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get a specific error by ID
router.get('/errors/:id', async (req, res) => {
  try {
    const errorId = req.params.id;
    const errors = getTrackedErrors();
    const error = errors.find(e => e.id === errorId);
    
    if (!error) {
      return res.status(404).json({
        success: false,
        message: `Error with ID ${errorId} not found`
      });
    }
    
    res.json({
      success: true,
      data: error,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to get error', { error, errorId: req.params.id });
    res.status(500).json({
      success: false,
      message: 'Failed to get error',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

// Mark an error as resolved
router.post('/errors/:id/resolve', async (req, res) => {
  try {
    const errorId = req.params.id;
    const resolved = resolveError(errorId);
    
    if (!resolved) {
      return res.status(404).json({
        success: false,
        message: `Error with ID ${errorId} not found`
      });
    }
    
    res.json({
      success: true,
      message: `Error ${errorId} marked as resolved`,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to resolve error', { error, errorId: req.params.id });
    res.status(500).json({
      success: false,
      message: 'Failed to resolve error',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

// Purge old resolved errors
router.post('/purge', async (req, res) => {
  try {
    let olderThan: Date | undefined;
    
    if (req.body.olderThan) {
      try {
        olderThan = new Date(req.body.olderThan);
      } catch (e) {
        return res.status(400).json({
          success: false,
          message: 'Invalid date format for olderThan'
        });
      }
    } else if (req.body.days) {
      const days = parseInt(req.body.days, 10);
      if (isNaN(days)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid number of days'
        });
      }
      olderThan = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
    }
    
    const purgedCount = purgeResolvedErrors(olderThan);
    
    res.json({
      success: true,
      message: `Purged ${purgedCount} resolved errors`,
      purgedCount,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to purge resolved errors', { error });
    res.status(500).json({
      success: false,
      message: 'Failed to purge resolved errors',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

// Manually track a test error (for debugging)
router.post('/test', async (req, res) => {
  try {
    // Check if the user is an admin
    if (!req.user?.isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Only administrators can create test errors'
      });
    }
    
    const { message, category, severity } = req.body;
    
    if (!message) {
      return res.status(400).json({
        success: false,
        message: 'Error message is required'
      });
    }
    
    // Create a test error
    const error = new Error(message);
    const trackedError = trackError(error, { source: 'test' }, { 
      category: category as ErrorCategory,
      severity: severity as ErrorSeverity
    });
    
    res.json({
      success: true,
      message: 'Test error tracked successfully',
      data: trackedError,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    logger.error('Failed to track test error', { error });
    res.status(500).json({
      success: false,
      message: 'Failed to track test error',
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

export default router;