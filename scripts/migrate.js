import { drizzle } from "drizzle-orm/neon-serverless";
import { Pool, neonConfig } from "@neondatabase/serverless";
import ws from "ws";
import { pgTable, serial, text } from "drizzle-orm/pg-core";

// Required for Neon serverless
neonConfig.webSocketConstructor = ws;

const runMigration = async () => {
  const connectionString = process.env.DATABASE_URL;
  
  if (!connectionString) {
    console.error("DATABASE_URL environment variable is not set");
    process.exit(1);
  }

  console.log("Connecting to database...");
  const pool = new Pool({ connectionString });
  
  try {
    const db = drizzle(pool);
    
    console.log("Creating tables if they don't exist...");
    
    // Create users table
    await db.execute(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL
      );
    `);
    
    // Create mods table
    await db.execute(`
      CREATE TABLE IF NOT EXISTS mods (
        id SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL,
        name TEXT NOT NULL,
        mod_id TEXT NOT NULL,
        description TEXT NOT NULL,
        version TEXT NOT NULL,
        minecraft_version TEXT NOT NULL,
        license TEXT NOT NULL,
        mod_loader TEXT NOT NULL,
        idea TEXT NOT NULL,
        feature_priority TEXT NOT NULL,
        coding_style TEXT NOT NULL,
        compile_frequency TEXT NOT NULL,
        auto_fix_level TEXT NOT NULL,
        auto_push_to_github BOOLEAN NOT NULL,
        generate_documentation BOOLEAN NOT NULL,
        github_repo TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
      );
    `);
    
    // Create builds table
    await db.execute(`
      CREATE TABLE IF NOT EXISTS builds (
        id SERIAL PRIMARY KEY,
        mod_id INTEGER NOT NULL,
        build_number INTEGER NOT NULL,
        status TEXT NOT NULL,
        error_count INTEGER NOT NULL DEFAULT 0,
        warning_count INTEGER NOT NULL DEFAULT 0,
        logs TEXT NOT NULL,
        download_url TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
        completed_at TIMESTAMP
      );
    `);
    
    // Create mod_files table
    await db.execute(`
      CREATE TABLE IF NOT EXISTS mod_files (
        id SERIAL PRIMARY KEY,
        mod_id INTEGER NOT NULL,
        path TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
      );
    `);
    
    console.log("Creating default admin user if it doesn't exist...");
    await db.execute(`
      INSERT INTO users (username, password)
      VALUES ('admin', 'admin')
      ON CONFLICT (username) DO NOTHING;
    `);
    
    console.log("Schema migration completed successfully!");
  } catch (error) {
    console.error("Error during migration:", error);
    process.exit(1);
  } finally {
    await pool.end();
  }
};

runMigration();