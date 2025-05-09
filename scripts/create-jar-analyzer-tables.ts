import { Pool, neonConfig } from '@neondatabase/serverless';
import ws from 'ws';
import { db } from '../server/db';
import { jarFiles, extractedClasses } from '../server/jar-analyzer-service';
import { drizzle } from 'drizzle-orm/neon-serverless';
import { migrate } from 'drizzle-orm/neon-serverless/migrator';

// Configure neon to use ws
neonConfig.webSocketConstructor = ws;

async function createJarAnalyzerTables() {
  try {
    console.log('Creating JAR analyzer tables...');
    
    if (!process.env.DATABASE_URL) {
      throw new Error('DATABASE_URL environment variable is not set');
    }

    const pool = new Pool({ connectionString: process.env.DATABASE_URL });
    
    // Create the tables using SQL directly to ensure they exist
    await pool.query(`
      CREATE TABLE IF NOT EXISTS jar_files (
        id SERIAL PRIMARY KEY,
        file_name TEXT NOT NULL,
        file_path TEXT,
        source TEXT NOT NULL,
        mod_loader TEXT NOT NULL,
        version TEXT,
        mc_version TEXT,
        extracted_class_count INTEGER NOT NULL DEFAULT 0,
        status TEXT NOT NULL DEFAULT 'pending',
        error_message TEXT,
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);
    
    await pool.query(`
      CREATE TABLE IF NOT EXISTS extracted_classes (
        id SERIAL PRIMARY KEY,
        jar_id INTEGER NOT NULL,
        class_name TEXT NOT NULL,
        package_name TEXT,
        class_type TEXT NOT NULL,
        content TEXT NOT NULL,
        imports TEXT[],
        methods JSONB,
        fields JSONB,
        is_public BOOLEAN NOT NULL DEFAULT TRUE,
        analyzed BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);
    
    console.log('JAR analyzer tables created successfully');
  } catch (error) {
    console.error('Error creating JAR analyzer tables:', error);
  } finally {
    process.exit(0);
  }
}

createJarAnalyzerTables();