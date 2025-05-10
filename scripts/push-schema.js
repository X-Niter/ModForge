#!/usr/bin/env node

const { execSync } = require('child_process');

function push() {
  try {
    console.log('🔍 Checking environment variables...');
    if (!process.env.DATABASE_URL) {
      console.error('❌ DATABASE_URL environment variable is not set');
      process.exit(1);
    }
    
    console.log('🚀 Starting schema push with automatic approval...');
    // Execute the command with automatic YES to all prompts and pipe output to process.stdout
    execSync('npx drizzle-kit push:pg --verbose', { 
      stdio: 'inherit',
      env: {
        ...process.env,
        DRIZZLE_KIT_AUTOCONFIRM: 'true' // Auto-confirm all prompts
      }
    });
    console.log('\n✅ Database schema pushed successfully');
  } catch (error) {
    console.error('\n❌ Error pushing database schema:', error.message);
    console.log('\n⚠️ Troubleshooting Tips:');
    console.log('  - Check your database connection (DATABASE_URL)');
    console.log('  - Ensure the database server is running');
    console.log('  - Verify you have the correct permissions');
    process.exit(1);
  }
}

// Start the automatic push process
console.log('🔄 Starting database schema synchronization...');
push();