import { pgTable, text, serial, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";
import { relations } from "drizzle-orm";
import { mods } from "./mods";

/**
 * User table schema
 * Central entity for authentication and user management
 */
export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  username: text("username").notNull().unique(),
  password: text("password").notNull(),
  email: text("email").unique(),
  fullName: text("full_name"),
  role: text("role").default("user").notNull(),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow()
});

/**
 * Schema for user insertion validation
 */
export const insertUserSchema = createInsertSchema(users)
  .omit({ id: true, createdAt: true, updatedAt: true, role: true })
  .extend({
    username: z.string().min(3).max(50),
    password: z.string().min(8),
    email: z.string().email().optional(),
    fullName: z.string().optional()
  });

/**
 * User relationships
 */
export const usersRelations = relations(users, ({ many }) => ({
  mods: many(mods)
}));

// Type exports
export type InsertUser = z.infer<typeof insertUserSchema>;
export type User = typeof users.$inferSelect;