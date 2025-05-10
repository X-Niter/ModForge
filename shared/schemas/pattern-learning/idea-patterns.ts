import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Idea Patterns schema
 * Stores patterns for mod idea generation
 */
export const ideaPatterns = pgTable("idea_patterns", {
  id: serial("id").primaryKey(),
  theme: text("theme"), // Can be null if general
  complexity: text("complexity").notNull(), // Simple, medium, complex
  modLoader: text("mod_loader"), // Can be null if any
  minecraftVersion: text("minecraft_version"), // Can be null if any
  prompt: text("prompt").notNull(), // The original prompt
  response: jsonb("response").notNull(), // The generated ideas
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Idea Expansion Patterns schema
 * Stores patterns for expanding mod ideas with details
 */
export const ideaExpansionPatterns = pgTable("idea_expansion_patterns", {
  id: serial("id").primaryKey(),
  ideaTitle: text("idea_title").notNull(),
  ideaDescription: text("idea_description").notNull(),
  keyTerms: text("key_terms").array(), // Array of key terms extracted for matching
  response: jsonb("response").notNull(), // The expanded idea
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Schema for idea pattern insertion validation
 */
export const insertIdeaPatternSchema = createInsertSchema(ideaPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    complexity: z.enum(['simple', 'medium', 'complex']),
    prompt: z.string().min(5)
  });

/**
 * Schema for idea expansion pattern insertion validation
 */
export const insertIdeaExpansionPatternSchema = createInsertSchema(ideaExpansionPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  });

// Type exports
export type InsertIdeaPattern = z.infer<typeof insertIdeaPatternSchema>;
export type IdeaPattern = typeof ideaPatterns.$inferSelect;

export type InsertIdeaExpansionPattern = z.infer<typeof insertIdeaExpansionPatternSchema>;
export type IdeaExpansionPattern = typeof ideaExpansionPatterns.$inferSelect;