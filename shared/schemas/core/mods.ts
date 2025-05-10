import { pgTable, text, serial, integer, boolean, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { relations } from "drizzle-orm";
import { users } from "./users";
import { builds } from "./builds";
import { modFiles } from "./files";

/**
 * Mod loaders
 */
export const modLoaders = ["Forge", "Fabric", "Bukkit", "Spigot", "Paper"] as const;
export type ModLoader = (typeof modLoaders)[number];

/**
 * Mod auto-fix levels
 */
export const autoFixLevels = ["Conservative", "Balanced", "Aggressive"] as const;
export type AutoFixLevel = (typeof autoFixLevels)[number];

/**
 * Mod compilation frequency
 */
export const compileFrequencies = ["After Every Change", "Every 5 Minutes", "Every 15 Minutes", "Manual Only"] as const;
export type CompileFrequency = (typeof compileFrequencies)[number];

/**
 * Mod schema
 * Core table that stores Minecraft mod definitions
 */
export const mods = pgTable("mods", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull(),
  name: text("name").notNull(),
  modId: text("mod_id").notNull(),
  description: text("description").notNull(),
  version: text("version").notNull(),
  minecraftVersion: text("minecraft_version").notNull(),
  license: text("license").notNull(),
  modLoader: text("mod_loader").notNull(),
  idea: text("idea").notNull(),
  featurePriority: text("feature_priority").notNull(),
  codingStyle: text("coding_style").notNull(),
  compileFrequency: text("compile_frequency").notNull(),
  autoFixLevel: text("auto_fix_level").notNull(),
  autoPushToGithub: boolean("auto_push_to_github").notNull(),
  generateDocumentation: boolean("generate_documentation").notNull(),
  githubRepo: text("github_repo"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

/**
 * Schema for mod insertion validation
 */
export const insertModSchema = createInsertSchema(mods)
  .omit({ id: true, userId: true, createdAt: true, updatedAt: true })
  .extend({
    modLoader: z.enum(modLoaders),
    compileFrequency: z.enum(compileFrequencies),
    autoFixLevel: z.enum(autoFixLevels),
    name: z.string().min(3).max(100),
    modId: z.string().min(3).max(100).regex(/^[a-z0-9_]+$/, {
      message: "Mod ID must only contain lowercase letters, numbers, and underscores"
    }),
    version: z.string().regex(/^\d+\.\d+\.\d+$/, {
      message: "Version must be in the format X.Y.Z (e.g. 1.0.0)"
    }),
    minecraftVersion: z.string().min(3).max(20),
    license: z.string().min(2).max(100)
  });

/**
 * Mod relationships
 */
export const modsRelations = relations(mods, ({ one, many }) => ({
  user: one(users, {
    fields: [mods.userId],
    references: [users.id],
  }),
  builds: many(builds),
  files: many(modFiles),
}));

// Type exports
export type InsertMod = z.infer<typeof insertModSchema>;
export type Mod = typeof mods.$inferSelect;