#!/usr/bin/env node

import { execSync } from 'child_process';
import { existsSync, writeFileSync, unlinkSync } from 'fs';
import { resolve } from 'path';

/**
 * Non-interactive database migration script.
 * This script automates the process of running database migrations, avoiding
 * any prompts that would require user input.
 * 
 * Usage: node scripts/db-migrate.mjs
 */

async function runMigration() {
  console.log('🔄 Starting database migration process...');
  
  try {
    // Check if DATABASE_URL is set
    if (!process.env.DATABASE_URL) {
      console.error('❌ DATABASE_URL environment variable is not set');
      process.exit(1);
    }
    
    // Create a temporary SQL file with all table creation statements
    console.log('📝 Running migration from SQL scripts...');
    
    // Execute the SQL script directly using psql
    try {
      console.log('🔄 Running pattern learning tables creation script...');
      execSync(`psql "${process.env.DATABASE_URL}" -f scripts/create-pattern-tables.sql`, { 
        stdio: 'inherit' 
      });
      console.log('✅ Pattern learning tables created or verified');
    } catch (error) {
      console.error('❌ Error executing SQL script:', error.message);
      throw error;
    }
    
    console.log('\n✅ Database migration completed successfully');
  } catch (error) {
    console.error('\n❌ Database migration failed:', error.message);
    console.log('\n⚠️ Troubleshooting Tips:');
    console.log('  - Check your database connection (DATABASE_URL)');
    console.log('  - Ensure the database server is running');
    console.log('  - Verify you have the correct permissions');
    process.exit(1);
  }
}

// Run the migration
runMigration();