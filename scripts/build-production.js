#!/usr/bin/env node

/**
 * Production build script for ModForge
 * 
 * This script compiles TypeScript to JavaScript for production deployment
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Configuration
const DIST_DIR = path.join(process.cwd(), 'dist');

/**
 * Clean previous build artifacts
 */
function cleanDistFolder() {
  console.log('🧹 Cleaning dist folder...');
  try {
    if (fs.existsSync(DIST_DIR)) {
      fs.rmSync(DIST_DIR, { recursive: true, force: true });
    }
    fs.mkdirSync(DIST_DIR, { recursive: true });
    console.log('✅ Dist folder cleaned');
  } catch (error) {
    console.error('❌ Failed to clean dist folder:', error);
    throw error;
  }
}

/**
 * Compile TypeScript to JavaScript
 */
function compileTypeScript() {
  console.log('🔄 Compiling TypeScript...');
  try {
    execSync('npx tsc --project tsconfig.json', { stdio: 'inherit' });
    console.log('✅ TypeScript compilation completed');
  } catch (error) {
    console.error('❌ TypeScript compilation failed');
    throw error;
  }
}

/**
 * Copy necessary files to dist folder
 */
function copyFiles() {
  console.log('📋 Copying necessary files...');
  try {
    // Copy package.json (for dependencies)
    const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
    
    // Simplify package.json for production
    const prodPackageJson = {
      name: packageJson.name,
      version: packageJson.version,
      description: packageJson.description,
      main: 'index.js',
      dependencies: packageJson.dependencies,
      engines: packageJson.engines || { node: ">=16.0.0" }
    };
    
    fs.writeFileSync(
      path.join(DIST_DIR, 'package.json'),
      JSON.stringify(prodPackageJson, null, 2)
    );
    
    // Copy any other necessary files
    // fs.copyFileSync('.env.example', path.join(DIST_DIR, '.env.example'));
    
    console.log('✅ File copying completed');
  } catch (error) {
    console.error('❌ Failed to copy files:', error);
    throw error;
  }
}

/**
 * Main build function
 */
async function build() {
  try {
    console.log('🚀 Starting production build...');
    
    // Clean dist folder
    cleanDistFolder();
    
    // Compile TypeScript
    compileTypeScript();
    
    // Copy necessary files
    copyFiles();
    
    console.log('✅ Production build completed successfully!');
  } catch (error) {
    console.error('❌ Build failed:', error);
    process.exit(1);
  }
}

// Run the build process
build();