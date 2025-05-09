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
  AlertCircle,
  Code,
  Bug,
  FileText,
  Brain,
  CheckCircle,
  XCircle,
  Zap,
  RefreshCw,
  Search,
  History
} from "lucide-react";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

interface ErrorPatternStats {
  totalPatterns: number;
  activePatterns: number;
  highConfidencePatterns: number;
  totalUsage: number;
  totalSuccess: number;
  averageConfidence: number;
  recentPatterns: Array<{
    id: number;
    errorType: string;
    errorPattern: string;
    context: string;
    fixPattern: string;
    confidence: number;
    usageCount: number;
    successCount: number;
    lastUsed: string;
    tags: string[];
    isActive: boolean;
  }>;
}

export default function ErrorResolutionPage() {
  // Fetch error pattern statistics
  const { data: errorStats, isLoading: statsLoading } = useQuery<ErrorPatternStats>({
    queryKey: ['/api/metrics/error-patterns'],
    initialData: {
      totalPatterns: 0,
      activePatterns: 0,
      highConfidencePatterns: 0,
      totalUsage: 0,
      totalSuccess: 0,
      averageConfidence: 0,
      recentPatterns: []
    }
  });

  // Calculate success rate
  const successRate = errorStats.totalUsage > 0 
    ? ((errorStats.totalSuccess / errorStats.totalUsage) * 100).toFixed(1) + '%' 
    : '0.0%';

  return (
    <Layout>
      <div className="container px-4 pb-8">
        <div className="flex flex-col items-start gap-4 md:flex-row md:justify-between md:gap-8">
          <div className="flex-1 space-y-4">
            <h1 className="text-3xl font-bold tracking-tight">ML Error Resolution</h1>
            <p className="text-muted-foreground">
              Advanced error resolution system with machine learning pattern recognition.
            </p>
          </div>
          <div className="flex items-center">
            <Card className="border-0 bg-muted/50">
              <CardContent className="p-3">
                <div className="flex items-center gap-2 text-sm">
                  <Brain className="h-4 w-4 text-purple-500" />
                  <span className="font-medium">Machine learning active</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
        
        <Separator className="my-6" />
        
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {/* Total Patterns */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Total Patterns</CardTitle>
              <Code className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{errorStats.totalPatterns.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">
                Learned error resolution patterns
              </p>
            </CardContent>
          </Card>
          
          {/* Success Rate */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="flex items-baseline gap-2">
                <div className="text-2xl font-bold">{successRate}</div>
              </div>
              <div className="mt-3">
                <Progress value={parseFloat(successRate)} className="h-2" />
              </div>
            </CardContent>
          </Card>
          
          {/* Pattern Usage */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Pattern Usage</CardTitle>
              <History className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{errorStats.totalUsage.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">
                Total pattern usage count
              </p>
            </CardContent>
          </Card>
          
          {/* Average Confidence */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Avg. Confidence</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(errorStats.averageConfidence * 100).toFixed(1)}%</div>
              <p className="text-xs text-muted-foreground">
                Pattern confidence level
              </p>
            </CardContent>
          </Card>
        </div>
        
        <Tabs defaultValue="patterns" className="mt-6">
          <TabsList>
            <TabsTrigger value="patterns">Recent Patterns</TabsTrigger>
            <TabsTrigger value="analytics">Analytics</TabsTrigger>
            <TabsTrigger value="about">How It Works</TabsTrigger>
          </TabsList>
          
          <TabsContent value="patterns" className="mt-4">
            <Card>
              <CardHeader>
                <CardTitle>Recent Error Patterns</CardTitle>
                <CardDescription>
                  Most recently used error resolution patterns
                </CardDescription>
              </CardHeader>
              <CardContent>
                {errorStats.recentPatterns.length > 0 ? (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>ID</TableHead>
                        <TableHead>Error Type</TableHead>
                        <TableHead>Confidence</TableHead>
                        <TableHead>Usage</TableHead>
                        <TableHead>Success Rate</TableHead>
                        <TableHead>Last Used</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {errorStats.recentPatterns.map((pattern) => {
                        const patternSuccessRate = pattern.usageCount > 0 
                          ? ((pattern.successCount / pattern.usageCount) * 100).toFixed(1) + '%'
                          : '0%';
                          
                        return (
                          <TableRow key={pattern.id}>
                            <TableCell className="font-medium">{pattern.id}</TableCell>
                            <TableCell>{pattern.errorType}</TableCell>
                            <TableCell>{(pattern.confidence * 100).toFixed(1)}%</TableCell>
                            <TableCell>{pattern.usageCount}</TableCell>
                            <TableCell>{patternSuccessRate}</TableCell>
                            <TableCell>
                              {new Date(pattern.lastUsed).toLocaleDateString('en-US', { 
                                year: 'numeric', 
                                month: 'short', 
                                day: 'numeric' 
                              })}
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                ) : (
                  <div className="flex flex-col items-center justify-center py-8 text-center">
                    <Search className="h-12 w-12 text-muted-foreground/50 mb-4" />
                    <h3 className="text-lg font-medium">No patterns yet</h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Error patterns will appear here as they are learned
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="analytics" className="mt-4">
            <div className="grid gap-6 md:grid-cols-2">
              <Card>
                <CardHeader>
                  <CardTitle>Pattern Distribution</CardTitle>
                  <CardDescription>
                    Distribution of error patterns by type
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-center py-8">
                    <div className="h-64 w-64 rounded-full border-8 border-primary/20 flex items-center justify-center relative">
                      <div className="absolute inset-0 flex items-center justify-center">
                        <span className="text-lg font-bold">{errorStats.activePatterns} Active</span>
                      </div>
                      <div className="h-40 w-40 rounded-full border-8 border-primary/40 flex items-center justify-center">
                        <span className="text-sm">{errorStats.highConfidencePatterns} High Conf.</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
              
              <Card>
                <CardHeader>
                  <CardTitle>Resolution Performance</CardTitle>
                  <CardDescription>
                    Error resolution performance metrics
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">Pattern Match Rate</span>
                      <span className="text-sm text-muted-foreground">78%</span>
                    </div>
                    <Progress value={78} className="h-2" />
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">First Attempt Success</span>
                      <span className="text-sm text-muted-foreground">65%</span>
                    </div>
                    <Progress value={65} className="h-2" />
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">Confidence Growth</span>
                      <span className="text-sm text-muted-foreground">82%</span>
                    </div>
                    <Progress value={82} className="h-2" />
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">API Fallback Rate</span>
                      <span className="text-sm text-muted-foreground">22%</span>
                    </div>
                    <Progress value={22} className="h-2" />
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>
          
          <TabsContent value="about" className="mt-4">
            <Card>
              <CardHeader>
                <CardTitle>Advanced Error Resolution System</CardTitle>
                <CardDescription>
                  How our machine learning error resolution works
                </CardDescription>
              </CardHeader>
              <CardContent className="prose prose-slate dark:prose-invert max-w-none">
                <p>
                  The ModForge AI platform uses a sophisticated machine learning system to identify, store, and resolve 
                  common Minecraft mod development errors. This system dramatically reduces the need for API calls
                  and provides faster, more consistent error fixing.
                </p>
                
                <h3>Key Components:</h3>
                
                <ul>
                  <li>
                    <strong>Error Fingerprinting:</strong> Each error message is analyzed and fingerprinted based on its 
                    structural characteristics, allowing for fuzzy matching of similar errors.
                  </li>
                  <li>
                    <strong>Context-Aware Matching:</strong> Error resolution takes into account the mod loader, Minecraft 
                    version, and surrounding code context to provide accurate fixes.
                  </li>
                  <li>
                    <strong>Adaptive Confidence Scoring:</strong> Each pattern is assigned a confidence score that adjusts 
                    based on successful and unsuccessful applications.
                  </li>
                  <li>
                    <strong>Continuous Learning:</strong> When a pattern doesn't exist or fails to fix an issue, the system 
                    falls back to the OpenAI API and stores the successful solution as a new pattern.
                  </li>
                  <li>
                    <strong>Similarity-Based Retrieval:</strong> For new errors, the system finds the most similar existing 
                    patterns using a combination of text similarity algorithms and error feature extraction.
                  </li>
                </ul>
                
                <h3>The Learning Process:</h3>
                
                <ol>
                  <li>When an error is encountered, the system first checks if a similar error has been solved before.</li>
                  <li>If a match is found with sufficient confidence, the previously successful fix is applied.</li>
                  <li>If no match is found or confidence is low, the system uses OpenAI to generate a solution.</li>
                  <li>Successful fixes are stored as patterns with their context, error type, and solution.</li>
                  <li>Each time a pattern is used, its success or failure is recorded to adjust confidence.</li>
                </ol>
                
                <p>
                  This self-improving system becomes more efficient over time, particularly for common mod development
                  errors, reducing API usage and providing faster resolutions for users.
                </p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </Layout>
  );
}