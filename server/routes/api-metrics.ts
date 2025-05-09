import { Router } from 'express';
import { getUsageMetrics } from '../ai-service-manager';
import { getErrorPatternStats } from '../ml-error-resolution';
import { z } from 'zod';

const router = Router();

// Get usage metrics
router.get('/usage', async (req, res) => {
  try {
    const metrics = getUsageMetrics();
    const totalRequests = metrics.totalRequests || 0;
    
    // Calculate pattern match rate
    const patternMatchRate = totalRequests > 0 
      ? ((metrics.patternMatches / totalRequests) * 100).toFixed(2) + '%'
      : '0.00%';
    
    // Calculate API call rate
    const apiCallRate = totalRequests > 0
      ? ((metrics.apiCalls / totalRequests) * 100).toFixed(2) + '%'
      : '0.00%';
    
    res.json({
      ...metrics,
      patternMatchRate,
      apiCallRate
    });
  } catch (error) {
    console.error('Error fetching metrics:', error);
    res.status(500).json({ 
      error: 'Failed to fetch metrics',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get error resolution statistics
router.get('/error-patterns', async (req, res) => {
  try {
    const stats = await getErrorPatternStats();
    res.json(stats);
  } catch (error) {
    console.error('Error fetching error pattern stats:', error);
    res.status(500).json({ 
      error: 'Failed to fetch error pattern statistics',
      message: error instanceof Error ? error.message : String(error)
    });
  }
});

// Get category data
router.get('/categories', async (req, res) => {
  // For demonstration purposes, we'll return some sample data
  // In a real implementation, this would come from the database
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
});

// Get trend data
router.get('/trend', async (req, res) => {
  // Generate sample trend data
  const generateTrendData = () => {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    const data = [];
    let rate = 50; // Starting at 50%
    
    for (let i = 0; i < 30; i++) {
      const currentDate = new Date(startDate);
      currentDate.setDate(currentDate.getDate() + i);
      
      // Generate an increasing pattern match rate (with some randomness)
      rate = Math.min(95, rate + (Math.random() * 2 - 0.5));
      
      data.push({
        date: currentDate.toISOString().split('T')[0],
        rate: parseFloat(rate.toFixed(1))
      });
    }
    
    return data;
  };
  
  res.json(generateTrendData());
});

export default router;