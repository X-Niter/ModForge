import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Feature Patterns schema
 * Stores patterns for adding features to mods
 */
export const featurePatterns = pgTable("feature_patterns", {
  id: serial("id").primaryKey(),
  featureType: text("feature_type").notNull(),
  modLoader: text("mod_loader").notNull(),
  keywords: text("keywords").array().notNull(),
  inputStructure: jsonb("input_structure").default({}).notNull(),
  outputCode: text("output_code").notNull(),
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
  codeType: text("code_type").notNull(),
  language: text("language").default("java").notNull(),
  style: text("style").default("standard").notNull(),
  inputCode: text("input_code").notNull(),
  outputDocumentation: text("output_documentation").notNull(),
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
    keywords: z.array(z.string()),
    inputStructure: z.record(z.any()).default({})
  });

/**
 * Schema for documentation pattern insertion validation
 */
export const insertDocPatternSchema = createInsertSchema(documentationPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    language: z.string().default('java'),
    style: z.string().default('standard')
  });

// Type exports
export type InsertFeaturePattern = z.infer<typeof insertFeaturePatternSchema>;
export type FeaturePattern = typeof featurePatterns.$inferSelect;

export type InsertDocPattern = z.infer<typeof insertDocPatternSchema>;
export type DocPattern = typeof documentationPatterns.$inferSelect;