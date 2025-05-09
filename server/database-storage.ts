import { eq } from "drizzle-orm";
import { db } from "./db";
import { 
  users, 
  mods, 
  builds, 
  modFiles, 
  type User, 
  type InsertUser, 
  type Mod, 
  type InsertMod, 
  type Build, 
  type InsertBuild, 
  type ModFile, 
  type InsertModFile 
} from "@shared/schema";
import { IStorage } from "./storage";

export class DatabaseStorage implements IStorage {
  // User operations
  async getUser(id: number): Promise<User | undefined> {
    const [user] = await db.select().from(users).where(eq(users.id, id));
    return user || undefined;
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    const [user] = await db.select().from(users).where(eq(users.username, username));
    return user || undefined;
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const [user] = await db
      .insert(users)
      .values(insertUser)
      .returning();
    return user;
  }

  // Mod operations
  async getMod(id: number): Promise<Mod | undefined> {
    const [mod] = await db.select().from(mods).where(eq(mods.id, id));
    return mod || undefined;
  }

  async getModsByUserId(userId: number): Promise<Mod[]> {
    return await db.select().from(mods).where(eq(mods.userId, userId));
  }

  async createMod(mod: InsertMod & { userId: number }): Promise<Mod> {
    const [newMod] = await db
      .insert(mods)
      .values(mod)
      .returning();
    return newMod;
  }

  async updateMod(id: number, modData: Partial<InsertMod>): Promise<Mod | undefined> {
    const [updatedMod] = await db
      .update(mods)
      .set({ ...modData, updatedAt: new Date() })
      .where(eq(mods.id, id))
      .returning();
    return updatedMod || undefined;
  }

  async deleteMod(id: number): Promise<boolean> {
    const result = await db.delete(mods).where(eq(mods.id, id));
    return true; // PostgreSQL doesn't return count, so we assume success
  }

  // Build operations
  async getBuild(id: number): Promise<Build | undefined> {
    const [build] = await db.select().from(builds).where(eq(builds.id, id));
    return build || undefined;
  }

  async getBuildsByModId(modId: number): Promise<Build[]> {
    return await db.select().from(builds).where(eq(builds.modId, modId));
  }

  async createBuild(build: InsertBuild): Promise<Build> {
    const [newBuild] = await db
      .insert(builds)
      .values(build)
      .returning();
    return newBuild;
  }

  async updateBuild(id: number, buildData: Partial<InsertBuild>): Promise<Build | undefined> {
    const [updatedBuild] = await db
      .update(builds)
      .set(buildData)
      .where(eq(builds.id, id))
      .returning();
    return updatedBuild || undefined;
  }

  async deleteBuild(id: number): Promise<boolean> {
    const result = await db.delete(builds).where(eq(builds.id, id));
    return true; // PostgreSQL doesn't return count, so we assume success
  }

  // ModFile operations
  async getModFile(id: number): Promise<ModFile | undefined> {
    const [file] = await db.select().from(modFiles).where(eq(modFiles.id, id));
    return file || undefined;
  }

  async getModFilesByModId(modId: number): Promise<ModFile[]> {
    return await db.select().from(modFiles).where(eq(modFiles.modId, modId));
  }

  async createModFile(file: InsertModFile): Promise<ModFile> {
    const [newFile] = await db
      .insert(modFiles)
      .values(file)
      .returning();
    return newFile;
  }

  async updateModFile(id: number, fileData: Partial<InsertModFile>): Promise<ModFile | undefined> {
    const [updatedFile] = await db
      .update(modFiles)
      .set({ ...fileData, updatedAt: new Date() })
      .where(eq(modFiles.id, id))
      .returning();
    return updatedFile || undefined;
  }

  async deleteModFile(id: number): Promise<boolean> {
    const result = await db.delete(modFiles).where(eq(modFiles.id, id));
    return true; // PostgreSQL doesn't return count, so we assume success
  }
}