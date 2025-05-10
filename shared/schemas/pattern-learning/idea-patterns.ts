import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Idea Patterns schema
 * Stores patterns for idea generation
 */
export const ideaPatterns = pgTable("idea_patterns", {
  id: serial("id").primaryKey(),
  keywords: text("keywords").array().notNull(),
  category: text("category").notNull(),
  responseContent: jsonb("response_content").notNull(),
  useCount: integer("use_count").default(0).notNull(),
  successRate: integer("success_rate").default(100).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Idea Expansion Patterns schema
 * Stores patterns for expanding basic ideas into detailed descriptions
 */
export const ideaExpansionPatterns = pgTable("idea_expansion_patterns", {
  id: serial("id").primaryKey(),
  originalIdeaTitle: text("original_idea_title").notNull(),
  keywords: text("keywords").array().notNull(),
  responseContent: jsonb("response_content").notNull(),
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
    keywords: z.array(z.string()),
    responseContent: z.record(z.any())
  });

/**
 * Schema for expansion pattern insertion validation
 */
export const insertExpansionPatternSchema = createInsertSchema(ideaExpansionPatterns)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    keywords: z.array(z.string()),
    responseContent: z.record(z.any())
  });

// Type exports
export type InsertIdeaPattern = z.infer<typeof insertIdeaPatternSchema>;
export type IdeaPattern = typeof ideaPatterns.$inferSelect;

export type InsertExpansionPattern = z.infer<typeof insertExpansionPatternSchema>;
export type ExpansionPattern = typeof ideaExpansionPatterns.$inferSelect;