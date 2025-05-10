import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Feature Patterns schema
 * Stores patterns for adding features to mods
 */
export const featurePatterns = pgTable("feature_patterns", {
  id: serial("id").primaryKey(),
  modLoader: text("mod_loader").notNull(),
  featureType: text("feature_type").notNull(), // e.g. "item", "block", "entity", "biome"
  featureDescription: text("feature_description").notNull(),
  keyTerms: text("key_terms").array(), // Array of key terms extracted for matching
  inputFiles: jsonb("input_files").notNull(), // File structure of the mod before feature addition
  outputFiles: jsonb("output_files").notNull(), // File structure after feature addition
  explanation: text("explanation").notNull(),
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Documentation Patterns schema
 * Stores patterns for generating documentation
 */
export const documentationPatterns = pgTable("documentation_patterns", {
  id: serial("id").primaryKey(),
  language: text("language").default("java").notNull(),
  codeFingerprint: text("code_fingerprint").notNull(), // A hash/fingerprint of the code for quick matching
  codeLength: integer("code_length").notNull(), // Length of the original code
  codeType: text("code_type").notNull(), // "class", "method", "module", etc.
  style: text("style").default("standard").notNull(), // Documentation style
  inputCode: text("input_code").notNull(), // The original code
  outputDocs: text("output_docs").notNull(), // The generated documentation
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Schema for feature pattern insertion validation
 */
export const insertFeaturePatternSchema = createInsertSchema(featurePatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    featureType: z.enum(['item', 'block', 'entity', 'biome', 'recipe', 'command', 'other']),
    featureDescription: z.string().min(5)
  });

/**
 * Schema for documentation pattern insertion validation
 */
export const insertDocumentationPatternSchema = createInsertSchema(documentationPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    language: z.string().default('java'),
    codeType: z.enum(['class', 'method', 'function', 'module', 'interface', 'other']),
    style: z.enum(['standard', 'javadoc', 'jsdoc', 'markdown', 'minimal'])
  });

// Type exports
export type InsertFeaturePattern = z.infer<typeof insertFeaturePatternSchema>;
export type FeaturePattern = typeof featurePatterns.$inferSelect;

export type InsertDocumentationPattern = z.infer<typeof insertDocumentationPatternSchema>;
export type DocumentationPattern = typeof documentationPatterns.$inferSelect;