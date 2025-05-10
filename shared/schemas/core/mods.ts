import { pgTable, text, serial, integer, jsonb, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { ModLoader } from "./types";

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
  description: text("description"),
  minecraftVersion: text("minecraft_version").notNull(),
  modLoader: text("mod_loader").notNull(), // forge, fabric, quilt, architectury
  githubRepo: text("github_repo"),
  status: text("status").default("draft").notNull(), // draft, in_progress, completed
  tags: text("tags").array(),
  metadata: jsonb("metadata").default({}).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * ModStatus enum
 * Defines the possible states of a mod
 */
export enum ModStatus {
  Draft = "draft",
  InProgress = "in_progress",
  Completed = "completed"
}

/**
 * Schema for mod insertion validation
 */
export const insertModSchema = createInsertSchema(mods)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
  })
  .extend({
    name: z.string().min(3).max(50),
    minecraftVersion: z.string().min(3),
    modLoader: z.nativeEnum(ModLoader),
    status: z.nativeEnum(ModStatus).default(ModStatus.Draft),
    metadata: z.record(z.any()).default({}),
  });

// Type exports
export type InsertMod = z.infer<typeof insertModSchema>;
export type Mod = typeof mods.$inferSelect;