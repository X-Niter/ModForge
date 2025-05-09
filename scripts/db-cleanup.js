import { drizzle } from "drizzle-orm/neon-serverless";
import { Pool, neonConfig } from "@neondatabase/serverless";
import ws from "ws";

// Required for Neon serverless
neonConfig.webSocketConstructor = ws;

/**
 * This utility script is for development and testing purposes.
 * It can be used to clean up the database tables while preserving the admin user.
 */
const cleanupDatabase = async () => {
  const connectionString = process.env.DATABASE_URL;
  
  if (!connectionString) {
    console.error("DATABASE_URL environment variable is not set");
    process.exit(1);
  }

  console.log("Connecting to database...");
  const pool = new Pool({ connectionString });
  
  try {
    const db = drizzle(pool);
    
    console.log("Cleaning up database tables...");
    
    // Delete all mod_files
    await db.execute(`DELETE FROM mod_files;`);
    console.log("✓ Cleared mod_files table");
    
    // Delete all builds
    await db.execute(`DELETE FROM builds;`);
    console.log("✓ Cleared builds table");
    
    // Delete all mods
    await db.execute(`DELETE FROM mods;`);
    console.log("✓ Cleared mods table");
    
    // Delete all users except admin
    await db.execute(`DELETE FROM users WHERE username != 'admin';`);
    console.log("✓ Cleared non-admin users");
    
    console.log("Database cleanup completed successfully!");
  } catch (error) {
    console.error("Error during database cleanup:", error);
    process.exit(1);
  } finally {
    await pool.end();
  }
};

cleanupDatabase();