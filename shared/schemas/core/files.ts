import { pgTable, text, serial, integer, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

// Forward reference for mods table
const mods = pgTable("mods", {
  id: serial("id").primaryKey(),
});

/**
 * ModFiles schema
 * Information about mod files and their content
 */
export const modFiles = pgTable("mod_files", {
  id: serial("id").primaryKey(),
  modId: integer("mod_id").notNull().references(() => mods.id),
  path: text("path").notNull(),
  content: text("content").notNull(),
  contentType: text("content_type").default("text/plain").notNull(),
  metadata: jsonb("metadata").default({}).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Schema for modFile insertion validation
 */
export const insertModFileSchema = createInsertSchema(modFiles)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    path: z.string().min(1),
    content: z.string(),
    contentType: z.string().default("text/plain"),
    metadata: z.record(z.any()).default({}),
  });

// Type exports
export type InsertModFile = z.infer<typeof insertModFileSchema>;
export type ModFile = typeof modFiles.$inferSelect;