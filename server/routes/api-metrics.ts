import { Express } from "express";
import { getUsageMetrics } from "../ai-service-manager";

/**
 * Register routes for API usage metrics and analytics
 * These endpoints provide insight into how the self-learning system
 * is reducing API costs over time
 */
export function registerMetricsRoutes(app: Express): void {
  // Get current API usage metrics
  app.get("/api/metrics/usage", (req, res) => {
    try {
      const metrics = getUsageMetrics();
      
      // Calculate useful derived metrics
      const patternMatchRate = metrics.totalRequests > 0 
        ? (metrics.patternMatches / metrics.totalRequests) * 100 
        : 0;
      
      res.json({
        ...metrics,
        patternMatchRate: patternMatchRate.toFixed(2) + "%",
        apiCallRate: (100 - patternMatchRate).toFixed(2) + "%",
      });
    } catch (error) {
      console.error("Error retrieving API metrics:", error);
      res.status(500).json({ message: "Failed to retrieve API metrics" });
    }
  });
  
  // The routes below would be implemented in a real app to provide more detailed analytics
  
  // Get pattern learning efficiency over time (mock endpoint for now)
  app.get("/api/metrics/efficiency-trend", (req, res) => {
    // In a real implementation, this would track metrics over time from a database
    res.json({
      message: "This endpoint would return pattern learning efficiency trends over time"
    });
  });
  
  // Get pattern usage by category (mock endpoint for now)
  app.get("/api/metrics/category-usage", (req, res) => {
    // In a real implementation, this would return usage by pattern category
    res.json({
      message: "This endpoint would return pattern usage by category (ideas, code, fixes, etc.)"
    });
  });
}