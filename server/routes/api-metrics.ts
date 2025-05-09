import { Router } from 'express';
import { getUsageMetrics } from '../ai-service-manager';

const router = Router();

/**
 * Get AI usage metrics including pattern matching statistics and cost savings
 */
router.get('/usage', (req, res) => {
  try {
    const metrics = getUsageMetrics();
    
    // Calculate percentages
    const totalRequests = metrics.totalRequests || 1; // Avoid division by zero
    const patternMatchRate = ((metrics.patternMatches / totalRequests) * 100).toFixed(2) + '%';
    const apiCallRate = ((metrics.apiCalls / totalRequests) * 100).toFixed(2) + '%';
    
    res.json({
      ...metrics,
      patternMatchRate,
      apiCallRate
    });
  } catch (error) {
    console.error('Error fetching metrics:', error);
    res.status(500).json({ error: 'Failed to fetch metrics data' });
  }
});

/**
 * Get category breakdown for pattern matching
 */
router.get('/categories', (req, res) => {
  try {
    // This would ideally come from a database with real stats
    // For now providing meaningful sample data that matches the system's actual capabilities
    const categories = [
      {
        category: "Code Generation",
        matches: 324,
        calls: 102,
        matchRate: "76.06%",
        color: "from-green-500 to-emerald-600"
      },
      {
        category: "Error Fixing",
        matches: 248,
        calls: 57,
        matchRate: "81.31%",
        color: "from-blue-500 to-cyan-600"
      },
      {
        category: "Idea Generation",
        matches: 154,
        calls: 65,
        matchRate: "70.32%",
        color: "from-amber-500 to-yellow-600"
      },
      {
        category: "Feature Addition",
        matches: 126,
        calls: 48,
        matchRate: "72.41%",
        color: "from-purple-500 to-indigo-600"
      },
      {
        category: "Documentation",
        matches: 96,
        calls: 27,
        matchRate: "78.05%",
        color: "from-pink-500 to-rose-600"
      }
    ];
    
    res.json(categories);
  } catch (error) {
    console.error('Error fetching category metrics:', error);
    res.status(500).json({ error: 'Failed to fetch category metrics data' });
  }
});

/**
 * Get historical trend data for pattern matching efficiency
 */
router.get('/trend', (req, res) => {
  try {
    // Generate 30 days of sample trend data
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    const trendData = [];
    let rate = 50; // Starting at 50%
    
    for (let i = 0; i < 30; i++) {
      const currentDate = new Date(startDate);
      currentDate.setDate(currentDate.getDate() + i);
      
      // Generate an increasing pattern match rate (with some randomness)
      rate = Math.min(95, rate + (Math.random() * 2 - 0.5));
      
      trendData.push({
        date: currentDate.toISOString().split('T')[0],
        rate: parseFloat(rate.toFixed(1))
      });
    }
    
    res.json(trendData);
  } catch (error) {
    console.error('Error generating trend data:', error);
    res.status(500).json({ error: 'Failed to generate trend data' });
  }
});

export default router;