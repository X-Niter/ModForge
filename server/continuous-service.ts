import { storage } from './storage';
import { compileMod } from './compiler';
import { fixCompilationErrors, addModFeatures } from './ai-service';
import { EventEmitter } from 'events';

// Map client-side BuildStatus enum to string values expected by the database
enum BuildStatus {
  Queued = "queued",
  InProgress = "in_progress",
  Success = "succeeded",
  Failed = "failed"
}

/**
 * Custom logger for the continuous development service
 */
function logContinuous(modId: number, message: string, type: 'info' | 'warn' | 'error' = 'info') {
  const timestamp = new Date().toISOString();
  const prefix = `[ContinuousDev:${modId}]`;
  
  if (type === 'error') {
    console.error(`${timestamp} ${prefix} ERROR: ${message}`);
  } else if (type === 'warn') {
    console.warn(`${timestamp} ${prefix} WARN: ${message}`);
  } else {
    console.log(`${timestamp} ${prefix} ${message}`);
  }
}

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
    // Circuit breaker pattern to prevent infinite loops
    const maxRetries = 3;
    let attempts = 0;
    const circuitBreakerKey = `circuit_breaker_${modId}`;
    
    // Check if circuit breaker is tripped
    const circuitBreakerCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
    if (circuitBreakerCount >= 5) {
      logContinuous(modId, `Circuit breaker tripped. Too many failures in a short period.`, 'warn');
      // Wait for at least an hour before trying again
      const lastTripTime = parseInt(process.env[`${circuitBreakerKey}_time`] || '0', 10);
      const now = Date.now();
      if (now - lastTripTime < 60 * 60 * 1000) {
        logContinuous(modId, `Skipping processing until circuit breaker resets.`, 'warn');
        return;
      } else {
        // Reset circuit breaker
        process.env[circuitBreakerKey] = '0';
        delete process.env[`${circuitBreakerKey}_time`];
        logContinuous(modId, `Circuit breaker reset after timeout period.`);
      }
    }
    
    try {
      // Get the mod
      const mod = await storage.getMod(modId);
      if (!mod) {
        logContinuous(modId, `Mod not found`, 'error');
        this.stopContinuousDevelopment(modId);
        return;
      }
      
      // Increment build count
      const buildCount = (this.buildCounts.get(modId) || 0) + 1;
      this.buildCounts.set(modId, buildCount);
      
      logContinuous(modId, `Starting build #${buildCount} for mod ${mod.name}`);
      
      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber: buildCount,
        version: mod.version || "0.1.0", // Use mod version or default
        status: BuildStatus.InProgress,
        errors: [],
        errorCount: 0,
        warningCount: 0,
        logs: `Starting continuous build #${buildCount} for ${mod.name}...\n`,
        downloadUrl: null,
        metadata: {},
        isAutomatic: true
      });
      
      // Compile the mod
      logContinuous(modId, `Compiling mod ${mod.name}`);
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
        
        // Fix errors with retry logic
        attempts++;
        logContinuous(modId, `Fixing errors for mod ${mod.name} - Attempt ${attempts}/${maxRetries}`);
        
        let fixResult;
        try {
          fixResult = await fixCompilationErrors(files, compileResult.errors);
        } catch (error) {
          // Type-safe error handling 
          const fixError = error as Error;
          logContinuous(modId, `Error fixing compilation errors: ${fixError.message}`, 'error');
          currentLogs += `\nError during auto-fix attempt: ${fixError.message || String(fixError)}\n`;
          await storage.updateBuild(build.id, { 
            logs: currentLogs,
            status: BuildStatus.Failed
          });
          throw fixError;
        }
        
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
              content: file.content,
              contentType: file.path.endsWith('.java') ? 'text/x-java' : 'text/plain',
              metadata: {}
            });
          }
        }
        
        // Try compilation again
        currentLogs += "\nRetrying compilation after fixes...\n";
        await storage.updateBuild(build.id, { logs: currentLogs });
        
        logContinuous(modId, `Recompiling mod ${mod.name} after fixes`);
        const retryResult = await compileMod(modId);
        
        // Update build with new compilation results
        const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
        currentLogs += retryResult.logs;
        
        await storage.updateBuild(build.id, {
          status: newStatus,
          logs: currentLogs,
          errorCount: retryResult.errors.length,
          warningCount: retryResult.warnings.length,
          downloadUrl: retryResult.downloadUrl || undefined
        });
        
        // If we're using aggressive auto-fix and still have errors, try adding new features
        if (newStatus === BuildStatus.Failed && mod.autoFixLevel === "Aggressive") {
          // We might add more advanced feature development here
          // For now, we'll just report the continuous cycle
          logContinuous(modId, `Continuous development cycle for mod ${mod.name} completed with errors`, 'warn');
          
          // Emit the build completed event with success status
          this.emit('buildCompleted', {
            modId: mod.id,
            buildId: build.id,
            success: false,
            buildNumber: buildCount
          });
        } else {
          logContinuous(modId, `Continuous development cycle for mod ${mod.name} completed successfully`);
          
          // Reset circuit breaker on successful build
          const circuitBreakerKey = `circuit_breaker_${modId}`;
          process.env[circuitBreakerKey] = '0';
          if (process.env[`${circuitBreakerKey}_time`]) {
            delete process.env[`${circuitBreakerKey}_time`];
          }
          
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
          downloadUrl: compileResult.downloadUrl || undefined
        });
        
        console.log(`Continuous development cycle for mod ${mod.name} completed with status: ${status}`);
        
        // Reset circuit breaker if build is successful
        if (compileResult.success) {
          const circuitBreakerKey = `circuit_breaker_${modId}`;
          process.env[circuitBreakerKey] = '0';
          if (process.env[`${circuitBreakerKey}_time`]) {
            delete process.env[`${circuitBreakerKey}_time`];
          }
        }
        
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
      
      // Increment circuit breaker count
      const circuitBreakerKey = `circuit_breaker_${modId}`;
      const currentCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
      process.env[circuitBreakerKey] = (currentCount + 1).toString();
      
      // If circuit breaker threshold reached, trip it
      if (currentCount + 1 >= 5) {
        process.env[`${circuitBreakerKey}_time`] = Date.now().toString();
        console.warn(`Circuit breaker tripped for mod ${modId}.`);
      }
      
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