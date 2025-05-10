import { pgTable, text, serial, boolean, timestamp, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

/**
 * Users schema
 * Primary user account information
 */
export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  username: text("username").notNull().unique(),
  email: text("email").unique(),
  password: text("password").notNull(),
  displayName: text("display_name"),
  avatarUrl: text("avatar_url"),
  githubId: text("github_id").unique(),
  githubToken: text("github_token"),
  stripeCustomerId: text("stripe_customer_id"),
  stripeSubscriptionId: text("stripe_subscription_id"),
  isAdmin: boolean("is_admin").default(false).notNull(),
  metadata: jsonb("metadata").default({}).notNull(),
  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/**
 * Schema for user insertion validation
 */
export const insertUserSchema = createInsertSchema(users)
  .omit({
    id: true,
    createdAt: true,
    updatedAt: true,
    isAdmin: true,
    metadata: true,
  })
  .extend({
    username: z.string().min(3).max(20).regex(/^[a-zA-Z0-9_\-$&]+$/, "Username can only contain letters, numbers, and the special characters: _ - $ &"),
    password: z.string().min(1),  // Allow empty password for OAuth users
    email: z.string().email().optional().nullable(),
    githubId: z.string().optional().nullable(),
    githubToken: z.string().optional().nullable(),
    avatarUrl: z.string().optional().nullable(),
    stripeCustomerId: z.string().optional().nullable(),
    stripeSubscriptionId: z.string().optional().nullable(),
    metadata: z.record(z.unknown()).optional().default({}),
  });

// Type exports
export type InsertUser = z.infer<typeof insertUserSchema>;
export type User = typeof users.$inferSelect;