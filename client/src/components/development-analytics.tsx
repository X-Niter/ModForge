import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useModContext } from "@/context/mod-context";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { 
  BarChart, 
  Bar, 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from "recharts";
import { Loader2, AlertTriangle, Info } from "lucide-react";

// Analytics data interface
interface DevelopmentAnalytics {
  modId: number;
  buildStats: {
    totalBuilds: number;
    successfulBuilds: number;
    failedBuilds: number;
    buildsByDay: {
      date: string;
      count: number;
      successful: number;
      failed: number;
    }[];
  };
  errorStats: {
    totalErrors: number;
    errorsFixed: number;
    errorCategories: {
      name: string;
      count: number;
    }[];
    errorsByDay: {
      date: string;
      count: number;
      fixed: number;
    }[];
  };
  featureStats: {
    totalFeatures: number;
    completedFeatures: number;
    featureCategories: {
      name: string;
      count: number;
    }[];
    featureProgress: {
      date: string;
      completed: number;
      total: number;
    }[];
  };
  performanceStats: {
    averageBuildTime: number;
    averageFixTime: number;
    buildTimes: {
      buildNumber: number;
      time: number;
    }[];
  };
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

export function DevelopmentAnalytics() {
  const { currentMod } = useModContext();
  const [timeRange, setTimeRange] = useState<string>("week");
  
  // In a real implementation, this would fetch from an API endpoint
  const { data, isLoading, error } = useQuery<DevelopmentAnalytics>({
    queryKey: ['/api/mods', currentMod?.id, 'analytics', timeRange],
    enabled: !!currentMod?.id,
    placeholderData: currentMod ? {
      modId: currentMod.id,
      buildStats: {
        totalBuilds: 37,
        successfulBuilds: 29,
        failedBuilds: 8,
        buildsByDay: [
          { date: "Mon", count: 5, successful: 3, failed: 2 },
          { date: "Tue", count: 7, successful: 5, failed: 2 },
          { date: "Wed", count: 6, successful: 5, failed: 1 },
          { date: "Thu", count: 8, successful: 7, failed: 1 },
          { date: "Fri", count: 6, successful: 5, failed: 1 },
          { date: "Sat", count: 3, successful: 2, failed: 1 },
          { date: "Sun", count: 2, successful: 2, failed: 0 }
        ]
      },
      errorStats: {
        totalErrors: 24,
        errorsFixed: 22,
        errorCategories: [
          { name: "Syntax", count: 9 },
          { name: "Type", count: 7 },
          { name: "Logical", count: 4 },
          { name: "Dependency", count: 3 },
          { name: "Other", count: 1 }
        ],
        errorsByDay: [
          { date: "Mon", count: 5, fixed: 4 },
          { date: "Tue", count: 6, fixed: 5 },
          { date: "Wed", count: 4, fixed: 4 },
          { date: "Thu", count: 3, fixed: 3 },
          { date: "Fri", count: 4, fixed: 4 },
          { date: "Sat", count: 2, fixed: 2 },
          { date: "Sun", count: 0, fixed: 0 }
        ]
      },
      featureStats: {
        totalFeatures: 12,
        completedFeatures: 8,
        featureCategories: [
          { name: "Weapons", count: 5 },
          { name: "Combat System", count: 3 },
          { name: "UI", count: 2 },
          { name: "Effects", count: 2 }
        ],
        featureProgress: [
          { date: "Week 1", completed: 2, total: 12 },
          { date: "Week 2", completed: 5, total: 12 },
          { date: "Week 3", completed: 8, total: 12 }
        ]
      },
      performanceStats: {
        averageBuildTime: 42,
        averageFixTime: 18,
        buildTimes: [
          { buildNumber: 1, time: 65 },
          { buildNumber: 2, time: 58 },
          { buildNumber: 3, time: 51 },
          { buildNumber: 4, time: 48 },
          { buildNumber: 5, time: 45 },
          { buildNumber: 6, time: 43 },
          { buildNumber: 7, time: 40 }
        ]
      }
    } : undefined,
    refetchInterval: 60000 // Refresh every minute
  });
  
  if (!currentMod) {
    return null;
  }
  
  if (isLoading) {
    return (
      <Card className="my-6">
        <CardContent className="flex items-center justify-center py-6">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">Loading analytics data...</span>
        </CardContent>
      </Card>
    );
  }
  
  if (error || !data) {
    return (
      <Card className="my-6">
        <CardContent className="flex items-center justify-center py-6 text-destructive">
          <AlertTriangle className="h-6 w-6 mr-2" />
          <span>Failed to load analytics data</span>
        </CardContent>
      </Card>
    );
  }
  
  return (
    <Card className="my-6">
      <CardHeader>
        <div className="flex justify-between items-center">
          <div>
            <CardTitle className="text-xl">Development Analytics</CardTitle>
            <CardDescription>
              Insights into your mod's continuous development progress
            </CardDescription>
          </div>
          <div>
            <Tabs defaultValue="week" value={timeRange} onValueChange={setTimeRange}>
              <TabsList>
                <TabsTrigger value="day">24h</TabsTrigger>
                <TabsTrigger value="week">Week</TabsTrigger>
                <TabsTrigger value="month">Month</TabsTrigger>
                <TabsTrigger value="all">All Time</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </div>
      </CardHeader>
      
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Build Success/Failure Chart */}
          <div className="bg-slate-50 dark:bg-slate-900 p-4 rounded-lg">
            <h3 className="text-sm font-medium mb-3">Build Success Rate</h3>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={data.buildStats.buildsByDay}
                  margin={{ top: 5, right: 30, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="successful" name="Successful" stackId="a" fill="#4ade80" />
                  <Bar dataKey="failed" name="Failed" stackId="a" fill="#f87171" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            
            <div className="flex justify-around mt-3 text-center text-sm">
              <div>
                <div className="text-xl font-semibold text-green-500">{data.buildStats.successfulBuilds}</div>
                <div className="text-muted-foreground">Successful</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-red-500">{data.buildStats.failedBuilds}</div>
                <div className="text-muted-foreground">Failed</div>
              </div>
              <div>
                <div className="text-xl font-semibold">{data.buildStats.totalBuilds}</div>
                <div className="text-muted-foreground">Total Builds</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-blue-500">
                  {Math.round((data.buildStats.successfulBuilds / data.buildStats.totalBuilds) * 100)}%
                </div>
                <div className="text-muted-foreground">Success Rate</div>
              </div>
            </div>
          </div>
          
          {/* Error Tracking */}
          <div className="bg-slate-50 dark:bg-slate-900 p-4 rounded-lg">
            <h3 className="text-sm font-medium mb-3">Error Categories</h3>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={data.errorStats.errorCategories}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="count"
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  >
                    {data.errorStats.errorCategories.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            
            <div className="flex justify-around mt-3 text-center text-sm">
              <div>
                <div className="text-xl font-semibold text-amber-500">{data.errorStats.totalErrors}</div>
                <div className="text-muted-foreground">Total Errors</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-green-500">{data.errorStats.errorsFixed}</div>
                <div className="text-muted-foreground">Fixed</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-blue-500">
                  {Math.round((data.errorStats.errorsFixed / data.errorStats.totalErrors) * 100)}%
                </div>
                <div className="text-muted-foreground">Fix Rate</div>
              </div>
            </div>
          </div>
          
          {/* Feature Progress */}
          <div className="bg-slate-50 dark:bg-slate-900 p-4 rounded-lg">
            <h3 className="text-sm font-medium mb-3">Feature Progress</h3>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={data.featureStats.featureProgress}
                  margin={{ top: 5, right: 30, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="completed" name="Completed Features" stroke="#8884d8" activeDot={{ r: 8 }} />
                  <Line type="monotone" dataKey="total" name="Total Features" stroke="#82ca9d" strokeDasharray="3 3" />
                </LineChart>
              </ResponsiveContainer>
            </div>
            
            <div className="flex justify-around mt-3 text-center text-sm">
              <div>
                <div className="text-xl font-semibold">{data.featureStats.totalFeatures}</div>
                <div className="text-muted-foreground">Total Features</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-purple-500">{data.featureStats.completedFeatures}</div>
                <div className="text-muted-foreground">Completed</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-amber-500">{data.featureStats.totalFeatures - data.featureStats.completedFeatures}</div>
                <div className="text-muted-foreground">Remaining</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-blue-500">
                  {Math.round((data.featureStats.completedFeatures / data.featureStats.totalFeatures) * 100)}%
                </div>
                <div className="text-muted-foreground">Complete</div>
              </div>
            </div>
          </div>
          
          {/* Performance */}
          <div className="bg-slate-50 dark:bg-slate-900 p-4 rounded-lg">
            <h3 className="text-sm font-medium mb-3">Build Performance</h3>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={data.performanceStats.buildTimes}
                  margin={{ top: 5, right: 30, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="buildNumber" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="time" name="Build Time (seconds)" stroke="#ff7c43" />
                </LineChart>
              </ResponsiveContainer>
            </div>
            
            <div className="flex justify-around mt-3 text-center text-sm">
              <div>
                <div className="text-xl font-semibold text-orange-500">{data.performanceStats.averageBuildTime}s</div>
                <div className="text-muted-foreground">Avg. Build Time</div>
              </div>
              <div>
                <div className="text-xl font-semibold text-blue-500">{data.performanceStats.averageFixTime}s</div>
                <div className="text-muted-foreground">Avg. Fix Time</div>
              </div>
            </div>
          </div>
        </div>
        
        <div className="mt-4 text-xs text-muted-foreground flex items-center">
          <Info className="h-3 w-3 mr-1" />
          <span>Analytics data is updated every minute during continuous development.</span>
        </div>
      </CardContent>
    </Card>
  );
}