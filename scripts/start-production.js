#!/usr/bin/env node

/**
 * Production startup script for ModForge
 * 
 * This script handles database migration, OpenAI API key verification, 
 * and proper application startup in production environments
 */

const { spawn, execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Configuration
const DB_MIGRATION_TIMEOUT = 60000; // 1 minute timeout for database migrations
const SERVER_START_TIMEOUT = 10000; // 10 seconds timeout for server startup
const MAX_STARTUP_RETRIES = 3;

// Set environment to production
process.env.NODE_ENV = 'production';

/**
 * Check if OpenAI API key is available
 */
function checkOpenAIApiKey() {
  if (!process.env.OPENAI_API_KEY) {
    console.warn('⚠️  WARNING: OPENAI_API_KEY is not set. AI-related features will not work properly.');
    return false;
  }
  return true;
}

/**
 * Migrate database schema using our non-interactive script
 */
async function migrateDatabase() {
  console.log('🔄 Running database migrations...');
  
  return new Promise((resolve, reject) => {
    try {
      const migrationProcess = spawn('node', ['scripts/db-push.js'], {
        stdio: 'inherit',
        env: process.env
      });
      
      // Set a timeout in case the migration hangs
      const timeoutId = setTimeout(() => {
        migrationProcess.kill();
        reject(new Error('Database migration timed out'));
      }, DB_MIGRATION_TIMEOUT);
      
      migrationProcess.on('close', (code) => {
        clearTimeout(timeoutId);
        if (code === 0) {
          console.log('✅ Database migrations completed successfully');
          resolve();
        } else {
          reject(new Error(`Database migration failed with code ${code}`));
        }
      });
      
      migrationProcess.on('error', (error) => {
        clearTimeout(timeoutId);
        reject(error);
      });
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Start the application server
 */
function startServer() {
  console.log('🚀 Starting server in production mode...');
  
  // Run the compiled server code
  const serverProcess = spawn('node', ['dist/index.js'], {
    stdio: 'inherit',
    env: process.env
  });
  
  // Handle server process events
  serverProcess.on('close', (code) => {
    if (code !== 0) {
      console.error(`❌ Server process exited with code ${code}`);
      process.exit(code);
    }
  });
  
  serverProcess.on('error', (error) => {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  });
  
  return serverProcess;
}

/**
 * Main startup function with retries
 */
async function startup(retryCount = 0) {
  try {
    // Check OpenAI API key
    checkOpenAIApiKey();
    
    // Migrate database
    await migrateDatabase();
    
    // Start the server
    const serverProcess = startServer();
    console.log('✅ Application started successfully in production mode');
    
    // Handle cleanup on termination signals
    const handleTermination = () => {
      console.log('🛑 Shutting down gracefully...');
      serverProcess.kill('SIGTERM');
      process.exit(0);
    };
    
    process.on('SIGINT', handleTermination);
    process.on('SIGTERM', handleTermination);
    process.on('SIGHUP', handleTermination);
    
  } catch (error) {
    console.error(`❌ Startup error: ${error.message}`);
    
    if (retryCount < MAX_STARTUP_RETRIES) {
      const retryDelay = Math.pow(2, retryCount) * 1000; // Exponential backoff
      console.log(`🔄 Retrying startup in ${retryDelay / 1000} seconds... (${retryCount + 1}/${MAX_STARTUP_RETRIES})`);
      
      setTimeout(() => {
        startup(retryCount + 1);
      }, retryDelay);
    } else {
      console.error('❌ Maximum retry attempts reached. Giving up.');
      process.exit(1);
    }
  }
}

// Run the startup process
startup();