import { db } from "./db";
import { sql } from "drizzle-orm";

/**
 * Check the health of the database connection
 * @returns {Promise<{ status: string; message: string; }>} Database health status
 */
export async function checkDatabaseHealth(): Promise<{ status: string; message: string; error?: any }> {
  try {
    // Execute a simple query to test the database connection
    const result = await db.execute(sql`SELECT 1 as health_check`);
    
    // Check if the query executed successfully
    if (result) {
      return { 
        status: "healthy", 
        message: "Database connection is healthy" 
      };
    } else {
      return { 
        status: "unhealthy", 
        message: "Database query returned unexpected result" 
      };
    }
  } catch (error) {
    console.error("Database health check failed:", error);
    return { 
      status: "error", 
      message: "Failed to connect to database", 
      error: error instanceof Error ? error.message : String(error) 
    };
  }
}