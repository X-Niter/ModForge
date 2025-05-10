import { pgTable, text, serial, integer, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { relations } from "drizzle-orm";
import { mods } from "./mods";

/**
 * Build statuses enum
 */
export const BuildStatus = {
  InProgress: "in_progress",
  Success: "success",
  Failed: "failed",
} as const;

export type BuildStatus = typeof BuildStatus[keyof typeof BuildStatus];

/**
 * Build schema
 * Tracks build/compilation attempts for mods
 */
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

/**
 * Schema for build insertion validation
 */
export const insertBuildSchema = createInsertSchema(builds)
  .omit({
    id: true,
    createdAt: true,
    completedAt: true,
  })
  .extend({
    status: z.enum([BuildStatus.InProgress, BuildStatus.Success, BuildStatus.Failed]),
    logs: z.string().default("")
  });

/**
 * Build relationships
 */
export const buildsRelations = relations(builds, ({ one }) => ({
  mod: one(mods, {
    fields: [builds.modId],
    references: [mods.id],
  }),
}));

// Type exports
export type InsertBuild = z.infer<typeof insertBuildSchema>;
export type Build = typeof builds.$inferSelect;