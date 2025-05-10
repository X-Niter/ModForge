import { pgTable, text, serial, integer, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { relations } from "drizzle-orm";
import { mods } from "./mods";

/**
 * ModFile schema
 * Stores source code files associated with a mod
 */
export const modFiles = pgTable("mod_files", {
  id: serial("id").primaryKey(),
  modId: integer("mod_id").notNull(),
  path: text("path").notNull(),
  content: text("content").notNull(),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

/**
 * Schema for mod file insertion validation
 */
export const insertModFileSchema = createInsertSchema(modFiles)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    path: z.string().min(1),
    content: z.string()
  });

/**
 * ModFile relationships
 */
export const modFilesRelations = relations(modFiles, ({ one }) => ({
  mod: one(mods, {
    fields: [modFiles.modId],
    references: [mods.id],
  }),
}));

// Type exports
export type InsertModFile = z.infer<typeof insertModFileSchema>;
export type ModFile = typeof modFiles.$inferSelect;