#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');

// Run the db-cleanup.js script using tsx
const tsxBin = path.resolve('./node_modules/.bin/tsx');
const scriptPath = path.resolve('./scripts/db-cleanup.js');

console.log('Running database cleanup...');
const child = spawn(tsxBin, [scriptPath], { stdio: 'inherit' });

child.on('close', (code) => {
  if (code !== 0) {
    console.error(`Cleanup script exited with code ${code}`);
    process.exit(code);
  }
  console.log('Cleanup completed successfully');
});