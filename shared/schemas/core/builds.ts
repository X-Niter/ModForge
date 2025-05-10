import { pgTable, text, serial, integer, jsonb, timestamp, boolean } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * BuildStatus enum
 * Defines the possible states of a build
 */
export enum BuildStatus {
  Queued = "queued",
  InProgress = "in_progress",
  Success = "succeeded",
  Failed = "failed"
}

// Forward reference for mods table
const mods = pgTable("mods", {
  id: serial("id").primaryKey(),
});

/**
 * Builds schema
 * Information about mod builds and their status
 */
export const builds = pgTable("builds", {
  id: serial("id").primaryKey(),
  modId: integer("mod_id").notNull().references(() => mods.id),
  buildNumber: integer("build_number").notNull(),
  version: text("version").notNull(),
  status: text("status").default(BuildStatus.Queued).notNull(),
  logs: text("logs"),
  errors: jsonb("errors").default([]).notNull(),
  errorCount: integer("error_count").default(0).notNull(),
  warningCount: integer("warning_count").default(0).notNull(),
  isAutomatic: boolean("is_automatic").default(false).notNull(),
  downloadUrl: text("download_url"),
  metadata: jsonb("metadata").default({}).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
  completedAt: timestamp("completed_at"),
});

/**
 * Schema for build insertion validation
 */
export const insertBuildSchema = createInsertSchema(builds)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
    completedAt: true,
  })
  .extend({
    buildNumber: z.number().int().positive(),
    version: z.string().min(1),
    status: z.nativeEnum(BuildStatus).default(BuildStatus.Queued),
    errors: z.array(z.any()).default([]),
    errorCount: z.number().int().min(0).default(0),
    warningCount: z.number().int().min(0).default(0),
    downloadUrl: z.string().nullable().optional(),
    metadata: z.record(z.any()).default({}),
  });

// Type exports
export type InsertBuild = z.infer<typeof insertBuildSchema>;
export type Build = typeof builds.$inferSelect;