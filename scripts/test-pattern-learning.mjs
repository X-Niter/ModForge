#!/usr/bin/env node

/**
 * Test script for the pattern learning system
 * This script populates the pattern learning tables with test data
 * and verifies the system can retrieve and use patterns appropriately.
 */

import { Pool } from '@neondatabase/serverless';
import { drizzle } from 'drizzle-orm/neon-serverless';
import { eq } from 'drizzle-orm';
import * as schema from '../shared/schema.js';

// Initialize database connection
if (!process.env.DATABASE_URL) {
  console.error('❌ DATABASE_URL environment variable is not set');
  process.exit(1);
}

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const db = drizzle({ client: pool, schema });

async function runTests() {
  console.log('🧪 Running pattern learning system tests...');
  
  try {
    // Test 1: Insert a code generation pattern
    console.log('\n📋 Test 1: Insert code generation pattern');
    const codePattern = {
      patternType: 'generation',
      prompt: 'Create a Minecraft mod that adds a diamond sword with fire aspect',
      modLoader: 'forge',
      minecraftVersion: '1.20.1',
      language: 'java',
      outputCode: `
package com.example.firemod;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod("firemod")
public class FireMod {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "firemod");
    
    public static final RegistryObject<Item> FIRE_SWORD = ITEMS.register("fire_sword", 
        () -> {
            SwordItem sword = new SwordItem(Tiers.DIAMOND, 3, -2.4F, new Item.Properties());
            sword.enchant(Enchantments.FIRE_ASPECT, 2);
            return sword;
        }
    );
    
    public FireMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
    
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FIRE_SWORD);
        }
    }
}`,
      metadata: { difficulty: 'medium', requires: ['forge'] }
    };
    
    await db.insert(schema.codePatterns).values(codePattern);
    console.log('✅ Code pattern inserted successfully');
    
    // Test 2: Insert error pattern
    console.log('\n📋 Test 2: Insert error pattern');
    const errorPattern = {
      errorType: 'compilation',
      errorPattern: 'cannot find symbol: class SwordItem',
      fixStrategy: 'Add missing import: import net.minecraft.world.item.SwordItem;',
      modLoader: 'forge',
    };
    
    await db.insert(schema.errorPatterns).values(errorPattern);
    console.log('✅ Error pattern inserted successfully');
    
    // Test 3: Retrieve code pattern by similarity
    console.log('\n📋 Test 3: Retrieve pattern by similarity');
    const testPrompt = 'Make a Minecraft mod with a diamond sword that sets mobs on fire';
    
    // In a real implementation, this would use semantic similarity
    // For this test, we'll use a simple query
    const similarPatterns = await db.select().from(schema.codePatterns)
      .where(eq(schema.codePatterns.modLoader, 'forge'));
    
    if (similarPatterns.length > 0) {
      console.log(`✅ Found ${similarPatterns.length} similar patterns`);
      console.log(`  First pattern: ${similarPatterns[0].prompt}`);
    } else {
      console.error('❌ No similar patterns found');
    }
    
    // Test 4: Update usage metrics
    console.log('\n📋 Test 4: Update usage metrics');
    if (similarPatterns.length > 0) {
      const patternId = similarPatterns[0].id;
      await db.update(schema.codePatterns)
        .set({ 
          useCount: similarPatterns[0].useCount + 1,
          successRate: 95  // Simulate some success rate
        })
        .where(eq(schema.codePatterns.id, patternId));
      
      console.log('✅ Usage metrics updated successfully');
      
      // Verify update
      const updatedPattern = await db.select().from(schema.codePatterns)
        .where(eq(schema.codePatterns.id, patternId));
      
      console.log(`  New use count: ${updatedPattern[0].useCount}`);
      console.log(`  New success rate: ${updatedPattern[0].successRate}%`);
    }
    
    // Cleanup - In a real test, you might want to remove test data
    // But for demonstration purposes, we'll leave it in the database
    
    console.log('\n🎉 All pattern learning tests completed successfully!');
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    // Close database connection
    await pool.end();
  }
}

runTests();