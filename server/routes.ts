import type { Express, Request, Response } from "express";
import { createServer, type Server } from "http";
import { storage } from "./storage";
import { compileMod } from "./compiler";
import { generateModCode, fixCompilationErrors, addModFeatures } from "./ai-service";
import { pushModToGitHub } from "./github";
import { z } from "zod";
import { insertModSchema } from "@shared/schema";
import { BuildStatus } from "@/types";
import axios from "axios";

export async function registerRoutes(app: Express): Promise<Server> {
  // Create HTTP server
  const httpServer = createServer(app);

  // Get all mods for a user
  app.get("/api/mods", async (req, res) => {
    try {
      // For this demo, we'll use user ID 1
      const userId = 1;
      const mods = await storage.getModsByUserId(userId);
      res.json({ mods });
    } catch (error) {
      console.error("Error fetching mods:", error);
      res.status(500).json({ message: "Failed to fetch mods" });
    }
  });

  // Get a specific mod
  app.get("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      res.json({ mod });
    } catch (error) {
      console.error("Error fetching mod:", error);
      res.status(500).json({ message: "Failed to fetch mod" });
    }
  });

  // Create a new mod
  app.post("/api/mods", async (req, res) => {
    try {
      // Validate request body
      const validationResult = insertModSchema.safeParse(req.body);
      if (!validationResult.success) {
        return res.status(400).json({ 
          message: "Invalid mod data",
          errors: validationResult.error.errors 
        });
      }

      // For this demo, we'll use user ID 1
      const userId = 1;
      const modData = validationResult.data;

      // Create the mod in storage
      const mod = await storage.createMod({
        ...modData,
        userId
      });

      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber: 1,
        status: BuildStatus.InProgress,
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #1 for ${mod.name}...\n`,
        downloadUrl: null
      });

      // Start generating code asynchronously
      generateModCodeAsync(mod, build);

      res.status(201).json({ mod, build });
    } catch (error) {
      console.error("Error creating mod:", error);
      res.status(500).json({ message: "Failed to create mod" });
    }
  });

  // Update an existing mod
  app.patch("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Validate request body (partial schema)
      const updatedMod = await storage.updateMod(modId, req.body);
      res.json({ mod: updatedMod });
    } catch (error) {
      console.error("Error updating mod:", error);
      res.status(500).json({ message: "Failed to update mod" });
    }
  });

  // Delete a mod
  app.delete("/api/mods/:id", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const success = await storage.deleteMod(modId);
      if (!success) {
        return res.status(404).json({ message: "Mod not found" });
      }

      res.status(204).send();
    } catch (error) {
      console.error("Error deleting mod:", error);
      res.status(500).json({ message: "Failed to delete mod" });
    }
  });

  // Get all builds for a mod
  app.get("/api/mods/:id/builds", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const builds = await storage.getBuildsByModId(modId);
      res.json({ builds });
    } catch (error) {
      console.error("Error fetching builds:", error);
      res.status(500).json({ message: "Failed to fetch builds" });
    }
  });

  // Get a specific build
  app.get("/api/mods/:modId/builds/:buildId", async (req, res) => {
    try {
      const buildId = parseInt(req.params.buildId, 10);
      if (isNaN(buildId)) {
        return res.status(400).json({ message: "Invalid build ID" });
      }

      const build = await storage.getBuild(buildId);
      if (!build) {
        return res.status(404).json({ message: "Build not found" });
      }

      res.json({ build });
    } catch (error) {
      console.error("Error fetching build:", error);
      res.status(500).json({ message: "Failed to fetch build" });
    }
  });

  // Manually trigger compilation
  app.post("/api/mods/:id/compile", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }

      // Get the latest build number for this mod
      const builds = await storage.getBuildsByModId(modId);
      const buildNumber = builds.length > 0 ? builds[0].buildNumber + 1 : 1;

      // Create a new build
      const build = await storage.createBuild({
        modId: mod.id,
        buildNumber,
        status: BuildStatus.InProgress,
        errorCount: 0,
        warningCount: 0,
        logs: `Starting build #${buildNumber} for ${mod.name}...\n`,
        downloadUrl: null
      });

      // Start compilation asynchronously
      compileModAsync(mod, build);

      res.status(201).json({ build });
    } catch (error) {
      console.error("Error starting compilation:", error);
      res.status(500).json({ message: "Failed to start compilation" });
    }
  });

  // Verify GitHub token
  app.post("/api/github/verify-token", async (req, res) => {
    try {
      const { token } = req.body;
      if (!token) {
        return res.status(400).json({ message: "GitHub token is required" });
      }
      
      try {
        // Test the token by making a request to the GitHub API
        const response = await axios.get('https://api.github.com/user', {
          headers: {
            'Authorization': `token ${token}`,
            'Accept': 'application/vnd.github.v3+json'
          }
        });
        
        // If we get here, the token is valid
        res.json({ 
          valid: true, 
          username: response.data.login 
        });
      } catch (githubError) {
        // Token is invalid or doesn't have the right permissions
        res.status(200).json({ 
          valid: false,
          message: "Invalid GitHub token or insufficient permissions"
        });
      }
    } catch (error) {
      console.error("Error verifying GitHub token:", error);
      res.status(500).json({ message: "Failed to verify GitHub token" });
    }
  });
  
  // Push to GitHub
  app.post("/api/mods/:id/push-to-github", async (req, res) => {
    try {
      const modId = parseInt(req.params.id, 10);
      if (isNaN(modId)) {
        return res.status(400).json({ message: "Invalid mod ID" });
      }

      const mod = await storage.getMod(modId);
      if (!mod) {
        return res.status(404).json({ message: "Mod not found" });
      }
      
      const { token } = req.body;
      if (!token) {
        return res.status(400).json({ message: "GitHub token is required" });
      }

      // Push to GitHub
      const result = await pushModToGitHub(modId, token);
      
      if (!result.success) {
        return res.status(400).json({ 
          success: false,
          error: result.error || "Failed to push to GitHub"
        });
      }

      res.json({ 
        success: true,
        repoUrl: result.repoUrl,
        owner: result.owner
      });
    } catch (error) {
      console.error("Error pushing to GitHub:", error);
      res.status(500).json({ 
        success: false,
        message: "Failed to push to GitHub",
        error: error instanceof Error ? error.message : String(error)
      });
    }
  });

  // Download mod JAR
  app.get("/api/mods/:id/download", (req, res) => {
    // In a real implementation, this would serve the compiled JAR file
    // For now, we'll just return a message
    res.status(404).json({ message: "Download functionality not implemented" });
  });

  // AI Service Endpoints
  
  // Generate code
  app.post("/api/ai/generate-code", async (req, res) => {
    try {
      const { modName, modDescription, modLoader, mcVersion, idea } = req.body;
      if (!modName || !modLoader || !idea) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await generateModCode(
        modName,
        modDescription,
        modLoader,
        mcVersion,
        idea
      );

      res.json(result);
    } catch (error) {
      console.error("Error generating code:", error);
      res.status(500).json({ message: "Failed to generate code" });
    }
  });

  // Fix compilation errors
  app.post("/api/ai/fix-errors", async (req, res) => {
    try {
      const { files, errors } = req.body;
      if (!files || !errors) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await fixCompilationErrors(files, errors);
      res.json(result);
    } catch (error) {
      console.error("Error fixing errors:", error);
      res.status(500).json({ message: "Failed to fix errors" });
    }
  });

  // Add features
  app.post("/api/ai/add-features", async (req, res) => {
    try {
      const { files, newFeatureDescription } = req.body;
      if (!files || !newFeatureDescription) {
        return res.status(400).json({ message: "Missing required fields" });
      }

      const result = await addModFeatures(files, newFeatureDescription);
      res.json(result);
    } catch (error) {
      console.error("Error adding features:", error);
      res.status(500).json({ message: "Failed to add features" });
    }
  });

  return httpServer;
}

// Asynchronous code generation process
async function generateModCodeAsync(mod: any, build: any) {
  try {
    // Generate code
    const result = await generateModCode(
      mod.name,
      mod.description,
      mod.modLoader,
      mod.minecraftVersion,
      mod.idea
    );

    // Update build logs - use null coalescing to handle undefined case
    const initialUpdate = await storage.updateBuild(build.id, {
      logs: build.logs + result.logs
    });
    
    if (!initialUpdate) {
      throw new Error(`Failed to update build ${build.id}`);
    }
    
    let currentLogs = initialUpdate.logs || '';

    // Store files in storage
    for (const file of result.files) {
      await storage.createModFile({
        modId: mod.id,
        path: file.path,
        content: file.content
      });
    }

    // Compile the mod - add to logs safely
    currentLogs += "\nStarting compilation process...\n";
    const preCompileUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
    
    if (preCompileUpdate) {
      currentLogs = preCompileUpdate.logs;
    }
    
    const compileResult = await compileMod(mod.id);

    // Update build with compilation results
    const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
    currentLogs += compileResult.logs;
    
    const postCompileUpdate = await storage.updateBuild(build.id, {
      status,
      logs: currentLogs,
      errorCount: compileResult.errors.length,
      warningCount: compileResult.warnings.length,
      downloadUrl: compileResult.downloadUrl
    });
    
    if (postCompileUpdate) {
      currentLogs = postCompileUpdate.logs;
    }

    // If compilation failed and auto-fix is enabled, try to fix errors
    if (!compileResult.success && compileResult.errors.length > 0 && 
        (mod.autoFixLevel === "Balanced" || mod.autoFixLevel === "Aggressive")) {
      
      // Add fix logs
      currentLogs += "\nAttempting to fix compilation errors...\n";
      const preFixUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preFixUpdate) {
        currentLogs = preFixUpdate.logs;
      }

      // Get all mod files
      const modFiles = await storage.getModFilesByModId(mod.id);
      const files = modFiles.map(file => ({
        path: file.path,
        content: file.content
      }));

      // Fix errors
      const fixResult = await fixCompilationErrors(files, compileResult.errors);

      // Update logs
      currentLogs += fixResult.logs;
      const fixLogsUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (fixLogsUpdate) {
        currentLogs = fixLogsUpdate.logs;
      }

      // Update mod files with fixes
      for (const file of fixResult.files) {
        const existingFile = modFiles.find(f => f.path === file.path);
        if (existingFile) {
          await storage.updateModFile(existingFile.id, {
            content: file.content
          });
        } else {
          await storage.createModFile({
            modId: mod.id,
            path: file.path,
            content: file.content
          });
        }
      }

      // Try compilation again
      currentLogs += "\nRetrying compilation after fixes...\n";
      const preRetryUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preRetryUpdate) {
        currentLogs = preRetryUpdate.logs;
      }

      const retryResult = await compileMod(mod.id);
      
      // Update build with new compilation results
      const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
      currentLogs += retryResult.logs;
      
      await storage.updateBuild(build.id, {
        status: newStatus,
        logs: currentLogs,
        errorCount: retryResult.errors.length,
        warningCount: retryResult.warnings.length,
        downloadUrl: retryResult.downloadUrl
      });
    }
  } catch (error) {
    console.error("Error in generateModCodeAsync:", error);
    // Update build with error
    await storage.updateBuild(build.id, {
      status: BuildStatus.Failed,
      logs: build.logs + `\nError during mod generation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1,
      warningCount: 0
    });
  }
}

// Asynchronous compilation process
async function compileModAsync(mod: any, build: any) {
  let currentLogs = build.logs || '';
  
  try {
    // Compile the mod
    const compileResult = await compileMod(mod.id);

    // Update build with compilation results
    const status = compileResult.success ? BuildStatus.Success : BuildStatus.Failed;
    currentLogs += compileResult.logs;
    
    const compileUpdate = await storage.updateBuild(build.id, {
      status,
      logs: currentLogs,
      errorCount: compileResult.errors.length,
      warningCount: compileResult.warnings.length,
      downloadUrl: compileResult.downloadUrl
    });
    
    if (compileUpdate) {
      currentLogs = compileUpdate.logs;
    }

    // If compilation failed and auto-fix is enabled, try to fix errors
    if (!compileResult.success && compileResult.errors.length > 0 && 
        (mod.autoFixLevel === "Balanced" || mod.autoFixLevel === "Aggressive")) {
      
      // Add to logs safely
      currentLogs += "\nAttempting to fix compilation errors...\n";
      const preFixUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preFixUpdate) {
        currentLogs = preFixUpdate.logs;
      }

      // Get all mod files
      const modFiles = await storage.getModFilesByModId(mod.id);
      const files = modFiles.map(file => ({
        path: file.path,
        content: file.content
      }));

      // Fix errors
      const fixResult = await fixCompilationErrors(files, compileResult.errors);

      // Update logs
      currentLogs += fixResult.logs;
      const fixLogsUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (fixLogsUpdate) {
        currentLogs = fixLogsUpdate.logs;
      }

      // Update mod files with fixes
      for (const file of fixResult.files) {
        const existingFile = modFiles.find(f => f.path === file.path);
        if (existingFile) {
          await storage.updateModFile(existingFile.id, {
            content: file.content
          });
        } else {
          await storage.createModFile({
            modId: mod.id,
            path: file.path,
            content: file.content
          });
        }
      }

      // Try compilation again
      currentLogs += "\nRetrying compilation after fixes...\n";
      const preRetryUpdate = await storage.updateBuild(build.id, { logs: currentLogs });
      
      if (preRetryUpdate) {
        currentLogs = preRetryUpdate.logs;
      }

      const retryResult = await compileMod(mod.id);
      
      // Update build with new compilation results
      const newStatus = retryResult.success ? BuildStatus.Success : BuildStatus.Failed;
      currentLogs += retryResult.logs;
      
      await storage.updateBuild(build.id, {
        status: newStatus,
        logs: currentLogs,
        errorCount: retryResult.errors.length,
        warningCount: retryResult.warnings.length,
        downloadUrl: retryResult.downloadUrl
      });
    }
  } catch (error) {
    console.error("Error in compileModAsync:", error);
    // Update build with error
    await storage.updateBuild(build.id, {
      status: BuildStatus.Failed,
      logs: build.logs + `\nError during compilation: ${error instanceof Error ? error.message : String(error)}\n`,
      errorCount: 1,
      warningCount: 0
    });
  }
}
