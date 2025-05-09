import { storage } from './storage';
import { compileMod } from './compiler';
import { fixCompilationErrors, addModFeatures } from './ai-service';
import { BuildStatus } from '@/types';
import { EventEmitter } from 'events';

/**
 * Service that manages continuous mod development
 * Runs in the background to continuously compile, fix errors, and improve mods
 */
class ContinuousService extends EventEmitter {
  private running: Map<number, boolean> = new Map();
  private intervals: Map<number, NodeJS.Timeout> = new Map();
  private buildCounts: Map<number, number> = new Map();
  
  /**
   * Start continuous development for a mod
   * @param modId The mod ID to continuously develop
   * @param frequency How often to check for changes and compile (in milliseconds)
   */
  public startContinuousDevelopment(modId: number, frequency: number = 5 * 60 * 1000): void {
    // Don't start if already running
    if (this.running.get(modId)) {
      return;
    }
    
    this.running.set(modId, true);
    this.buildCounts.set(modId, 0);
    console.log(`Starting continuous development for mod ${modId} at ${frequency}ms intervals`);
    
    // Initial build
    this.processMod(modId).catch(err => {
      console.error(`Error in initial build for mod ${modId}:`, err);
    });
    
    // Set up interval for future builds
    const interval = setInterval(() => {
      if (!this.running.get(modId)) {
        clearInterval(interval);
        return;
      }
      
      this.processMod(modId).catch(err => {
        console.error(`Error in continuous development for mod ${modId}:`, err);
      });
    }, frequency);
    
    this.intervals.set(modId, interval);
    this.emit('started', modId);
  }
  
  /**
   * Stop continuous development for a mod
   * @param modId The mod ID to stop developing
   */
  public stopContinuousDevelopment(modId: number): void {
    if (!this.running.get(modId)) {
      return;
    }
    
    console.log(`Stopping continuous development for mod ${modId}`);
    this.running.set(modId, false);
    
    const interval = this.intervals.get(modId);
    if (interval) {
      clearInterval(interval);
      this.intervals.delete(modId);
    }
    
    this.emit('stopped', modId);
  }
  
  /**
   * Check if continuous development is running for a mod
   * @param modId The mod ID to check
   * @returns Whether continuous development is running
   */
  public isRunning(modId: number): boolean {
    return !!this.running.get(modId);
  }
  
  /**
   * Process a mod: compile, fix errors, and possibly add features
   * @param modId The mod ID to process
   */
  private async processMod(modId: number): Promise<void> {
    try {
      // Get the mod
      const mod = await storage.getMod(modId);
      if (!mod) {
        console.error(`Mod ${modId} not found`);
        this.stopContinuousDevelopment(modId);
        return;
      }
      
      // Increment build count
      const buildCount = (this.buildCounts.get(modId) || 0) + 1;
      this.buildCounts.set(modId, buildCount);
      
      console.log(`Starting build #${buildCount} for mod ${mod.name} (${modId})`);
      
      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber: buildCount,
        status: BuildStatus.InProgress,
        errorCount: 0,
        warningCount: 0,
        logs: `Starting continuous build #${buildCount} for ${mod.name}...\n`,
        downloadUrl: null
      });
      
      // Compile the mod
      console.log(`Compiling mod ${mod.name} (${modId})`);
      const compileResult = await compileMod(modId);
      
      // Update build logs
      let currentLogs = build.logs + compileResult.logs;
      await storage.updateBuild(build.id, {
        logs: currentLogs
      });
      
      // If compilation failed and auto-fix is enabled, try to fix errors
      if (!compileResult.success && compileResult.errors.length > 0 && 
          (mod.autoFixLevel === "Balanced" || mod.autoFixLevel === "Aggressive")) {
        
        // Add fix logs
        currentLogs += "\nAttempting to fix compilation errors...\n";
        await storage.updateBuild(build.id, { logs: currentLogs });
        
        // Get all mod files
        const modFiles = await storage.getModFilesByModId(modId);
        const files = modFiles.map(file => ({
          path: file.path,
          content: file.content
        }));
        
        // Fix errors
        console.log(`Fixing errors for mod ${mod.name} (${modId})`);
        const fixResult = await fixCompilationErrors(files, compileResult.errors);
        
        // Update logs
        currentLogs += fixResult.logs;
        await storage.updateBuild(build.id, { logs: currentLogs });
        
        // Update mod files with fixes
        for (const file of fixResult.files) {
          const existingFile = modFiles.find(f => f.path === file.path);
          if (existingFile) {
            await storage.updateModFile(existingFile.id, {
              content: file.content
            });
          } else {
            await storage.createModFile({
              modId: mod.id,
              path: file.path,
              content: file.content
            });
          }
        }
        
        // Try compilation again
        currentLogs += "\nRetrying compilation after fixes...\n";
        await storage.updateBuild(build.id, { logs: currentLogs });
        
        console.log(`Recompiling mod ${mod.name} (${modId}) after fixes`);
        const retryResult = await compileMod(modId);
        
        // Update build with new compilation results
        const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
        currentLogs += retryResult.logs;
        
        await storage.updateBuild(build.id, {
          status: newStatus,
          logs: currentLogs,
          errorCount: retryResult.errors.length,
          warningCount: retryResult.warnings.length,
          downloadUrl: retryResult.downloadUrl
        });
        
        // If we're using aggressive auto-fix and still have errors, try adding new features
        if (newStatus === BuildStatus.Failed && mod.autoFixLevel === "Aggressive") {
          // We might add more advanced feature development here
          // For now, we'll just report the continuous cycle
          console.log(`Continuous development cycle for mod ${mod.name} completed with errors`);
          
          // Emit the build completed event with success status
          this.emit('buildCompleted', {
            modId: mod.id,
            buildId: build.id,
            success: false,
            buildNumber: buildCount
          });
        } else {
          console.log(`Continuous development cycle for mod ${mod.name} completed successfully`);
          
          // Emit the build completed event with success status
          this.emit('buildCompleted', {
            modId: mod.id,
            buildId: build.id,
            success: true,
            buildNumber: buildCount
          });
        }
      } else {
        // Compilation succeeded or no auto-fix
        const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
        
        await storage.updateBuild(build.id, {
          status,
          logs: currentLogs,
          errorCount: compileResult.errors.length,
          warningCount: compileResult.warnings.length,
          downloadUrl: compileResult.downloadUrl
        });
        
        console.log(`Continuous development cycle for mod ${mod.name} completed with status: ${status}`);
        
        // Emit the build completed event with success status
        this.emit('buildCompleted', {
          modId: mod.id,
          buildId: build.id,
          success: compileResult.success,
          buildNumber: buildCount
        });
      }
    } catch (error) {
      console.error(`Error in continuous development cycle for mod ${modId}:`, error);
      
      // Emit the error event
      this.emit('error', {
        modId,
        error: error instanceof Error ? error.message : String(error)
      });
    }
  }
  
  /**
   * Get statistics about the continuous development process
   * @param modId The mod ID to get statistics for
   */
  public getStatistics(modId: number) {
    return {
      isRunning: this.isRunning(modId),
      buildCount: this.buildCounts.get(modId) || 0
    };
  }
}

// Singleton instance
export const continuousService = new ContinuousService();