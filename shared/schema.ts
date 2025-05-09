import { pgTable, text, serial, integer, boolean, timestamp, primaryKey, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { relations } from "drizzle-orm";

export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  username: text("username").notNull().unique(),
  password: text("password").notNull(),
});

export const insertUserSchema = createInsertSchema(users).pick({
  username: true,
  password: true,
});

export type InsertUser = z.infer<typeof insertUserSchema>;
export type User = typeof users.$inferSelect;

// Mod loaders
export const modLoaders = ["Forge", "Fabric", "Bukkit", "Spigot", "Paper"] as const;
export type ModLoader = (typeof modLoaders)[number];

// Mod auto-fix levels
export const autoFixLevels = ["Conservative", "Balanced", "Aggressive"] as const;
export type AutoFixLevel = (typeof autoFixLevels)[number];

// Mod compilation frequency
export const compileFrequencies = ["After Every Change", "Every 5 Minutes", "Every 15 Minutes", "Manual Only"] as const;
export type CompileFrequency = (typeof compileFrequencies)[number];

// Mod schema
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

export const insertModSchema = createInsertSchema(mods)
  .omit({ id: true, userId: true, createdAt: true, updatedAt: true })
  .extend({
    modLoader: z.enum(modLoaders),
    compileFrequency: z.enum(compileFrequencies),
    autoFixLevel: z.enum(autoFixLevels),
  });

export type InsertMod = z.infer<typeof insertModSchema>;
export type Mod = typeof mods.$inferSelect;

// Build schema
export const builds = pgTable("builds", {
  id: serial("id").primaryKey(),
  modId: integer("mod_id").notNull(),
  buildNumber: integer("build_number").notNull(),
  status: text("status").notNull(), // success, failed, in_progress
  errorCount: integer("error_count").notNull().default(0),
  warningCount: integer("warning_count").notNull().default(0),
  logs: text("logs").notNull(),
  downloadUrl: text("download_url"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  completedAt: timestamp("completed_at"),
});

export const insertBuildSchema = createInsertSchema(builds).omit({
  id: true,
  createdAt: true,
  completedAt: true,
});

export type InsertBuild = z.infer<typeof insertBuildSchema>;
export type Build = typeof builds.$inferSelect;

// ModFile schema
export const modFiles = pgTable("mod_files", {
  id: serial("id").primaryKey(),
  modId: integer("mod_id").notNull(),
  path: text("path").notNull(),
  content: text("content").notNull(),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const insertModFileSchema = createInsertSchema(modFiles).omit({
  id: true,
  createdAt: true,
  updatedAt: true,
});

export type InsertModFile = z.infer<typeof insertModFileSchema>;
export type ModFile = typeof modFiles.$inferSelect;

// Relationships
export const usersRelations = relations(users, ({ many }) => ({
  mods: many(mods),
}));

export const modsRelations = relations(mods, ({ one, many }) => ({
  user: one(users, {
    fields: [mods.userId],
    references: [users.id],
  }),
  builds: many(builds),
  files: many(modFiles),
}));

export const buildsRelations = relations(builds, ({ one }) => ({
  mod: one(mods, {
    fields: [builds.modId],
    references: [mods.id],
  }),
}));

export const modFilesRelations = relations(modFiles, ({ one }) => ({
  mod: one(mods, {
    fields: [modFiles.modId],
    references: [mods.id],
  }),
}));

// Pattern learning tables
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

export const insertCodePatternSchema = createInsertSchema(codePatterns).omit({
  id: true,
  createdAt: true,
  updatedAt: true,
});

export const insertErrorPatternSchema = createInsertSchema(errorPatterns).omit({
  id: true,
  createdAt: true,
  updatedAt: true,
});

export type InsertCodePattern = z.infer<typeof insertCodePatternSchema>;
export type CodePattern = typeof codePatterns.$inferSelect;

export type InsertErrorPattern = z.infer<typeof insertErrorPatternSchema>;
export type ErrorPattern = typeof errorPatterns.$inferSelect;
