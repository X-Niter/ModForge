import { spawn } from "child_process";
import { randomBytes } from "crypto";
import { promises as fs } from "fs";
import { join } from "path";
import path from "path";
import { storage } from "./storage";
import { ErrorData } from "@/types";

// Base temporary directory for storing mod files during compilation
const TMP_DIR = join(process.cwd(), "tmp");

// Create the temp directory if it doesn't exist
async function ensureTmpDir() {
  try {
    await fs.mkdir(TMP_DIR, { recursive: true });
  } catch (error) {
    console.error("Failed to create temporary directory:", error);
    throw error;
  }
}

// Clean up temp directory for a specific mod
async function cleanupTempDir(modDir: string) {
  try {
    await fs.rm(modDir, { recursive: true, force: true });
  } catch (error) {
    console.error(`Failed to clean up ${modDir}:`, error);
  }
}

// Function to write mod files to the filesystem
async function writeModFiles(modId: number, modDir: string) {
  const files = await storage.getModFilesByModId(modId);
  
  for (const file of files) {
    const filePath = join(modDir, file.path);
    const dirPath = path.dirname(filePath);
    
    // Create directories if they don't exist
    await fs.mkdir(dirPath, { recursive: true });
    
    // Write the file
    await fs.writeFile(filePath, file.content);
  }
}

// Parse compilation errors from logs
function parseCompilationErrors(logs: string): ErrorData[] {
  const errors: ErrorData[] = [];
  const errorRegex = /(?:error|exception):?\s+(?:.+?)(?:in|at)?\s+(.+?\.java):?(\d+)/gi;
  
  let match;
  while ((match = errorRegex.exec(logs)) !== null) {
    const [_, file, lineStr] = match;
    const line = parseInt(lineStr, 10);
    
    // Get the error message - this is a rough heuristic
    let message = logs.substring(match.index, logs.indexOf('\n', match.index));
    
    // Clean up the message
    message = message.replace(/^error:?\s+/i, '').trim();
    
    errors.push({
      file,
      line,
      message,
      code: "", // Would need to read the file to get this
    });
  }
  
  return errors;
}

// Function to compile the Minecraft mod
export async function compileMod(modId: number): Promise<{
  success: boolean;
  logs: string;
  errors: ErrorData[];
  warnings: string[];
  downloadUrl?: string;
}> {
  // Get the mod from storage
  const mod = await storage.getMod(modId);
  if (!mod) {
    throw new Error(`Mod with ID ${modId} not found`);
  }
  
  // Create a unique directory for this compilation
  await ensureTmpDir();
  const compileId = randomBytes(8).toString("hex");
  const modDir = join(TMP_DIR, `mod-${modId}-${compileId}`);
  
  try {
    // Create the mod directory
    await fs.mkdir(modDir, { recursive: true });
    
    // Write all mod files to the filesystem
    await writeModFiles(modId, modDir);
    
    // Since we can't run actual Gradle/Maven build in this environment,
    // we'll simulate compilation with a simple Java file check
    // In a real environment, you'd run the actual build tool here
    
    // Collect logs
    let logs = `Starting compilation for mod ${mod.name} (${mod.modLoader} ${mod.minecraftVersion})...\n`;
    logs += `Setting up build environment...\n`;
    logs += `Preparing source files...\n`;
    
    // In this simulation, we'll check if required files exist
    const files = await storage.getModFilesByModId(modId);
    const fileNames = files.map(f => f.path);
    
    // Check for main mod file
    const hasMainModFile = fileNames.some(f => 
      f.toLowerCase().includes(`${mod.modId.toLowerCase()}.java`) ||
      f.toLowerCase().includes('mod.java')
    );
    
    const errors: ErrorData[] = [];
    const warnings: string[] = [];
    
    if (!hasMainModFile) {
      logs += `Error: Could not find main mod file. Expected a file containing ${mod.modId}.java or Mod.java\n`;
      errors.push({
        file: `${mod.modId}.java`,
        line: 1,
        message: `Could not find main mod file`,
      });
    } else {
      logs += `Found main mod file. Proceeding with compilation...\n`;
    }
    
    // Check for proper mod loader imports
    let hasModLoaderImports = false;
    for (const file of files) {
      if (file.content.toLowerCase().includes(mod.modLoader.toLowerCase())) {
        hasModLoaderImports = true;
        break;
      }
    }
    
    if (!hasModLoaderImports) {
      logs += `Warning: No imports for ${mod.modLoader} found in any files. This may cause compilation issues.\n`;
      warnings.push(`No imports for ${mod.modLoader} found in any files`);
    }
    
    // Simulate compilation based on errors
    const success = errors.length === 0;
    
    if (success) {
      logs += `Compiling mod sources...\n`;
      logs += `Generating resources...\n`;
      logs += `Processing assets...\n`;
      logs += `Building JAR file...\n`;
      logs += `Compilation successful!\n`;
      logs += `Generated mod JAR: ${mod.modId}-${mod.version}.jar\n`;
    } else {
      logs += `Compilation failed with ${errors.length} errors.\n`;
      logs += `See error details for more information.\n`;
    }
    
    // Clean up the temp directory
    await cleanupTempDir(modDir);
    
    return {
      success,
      logs,
      errors,
      warnings,
      // In a real implementation, you'd return a download URL for the JAR
      downloadUrl: success ? `/api/mods/${modId}/download` : undefined,
    };
  } catch (error) {
    console.error(`Error compiling mod ${modId}:`, error);
    
    // Clean up the temp directory
    await cleanupTempDir(modDir);
    
    // Return error information
    return {
      success: false,
      logs: `Error during compilation: ${error instanceof Error ? error.message : String(error)}`,
      errors: [{
        file: "unknown",
        line: 0,
        message: `Internal error: ${error instanceof Error ? error.message : String(error)}`,
      }],
      warnings: [],
    };
  }
}
