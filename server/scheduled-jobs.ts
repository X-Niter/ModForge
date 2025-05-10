/**
 * Scheduled Job Manager for ModForge
 * 
 * This module provides a centralized way to manage scheduled jobs and tasks
 * that need to run periodically in the background.
 */

import { getLogger } from "./logging";

// Set up logger
const logger = getLogger("scheduled-jobs");

// Map to track all running jobs
interface ScheduledJob {
  id: string;
  name: string;
  interval: number;
  timer: NodeJS.Timeout;
  lastRun: Date | null;
  nextRun: Date;
  failCount: number;
  enabled: boolean;
}

// Map of all scheduled jobs by ID
const scheduledJobs = new Map<string, ScheduledJob>();

// Generate a unique ID for a job
function generateJobId(name: string): string {
  return `${name}-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

/**
 * Schedule a job to run periodically
 * @param name Human-readable name of the job
 * @param interval Interval in milliseconds between job runs
 * @param jobFunction Function to execute when the job runs
 * @returns Cleanup function to cancel the scheduled job
 */
export function scheduleJob(
  name: string,
  interval: number,
  jobFunction: () => Promise<void>
): () => void {
  // Generate a unique ID for this job
  const id = generateJobId(name);
  
  // Calculate next run time
  const nextRun = new Date(Date.now() + interval);
  
  // Wrapper function to handle errors and tracking
  const runJob = async () => {
    const job = scheduledJobs.get(id);
    if (!job || !job.enabled) return;
    
    logger.debug(`Running scheduled job: ${name}`);
    
    job.lastRun = new Date();
    job.nextRun = new Date(Date.now() + interval);
    
    try {
      await jobFunction();
      job.failCount = 0; // Reset fail count on success
    } catch (error) {
      job.failCount++;
      logger.error(`Scheduled job ${name} failed`, { 
        error, 
        failCount: job.failCount 
      });
      
      // If a job fails too many times, we might want to disable it
      if (job.failCount >= 5) {
        logger.warn(`Disabling scheduled job ${name} after ${job.failCount} consecutive failures`);
        job.enabled = false;
      }
    }
  };
  
  // Create the timer
  const timer = setInterval(runJob, interval);
  
  // Track the job
  scheduledJobs.set(id, {
    id,
    name,
    interval,
    timer,
    lastRun: null,
    nextRun,
    failCount: 0,
    enabled: true
  });
  
  logger.info(`Scheduled job [${name}] to run every ${interval}ms`);
  
  // Run the job immediately for the first time
  runJob().catch(error => {
    logger.error(`Initial run of scheduled job ${name} failed`, { error });
  });
  
  // Return a cleanup function
  return () => {
    const job = scheduledJobs.get(id);
    if (job) {
      clearInterval(job.timer);
      scheduledJobs.delete(id);
      logger.info(`Stopped scheduled job: ${name}`);
    }
  };
}

/**
 * Get status information about all scheduled jobs
 */
export function getScheduledJobsStatus(): Array<{
  id: string;
  name: string;
  interval: number;
  lastRun: string | null;
  nextRun: string;
  failCount: number;
  enabled: boolean;
  status: 'healthy' | 'failing' | 'disabled';
}> {
  return Array.from(scheduledJobs.values()).map(job => ({
    id: job.id,
    name: job.name,
    interval: job.interval,
    lastRun: job.lastRun ? job.lastRun.toISOString() : null,
    nextRun: job.nextRun.toISOString(),
    failCount: job.failCount,
    enabled: job.enabled,
    status: !job.enabled ? 'disabled' : job.failCount > 0 ? 'failing' : 'healthy'
  }));
}

/**
 * Reset a failing job to allow it to run again
 * @param jobId ID of the job to reset
 * @returns Whether the reset was successful
 */
export function resetJob(jobId: string): boolean {
  const job = scheduledJobs.get(jobId);
  if (!job) return false;
  
  job.failCount = 0;
  job.enabled = true;
  job.nextRun = new Date(Date.now() + 1000); // Run again in 1 second
  
  logger.info(`Reset scheduled job: ${job.name}`);
  
  return true;
}

/**
 * Stop all scheduled jobs
 */
export function stopAllJobs(): void {
  logger.info(`Stopping all ${scheduledJobs.size} scheduled jobs`);
  
  scheduledJobs.forEach(job => {
    clearInterval(job.timer);
    logger.debug(`Stopped scheduled job: ${job.name}`);
  });
  
  scheduledJobs.clear();
  
  logger.info('All scheduled jobs stopped');
}