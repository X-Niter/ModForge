/**
 * @file relations.ts
 * Central place to define relationships between tables
 * This breaks the circular dependency cycle
 */

import { relations } from "drizzle-orm";
import { users } from "./core/users";
import { mods } from "./core/mods";
import { builds } from "./core/builds";
import { modFiles } from "./core/files";

/**
 * Define relations for the users table
 */
export const userRelations = relations(users, ({ many }) => ({
  // A user can have many mods
  mods: many(mods),
}));

/**
 * Define relations for the mods table
 */
export const modRelations = relations(mods, ({ one, many }) => ({
  // A mod belongs to one user
  user: one(users, {
    fields: [mods.userId],
    references: [users.id],
  }),
  // A mod can have many builds
  builds: many(builds),
  // A mod can have many files
  files: many(modFiles),
}));

/**
 * Define relations for the builds table
 */
export const buildRelations = relations(builds, ({ one }) => ({
  // A build belongs to one mod
  mod: one(mods, {
    fields: [builds.modId],
    references: [mods.id],
  }),
}));

/**
 * Define relations for the modFiles table
 */
export const modFileRelations = relations(modFiles, ({ one }) => ({
  // A file belongs to one mod
  mod: one(mods, {
    fields: [modFiles.modId],
    references: [mods.id],
  }),
}));