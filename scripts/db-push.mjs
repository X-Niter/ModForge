#!/usr/bin/env node

import { execSync } from 'child_process';

/**
 * Script to push Drizzle schema changes to the database
 * in a non-interactive way, suitable for CI/CD and production environments.
 */
async function pushSchema() {
  console.log('🔄 Applying database schema changes...');
  
  try {
    // Check if DATABASE_URL is set
    if (!process.env.DATABASE_URL) {
      console.error('❌ DATABASE_URL environment variable is not set');
      process.exit(1);
    }
    
    // Use --force to apply changes without confirmation prompts
    console.log('🚀 Pushing schema changes with --force flag...');
    execSync('npx drizzle-kit push --force', { 
      stdio: 'inherit'
    });
    
    console.log('\n✅ Database schema updated successfully');
  } catch (error) {
    console.error('\n❌ Error pushing database schema:', error.message);
    console.log('\n⚠️ Troubleshooting Tips:');
    console.log('  - Check your database connection (DATABASE_URL)');
    console.log('  - Ensure the database server is running');
    console.log('  - Verify you have the correct permissions');
    process.exit(1);
  }
}

// Run the function
pushSchema();