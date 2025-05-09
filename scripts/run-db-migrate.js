#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');

// Run the db-push.ts script using tsx
const tsxBin = path.resolve('./node_modules/.bin/tsx');
const scriptPath = path.resolve('./scripts/db-push.ts');

console.log('Running database migration...');
const child = spawn(tsxBin, [scriptPath], { stdio: 'inherit' });

child.on('close', (code) => {
  if (code !== 0) {
    console.error(`Migration script exited with code ${code}`);
    process.exit(code);
  }
  console.log('Migration completed successfully');
});