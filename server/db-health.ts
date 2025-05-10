import { db } from "./db";
import { sql } from "drizzle-orm";
import { pool } from "./db";

interface HealthCheckResult {
  status: 'healthy' | 'unhealthy' | 'error';
  message: string;
  databaseInfo?: {
    version?: string;
    connection?: string;
    uptime?: number;
  };
  error?: any;
  timestamp: string;
}

/**
 * Check the health of the database connection
 * @returns {Promise<HealthCheckResult>} Detailed database health status
 */
export async function checkDatabaseHealth(): Promise<HealthCheckResult> {
  const timestamp = new Date().toISOString();
  
  try {
    // First check basic connectivity using the pool directly
    const connectionCheck = await pool.query('SELECT 1 as health_check');
    
    if (!connectionCheck || connectionCheck.rows.length === 0) {
      return { 
        status: "unhealthy", 
        message: "Database query returned unexpected result",
        timestamp
      };
    }
    
    // Get database information
    try {
      // Query database version and uptime
      const versionResult = await pool.query('SELECT version() as version');
      const uptimeResult = await pool.query('SELECT extract(epoch from current_timestamp - pg_postmaster_start_time()) as uptime');
      
      const databaseVersion = versionResult.rows[0]?.version || 'Unknown';
      const uptime = Number(uptimeResult.rows[0]?.uptime || 0);
      
      // Sanitize the connection string for display
      const connectionString = process.env.DATABASE_URL || '';
      const sanitizedConnection = connectionString.includes('@') 
        ? connectionString.split('@')[1]?.split('/')[0] || 'Unknown'
        : 'Connected';
      
      return { 
        status: "healthy", 
        message: "Database connection is healthy", 
        databaseInfo: {
          version: databaseVersion,
          connection: sanitizedConnection,
          uptime: uptime
        },
        timestamp
      };
    } catch (infoError) {
      // Even if we can't get detailed info, the database is still connected
      return { 
        status: "healthy", 
        message: "Database connection is healthy, but couldn't retrieve detailed information",
        timestamp,
        error: infoError instanceof Error ? infoError.message : String(infoError)
      };
    }
  } catch (error) {
    console.error("Database health check failed:", error);
    return { 
      status: "error", 
      message: "Failed to connect to database", 
      error: error instanceof Error 
        ? {
            message: error.message,
            name: error.name,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
          } 
        : String(error),
      timestamp
    };
  }
}