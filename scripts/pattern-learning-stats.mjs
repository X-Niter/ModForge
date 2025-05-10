#!/usr/bin/env node

/**
 * Pattern Learning Statistics Tool
 * 
 * This script provides statistics about the pattern learning system.
 * It shows how many patterns have been stored, usage counts, success rates,
 * and estimated cost savings from using patterns instead of API calls.
 */

import { Pool } from '@neondatabase/serverless';
import { drizzle } from 'drizzle-orm/neon-serverless';
import * as schema from '../shared/schema.js';

// Initialize database connection
if (!process.env.DATABASE_URL) {
  console.error('❌ DATABASE_URL environment variable is not set');
  process.exit(1);
}

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const db = drizzle({ client: pool, schema });

// Constants for cost estimation
const AVG_TOKENS_PER_REQUEST = 2000; 
const COST_PER_1K_TOKENS = 0.03; // Approximate cost for GPT-4o

async function getStatistics() {
  console.log('📊 Pattern Learning System Statistics');
  console.log('=====================================');
  
  try {
    // 1. Count patterns by type
    const codePatternCount = await db.select({ count: schema.codePatterns }).count().from(schema.codePatterns);
    const errorPatternCount = await db.select({ count: schema.errorPatterns }).count().from(schema.errorPatterns);
    const ideaPatternCount = await db.select({ count: schema.ideaPatterns }).count().from(schema.ideaPatterns);
    const expansionPatternCount = await db.select({ count: schema.ideaExpansionPatterns }).count().from(schema.ideaExpansionPatterns);
    const featurePatternCount = await db.select({ count: schema.featurePatterns }).count().from(schema.featurePatterns);
    const docPatternCount = await db.select({ count: schema.documentationPatterns }).count().from(schema.documentationPatterns);
    
    console.log('\n📁 Pattern Counts:');
    console.log(`  Code Generation Patterns: ${codePatternCount.length > 0 ? codePatternCount[0].count : 0}`);
    console.log(`  Error Fixing Patterns: ${errorPatternCount.length > 0 ? errorPatternCount[0].count : 0}`);
    console.log(`  Idea Generation Patterns: ${ideaPatternCount.length > 0 ? ideaPatternCount[0].count : 0}`);
    console.log(`  Idea Expansion Patterns: ${expansionPatternCount.length > 0 ? expansionPatternCount[0].count : 0}`);
    console.log(`  Feature Implementation Patterns: ${featurePatternCount.length > 0 ? featurePatternCount[0].count : 0}`);
    console.log(`  Documentation Patterns: ${docPatternCount.length > 0 ? docPatternCount[0].count : 0}`);
    
    // 2. Calculate total pattern usage
    const codePatternsUsage = await db.select({
      totalUses: schema.codePatterns.useCount,
    }).from(schema.codePatterns);
    
    const ideaPatternsUsage = await db.select({
      totalUses: schema.ideaPatterns.useCount,
    }).from(schema.ideaPatterns);
    
    const docPatternsUsage = await db.select({
      totalUses: schema.documentationPatterns.useCount,
    }).from(schema.documentationPatterns);
    
    const totalCodeUses = codePatternsUsage.reduce((sum, p) => sum + Number(p.totalUses), 0);
    const totalIdeaUses = ideaPatternsUsage.reduce((sum, p) => sum + Number(p.totalUses), 0);
    const totalDocUses = docPatternsUsage.reduce((sum, p) => sum + Number(p.totalUses), 0);
    
    const totalPatternUses = totalCodeUses + totalIdeaUses + totalDocUses;
    
    console.log('\n🔄 Pattern Usage:');
    console.log(`  Total Pattern Uses: ${totalPatternUses}`);
    console.log(`  Code Generation Uses: ${totalCodeUses}`);
    console.log(`  Idea Generation Uses: ${totalIdeaUses}`);
    console.log(`  Documentation Uses: ${totalDocUses}`);
    
    // 3. Estimate cost savings
    const estimatedTokensSaved = totalPatternUses * AVG_TOKENS_PER_REQUEST;
    const estimatedCostSaved = (estimatedTokensSaved / 1000) * COST_PER_1K_TOKENS;
    
    console.log('\n💰 Estimated Savings:');
    console.log(`  Tokens Saved: ${estimatedTokensSaved.toLocaleString()}`);
    console.log(`  Cost Saved: $${estimatedCostSaved.toFixed(2)}`);
    
    // 4. Pattern success rates
    console.log('\n📈 Pattern Success Rates:');
    
    // For code patterns
    if (codePatternsUsage.length > 0) {
      const codePatterns = await db.select({
        successRate: schema.codePatterns.successRate,
      }).from(schema.codePatterns);
      
      const avgCodeSuccessRate = codePatterns.reduce((sum, p) => sum + Number(p.successRate), 0) / codePatterns.length;
      console.log(`  Code Generation Success Rate: ${avgCodeSuccessRate.toFixed(1)}%`);
    } else {
      console.log(`  Code Generation Success Rate: N/A (no patterns used)`);
    }
    
    // For error patterns
    const errorPatterns = await db.select({
      successCount: schema.errorPatterns.successCount,
      failureCount: schema.errorPatterns.failureCount,
    }).from(schema.errorPatterns);
    
    if (errorPatterns.length > 0) {
      const totalSuccesses = errorPatterns.reduce((sum, p) => sum + Number(p.successCount), 0);
      const totalFailures = errorPatterns.reduce((sum, p) => sum + Number(p.failureCount), 0);
      const totalAttempts = totalSuccesses + totalFailures;
      
      const errorSuccessRate = totalAttempts > 0 ? (totalSuccesses / totalAttempts) * 100 : 0;
      console.log(`  Error Fixing Success Rate: ${errorSuccessRate.toFixed(1)}%`);
    } else {
      console.log(`  Error Fixing Success Rate: N/A (no patterns used)`);
    }
    
    // 5. Pattern growth over time
    console.log('\n🌱 Pattern Growth:');
    // You would need to query by date ranges to show growth over time
    // For simplicity, we're skipping detailed time series analysis
    console.log(`  Total Patterns: ${
      (codePatternCount.length > 0 ? codePatternCount[0].count : 0) +
      (errorPatternCount.length > 0 ? errorPatternCount[0].count : 0) +
      (ideaPatternCount.length > 0 ? ideaPatternCount[0].count : 0) +
      (expansionPatternCount.length > 0 ? expansionPatternCount[0].count : 0) +
      (featurePatternCount.length > 0 ? featurePatternCount[0].count : 0) +
      (docPatternCount.length > 0 ? docPatternCount[0].count : 0)
    }`);
    
  } catch (error) {
    console.error('❌ Error getting statistics:', error);
  } finally {
    // Close database connection
    await pool.end();
  }
}

getStatistics();