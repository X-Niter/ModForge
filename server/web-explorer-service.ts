import { pgTable, serial, text, integer, jsonb, timestamp, boolean } from "drizzle-orm/pg-core";
import { db } from "./db";
import { eq, and, desc, sql } from "drizzle-orm";
import axios from "axios";
import { JSDOM } from "jsdom";
import * as cheerio from "cheerio";

// Define schemas for web sources and extracted content
export const webSources = pgTable('web_sources', {
  id: serial('id').primaryKey(),
  url: text('url').notNull(),
  title: text('title').notNull(),
  description: text('description'),
  lastScraped: timestamp('last_scraped').defaultNow(),
  status: text('status').notNull().default('pending'),
  contentType: text('content_type').notNull(),
  discoveredPages: integer('discovered_pages').notNull().default(0),
  extractedPatterns: integer('extracted_patterns').notNull().default(0),
  tags: text('tags').array(),
  createdAt: timestamp('created_at').defaultNow(),
});

export const webPages = pgTable('web_pages', {
  id: serial('id').primaryKey(),
  sourceId: integer('source_id').notNull(),
  url: text('url').notNull(),
  title: text('title'),
  content: text('content'),
  lastScraped: timestamp('last_scraped').defaultNow(),
  status: text('status').notNull().default('pending'),
  metaData: jsonb('meta_data'),
  createdAt: timestamp('created_at').defaultNow(),
});

export const extractedContent = pgTable('extracted_content', {
  id: serial('id').primaryKey(),
  pageId: integer('page_id').notNull(),
  sourceId: integer('source_id').notNull(),
  contentType: text('content_type').notNull(), // 'code', 'error', 'tutorial', 'api'
  title: text('title'),
  content: text('content').notNull(),
  context: text('context'),
  language: text('language'),
  tags: text('tags').array(),
  confidence: integer('confidence').notNull().default(100), // 0-100
  createdAt: timestamp('created_at').defaultNow(),
});

/**
 * Add a new documentation source to be explored
 */
export async function addWebSource(
  url: string,
  description: string = "",
  contentType: string = "documentation",
  tags: string[] = []
): Promise<typeof webSources.$inferSelect> {
  try {
    // Get page title by making a request
    let title = url;
    try {
      const response = await axios.get(url, { timeout: 5000 });
      const dom = new JSDOM(response.data);
      const pageTitle = dom.window.document.title;
      if (pageTitle) {
        title = pageTitle;
      }
    } catch (error) {
      console.error(`Error fetching page title from ${url}:`, error);
    }

    // Insert the new source
    const [newSource] = await db.insert(webSources).values({
      url,
      title,
      description,
      contentType,
      tags,
      status: 'pending'
    }).returning();

    return newSource;
  } catch (error) {
    console.error("Error adding web source:", error);
    throw error;
  }
}

/**
 * Get all web sources
 */
export async function getWebSources(): Promise<typeof webSources.$inferSelect[]> {
  try {
    return await db.select().from(webSources).orderBy(desc(webSources.createdAt));
  } catch (error) {
    console.error("Error getting web sources:", error);
    return [];
  }
}

/**
 * Get web source by ID
 */
export async function getWebSourceById(id: number): Promise<typeof webSources.$inferSelect | undefined> {
  try {
    const [source] = await db.select().from(webSources).where(eq(webSources.id, id));
    return source;
  } catch (error) {
    console.error(`Error getting web source with ID ${id}:`, error);
    return undefined;
  }
}

/**
 * Update a web source
 */
export async function updateWebSource(
  id: number,
  data: Partial<typeof webSources.$inferInsert>
): Promise<typeof webSources.$inferSelect | undefined> {
  try {
    const [updatedSource] = await db.update(webSources)
      .set(data)
      .where(eq(webSources.id, id))
      .returning();
    
    return updatedSource;
  } catch (error) {
    console.error(`Error updating web source with ID ${id}:`, error);
    return undefined;
  }
}

/**
 * Delete a web source
 */
export async function deleteWebSource(id: number): Promise<boolean> {
  try {
    const result = await db.delete(webSources).where(eq(webSources.id, id));
    return true;
  } catch (error) {
    console.error(`Error deleting web source with ID ${id}:`, error);
    return false;
  }
}

/**
 * Extract content from a page
 * @param html The HTML content of the page
 * @param url The URL of the page
 * @returns Extracted content items
 */
type ExtractedContent = {
  contentType: string;
  title: string;
  content: string;
  language?: string;
  context?: string;
};

function extractContentFromPage(html: string, url: string): Array<ExtractedContent> {
  const results: ExtractedContent[] = [];
  const $ = cheerio.load(html);

  // Extract code blocks
  $('pre code').each((i: number, element: any) => {
    const codeContent = $(element).text();
    if (codeContent.trim().length > 10) { // Only capture non-trivial code
      let language = $(element).attr('class') || '';
      if (language.includes('language-')) {
        language = language.split('language-')[1].split(' ')[0];
      } else {
        language = guessCodeLanguage(codeContent);
      }

      // Try to find a heading above this code block
      let title = '';
      let context = '';
      
      // Look for a heading (h1-h4) that precedes this code block
      let heading = $(element).closest('div, section').prev('h1, h2, h3, h4').first();
      if (heading.length) {
        title = heading.text().trim();
      }
      
      // Look for a paragraph that might provide context
      let paragraph = $(element).closest('div, section').prev('p').first();
      if (paragraph.length) {
        context = paragraph.text().trim();
      }

      results.push({
        contentType: 'code',
        title: title || `Code snippet from ${url}`,
        content: codeContent,
        language,
        context
      });
    }
  });

  // Extract error messages and solutions
  $('.error, .exception, .warning, .alert, [class*="error"], [class*="exception"]').each((i: number, element: any) => {
    const errorContent = $(element).text().trim();
    if (errorContent.length > 10 && containsErrorPattern(errorContent)) {
      let solution = '';
      
      // Look for solution text nearby
      const nextElements = $(element).nextAll('p, div, pre').slice(0, 3);
      if (nextElements.length) {
        solution = nextElements.map((i: number, el: any) => $(el).text().trim()).get().join('\n');
      }
      
      results.push({
        contentType: 'error',
        title: `Error solution from ${url}`,
        content: errorContent,
        context: solution
      });
    }
  });

  // Extract tutorials
  $('article, .tutorial, [class*="tutorial"], .guide, [class*="guide"]').each((i: number, element: any) => {
    let title = $(element).find('h1, h2, h3').first().text().trim();
    const content = $(element).text().trim();
    
    if (content.length > 100) { // Only capture substantial tutorials
      if (!title) {
        title = `Tutorial from ${url}`;
      }
      
      results.push({
        contentType: 'tutorial',
        title,
        content: content.substring(0, 5000) // Limit content length
      });
    }
  });

  return results;
}

/**
 * Simple function to guess the programming language from code content
 */
function guessCodeLanguage(code: string): string {
  if (code.includes('public class') || code.includes('private void') || code.includes('extends')) {
    return 'java';
  }
  if (code.includes('function') || code.includes('const ') || code.includes('let ')) {
    return 'javascript';
  }
  if (code.includes('def ') || code.includes('import ') && code.includes(':')) {
    return 'python';
  }
  if (code.includes('<html>') || code.includes('</div>')) {
    return 'html';
  }
  if (code.includes('@media') || code.includes('margin:') || code.includes('padding:')) {
    return 'css';
  }
  if (code.includes('dependencies {') || code.includes('plugins {')) {
    return 'gradle';
  }
  return 'unknown';
}

/**
 * Check if text contains error patterns
 */
function containsErrorPattern(text: string): boolean {
  const errorPatterns = [
    'error', 'exception', 'failed', 'failure', 'crash', 'problem',
    'unable to', 'cannot find', 'not found', 'is null', 'NullPointerException',
    'IndexOutOfBoundsException', 'ClassNotFoundException'
  ];
  
  return errorPatterns.some(pattern => text.toLowerCase().includes(pattern));
}

/**
 * Get web scraping statistics
 */
export async function getWebScrapingStats() {
  try {
    // Get total sources count
    const [{ count: sourcesCount }] = await db
      .select({ count: sql`count(*)` })
      .from(webSources);
    
    // Get total pages count
    const [{ count: pagesCount }] = await db
      .select({ count: sql`count(*)` })
      .from(webPages);
    
    // Get extracted content stats
    const [{ count: patternsCount }] = await db
      .select({ count: sql`count(*)` })
      .from(extractedContent);
    
    // Get code snippets count
    const [{ count: codeSnippets }] = await db
      .select({ count: sql`count(*)` })
      .from(extractedContent)
      .where(eq(extractedContent.contentType, 'code'));
    
    // Get error examples count
    const [{ count: errorExamples }] = await db
      .select({ count: sql`count(*)` })
      .from(extractedContent)
      .where(eq(extractedContent.contentType, 'error'));
    
    // Get active scrapes count
    const [{ count: activeScrapes }] = await db
      .select({ count: sql`count(*)` })
      .from(webSources)
      .where(eq(webSources.status, 'active'));
    
    // Get pending sources count
    const [{ count: pendingSources }] = await db
      .select({ count: sql`count(*)` })
      .from(webSources)
      .where(eq(webSources.status, 'pending'));
    
    // Get failed sources count
    const [{ count: failedSources }] = await db
      .select({ count: sql`count(*)` })
      .from(webSources)
      .where(eq(webSources.status, 'error'));
    
    return {
      totalSources: Number(sourcesCount),
      totalPages: Number(pagesCount),
      totalPatterns: Number(patternsCount),
      codeSnippets: Number(codeSnippets),
      errorExamples: Number(errorExamples),
      lastUpdated: new Date().toISOString(),
      activeScrapes: Number(activeScrapes),
      pendingSources: Number(pendingSources),
      failedSources: Number(failedSources)
    };
  } catch (error) {
    console.error("Error getting web scraping stats:", error);
    return {
      totalSources: 0,
      totalPages: 0,
      totalPatterns: 0,
      codeSnippets: 0,
      errorExamples: 0,
      lastUpdated: new Date().toISOString(),
      activeScrapes: 0,
      pendingSources: 0,
      failedSources: 0
    };
  }
}

/**
 * Trigger web scraping for pending sources
 */
export async function triggerWebScraping(): Promise<{
  success: boolean;
  message: string;
}> {
  try {
    // In a real implementation, this would start a background process
    // For demo purposes, we'll just mark a source as active
    const pendingSources = await db
      .select()
      .from(webSources)
      .where(eq(webSources.status, 'pending'))
      .limit(1);
    
    if (pendingSources.length > 0) {
      await db.update(webSources)
        .set({ status: 'active' })
        .where(eq(webSources.id, pendingSources[0].id));
      
      return {
        success: true,
        message: `Started scraping for source: ${pendingSources[0].url}`
      };
    }
    
    return {
      success: true,
      message: "No pending sources to scrape"
    };
  } catch (error) {
    console.error("Error triggering web scraping:", error);
    return {
      success: false,
      message: `Error: ${error instanceof Error ? error.message : String(error)}`
    };
  }
}