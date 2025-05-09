import { Request, Response, Router } from "express";
import { z } from "zod";
import * as webExplorerService from "../web-explorer-service";

const router = Router();

// Validation schema for adding a new web source
const addWebSourceSchema = z.object({
  url: z.string().url({ message: "Please enter a valid URL" }),
  description: z.string().optional(),
  contentType: z.string(),
  tags: z.array(z.string())
});

// Get all web sources
router.get("/sources", async (_req: Request, res: Response) => {
  try {
    const sources = await webExplorerService.getWebSources();
    return res.json(sources);
  } catch (error) {
    console.error("Error fetching web sources:", error);
    return res.status(500).json({ error: "Failed to fetch web sources" });
  }
});

// Get a single web source by ID
router.get("/sources/:id", async (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID format" });
    }

    const source = await webExplorerService.getWebSourceById(id);
    if (!source) {
      return res.status(404).json({ error: "Web source not found" });
    }

    return res.json(source);
  } catch (error) {
    console.error(`Error fetching web source ${req.params.id}:`, error);
    return res.status(500).json({ error: "Failed to fetch web source" });
  }
});

// Add a new web source
router.post("/sources", async (req: Request, res: Response) => {
  try {
    const validatedData = addWebSourceSchema.parse(req.body);
    
    const source = await webExplorerService.addWebSource({
      url: validatedData.url,
      description: validatedData.description,
      contentType: validatedData.contentType,
      tags: validatedData.tags
    });

    return res.status(201).json(source);
  } catch (error) {
    if (error instanceof z.ZodError) {
      return res.status(400).json({ 
        error: "Invalid source data", 
        details: error.errors 
      });
    }
    
    console.error("Error adding web source:", error);
    return res.status(500).json({ error: "Failed to add web source" });
  }
});

// Delete a web source
router.delete("/sources/:id", async (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID format" });
    }

    const success = await webExplorerService.deleteWebSource(id);
    if (!success) {
      return res.status(404).json({ error: "Web source not found" });
    }

    return res.json({ success: true });
  } catch (error) {
    console.error(`Error deleting web source ${req.params.id}:`, error);
    return res.status(500).json({ error: "Failed to delete web source" });
  }
});

// Get web scraping statistics
router.get("/stats", async (_req: Request, res: Response) => {
  try {
    const stats = await webExplorerService.getWebScrapingStats();
    return res.json(stats);
  } catch (error) {
    console.error("Error fetching web scraping stats:", error);
    return res.status(500).json({ error: "Failed to fetch web scraping statistics" });
  }
});

// Trigger web scraping for pending sources
router.post("/trigger", async (_req: Request, res: Response) => {
  try {
    const result = await webExplorerService.triggerWebScraping();
    return res.json(result);
  } catch (error) {
    console.error("Error triggering web scraping:", error);
    return res.status(500).json({ error: "Failed to trigger web scraping" });
  }
});

export default router;