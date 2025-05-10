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
  private circuitBreakerMaintenanceInterval: NodeJS.Timeout | null = null;
  
  constructor() {
    super();
    
    // Set up periodic circuit breaker maintenance
    this.startCircuitBreakerMaintenance();
  }
  
  /**
   * Start a periodic task to check for and reset expired circuit breakers
   * This helps ensure that temporary issues don't permanently stop continuous development
   */
  private startCircuitBreakerMaintenance() {
    // Check every 15 minutes
    this.circuitBreakerMaintenanceInterval = setInterval(() => {
      try {
        const now = Date.now();
        const circuitBreakerTimeKeys = Object.keys(process.env)
          .filter(key => key.startsWith('circuit_breaker_') && key.endsWith('_time'));
          
        for (const timeKey of circuitBreakerTimeKeys) {
          const tripTime = parseInt(process.env[timeKey] || '0', 10);
          const resetThreshold = 60 * 60 * 1000; // 1 hour
          
          // If the circuit breaker has been tripped for more than the threshold, reset it
          if (tripTime > 0 && now - tripTime >= resetThreshold) {
            const modIdStr = timeKey.replace('circuit_breaker_', '').replace('_time', '');
            const modId = parseInt(modIdStr, 10);
            
            if (!isNaN(modId)) {
              const circuitBreakerKey = `circuit_breaker_${modId}`;
              process.env[circuitBreakerKey] = '0';
              delete process.env[timeKey];
              
              logContinuous(modId, "Circuit breaker automatically reset after timeout period", 'info');
            }
          }
        }
      } catch (error) {
        // Don't let maintenance errors crash the service
        console.error("Error in circuit breaker maintenance:", 
          error instanceof Error ? error.message : String(error));
      }
    }, 15 * 60 * 1000);
  }
  
  /**
   * Start continuous development for a mod
   * @param modId The mod ID to continuously develop
   * @param frequency How often to check for changes and compile (in milliseconds)
   * @returns Object with success status and any relevant message
   */
  public startContinuousDevelopment(modId: number, frequency: number = 5 * 60 * 1000): {success: boolean, message?: string} {
    // Don't start if already running
    if (this.running.get(modId)) {
      return {success: false, message: "Continuous development is already running for this mod"};
    }
    
    // Check circuit breaker status
    const circuitBreakerKey = `circuit_breaker_${modId}`;
    const circuitBreakerCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
    const lastTripTime = parseInt(process.env[`${circuitBreakerKey}_time`] || '0', 10);
    
    if (circuitBreakerCount >= 5 && lastTripTime > 0) {
      const now = Date.now();
      if (now - lastTripTime < 60 * 60 * 1000) {
        const remainingTimeMinutes = Math.ceil((60 * 60 * 1000 - (now - lastTripTime)) / (60 * 1000));
        return {
          success: false, 
          message: `Circuit breaker is tripped for this mod due to previous errors. Automatic retry in ${remainingTimeMinutes} minutes.`
        };
      } else {
        // Reset circuit breaker since timeout has elapsed
        process.env[circuitBreakerKey] = '0';
        delete process.env[`${circuitBreakerKey}_time`];
      }
    }
    
    this.running.set(modId, true);
    this.buildCounts.set(modId, 0);
    logContinuous(modId, `Starting continuous development at ${frequency}ms intervals`);
    
    // Initial build
    this.processMod(modId).catch(err => {
      logContinuous(modId, `Error in initial build: ${err instanceof Error ? err.message : String(err)}`, 'error');
    });
    
    // Set up interval for future builds
    const interval = setInterval(() => {
      if (!this.running.get(modId)) {
        clearInterval(interval);
        return;
      }
      
      this.processMod(modId).catch(err => {
        logContinuous(modId, `Error in scheduled build: ${err instanceof Error ? err.message : String(err)}`, 'error');
      });
    }, frequency);
    
    this.intervals.set(modId, interval);
    this.emit('started', modId);
    
    return {success: true};
  }
  
  /**
   * Stop continuous development for a mod
   * @param modId The mod ID to stop developing
   * @returns Object with success status and any relevant message
   */
  public stopContinuousDevelopment(modId: number): {success: boolean, message?: string} {
    if (!this.running.get(modId)) {
      return {success: false, message: "Continuous development is not running for this mod"};
    }
    
    logContinuous(modId, `Stopping continuous development`);
    this.running.set(modId, false);
    
    const interval = this.intervals.get(modId);
    if (interval) {
      clearInterval(interval);
      this.intervals.delete(modId);
    }
    
    // Gracefully handle any in-progress builds
    try {
      // Find any in-progress builds and mark them as stopped
      storage.getBuildsByModId(modId).then(builds => {
        const inProgressBuilds = builds.filter(b => b.status === BuildStatus.InProgress || b.status === BuildStatus.Queued);
        
        for (const build of inProgressBuilds) {
          storage.updateBuild(build.id, {
            status: BuildStatus.Failed,
            logs: build.logs + '\n\nBuild stopped by user or system.'
          }).catch(err => {
            logContinuous(modId, `Error cleaning up build ${build.id}: ${err instanceof Error ? err.message : String(err)}`, 'error');
          });
        }
      }).catch(err => {
        logContinuous(modId, `Error retrieving builds during shutdown: ${err instanceof Error ? err.message : String(err)}`, 'error');
      });
    } catch (err) {
      // Don't let cleanup errors prevent stopping
      logContinuous(modId, `Error during cleanup: ${err instanceof Error ? err.message : String(err)}`, 'error');
    }
    
    this.emit('stopped', modId);
    return {success: true};
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
   * Includes auto-retry logic for transient errors
   * @param modId The mod ID to process
   * @param retryCount Current retry attempt (for internal use)
   */
  private async processMod(modId: number, retryCount: number = 0): Promise<void> {
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
        
        logContinuous(modId, `Continuous development cycle for mod ${mod.name} completed with status: ${status}`, status === BuildStatus.Failed ? 'warn' : 'info');
        
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
      const errorMessage = error instanceof Error ? error.message : String(error);
      logContinuous(modId, `Error in continuous development cycle: ${errorMessage}`, 'error');
      
      // Implement retry for transient errors before incrementing circuit breaker
      // Only retry for specific transient errors that might resolve themselves
      const isTransientError = error instanceof Error && (
        error.message.includes('ECONNREFUSED') ||
        error.message.includes('ETIMEDOUT') ||
        error.message.includes('network') ||
        error.message.includes('temporary')
      );
      
      // Retry logic for transient errors
      if (isTransientError && retryCount < 2) {
        const nextRetry = retryCount + 1;
        const delayMs = nextRetry * 5000; // Exponential backoff: 5s, 10s
        
        logContinuous(modId, `Transient error detected. Retry ${nextRetry}/2 scheduled in ${delayMs/1000}s`, 'warn');
        
        // Schedule retry with delay
        setTimeout(() => {
          if (this.isRunning(modId)) {
            this.processMod(modId, nextRetry).catch(retryError => {
              logContinuous(modId, `Retry ${nextRetry} failed: ${retryError instanceof Error ? retryError.message : String(retryError)}`, 'error');
            });
          }
        }, delayMs);
        
        return; // Exit to prevent duplicate processing
      }
      
      // If all retries failed or it's not a transient error, increment circuit breaker
      const circuitBreakerKey = `circuit_breaker_${modId}`;
      const currentCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
      process.env[circuitBreakerKey] = (currentCount + 1).toString();
      
      // If circuit breaker threshold reached, trip it
      if (currentCount + 1 >= 5) {
        process.env[`${circuitBreakerKey}_time`] = Date.now().toString();
        logContinuous(modId, `Circuit breaker tripped after ${currentCount + 1} consecutive failures.`, 'warn');
        
        try {
          // Update any ongoing builds to failed status
          const mod = await storage.getMod(modId);
          if (mod) {
            const builds = await storage.getBuildsByModId(modId);
            const inProgressBuilds = builds.filter(b => b.status === BuildStatus.InProgress);
            
            for (const build of inProgressBuilds) {
              await storage.updateBuild(build.id, {
                status: BuildStatus.Failed,
                logs: build.logs + '\n\nBuild terminated due to circuit breaker activation after multiple failures.'
              });
            }
          }
        } catch (innerError) {
          // Don't let this error handling cause another error
          logContinuous(modId, `Error while cleaning up builds after circuit breaker trip: ${innerError instanceof Error ? innerError.message : String(innerError)}`, 'error');
        }
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
    // Get circuit breaker status
    const circuitBreakerKey = `circuit_breaker_${modId}`;
    const circuitBreakerCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
    const circuitBreakerTripped = circuitBreakerCount >= 5;
    
    // Get circuit breaker trip time if applicable
    let circuitBreakerResetTime = null;
    if (circuitBreakerTripped) {
      const lastTripTime = parseInt(process.env[`${circuitBreakerKey}_time`] || '0', 10);
      if (lastTripTime > 0) {
        // Calculate when the circuit breaker will reset (1 hour from trip time)
        const resetTime = new Date(lastTripTime + 60 * 60 * 1000);
        circuitBreakerResetTime = resetTime.toISOString();
      }
    }
    
    return {
      isRunning: this.isRunning(modId),
      buildCount: this.buildCounts.get(modId) || 0,
      circuitBreakerStatus: {
        tripped: circuitBreakerTripped,
        failureCount: circuitBreakerCount,
        resetTime: circuitBreakerResetTime
      }
    };
  }
  
  /**
   * Get the health status of the continuous development service
   */
  public getHealthStatus() {
    // Get a list of all running mods using a more compatible approach
    const runningMods: number[] = [];
    this.running.forEach((isRunning, modId) => {
      if (isRunning) {
        runningMods.push(modId);
      }
    });
    
    const trippedCircuitBreakers = Object.keys(process.env)
      .filter(key => key.startsWith('circuit_breaker_') && !key.includes('_time'))
      .filter(key => parseInt(process.env[key] || '0', 10) >= 5)
      .map(key => parseInt(key.replace('circuit_breaker_', ''), 10));
    
    return {
      status: 'healthy',
      runningMods,
      trippedCircuitBreakers,
      activeJobs: runningMods.length,
      timestamp: new Date().toISOString()
    };
  }
  
  /**
   * Cleanup resources when service is shutting down
   * This should be called when the application is gracefully shutting down
   */
  public cleanup() {
    // Clear the circuit breaker maintenance interval
    if (this.circuitBreakerMaintenanceInterval) {
      clearInterval(this.circuitBreakerMaintenanceInterval);
      this.circuitBreakerMaintenanceInterval = null;
    }
    
    // Shutdown all continuous development processes
    this.shutdownAll("Application shutting down");
    
    // Clear all intervals using a different approach to avoid TypeScript iterator issues
    this.intervals.forEach((interval, modId) => {
      clearInterval(interval);
      this.intervals.delete(modId);
    });
    
    console.log("[ContinuousService] Cleanup complete - all resources released");
  }
  
  /**
   * Shutdown all continuous development processes
   * @param reason Optional reason for the shutdown
   * @returns Object with success status and summary information
   */
  public shutdownAll(reason: string = "System maintenance"): {success: boolean, summary: {shutdownCount: number, failedIds: number[]}} {
    // Get a list of all running mods using a more compatible approach
    const runningMods: number[] = [];
    this.running.forEach((isRunning, modId) => {
      if (isRunning) {
        runningMods.push(modId);
      }
    });
    
    const failedIds: number[] = [];
    let shutdownCount = 0;
    
    // Attempt to stop each running mod
    for (const modId of runningMods) {
      try {
        this.running.set(modId, false);
        
        const interval = this.intervals.get(modId);
        if (interval) {
          clearInterval(interval);
          this.intervals.delete(modId);
        }
        
        // Log the shutdown
        logContinuous(modId, `Continuous development stopped during system shutdown: ${reason}`);
        
        // Update any in-progress builds
        storage.getBuildsByModId(modId).then(builds => {
          const inProgressBuilds = builds.filter(b => b.status === "in_progress" || b.status === "queued");
          
          for (const build of inProgressBuilds) {
            storage.updateBuild(build.id, {
              status: "failed",
              logs: build.logs + `\n\nBuild terminated during system shutdown: ${reason}`
            }).catch(err => {
              logContinuous(modId, `Error marking build as failed: ${err instanceof Error ? err.message : String(err)}`, 'error');
            });
          }
        }).catch(err => {
          logContinuous(modId, `Error retrieving builds during shutdown: ${err instanceof Error ? err.message : String(err)}`, 'error');
        });
        
        // Emit the stopped event
        this.emit('stopped', modId);
        shutdownCount++;
      } catch (error) {
        failedIds.push(modId);
      }
    }
    
    return {
      success: true,
      summary: {
        shutdownCount,
        failedIds
      }
    };
  }
  
  /**
   * Reset the circuit breaker for a mod
   * @param modId The mod ID to reset the circuit breaker for
   * @returns Object with success status and any relevant message
   */
  public resetCircuitBreaker(modId: number): {success: boolean, message?: string} {
    const circuitBreakerKey = `circuit_breaker_${modId}`;
    const circuitBreakerCount = parseInt(process.env[circuitBreakerKey] || '0', 10);
    const lastTripTime = parseInt(process.env[`${circuitBreakerKey}_time`] || '0', 10);
    
    if (circuitBreakerCount === 0 && lastTripTime === 0) {
      return {success: false, message: "Circuit breaker is not tripped for this mod"};
    }
    
    // Reset circuit breaker
    process.env[circuitBreakerKey] = '0';
    if (process.env[`${circuitBreakerKey}_time`]) {
      delete process.env[`${circuitBreakerKey}_time`];
    }
    
    logContinuous(modId, "Circuit breaker manually reset");
    
    return {success: true, message: "Circuit breaker reset successfully"};
  }
}

// Singleton instance
export const continuousService = new ContinuousService();