import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getUsageMetrics, getPatternLearningMetrics, UsageMetrics, PatternMetrics } from "@/lib/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { Loader2 } from "lucide-react";

export function AIMetrics() {
  const [activeTab, setActiveTab] = useState("usage");

  // Fetch usage metrics
  const usageQuery = useQuery<UsageMetrics>({
    queryKey: ["/api/metrics/usage"],
    refetchInterval: 60000, // Refresh every minute
  });

  // Fetch pattern learning metrics
  const patternQuery = useQuery<PatternMetrics>({
    queryKey: ["/api/pattern-learning/metrics"],
    refetchInterval: 60000, // Refresh every minute
  });

  const isLoading = usageQuery.isLoading || patternQuery.isLoading;
  const isError = usageQuery.isError || patternQuery.isError;

  // Format data for the charts
  const usageData = usageQuery.data ? [
    {
      name: "API Usage",
      Patterns: usageQuery.data.patternMatches,
      API: usageQuery.data.apiCalls,
    }
  ] : [];

  const costData = usageQuery.data ? [
    {
      name: "Cost Savings",
      Saved: parseFloat(usageQuery.data.estimatedCostSaved.toFixed(2)),
      Spent: parseFloat(((usageQuery.data.apiCalls * 2000 / 1000) * 0.03).toFixed(2))
    }
  ] : [];

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>AI System Metrics</CardTitle>
        <CardDescription>
          Real-time metrics showing AI usage patterns and cost optimizations
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="usage">Usage &amp; Costs</TabsTrigger>
            <TabsTrigger value="patterns">Pattern Learning</TabsTrigger>
          </TabsList>
          
          <TabsContent value="usage" className="space-y-4">
            {isLoading ? (
              <div className="flex justify-center items-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : isError ? (
              <div className="text-destructive text-center py-8">
                Failed to load metrics data
              </div>
            ) : (
              <div className="space-y-8">
                <div>
                  <h3 className="text-lg font-medium mb-2">Request Distribution</h3>
                  <div className="h-[300px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={usageData}
                        margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="Patterns" fill="#10b981" />
                        <Bar dataKey="API" fill="#6366f1" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
                
                <div>
                  <h3 className="text-lg font-medium mb-2">Cost Analysis (USD)</h3>
                  <div className="h-[300px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={costData}
                        margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="Saved" fill="#22c55e" />
                        <Bar dataKey="Spent" fill="#ef4444" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-4">
                  <MetricCard
                    title="Total Requests"
                    value={usageQuery.data?.totalRequests || 0}
                    description="All AI requests processed"
                  />
                  <MetricCard
                    title="Pattern Matches"
                    value={usageQuery.data?.patternMatches || 0}
                    description="Requests served from patterns"
                  />
                  <MetricCard
                    title="API Calls"
                    value={usageQuery.data?.apiCalls || 0}
                    description="Fallback OpenAI API calls"
                  />
                  <MetricCard
                    title="Cost Saved"
                    value={`$${usageQuery.data?.estimatedCostSaved?.toFixed(2) || "0.00"}`}
                    description="Estimated API cost savings"
                  />
                </div>
              </div>
            )}
          </TabsContent>
          
          <TabsContent value="patterns" className="space-y-4">
            {isLoading ? (
              <div className="flex justify-center items-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : isError ? (
              <div className="text-destructive text-center py-8">
                Failed to load pattern metrics data
              </div>
            ) : patternQuery.data ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <PatternMetricsCard
                    title="Idea Generation"
                    data={patternQuery.data.ideas}
                  />
                  <PatternMetricsCard
                    title="Code Generation"
                    data={patternQuery.data.code}
                  />
                  <PatternMetricsCard
                    title="Error Fixing"
                    data={patternQuery.data.fixes}
                  />
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <PatternMetricsCard
                    title="Feature Addition"
                    data={patternQuery.data.features}
                  />
                  <PatternMetricsCard
                    title="Documentation"
                    data={patternQuery.data.documentation}
                  />
                </div>
                
                <div className="bg-muted p-4 rounded-lg">
                  <h3 className="font-medium mb-2">System Health</h3>
                  <p className="text-sm">
                    Pattern learning database contains {patternQuery.data.overall.totalPatterns} patterns 
                    with an average success rate of {patternQuery.data.overall.averageSuccessRate?.toFixed(1) || 0}%.
                    The system has achieved {patternQuery.data.overall.totalSuccesses} successful matches.
                  </p>
                </div>
              </div>
            ) : null}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}

function MetricCard({ title, value, description }: { 
  title: string; 
  value: number | string; 
  description: string;
}) {
  return (
    <div className="bg-card border rounded-lg p-4">
      <h4 className="text-sm font-medium text-muted-foreground">{title}</h4>
      <div className="text-2xl font-bold my-1">{value}</div>
      <p className="text-xs text-muted-foreground">{description}</p>
    </div>
  );
}

function PatternMetricsCard({ title, data }: {
  title: string;
  data: {
    patterns: number;
    matches: number;
    successRate: number;
    avgConfidence: number;
  }
}) {
  return (
    <div className="border rounded-lg p-4">
      <h3 className="font-medium mb-2">{title}</h3>
      <div className="grid grid-cols-2 gap-2 text-sm">
        <div>
          <div className="text-muted-foreground">Patterns</div>
          <div className="font-semibold">{data.patterns}</div>
        </div>
        <div>
          <div className="text-muted-foreground">Matches</div>
          <div className="font-semibold">{data.matches}</div>
        </div>
        <div>
          <div className="text-muted-foreground">Success Rate</div>
          <div className="font-semibold">{data.successRate?.toFixed(1) || 0}%</div>
        </div>
        <div>
          <div className="text-muted-foreground">Avg. Confidence</div>
          <div className="font-semibold">{data.avgConfidence?.toFixed(2) || 0}</div>
        </div>
      </div>
    </div>
  );
}