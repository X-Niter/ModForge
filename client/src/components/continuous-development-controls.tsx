import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/queryClient";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { useModContext } from "@/context/mod-context";
import { AlertCircle, Play, SkipForward, Square, AlertTriangle } from "lucide-react";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

interface ContinuousDevelopmentStatus {
  modId: number;
  status: {
    isRunning: boolean;
    buildCount: number;
  };
}

export function ContinuousDevelopmentControls() {
  const { currentMod } = useModContext();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [compileFrequency, setCompileFrequency] = useState<string>(currentMod?.compileFrequency || "Every 5 Minutes");

  // Get status of continuous development
  const { data: statusData, isLoading: isStatusLoading, error: statusError } = useQuery<ContinuousDevelopmentStatus>({
    queryKey: ['/api/mods', currentMod?.id, 'continuous-development', 'status'],
    enabled: !!currentMod?.id,
    refetchInterval: 10000, // Refresh every 10 seconds
  });

  // Start continuous development
  const startMutation = useMutation({
    mutationFn: async () => {
      if (!currentMod?.id) return null;
      return apiRequest('POST', `/api/mods/${currentMod.id}/continuous-development/start`);
    },
    onSuccess: () => {
      toast({
        title: "Continuous Development Started",
        description: "Your mod will be continuously built and improved.",
      });
      queryClient.invalidateQueries({ queryKey: ['/api/mods', currentMod?.id, 'continuous-development', 'status'] });
    },
    onError: (error) => {
      toast({
        title: "Failed to Start",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    }
  });

  // Stop continuous development
  const stopMutation = useMutation({
    mutationFn: async () => {
      if (!currentMod?.id) return null;
      return apiRequest('POST', `/api/mods/${currentMod.id}/continuous-development/stop`);
    },
    onSuccess: () => {
      toast({
        title: "Continuous Development Stopped",
        description: "Your mod will no longer be continuously built.",
      });
      queryClient.invalidateQueries({ queryKey: ['/api/mods', currentMod?.id, 'continuous-development', 'status'] });
    },
    onError: (error) => {
      toast({
        title: "Failed to Stop",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    }
  });

  // Update mod compile frequency
  const updateFrequencyMutation = useMutation({
    mutationFn: async (newFrequency: string) => {
      if (!currentMod?.id) return null;
      return apiRequest('PATCH', `/api/mods/${currentMod.id}`, { compileFrequency: newFrequency });
    },
    onSuccess: () => {
      toast({
        title: "Frequency Updated",
        description: "Compile frequency has been updated.",
      });
      // Restart continuous development if it's already running
      if (statusData?.status.isRunning) {
        stopMutation.mutate();
        setTimeout(() => startMutation.mutate(), 1000);
      }
    },
    onError: (error) => {
      toast({
        title: "Failed to Update Frequency",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    }
  });

  // Handle frequency change
  const handleFrequencyChange = (value: string) => {
    setCompileFrequency(value);
    updateFrequencyMutation.mutate(value);
  };

  // Reset frequency when mod changes
  useEffect(() => {
    if (currentMod?.compileFrequency) {
      setCompileFrequency(currentMod.compileFrequency);
    }
  }, [currentMod?.id, currentMod?.compileFrequency]);

  if (!currentMod) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-center text-muted-foreground">
            <AlertCircle className="mr-2 h-5 w-5" />
            <p>Select a mod to enable continuous development</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="mb-6">
      <CardHeader className="pb-3">
        <CardTitle className="text-xl">Continuous Development</CardTitle>
        <CardDescription>
          Automatically build, test, and improve your mod
        </CardDescription>
      </CardHeader>

      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">Status</p>
              <div className="mt-1 flex items-center">
                {isStatusLoading ? (
                  <Badge variant="outline">Loading...</Badge>
                ) : statusError ? (
                  <Badge variant="destructive">Error</Badge>
                ) : (
                  <Badge variant={statusData?.status.isRunning ? "secondary" : "outline"}>
                    {statusData?.status.isRunning ? "Running" : "Stopped"}
                  </Badge>
                )}
                
                {statusData?.status.isRunning && statusData.status.buildCount > 0 && (
                  <span className="ml-2 text-sm text-muted-foreground">
                    {statusData.status.buildCount} builds completed
                  </span>
                )}
              </div>
            </div>

            <div className="flex flex-col">
              <p className="font-medium mb-1">Frequency</p>
              <Select value={compileFrequency} onValueChange={handleFrequencyChange} disabled={statusData?.status.isRunning}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="Select frequency" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="After Every Change">After Every Change</SelectItem>
                  <SelectItem value="Every 5 Minutes">Every 5 Minutes</SelectItem>
                  <SelectItem value="Every 15 Minutes">Every 15 Minutes</SelectItem>
                  <SelectItem value="Manual Only">Manual Only</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {currentMod.compileFrequency === "Manual Only" && (
            <div className="bg-amber-100 dark:bg-amber-950 border border-amber-200 dark:border-amber-900 rounded-md p-3">
              <div className="flex items-start">
                <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-500 mt-0.5 mr-3" />
                <div>
                  <p className="text-sm font-medium text-amber-800 dark:text-amber-400">
                    Manual Mode Active
                  </p>
                  <p className="text-xs text-amber-700 dark:text-amber-500 mt-1">
                    Continuous development is disabled when in Manual Mode. Change the frequency to enable this feature.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </CardContent>

      <CardFooter className="flex justify-between pt-3">
        <Button 
          variant="outline"
          onClick={() => stopMutation.mutate()}
          disabled={
            isStatusLoading || 
            startMutation.isPending || 
            stopMutation.isPending || 
            !statusData?.status.isRunning ||
            currentMod.compileFrequency === "Manual Only"
          }
        >
          <Square className="mr-2 h-4 w-4" />
          Stop
        </Button>
        
        <Button
          onClick={() => startMutation.mutate()}
          disabled={
            isStatusLoading || 
            startMutation.isPending || 
            stopMutation.isPending ||
            (statusData?.status.isRunning || false) ||
            currentMod.compileFrequency === "Manual Only"
          }
        >
          <Play className="mr-2 h-4 w-4" />
          Start Continuous Development
        </Button>
      </CardFooter>
    </Card>
  );
}