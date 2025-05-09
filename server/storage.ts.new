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
import { DatabaseStorage } from "./database-storage";

// Full storage interface
export interface IStorage {
  // User operations
  getUser(id: number): Promise<User | undefined>;
  getUserByUsername(username: string): Promise<User | undefined>;
  createUser(user: InsertUser): Promise<User>;
  
  // Mod operations
  getMod(id: number): Promise<Mod | undefined>;
  getModsByUserId(userId: number): Promise<Mod[]>;
  createMod(mod: InsertMod & { userId: number }): Promise<Mod>;
  updateMod(id: number, mod: Partial<InsertMod>): Promise<Mod | undefined>;
  deleteMod(id: number): Promise<boolean>;
  
  // Build operations
  getBuild(id: number): Promise<Build | undefined>;
  getBuildsByModId(modId: number): Promise<Build[]>;
  createBuild(build: InsertBuild): Promise<Build>;
  updateBuild(id: number, build: Partial<InsertBuild>): Promise<Build | undefined>;
  deleteBuild(id: number): Promise<boolean>;
  
  // ModFile operations
  getModFile(id: number): Promise<ModFile | undefined>;
  getModFilesByModId(modId: number): Promise<ModFile[]>;
  createModFile(file: InsertModFile): Promise<ModFile>;
  updateModFile(id: number, file: Partial<InsertModFile>): Promise<ModFile | undefined>;
  deleteModFile(id: number): Promise<boolean>;
}

export class MemStorage implements IStorage {
  private users: Map<number, User>;
  private mods: Map<number, Mod>;
  private builds: Map<number, Build>;
  private modFiles: Map<number, ModFile>;
  private currentUserId: number;
  private currentModId: number;
  private currentBuildId: number;
  private currentFileId: number;

  constructor() {
    this.users = new Map();
    this.mods = new Map();
    this.builds = new Map();
    this.modFiles = new Map();
    
    this.currentUserId = 1;
    this.currentModId = 1;
    this.currentBuildId = 1;
    this.currentFileId = 1;
    
    // Add a default user
    this.createUser({
      username: "demo",
      password: "password"
    });
  }

  // User operations
  async getUser(id: number): Promise<User | undefined> {
    return this.users.get(id);
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    return Array.from(this.users.values()).find(
      (user) => user.username === username,
    );
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const id = this.currentUserId++;
    const user: User = { ...insertUser, id };
    this.users.set(id, user);
    return user;
  }
  
  // Mod operations
  async getMod(id: number): Promise<Mod | undefined> {
    return this.mods.get(id);
  }
  
  async getModsByUserId(userId: number): Promise<Mod[]> {
    return Array.from(this.mods.values()).filter(
      (mod) => mod.userId === userId
    );
  }
  
  async createMod(mod: InsertMod & { userId: number }): Promise<Mod> {
    const id = this.currentModId++;
    const createdAt = new Date();
    const updatedAt = createdAt;
    
    // Ensure all required fields are present with defaults if needed
    const newMod: Mod = { 
      ...mod, 
      id, 
      createdAt, 
      updatedAt,
      githubRepo: null // Ensure githubRepo has a default value
    };
    
    this.mods.set(id, newMod);
    return newMod;
  }
  
  async updateMod(id: number, modData: Partial<InsertMod>): Promise<Mod | undefined> {
    const mod = this.mods.get(id);
    if (!mod) return undefined;
    
    // Handle the githubRepo field explicitly to avoid type issues
    let githubRepo = mod.githubRepo;
    if ('githubRepo' in modData) {
      githubRepo = modData.githubRepo as string | null;
    }
    
    const updatedMod: Mod = { 
      ...mod, 
      ...modData, 
      githubRepo, // Use the explicitly handled value
      updatedAt: new Date() 
    };
    
    this.mods.set(id, updatedMod);
    return updatedMod;
  }
  
  async deleteMod(id: number): Promise<boolean> {
    return this.mods.delete(id);
  }
  
  // Build operations
  async getBuild(id: number): Promise<Build | undefined> {
    return this.builds.get(id);
  }
  
  async getBuildsByModId(modId: number): Promise<Build[]> {
    return Array.from(this.builds.values())
      .filter(build => build.modId === modId)
      .sort((a, b) => b.buildNumber - a.buildNumber); // Sort by build number desc
  }
  
  async createBuild(build: InsertBuild): Promise<Build> {
    const id = this.currentBuildId++;
    const createdAt = new Date();
    
    // Ensure all required fields are present with defaults
    const newBuild: Build = { 
      ...build, 
      id, 
      createdAt,
      completedAt: null,
      // Ensure required numeric fields have defaults
      errorCount: build.errorCount ?? 0,
      warningCount: build.warningCount ?? 0,
      // Ensure downloadUrl has a default value
      downloadUrl: build.downloadUrl ?? null
    };
    
    this.builds.set(id, newBuild);
    return newBuild;
  }
  
  async updateBuild(id: number, buildData: Partial<InsertBuild>): Promise<Build | undefined> {
    const build = this.builds.get(id);
    if (!build) return undefined;
    
    const updatedBuild: Build = { 
      ...build, 
      ...buildData,
      // If status is changing to 'success' or 'failed', set completedAt
      completedAt: 
        (buildData.status === 'success' || buildData.status === 'failed') 
          ? new Date() 
          : build.completedAt
    };
    
    this.builds.set(id, updatedBuild);
    return updatedBuild;
  }
  
  async deleteBuild(id: number): Promise<boolean> {
    return this.builds.delete(id);
  }
  
  // ModFile operations
  async getModFile(id: number): Promise<ModFile | undefined> {
    return this.modFiles.get(id);
  }
  
  async getModFilesByModId(modId: number): Promise<ModFile[]> {
    return Array.from(this.modFiles.values()).filter(
      (file) => file.modId === modId
    );
  }
  
  async createModFile(file: InsertModFile): Promise<ModFile> {
    const id = this.currentFileId++;
    const createdAt = new Date();
    const updatedAt = createdAt;
    
    const newFile: ModFile = { 
      ...file, 
      id, 
      createdAt, 
      updatedAt 
    };
    
    this.modFiles.set(id, newFile);
    return newFile;
  }
  
  async updateModFile(id: number, fileData: Partial<InsertModFile>): Promise<ModFile | undefined> {
    const file = this.modFiles.get(id);
    if (!file) return undefined;
    
    const updatedFile: ModFile = { 
      ...file, 
      ...fileData, 
      updatedAt: new Date() 
    };
    
    this.modFiles.set(id, updatedFile);
    return updatedFile;
  }
  
  async deleteModFile(id: number): Promise<boolean> {
    return this.modFiles.delete(id);
  }
}

// Use database storage
export const storage = new DatabaseStorage();

// Uncomment to use memory storage instead
// export const storage = new MemStorage();