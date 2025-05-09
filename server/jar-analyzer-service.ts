import { pgTable, serial, text, integer, jsonb, timestamp, boolean } from "drizzle-orm/pg-core";
import { db } from "./db";
import { eq, and, desc, sql } from "drizzle-orm";
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import axios from 'axios';
import { spawn } from 'child_process';
import { promisify } from 'util';
import * as cheerio from 'cheerio';

// Define schema for jar files and extracted classes
export const jarFiles = pgTable('jar_files', {
  id: serial('id').primaryKey(),
  fileName: text('file_name').notNull(),
  filePath: text('file_path'),
  source: text('source').notNull(), // 'upload', 'curseforge', 'modrinth', etc.
  modLoader: text('mod_loader').notNull(), // 'forge', 'neoforge', 'fabric', 'quilt', etc.
  version: text('version'),
  mcVersion: text('mc_version'),
  extractedClassCount: integer('extracted_class_count').notNull().default(0),
  status: text('status').notNull().default('pending'), // 'pending', 'processing', 'completed', 'error'
  errorMessage: text('error_message'),
  createdAt: timestamp('created_at').defaultNow(),
});

export const extractedClasses = pgTable('extracted_classes', {
  id: serial('id').primaryKey(),
  jarId: integer('jar_id').notNull(),
  className: text('class_name').notNull(),
  packageName: text('package_name'),
  classType: text('class_type').notNull(), // 'class', 'interface', 'enum', 'annotation'
  content: text('content').notNull(),
  imports: text('imports').array(),
  methods: jsonb('methods'),
  fields: jsonb('fields'),
  isPublic: boolean('is_public').notNull().default(true),
  analyzed: boolean('analyzed').notNull().default(false),
  createdAt: timestamp('created_at').defaultNow(),
});

/**
 * Get all JAR files in the system
 */
export async function getAllJarFiles(): Promise<typeof jarFiles.$inferSelect[]> {
  try {
    return await db.select().from(jarFiles).orderBy(desc(jarFiles.createdAt));
  } catch (error) {
    console.error("Error fetching JAR files:", error);
    return [];
  }
}

/**
 * Get JAR file by ID
 */
export async function getJarFileById(id: number): Promise<typeof jarFiles.$inferSelect | undefined> {
  try {
    const [jarFile] = await db.select().from(jarFiles).where(eq(jarFiles.id, id));
    return jarFile;
  } catch (error) {
    console.error(`Error getting JAR file with ID ${id}:`, error);
    return undefined;
  }
}

/**
 * Get extracted classes for a JAR file
 */
export async function getExtractedClassesByJarId(
  jarId: number
): Promise<typeof extractedClasses.$inferSelect[]> {
  try {
    return await db.select()
      .from(extractedClasses)
      .where(eq(extractedClasses.jarId, jarId))
      .orderBy(extractedClasses.packageName, extractedClasses.className);
  } catch (error) {
    console.error(`Error getting extracted classes for JAR ID ${jarId}:`, error);
    return [];
  }
}

/**
 * Process an uploaded JAR file
 * @param filePath Path to the uploaded JAR file
 * @param fileName Original file name
 * @param modLoader Mod loader type (forge, fabric, etc.)
 * @param version Version of the mod loader
 * @param mcVersion Minecraft version
 */
export async function processUploadedJarFile(
  filePath: string,
  fileName: string,
  modLoader: string,
  version?: string,
  mcVersion?: string
): Promise<typeof jarFiles.$inferSelect> {
  try {
    // Insert the JAR file record
    const [jarFile] = await db.insert(jarFiles).values({
      fileName,
      filePath,
      source: 'upload',
      modLoader,
      version,
      mcVersion,
      status: 'pending'
    }).returning();

    // Start processing the JAR file in the background
    setTimeout(() => {
      extractJarContent(jarFile.id, filePath).catch(err => {
        console.error(`Error extracting JAR content for ID ${jarFile.id}:`, err);
        updateJarFileStatus(jarFile.id, 'error', err.message).catch(console.error);
      });
    }, 0);

    return jarFile;
  } catch (error) {
    console.error("Error processing uploaded JAR file:", error);
    throw error;
  }
}

/**
 * Update the status of a JAR file
 */
async function updateJarFileStatus(
  jarId: number, 
  status: string, 
  errorMessage?: string
): Promise<void> {
  try {
    await db.update(jarFiles)
      .set({
        status,
        errorMessage
      })
      .where(eq(jarFiles.id, jarId));
  } catch (error) {
    console.error(`Error updating JAR file status for ID ${jarId}:`, error);
  }
}

/**
 * Update the extracted class count for a JAR file
 */
async function updateJarExtractedClassCount(jarId: number, count: number): Promise<void> {
  try {
    await db.update(jarFiles)
      .set({
        extractedClassCount: count
      })
      .where(eq(jarFiles.id, jarId));
  } catch (error) {
    console.error(`Error updating extracted class count for JAR ID ${jarId}:`, error);
  }
}

/**
 * Extract content from a JAR file
 */
async function extractJarContent(jarId: number, jarFilePath: string): Promise<void> {
  try {
    await updateJarFileStatus(jarId, 'processing');
    
    // Create a temporary directory for extraction
    const tempDir = path.join(os.tmpdir(), `jar-extract-${jarId}-${Date.now()}`);
    
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir, { recursive: true });
    }

    // Execute the jar extraction command
    await executeCommand('unzip', ['-o', jarFilePath, '-d', tempDir]);
    
    // Process the extracted files
    const classFiles: Array<{ path: string, content: string }> = [];
    await findJavaClassFiles(tempDir, classFiles);
    
    // Process and store each class file
    for (const classFile of classFiles) {
      try {
        await processClassFile(jarId, classFile.path, classFile.content);
      } catch (classError) {
        console.error(`Error processing class file ${classFile.path}:`, classError);
      }
    }
    
    // Update the JAR file status
    await updateJarFileStatus(jarId, 'completed');
    await updateJarExtractedClassCount(jarId, classFiles.length);
    
    // Clean up the temp directory
    try {
      fs.rmSync(tempDir, { recursive: true, force: true });
    } catch (cleanupError) {
      console.error(`Error cleaning up temp directory ${tempDir}:`, cleanupError);
    }
  } catch (error) {
    console.error(`Error extracting JAR content for ID ${jarId}:`, error);
    await updateJarFileStatus(jarId, 'error', error instanceof Error ? error.message : String(error));
  }
}

/**
 * Execute a shell command as a Promise
 */
function executeCommand(command: string, args: string[]): Promise<string> {
  return new Promise((resolve, reject) => {
    const process = spawn(command, args);
    let stdout = '';
    let stderr = '';

    process.stdout.on('data', (data) => {
      stdout += data.toString();
    });

    process.stderr.on('data', (data) => {
      stderr += data.toString();
    });

    process.on('close', (code) => {
      if (code === 0) {
        resolve(stdout);
      } else {
        reject(new Error(`Command failed with code ${code}: ${stderr}`));
      }
    });

    process.on('error', (err) => {
      reject(err);
    });
  });
}

/**
 * Recursively find all Java class files in a directory
 */
async function findJavaClassFiles(
  dir: string, 
  results: Array<{ path: string, content: string }>
): Promise<void> {
  const files = fs.readdirSync(dir);
  
  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    
    if (stat.isDirectory()) {
      await findJavaClassFiles(fullPath, results);
    } else if (file.endsWith('.class')) {
      try {
        // Use the javap tool to decompile the class file
        const decompiled = await executeCommand('javap', ['-private', '-verbose', fullPath]);
        results.push({
          path: fullPath,
          content: decompiled
        });
      } catch (error) {
        console.error(`Error decompiling class file ${fullPath}:`, error);
      }
    } else if (file.endsWith('.java')) {
      // Java source files can be read directly
      try {
        const content = fs.readFileSync(fullPath, 'utf8');
        results.push({
          path: fullPath,
          content
        });
      } catch (error) {
        console.error(`Error reading Java file ${fullPath}:`, error);
      }
    }
  }
}

/**
 * Process a Java class file and store its information
 */
async function processClassFile(
  jarId: number, 
  filePath: string, 
  content: string
): Promise<void> {
  // Extract class information from the file path
  const relativePath = path.basename(filePath);
  const pathParts = relativePath.split('/');
  const className = pathParts[pathParts.length - 1].replace('.class', '').replace('.java', '');
  
  // Extract package name from the file path
  let packageName = '';
  const packageMatch = content.match(/package\s+([^;]+);/);
  if (packageMatch) {
    packageName = packageMatch[1];
  }
  
  // Determine class type (class, interface, enum, annotation)
  let classType = 'class';
  if (content.includes('interface ')) {
    classType = 'interface';
  } else if (content.includes('enum ')) {
    classType = 'enum';
  } else if (content.includes('@interface ')) {
    classType = 'annotation';
  }
  
  // Extract imports
  const imports: string[] = [];
  const importRegex = /import\s+([^;]+);/g;
  let importMatch;
  while ((importMatch = importRegex.exec(content)) !== null) {
    imports.push(importMatch[1]);
  }
  
  // Extract methods (basic information)
  const methods: {name: string, returnType: string, parameters: string[], isPublic: boolean}[] = [];
  const methodRegex = /(?:public|private|protected)?\s+(?:static\s+)?(?:final\s+)?([a-zA-Z0-9_<>]+)\s+([a-zA-Z0-9_]+)\s*\(([^)]*)\)/g;
  let methodMatch;
  
  while ((methodMatch = methodRegex.exec(content)) !== null) {
    const returnType = methodMatch[1];
    const methodName = methodMatch[2];
    const params = methodMatch[3].trim();
    const parameters = params.split(',').map((p: string) => p.trim()).filter((p: string) => p);
    const isPublic = methodMatch[0].includes('public');
    
    methods.push({
      name: methodName,
      returnType,
      parameters,
      isPublic
    });
  }
  
  // Extract fields (basic information)
  const fields: {name: string, type: string, isPublic: boolean}[] = [];
  const fieldRegex = /(?:public|private|protected)?\s+(?:static\s+)?(?:final\s+)?([a-zA-Z0-9_<>]+)\s+([a-zA-Z0-9_]+)\s*(?:=|;)/g;
  let fieldMatch;
  
  while ((fieldMatch = fieldRegex.exec(content)) !== null) {
    const fieldType = fieldMatch[1];
    const fieldName = fieldMatch[2];
    const isPublic = fieldMatch[0].includes('public');
    
    fields.push({
      name: fieldName,
      type: fieldType,
      isPublic
    });
  }
  
  // Is the class public?
  const isPublic = content.includes('public class') || 
                 content.includes('public interface') || 
                 content.includes('public enum') ||
                 content.includes('public @interface');
  
  // Insert into database
  await db.insert(extractedClasses).values({
    jarId,
    className,
    packageName,
    classType,
    content,
    imports,
    methods,
    fields,
    isPublic
  });
}

/**
 * Search for and download JAR files from CurseForge
 */
export async function searchCurseForgeJars(query: string): Promise<any[]> {
  try {
    // Note: This is a mock implementation
    // In a real implementation, you would need to use the CurseForge API 
    // which requires an API key
    console.log(`Searching CurseForge for: ${query}`);
    return [
      {
        name: "Example Mod",
        version: "1.0.0", 
        downloadUrl: "https://example.com/download/example-mod.jar",
        mcVersion: "1.19.2",
        modLoader: "forge"
      }
    ];
  } catch (error) {
    console.error("Error searching CurseForge:", error);
    throw error;
  }
}

/**
 * Search for and download JAR files from Modrinth
 */
export async function searchModrinthJars(query: string): Promise<any[]> {
  try {
    // Modrinth has a public API that doesn't require authentication
    const response = await axios.get(`https://api.modrinth.com/v2/search`, {
      params: {
        query,
        limit: 10,
        index: 'relevance',
        facets: [
          JSON.stringify(["categories:forge", "categories:fabric", "categories:quilt"])
        ]
      }
    });
    
    return response.data.hits.map((hit: any) => ({
      id: hit.project_id,
      name: hit.title,
      description: hit.description,
      url: `https://modrinth.com/mod/${hit.slug}`,
      author: hit.author,
      downloads: hit.downloads,
      modLoader: hit.categories.includes("forge") ? "forge" : 
                hit.categories.includes("fabric") ? "fabric" :
                hit.categories.includes("quilt") ? "quilt" : "unknown"
    }));
  } catch (error) {
    console.error("Error searching Modrinth:", error);
    return [];
  }
}

/**
 * Download a JAR file from a URL
 */
export async function downloadJarFile(
  url: string,
  modName: string,
  modLoader: string,
  version: string,
  mcVersion: string
): Promise<typeof jarFiles.$inferSelect | undefined> {
  try {
    const tempDir = path.join(os.tmpdir(), 'jar-downloads');
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir, { recursive: true });
    }
    
    const fileName = `${modName}-${version}-${mcVersion}.jar`;
    const filePath = path.join(tempDir, fileName);
    
    // Download the file
    const response = await axios({
      method: 'GET',
      url,
      responseType: 'stream'
    });
    
    const writer = fs.createWriteStream(filePath);
    response.data.pipe(writer);
    
    await new Promise((resolve, reject) => {
      writer.on('finish', resolve);
      writer.on('error', reject);
    });
    
    // Process the downloaded JAR file
    const jarFile = await processUploadedJarFile(
      filePath,
      fileName,
      modLoader,
      version,
      mcVersion
    );
    
    return jarFile;
  } catch (error) {
    console.error("Error downloading JAR file:", error);
    return undefined;
  }
}

/**
 * Get JAR analysis statistics
 */
export async function getJarAnalysisStats() {
  try {
    // Get total JAR files count
    const [{ count: jarsCount }] = await db
      .select({ count: sql`count(*)` })
      .from(jarFiles);
    
    // Get total classes count
    const [{ count: classesCount }] = await db
      .select({ count: sql`count(*)` })
      .from(extractedClasses);
    
    // Get class types breakdown
    const classTypes = await db
      .select({
        classType: extractedClasses.classType,
        count: sql`count(*)`
      })
      .from(extractedClasses)
      .groupBy(extractedClasses.classType);
    
    // Get mod loader breakdown
    const modLoaders = await db
      .select({
        modLoader: jarFiles.modLoader,
        count: sql`count(*)`
      })
      .from(jarFiles)
      .groupBy(jarFiles.modLoader);
    
    // Get completed vs pending counts
    const [{ count: completedCount }] = await db
      .select({ count: sql`count(*)` })
      .from(jarFiles)
      .where(eq(jarFiles.status, 'completed'));
    
    const [{ count: pendingCount }] = await db
      .select({ count: sql`count(*)` })
      .from(jarFiles)
      .where(eq(jarFiles.status, 'pending'));
    
    const [{ count: processingCount }] = await db
      .select({ count: sql`count(*)` })
      .from(jarFiles)
      .where(eq(jarFiles.status, 'processing'));
    
    const [{ count: errorCount }] = await db
      .select({ count: sql`count(*)` })
      .from(jarFiles)
      .where(eq(jarFiles.status, 'error'));
    
    return {
      totalJars: Number(jarsCount),
      totalClasses: Number(classesCount),
      classTypes: classTypes.map(item => ({
        type: item.classType,
        count: Number(item.count)
      })),
      modLoaders: modLoaders.map(item => ({
        loader: item.modLoader,
        count: Number(item.count)
      })),
      status: {
        completed: Number(completedCount),
        pending: Number(pendingCount),
        processing: Number(processingCount),
        error: Number(errorCount)
      },
      lastUpdated: new Date().toISOString(),
    };
  } catch (error) {
    console.error("Error getting JAR analysis stats:", error);
    return {
      totalJars: 0,
      totalClasses: 0,
      classTypes: [],
      modLoaders: [],
      status: {
        completed: 0,
        pending: 0,
        processing: 0,
        error: 0
      },
      lastUpdated: new Date().toISOString(),
    };
  }
}

/**
 * Delete a JAR file and its extracted classes
 */
export async function deleteJarFile(id: number): Promise<boolean> {
  try {
    // First, delete the extracted classes
    await db.delete(extractedClasses).where(eq(extractedClasses.jarId, id));
    
    // Then, delete the JAR file record
    await db.delete(jarFiles).where(eq(jarFiles.id, id));
    
    return true;
  } catch (error) {
    console.error(`Error deleting JAR file with ID ${id}:`, error);
    return false;
  }
}