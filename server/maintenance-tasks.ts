/**
 * Maintenance Tasks
 * 
 * This module provides utilities for system maintenance during 24/7 operation
 * to ensure long-term stability and resource management.
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

// ES Module equivalent for __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Interface for cleanup task results
 */
interface CleanupResult {
  success: boolean;
  taskName: string;
  itemsRemoved: number;
  errors: string[];
  timestamp: string;
}

/**
 * Cleans up temporary files that may accumulate during long running operations
 * @param directory Directory to clean up
 * @param maxAgeHours Maximum age of files to keep (in hours)
 * @returns Cleanup results
 */
export async function cleanupTempFiles(directory: string, maxAgeHours: number = 24): Promise<CleanupResult> {
  const result: CleanupResult = {
    success: true,
    taskName: 'temp-file-cleanup',
    itemsRemoved: 0,
    errors: [],
    timestamp: new Date().toISOString()
  };
  
  try {
    // Ensure the directory exists
    try {
      await fs.access(directory);
    } catch {
      result.errors.push(`Directory ${directory} does not exist or is not accessible`);
      result.success = false;
      return result;
    }
    
    // Get all files in the directory
    const files = await fs.readdir(directory);
    
    // Current time
    const now = Date.now();
    const maxAgeMs = maxAgeHours * 60 * 60 * 1000;
    
    // Process each file
    for (const file of files) {
      try {
        const filePath = path.join(directory, file);
        
        // Get file stats
        const stats = await fs.stat(filePath);
        
        // Skip directories
        if (stats.isDirectory()) {
          continue;
        }
        
        // Check file age
        const fileAgeMs = now - stats.mtimeMs;
        
        if (fileAgeMs > maxAgeMs) {
          // Remove file if it's older than maxAgeHours
          await fs.unlink(filePath);
          result.itemsRemoved++;
        }
      } catch (error) {
        result.errors.push(`Error processing file ${file}: ${error instanceof Error ? error.message : String(error)}`);
      }
    }
  } catch (error) {
    result.success = false;
    result.errors.push(`Failed to clean temp directory: ${error instanceof Error ? error.message : String(error)}`);
  }
  
  return result;
}

/**
 * Clean up unused compiled modules to free disk space
 * @returns Cleanup results
 */
export async function cleanupCompiledModules(): Promise<CleanupResult> {
  const result: CleanupResult = {
    success: true,
    taskName: 'compiled-modules-cleanup',
    itemsRemoved: 0,
    errors: [],
    timestamp: new Date().toISOString()
  };
  
  try {
    // This function would identify and remove compiled modules that are no longer needed
    // For example, old build artifacts, temporary compilation directories, etc.
    
    // For now, returning placeholder result as the specific implementation
    // would depend on the project's compilation process
    result.itemsRemoved = 0;
  } catch (error) {
    result.success = false;
    result.errors.push(`Failed to clean compiled modules: ${error instanceof Error ? error.message : String(error)}`);
  }
  
  return result;
}

/**
 * Schedule periodic maintenance tasks
 * @param tempDir Directory containing temporary files to clean
 * @returns Cleanup function to cancel scheduled tasks
 */
export function scheduleMaintenanceTasks(tempDir: string = path.join(__dirname, '../.temp')): () => void {
  // Schedule temp file cleanup every 24 hours
  const tempCleanupInterval = setInterval(async () => {
    try {
      console.log('[Maintenance] Running scheduled temp file cleanup');
      const result = await cleanupTempFiles(tempDir, 24);
      
      if (result.success) {
        console.log(`[Maintenance] Temp cleanup completed: removed ${result.itemsRemoved} files`);
      } else {
        console.error('[Maintenance] Temp cleanup failed:', result.errors.join(', '));
      }
    } catch (error) {
      console.error('[Maintenance] Error during temp cleanup:', 
        error instanceof Error ? error.message : String(error));
    }
  }, 24 * 60 * 60 * 1000); // 24 hours
  
  // Schedule unused module cleanup weekly
  const moduleCleanupInterval = setInterval(async () => {
    try {
      console.log('[Maintenance] Running scheduled module cleanup');
      const result = await cleanupCompiledModules();
      
      if (result.success) {
        console.log(`[Maintenance] Module cleanup completed: removed ${result.itemsRemoved} items`);
      } else {
        console.error('[Maintenance] Module cleanup failed:', result.errors.join(', '));
      }
    } catch (error) {
      console.error('[Maintenance] Error during module cleanup:', 
        error instanceof Error ? error.message : String(error));
    }
  }, 7 * 24 * 60 * 60 * 1000); // 7 days
  
  // Return cleanup function
  return () => {
    clearInterval(tempCleanupInterval);
    clearInterval(moduleCleanupInterval);
    console.log('[Maintenance] Scheduled maintenance tasks canceled');
  };
}