import { pgTable, text, serial, integer, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { ModLoader, AutoFixLevel, CompileFrequency } from "./types";

// Forward references to avoid circular dependencies
const users = pgTable("users", {
  id: serial("id").primaryKey(),
});

/**
 * Mods schema
 * Information about Minecraft mods
 */
export const mods = pgTable("mods", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id),
  name: text("name").notNull(),
  modId: text("mod_id").notNull(),
  description: text("description").notNull(),
  version: text("version").notNull(),
  minecraftVersion: text("minecraft_version").notNull(),
  license: text("license").notNull(),
  modLoader: text("mod_loader").notNull(), // forge, fabric, quilt, architectury
  idea: text("idea").notNull(),
  featurePriority: text("feature_priority").notNull(),
  codingStyle: text("coding_style").notNull(),
  compileFrequency: text("compile_frequency").notNull(),
  autoFixLevel: text("auto_fix_level").notNull(),
  autoPushToGithub: integer("auto_push_to_github").notNull(), // Using integer for boolean (1/0)
  generateDocumentation: integer("generate_documentation").notNull(), // Using integer for boolean (1/0)
  githubRepo: text("github_repo"),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

// Create a simplified version of the schema for validation
const insertModValidationSchema = z.object({
  userId: z.number(),
  name: z.string().min(3).max(50),
  modId: z.string().min(3).max(50),
  description: z.string().min(5),
  version: z.string(),
  minecraftVersion: z.string().min(3),
  license: z.string(),
  modLoader: z.nativeEnum(ModLoader),
  idea: z.string(),
  featurePriority: z.string(),
  codingStyle: z.string(),
  compileFrequency: z.nativeEnum(CompileFrequency),
  autoFixLevel: z.nativeEnum(AutoFixLevel),
  autoPushToGithub: z.boolean().transform(val => val ? 1 : 0),
  generateDocumentation: z.boolean().transform(val => val ? 1 : 0),
  githubRepo: z.string().optional(),
});

export const insertModSchema = insertModValidationSchema;

// Type exports
export type InsertMod = z.infer<typeof insertModSchema>;
export type Mod = typeof mods.$inferSelect;