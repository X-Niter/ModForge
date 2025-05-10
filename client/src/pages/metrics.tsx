import { useQuery } from '@tanstack/react-query';
import { apiRequest } from '@/lib/queryClient';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Progress } from "@/components/ui/progress";
import { AlertTriangle, BarChart, BrainCircuit, CheckCircle, TrendingUp } from "lucide-react";

export default function MetricsPage() {
  const { data: apiMetrics, isLoading: isLoadingApiMetrics } = useQuery({
    queryKey: ['/api/metrics/usage'],
    staleTime: 1000 * 60, // 1 minute
  });

  const { data: patternMetrics, isLoading: isLoadingPatternMetrics } = useQuery({
    queryKey: ['/api/pattern-learning/metrics'],
    staleTime: 1000 * 60, // 1 minute
  });

  // Helper to format numbers with commas
  const formatNumber = (num: number) => {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  };

  // Helper to format currency
  const formatCurrency = (amount: number) => {
    return `$${amount.toFixed(2)}`;
  };

  return (
    <div className="container mx-auto p-6 max-w-7xl">
      <h1 className="text-3xl font-bold mb-4">System Metrics</h1>
      <p className="text-muted-foreground mb-6">
        Performance insights and pattern learning statistics for the autonomous Minecraft mod development system.
      </p>

      <Tabs defaultValue="pattern-learning">
        <TabsList className="mb-4">
          <TabsTrigger value="pattern-learning">
            <BrainCircuit className="mr-2 h-4 w-4" />
            Pattern Learning
          </TabsTrigger>
          <TabsTrigger value="api-usage">
            <BarChart className="mr-2 h-4 w-4" />
            API Usage
          </TabsTrigger>
        </TabsList>

        <TabsContent value="pattern-learning">
          {isLoadingPatternMetrics ? (
            <div className="flex justify-center items-center p-12">
              <div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" aria-label="Loading"/>
            </div>
          ) : patternMetrics ? (
            <div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Pattern Database</CardTitle>
                    <CardDescription>Total stored patterns</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold">
                      {formatNumber(patternMetrics.overall.totalPatterns)}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Pattern Usage</CardTitle>
                    <CardDescription>Successful pattern matches</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold">
                      {formatNumber(patternMetrics.overall.totalUses)}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Success Rate</CardTitle>
                    <CardDescription>Pattern matching effectiveness</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold text-green-600">
                      {patternMetrics.overall.successRate}%
                    </div>
                    <Progress 
                      value={patternMetrics.overall.successRate} 
                      className="h-2 mt-2" 
                    />
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Cost Savings</CardTitle>
                    <CardDescription>Estimated API cost reduced</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold text-green-600">
                      {formatCurrency(patternMetrics.overall.estimatedCostSaved)}
                    </div>
                    <div className="text-sm text-muted-foreground mt-1">
                      {formatNumber(patternMetrics.overall.estimatedTokensSaved)} tokens saved
                    </div>
                  </CardContent>
                </Card>
              </div>

              <h2 className="text-xl font-bold mb-4">Pattern Type Effectiveness</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
                {Object.entries(patternMetrics.byType).map(([type, metrics]: [string, any]) => (
                  <Card key={type}>
                    <CardHeader>
                      <CardTitle className="capitalize">{type} Patterns</CardTitle>
                      <CardDescription>{metrics.patterns} patterns stored</CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="flex justify-between items-center mb-2">
                        <span>Success Rate:</span>
                        <span className="font-bold">{metrics.successRate}%</span>
                      </div>
                      <Progress 
                        value={metrics.successRate} 
                        className="h-2 mb-4" 
                      />
                      <div className="text-sm text-muted-foreground">
                        Used {formatNumber(metrics.uses)} times
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              <Alert>
                <TrendingUp className="h-4 w-4" />
                <AlertTitle>Improvement Opportunity</AlertTitle>
                <AlertDescription>
                  The pattern learning system becomes more effective over time as it learns from more examples.
                  The initial pattern collection phase is key to building a reliable pattern database.
                </AlertDescription>
              </Alert>
            </div>
          ) : (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>Error Loading Metrics</AlertTitle>
              <AlertDescription>
                Unable to load pattern learning metrics. Please try again later.
              </AlertDescription>
            </Alert>
          )}
        </TabsContent>

        <TabsContent value="api-usage">
          {isLoadingApiMetrics ? (
            <div className="flex justify-center items-center p-12">
              <div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" aria-label="Loading"/>
            </div>
          ) : apiMetrics ? (
            <div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Total Requests</CardTitle>
                    <CardDescription>All AI processing requests</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold">
                      {formatNumber(apiMetrics.totalRequests)}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Pattern Matches</CardTitle>
                    <CardDescription>Requests served from patterns</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold text-green-600">
                      {formatNumber(apiMetrics.patternMatches)}
                    </div>
                    <div className="text-sm text-muted-foreground mt-1">
                      {apiMetrics.patternMatchRate} of total
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">API Calls</CardTitle>
                    <CardDescription>Requests requiring API</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold text-amber-600">
                      {formatNumber(apiMetrics.apiCalls)}
                    </div>
                    <div className="text-sm text-muted-foreground mt-1">
                      {apiMetrics.apiCallRate} of total
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-lg">Cost Saved</CardTitle>
                    <CardDescription>Estimated API cost reduction</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-3xl font-bold text-green-600">
                      {formatCurrency(apiMetrics.estimatedCostSaved)}
                    </div>
                    <div className="text-sm text-muted-foreground mt-1">
                      {formatNumber(apiMetrics.estimatedTokensSaved)} tokens saved
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Alert>
                <CheckCircle className="h-4 w-4" />
                <AlertTitle>Efficiency Achieved</AlertTitle>
                <AlertDescription>
                  The pattern learning system has significantly reduced API costs while maintaining high quality outputs.
                  Continue using the system to improve pattern recognition and further reduce costs.
                </AlertDescription>
              </Alert>
            </div>
          ) : (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>Error Loading Metrics</AlertTitle>
              <AlertDescription>
                Unable to load API usage metrics. Please try again later.
              </AlertDescription>
            </Alert>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}