import { db } from "./db";
import { sql } from "drizzle-orm";
import { QueryResult } from "@neondatabase/serverless";

// Define the expected query result types
interface VersionQueryResult {
  version: string;
}

interface UptimeQueryResult {
  uptime: number | string;
}

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
    // First check basic connectivity
    const connectionResult = await db.execute(sql`SELECT 1 as health_check`);
    
    if (!connectionResult) {
      return { 
        status: "unhealthy", 
        message: "Database query returned unexpected result",
        timestamp
      };
    }
    
    // Get database information
    try {
      // Execute queries with proper type casting
      const versionResult = await db.execute<VersionQueryResult>(sql`SELECT version() as version`);
      const uptimeResult = await db.execute<UptimeQueryResult>(sql`SELECT extract(epoch from current_timestamp - pg_postmaster_start_time()) as uptime`);
      
      // Extract result data safely
      const versionRows = versionResult.rows || [];
      const uptimeRows = uptimeResult.rows || [];
      
      const databaseVersion = versionRows.length > 0 ? versionRows[0].version : 'Unknown';
      const uptime = uptimeRows.length > 0 ? Number(uptimeRows[0].uptime) : 0;
      
      return { 
        status: "healthy", 
        message: "Database connection is healthy", 
        databaseInfo: {
          version: databaseVersion,
          connection: process.env.DATABASE_URL?.split('@')[1]?.split('/')[0] || 'Unknown',
          uptime: Number(uptime)
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