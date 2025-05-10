import { useQuery } from "@tanstack/react-query";
import { getUsageMetrics, getSystemHealth } from "@/lib/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Progress } from "@/components/ui/progress";
import { Link } from "wouter";
import { CheckCircle, AlertTriangle, ServerCrash, Activity, BrainCircuit, Cpu, Code, Workflow } from "lucide-react";

export function DashboardOverview() {
  // Define types for API responses
  interface UsageMetrics {
    totalRequests: number;
    patternMatches: number;
    apiCalls: number;
    estimatedTokensSaved: number;
    estimatedCostSaved: number;
  }

  interface SystemHealth {
    status: "healthy" | "unhealthy" | "error";
    message: string;
    database?: any;
    ai?: any;
    server?: any;
  }

  // Get usage metrics
  const usageQuery = useQuery<UsageMetrics>({
    queryKey: ["/api/metrics/usage"],
    refetchInterval: 60000, // Refresh every minute
  });

  // Get system health
  const healthQuery = useQuery<SystemHealth>({
    queryKey: ["/api/health"],
  });

  // Format as currency
  const formatCurrency = (num: number) => {
    return `$${num.toFixed(2)}`;
  };

  // Calculate effectiveness based on pattern matching rate
  const calculateEffectiveness = () => {
    if (!usageQuery.data) return 0;
    const total = usageQuery.data.totalRequests;
    const patternMatches = usageQuery.data.patternMatches;
    return total > 0 ? Math.round((patternMatches / total) * 100) : 0;
  };

  // Determine system status
  const getSystemStatus = () => {
    if (healthQuery.isLoading) return { color: "text-gray-500", text: "Checking..." };
    if (healthQuery.isError) return { color: "text-red-500", text: "Error" };
    
    const health = healthQuery.data;
    if (!health) return { color: "text-red-500", text: "Unavailable" };
    
    if (health.status === "healthy") {
      return { color: "text-green-500", text: "Operational" };
    } else if (health.status === "unhealthy") {
      return { color: "text-amber-500", text: "Degraded" };
    } else {
      return { color: "text-red-500", text: "Offline" };
    }
  };

  const systemStatus = getSystemStatus();
  const effectiveness = calculateEffectiveness();

  return (
    <div className="space-y-6">
      <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
      <p className="text-muted-foreground">
        Overview of your Minecraft mod development system and AI performance.
      </p>

      {/* System Status */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg flex items-center">
              <Activity className="mr-2 h-5 w-5" />
              System Status
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${systemStatus.color}`}>
              {systemStatus.text}
            </div>
            {healthQuery.data && (
              <div className="text-sm text-muted-foreground mt-1">
                Last checked: {new Date().toLocaleTimeString()}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg flex items-center">
              <BrainCircuit className="mr-2 h-5 w-5" />
              AI Effectiveness
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {effectiveness}%
            </div>
            <Progress value={effectiveness} className="h-2 mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg flex items-center">
              <Cpu className="mr-2 h-5 w-5" />
              API Cost Optimization
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {usageQuery.data ? formatCurrency(usageQuery.data.estimatedCostSaved) : "$0.00"}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              {usageQuery.data ? usageQuery.data.patternMatches : 0} requests optimized
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg flex items-center">
              <Code className="mr-2 h-5 w-5" />
              Total Mod Builds
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {/* Placeholder for now - would come from builds API */}
              0
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              0 successful, 0 failed
            </div>
          </CardContent>
        </Card>
      </div>

      {/* System Health Alert */}
      {healthQuery.data && (
        <Alert
          variant={healthQuery.data.status === "healthy" ? "default" : "destructive"}
          className={healthQuery.data.status === "healthy" ? "border-green-500 bg-green-50" : ""}
        >
          {healthQuery.data.status === "healthy" ? (
            <CheckCircle className="h-5 w-5 text-green-500" />
          ) : (
            <ServerCrash className="h-5 w-5" />
          )}
          <AlertTitle>
            {healthQuery.data.status === "healthy"
              ? "All Systems Operational"
              : "System Issues Detected"}
          </AlertTitle>
          <AlertDescription>
            {healthQuery.data.status === "healthy" ? (
              <>
                Database, AI services, and pattern learning systems are functioning normally.
                The autonomous mod development pipeline is ready for use.
              </>
            ) : (
              <>
                {healthQuery.data.message} Please check the system logs for more information.
              </>
            )}
          </AlertDescription>
        </Alert>
      )}

      {/* Quick Actions */}
      <div className="space-y-3">
        <h3 className="text-lg font-medium">Quick Actions</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
          <Link href="/idea-generator">
            <div className="border rounded-lg p-4 cursor-pointer hover:bg-accent hover:text-accent-foreground transition-colors">
              <h4 className="font-medium flex items-center">
                <BrainCircuit className="h-4 w-4 mr-2" />
                Generate Mod Ideas
              </h4>
              <p className="text-sm text-muted-foreground mt-1">
                Create and expand new Minecraft mod concepts
              </p>
            </div>
          </Link>
          
          <Link href="/code-generator">
            <div className="border rounded-lg p-4 cursor-pointer hover:bg-accent hover:text-accent-foreground transition-colors">
              <h4 className="font-medium flex items-center">
                <Code className="h-4 w-4 mr-2" />
                Generate Mod Code
              </h4>
              <p className="text-sm text-muted-foreground mt-1">
                Turn ideas into working Minecraft mod code
              </p>
            </div>
          </Link>
          
          <Link href="/continuous-development">
            <div className="border rounded-lg p-4 cursor-pointer hover:bg-accent hover:text-accent-foreground transition-colors">
              <h4 className="font-medium flex items-center">
                <Workflow className="h-4 w-4 mr-2" />
                Continuous Development
              </h4>
              <p className="text-sm text-muted-foreground mt-1">
                Set up automated mod development workflows
              </p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}