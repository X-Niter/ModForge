#!/usr/bin/env node

const { execSync } = require('child_process');
const readline = require('readline');

function push() {
  try {
    // Execute the command and pipe output to process.stdout
    execSync('npx drizzle-kit push:pg', { stdio: 'inherit' });
    console.log('\n✅ Database schema pushed successfully');
  } catch (error) {
    console.error('\n❌ Error pushing database schema:', error.message);
    process.exit(1);
  }
}

// Start the automatic push process
console.log('🚀 Starting automatic schema push...');
push();