import { Request, Response, Router } from "express";
import { z } from "zod";
import * as jarAnalyzerService from "../jar-analyzer-service";
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import multer from 'multer';

const router = Router();

// Set up multer storage for file uploads
const uploadDir = path.join(os.tmpdir(), 'jar-uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    const filename = uniqueSuffix + '-' + file.originalname;
    cb(null, filename);
  }
});

const upload = multer({ 
  storage: storage,
  fileFilter: (req, file, cb) => {
    // Accept only jar files
    if (file.mimetype === 'application/java-archive' || 
        file.originalname.endsWith('.jar')) {
      cb(null, true);
    } else {
      cb(null, false);
      return cb(new Error('Only JAR files are allowed'));
    }
  },
  limits: {
    fileSize: 100 * 1024 * 1024 // 100MB max file size
  }
});

// Validation schema for JAR uploads
const jarUploadSchema = z.object({
  modLoader: z.string().optional(), // Now optional since we auto-detect it
  version: z.string().optional(),
  mcVersion: z.string().optional() // Optional since we auto-detect it
});

// Get all JAR files
router.get("/jars", async (req: Request, res: Response) => {
  try {
    const jars = await jarAnalyzerService.getAllJarFiles();
    return res.json(jars);
  } catch (error) {
    console.error("Error fetching JAR files:", error);
    return res.status(500).json({ error: "Failed to fetch JAR files" });
  }
});

// Get a single JAR file by ID
router.get("/jars/:id", async (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID format" });
    }

    const jar = await jarAnalyzerService.getJarFileById(id);
    if (!jar) {
      return res.status(404).json({ error: "JAR file not found" });
    }

    return res.json(jar);
  } catch (error) {
    console.error(`Error fetching JAR file ${req.params.id}:`, error);
    return res.status(500).json({ error: "Failed to fetch JAR file" });
  }
});

// Get extracted classes for a JAR file
router.get("/jars/:id/classes", async (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID format" });
    }

    const classes = await jarAnalyzerService.getExtractedClassesByJarId(id);
    return res.json(classes);
  } catch (error) {
    console.error(`Error fetching classes for JAR ${req.params.id}:`, error);
    return res.status(500).json({ error: "Failed to fetch classes" });
  }
});

// Upload and process a JAR file
router.post("/jars/upload", upload.single('jarFile'), async (req: Request, res: Response) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "No JAR file provided" });
    }

    const validationResult = jarUploadSchema.safeParse(req.body);
    if (!validationResult.success) {
      return res.status(400).json({ 
        error: "Invalid jar data", 
        details: validationResult.error.errors 
      });
    }

    const { modLoader, version, mcVersion } = validationResult.data;
    
    const jarFile = await jarAnalyzerService.processUploadedJarFile(
      req.file.path,
      req.file.originalname,
      modLoader,
      version,
      mcVersion
    );

    return res.status(201).json(jarFile);
  } catch (error) {
    console.error("Error uploading JAR file:", error);
    return res.status(500).json({ 
      error: "Failed to upload JAR file",
      details: error instanceof Error ? error.message : String(error)
    });
  }
});

// Delete a JAR file
router.delete("/jars/:id", async (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID format" });
    }

    const success = await jarAnalyzerService.deleteJarFile(id);
    if (!success) {
      return res.status(404).json({ error: "JAR file not found" });
    }

    return res.json({ success: true });
  } catch (error) {
    console.error(`Error deleting JAR file ${req.params.id}:`, error);
    return res.status(500).json({ error: "Failed to delete JAR file" });
  }
});

// Search for JAR files on CurseForge
router.get("/search/curseforge", async (req: Request, res: Response) => {
  try {
    const query = req.query.q as string;
    if (!query) {
      return res.status(400).json({ error: "Search query is required" });
    }

    const results = await jarAnalyzerService.searchCurseForgeJars(query);
    return res.json(results);
  } catch (error) {
    console.error("Error searching CurseForge:", error);
    return res.status(500).json({ error: "Failed to search CurseForge" });
  }
});

// Search for JAR files on Modrinth
router.get("/search/modrinth", async (req: Request, res: Response) => {
  try {
    const query = req.query.q as string;
    if (!query) {
      return res.status(400).json({ error: "Search query is required" });
    }

    const results = await jarAnalyzerService.searchModrinthJars(query);
    return res.json(results);
  } catch (error) {
    console.error("Error searching Modrinth:", error);
    return res.status(500).json({ error: "Failed to search Modrinth" });
  }
});

// Download a JAR file from a URL
router.post("/jars/download", async (req: Request, res: Response) => {
  try {
    const { url, modName, modLoader, version, mcVersion } = req.body;
    
    // Only URL and modName are required, other fields will be auto-detected
    if (!url || !modName) {
      return res.status(400).json({ error: "URL and mod name are required" });
    }

    const jarFile = await jarAnalyzerService.downloadJarFile(
      url,
      modName,
      modLoader,
      version,
      mcVersion
    );

    if (!jarFile) {
      return res.status(500).json({ error: "Failed to download JAR file" });
    }

    return res.status(201).json(jarFile);
  } catch (error) {
    console.error("Error downloading JAR file:", error);
    return res.status(500).json({ error: "Failed to download JAR file" });
  }
});

// Get JAR analysis statistics
router.get("/stats", async (_req: Request, res: Response) => {
  try {
    const stats = await jarAnalyzerService.getJarAnalysisStats();
    return res.json(stats);
  } catch (error) {
    console.error("Error fetching JAR analysis stats:", error);
    return res.status(500).json({ error: "Failed to fetch JAR analysis statistics" });
  }
});

export default router;