import { Router } from 'express';
import { z } from 'zod';
import {
  addWebSource,
  getWebSources,
  getWebSourceById,
  updateWebSource,
  deleteWebSource,
  getWebScrapingStats,
  triggerWebScraping
} from '../web-explorer-service';

const router = Router();

// Schema for adding a web source
const addWebSourceSchema = z.object({
  url: z.string().url(),
  description: z.string().optional(),
  contentType: z.string(),
  tags: z.array(z.string()).optional()
});

// Get all web sources
router.get('/sources', async (req, res) => {
  try {
    const sources = await getWebSources();
    res.json(sources);
  } catch (error) {
    console.error('Error fetching web sources:', error);
    res.status(500).json({ 
      error: 'Failed to fetch web sources',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get web source by ID
router.get('/sources/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: 'Invalid ID' });
    }

    const source = await getWebSourceById(id);
    if (!source) {
      return res.status(404).json({ error: 'Source not found' });
    }

    res.json(source);
  } catch (error) {
    console.error(`Error fetching web source:`, error);
    res.status(500).json({ 
      error: 'Failed to fetch web source',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Add a new web source
router.post('/sources', async (req, res) => {
  try {
    const validationResult = addWebSourceSchema.safeParse(req.body);
    if (!validationResult.success) {
      return res.status(400).json({ 
        error: 'Invalid request data',
        details: validationResult.error.format()
      });
    }

    const { url, description, contentType, tags } = validationResult.data;
    const newSource = await addWebSource(url, description, contentType, tags);
    res.status(201).json(newSource);
  } catch (error) {
    console.error('Error adding web source:', error);
    res.status(500).json({ 
      error: 'Failed to add web source',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Update a web source
router.patch('/sources/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: 'Invalid ID' });
    }

    const source = await getWebSourceById(id);
    if (!source) {
      return res.status(404).json({ error: 'Source not found' });
    }

    const updatedSource = await updateWebSource(id, req.body);
    res.json(updatedSource);
  } catch (error) {
    console.error(`Error updating web source:`, error);
    res.status(500).json({ 
      error: 'Failed to update web source',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Delete a web source
router.delete('/sources/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: 'Invalid ID' });
    }

    const success = await deleteWebSource(id);
    if (!success) {
      return res.status(404).json({ error: 'Source not found' });
    }

    res.status(204).send();
  } catch (error) {
    console.error(`Error deleting web source:`, error);
    res.status(500).json({ 
      error: 'Failed to delete web source',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get web scraping statistics
router.get('/stats', async (req, res) => {
  try {
    const stats = await getWebScrapingStats();
    res.json(stats);
  } catch (error) {
    console.error('Error fetching web scraping stats:', error);
    res.status(500).json({ 
      error: 'Failed to fetch web scraping stats',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Trigger web scraping
router.post('/trigger', async (req, res) => {
  try {
    const result = await triggerWebScraping();
    res.json(result);
  } catch (error) {
    console.error('Error triggering web scraping:', error);
    res.status(500).json({ 
      error: 'Failed to trigger web scraping',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

export default router;