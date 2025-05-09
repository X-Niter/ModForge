import { useState, useEffect } from "react";
import { Layout } from "@/components/ui/layout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";
import { Progress } from "@/components/ui/progress";
import { queryClient } from "@/lib/queryClient";
import { useQuery } from "@tanstack/react-query";
import {
  BarChart3,
  TrendingUp,
  LightbulbIcon,
  Code,
  Bug,
  FileText,
  Bot,
  Zap,
} from "lucide-react";

// Type definitions for API responses
interface MetricsData {
  totalRequests: number;
  patternMatches: number;
  apiCalls: number;
  estimatedTokensSaved: number;
  estimatedCostSaved: number;
  patternMatchRate: string;
  apiCallRate: string;
}

interface CategoryData {
  category: string;
  matches: number;
  calls: number;
  matchRate: string;
  icon: JSX.Element;
  color: string;
}

export default function MetricsPage() {
  // Fetch usage metrics
  const { data: metricsData, isLoading: metricsLoading } = useQuery({
    queryKey: ['/api/metrics/usage'],
    // Mock data until backend is fully implemented
    initialData: {
      totalRequests: 1247,
      patternMatches: 948,
      apiCalls: 299,
      estimatedTokensSaved: 1896000,
      estimatedCostSaved: 56.88,
      patternMatchRate: "76.02%",
      apiCallRate: "23.98%"
    } as MetricsData
  });

  // Mock category data for now
  const categoryData: CategoryData[] = [
    {
      category: "Code Generation",
      matches: 324,
      calls: 102,
      matchRate: "76.06%",
      icon: <Code className="w-5 h-5" />,
      color: "from-green-500 to-emerald-600"
    },
    {
      category: "Error Fixing",
      matches: 248,
      calls: 57,
      matchRate: "81.31%",
      icon: <Bug className="w-5 h-5" />,
      color: "from-blue-500 to-cyan-600"
    },
    {
      category: "Idea Generation",
      matches: 154,
      calls: 65,
      matchRate: "70.32%",
      icon: <LightbulbIcon className="w-5 h-5" />,
      color: "from-amber-500 to-yellow-600"
    },
    {
      category: "Feature Addition",
      matches: 126,
      calls: 48,
      matchRate: "72.41%",
      icon: <Zap className="w-5 h-5" />,
      color: "from-purple-500 to-indigo-600"
    },
    {
      category: "Documentation",
      matches: 96,
      calls: 27,
      matchRate: "78.05%",
      icon: <FileText className="w-5 h-5" />,
      color: "from-pink-500 to-rose-600"
    }
  ];

  // Mock trend data
  const [trendData, setTrendData] = useState<{ date: string; rate: number }[]>([]);

  useEffect(() => {
    // Generate sample trend data when component mounts
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
    
    setTrendData(generateTrendData());
  }, []);

  return (
    <Layout>
      <div className="container px-4 pb-8">
        <div className="flex flex-col items-start gap-4 md:flex-row md:justify-between md:gap-8">
          <div className="flex-1 space-y-4">
            <h1 className="text-3xl font-bold tracking-tight">AI Usage Metrics</h1>
            <p className="text-muted-foreground">
              Monitor pattern learning efficiency and cost savings from the self-learning system.
            </p>
          </div>
          <div className="flex items-center">
            <Card className="border-0 bg-muted/50">
              <CardContent className="p-3">
                <div className="flex items-center gap-2 text-sm">
                  <Bot className="h-4 w-4 text-purple-500" />
                  <span className="font-medium">Pattern learning active</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
        
        <Separator className="my-6" />
        
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {/* Total Requests */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metricsData?.totalRequests.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">
                Lifetime AI operations processed
              </p>
            </CardContent>
          </Card>
          
          {/* Pattern Match Rate */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Pattern Match Rate</CardTitle>
              <Bot className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="flex items-baseline gap-2">
                <div className="text-2xl font-bold">{metricsData?.patternMatchRate}</div>
                <span className="text-xs text-green-500">(+2.4% this week)</span>
              </div>
              <div className="mt-3">
                <Progress value={parseFloat(metricsData?.patternMatchRate || "0")} className="h-2" />
              </div>
            </CardContent>
          </Card>
          
          {/* Cost Savings */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Estimated Cost Savings</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">${metricsData?.estimatedCostSaved.toFixed(2)}</div>
              <p className="text-xs text-muted-foreground">
                Based on estimated API usage reduction
              </p>
            </CardContent>
          </Card>
          
          {/* Tokens Saved */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Tokens Saved</CardTitle>
              <Zap className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(metricsData?.estimatedTokensSaved / 1000000).toFixed(2)}M</div>
              <p className="text-xs text-muted-foreground">
                Total API tokens saved via pattern matching
              </p>
            </CardContent>
          </Card>
        </div>
        
        <div className="mt-6 grid gap-6 md:grid-cols-7">
          {/* Pattern Categories */}
          <Card className="md:col-span-3">
            <CardHeader>
              <CardTitle>Pattern Categories</CardTitle>
              <CardDescription>
                Pattern learning efficiency by category
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {categoryData.map((category) => (
                  <div key={category.category} className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className={`flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br ${category.color} text-white`}>
                          {category.icon}
                        </div>
                        <span className="font-medium">{category.category}</span>
                      </div>
                      <div className="text-sm font-medium">{category.matchRate}</div>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                      <div 
                        className={`h-full rounded-full bg-gradient-to-r ${category.color}`}
                        style={{ width: category.matchRate }}
                      ></div>
                    </div>
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>Matches: {category.matches}</span>
                      <span>API Calls: {category.calls}</span>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
          
          {/* Trend Chart */}
          <Card className="md:col-span-4">
            <CardHeader>
              <CardTitle>Efficiency Trend</CardTitle>
              <CardDescription>
                Pattern match rate over the last 30 days
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-[300px] w-full relative">
                {/* Simple chart visualization */}
                <div className="absolute inset-0 flex items-end">
                  {trendData.map((day, index) => (
                    <div
                      key={day.date}
                      className="flex-1 group relative"
                      style={{ height: '100%' }}
                    >
                      <div
                        className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-purple-500 to-indigo-500 rounded-t"
                        style={{ height: `${day.rate}%` }}
                      ></div>
                      {/* Tooltip on hover */}
                      <div className="hidden group-hover:block absolute -top-10 left-1/2 transform -translate-x-1/2 bg-background border border-border p-2 rounded text-xs whitespace-nowrap">
                        {day.date}: {day.rate}%
                      </div>
                    </div>
                  ))}
                </div>
                
                {/* Y-axis labels */}
                <div className="absolute left-0 top-0 bottom-0 flex flex-col justify-between text-xs text-muted-foreground">
                  <span>100%</span>
                  <span>75%</span>
                  <span>50%</span>
                  <span>25%</span>
                  <span>0%</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Self-Learning System</CardTitle>
            <CardDescription>
              How the pattern learning system becomes more efficient over time
            </CardDescription>
          </CardHeader>
          <CardContent className="prose prose-slate dark:prose-invert max-w-none">
            <p>
              The ModForge AI platform uses a sophisticated pattern learning system that identifies common patterns in 
              successful operations. Every time the system performs an action (generating code, fixing errors, creating 
              documentation), it stores the pattern and result in specialized databases.
            </p>
            
            <p>
              <strong>How it works:</strong>
            </p>
            
            <ol>
              <li>
                <strong>Pattern Storage:</strong> When the system generates a successful solution using OpenAI, the input, 
                output, and context are stored as a pattern.
              </li>
              <li>
                <strong>Pattern Matching:</strong> For new requests, the system first checks if it has a similar pattern in 
                its database before calling external APIs.
              </li>
              <li>
                <strong>Confidence Scoring:</strong> Each pattern is given a confidence score based on similarity to the current request.
              </li>
              <li>
                <strong>Success Tracking:</strong> The system tracks whether pattern matches produce successful results and 
                adjusts confidence scores accordingly.
              </li>
              <li>
                <strong>Progressive Improvement:</strong> Over time, the pattern database grows and becomes increasingly specialized 
                for Minecraft modding tasks.
              </li>
            </ol>
            
            <p>
              This self-learning approach allows the system to become more efficient, faster, and cost-effective with each operation,
              while maintaining quality results with minimal API usage.
            </p>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}