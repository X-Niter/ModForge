import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Code Patterns schema
 * Stores patterns for code generation, fixing, and enhancement
 */
export const codePatterns = pgTable("code_patterns", {
  id: serial("id").primaryKey(),
  patternType: text("pattern_type").notNull(), // 'generation', 'fix', 'enhancement'
  prompt: text("prompt").notNull(),
  modLoader: text("mod_loader").notNull(),
  minecraftVersion: text("minecraft_version").notNull(),
  language: text("language").default("java").notNull(),
  inputPattern: text("input_pattern"), // For fixes, this would be the error pattern
  outputCode: text("output_code").notNull(),
  metadata: jsonb("metadata").default({}).notNull(),
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Error Patterns schema
 * Stores patterns for error detection and resolution
 */
export const errorPatterns = pgTable("error_patterns", {
  id: serial("id").primaryKey(),
  errorType: text("error_type").notNull(),
  errorPattern: text("error_pattern").notNull(),
  fixStrategy: text("fix_strategy").notNull(),
  modLoader: text("mod_loader").notNull(),
  successCount: integer("success_count").default(0).notNull(),
  failureCount: integer("failure_count").default(0).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Schema for code pattern insertion validation
 */
export const insertCodePatternSchema = createInsertSchema(codePatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    patternType: z.enum(['generation', 'fix', 'enhancement']),
    prompt: z.string().min(5),
    language: z.string().default('java')
  });

/**
 * Schema for error pattern insertion validation
 */
export const insertErrorPatternSchema = createInsertSchema(errorPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  });

// Type exports
export type InsertCodePattern = z.infer<typeof insertCodePatternSchema>;
export type CodePattern = typeof codePatterns.$inferSelect;

export type InsertErrorPattern = z.infer<typeof insertErrorPatternSchema>;
export type ErrorPattern = typeof errorPatterns.$inferSelect;