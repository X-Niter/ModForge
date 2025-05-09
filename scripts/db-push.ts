import { drizzle } from "drizzle-orm/neon-serverless";
import { migrate } from "drizzle-orm/neon-serverless/migrator";
import { Pool, neonConfig } from "@neondatabase/serverless";
import ws from "ws";
import * as schema from "../shared/schema";

// Required for Neon serverless
neonConfig.webSocketConstructor = ws;

async function main() {
  const connectionString = process.env.DATABASE_URL;
  
  if (!connectionString) {
    console.error("DATABASE_URL environment variable is not set");
    process.exit(1);
  }

  console.log("Connecting to database...");
  const pool = new Pool({ connectionString });
  const db = drizzle(pool, { schema });
  
  try {
    console.log("Pushing schema changes to database...");
    
    // Using raw SQL queries to manage schema because 
    // drizzle-kit doesn't support direct schema push yet in this environment
    
    // Create the tables if they don't exist
    await db.execute(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL
      );
      
      CREATE TABLE IF NOT EXISTS mods (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        mod_id TEXT NOT NULL,
        description TEXT,
        version TEXT NOT NULL,
        minecraft_version TEXT NOT NULL,
        license TEXT NOT NULL,
        mod_loader TEXT NOT NULL,
        idea TEXT NOT NULL,
        feature_priority TEXT,
        coding_style TEXT,
        compile_frequency TEXT NOT NULL,
        auto_fix_level TEXT NOT NULL,
        auto_push_to_github BOOLEAN NOT NULL,
        generate_documentation BOOLEAN NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
      );
      
      CREATE TABLE IF NOT EXISTS builds (
        id SERIAL PRIMARY KEY,
        mod_id INTEGER NOT NULL REFERENCES mods(id) ON DELETE CASCADE,
        build_number INTEGER NOT NULL,
        status TEXT NOT NULL,
        error_count INTEGER NOT NULL,
        warning_count INTEGER NOT NULL,
        logs TEXT,
        download_url TEXT,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
      
      CREATE TABLE IF NOT EXISTS mod_files (
        id SERIAL PRIMARY KEY,
        mod_id INTEGER NOT NULL REFERENCES mods(id) ON DELETE CASCADE,
        path TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
    `);
    
    console.log("✓ Database schema updated successfully");
    
    // Create admin user if it doesn't exist
    const adminResult = await db.execute(`
      INSERT INTO users (username, password)
      VALUES ('admin', 'admin')
      ON CONFLICT (username) DO NOTHING
      RETURNING id;
    `);
    
    if (adminResult.length > 0) {
      console.log("✓ Admin user created");
    } else {
      console.log("✓ Admin user already exists");
    }
    
    console.log("Database migration completed successfully!");
  } catch (error) {
    console.error("Error during database migration:", error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

main();