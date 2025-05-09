import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/lib/queryClient";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useState } from "react";
import { Loader2, RefreshCw } from "lucide-react";

interface DbHealthResponse {
  status: "healthy" | "unhealthy" | "error";
  message: string;
  database: {
    status: string;
    message: string;
    error?: string;
  };
}

export function DatabaseStatus() {
  const [isRefreshing, setIsRefreshing] = useState(false);
  
  const { data, isLoading, isError, error, refetch } = useQuery<DbHealthResponse>({
    queryKey: ['/api/health'],
    refetchOnWindowFocus: false,
    refetchInterval: 60000, // Refresh every minute
  });

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await refetch();
    setIsRefreshing(false);
  };

  const getStatusColor = (status: string | undefined) => {
    if (!status) return "default";
    
    switch (status) {
      case "healthy":
        return "secondary"; // Green-like variant
      case "unhealthy":
        return "outline";   // Yellow-like variant
      case "error":
        return "destructive";
      default:
        return "default";
    }
  };

  return (
    <div className="flex flex-col space-y-2 p-4 border rounded-lg shadow-sm">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-medium">Database Status</h3>
        <Button 
          size="sm" 
          variant="outline" 
          onClick={handleRefresh}
          disabled={isLoading || isRefreshing}
        >
          {(isLoading || isRefreshing) ? (
            <Loader2 className="h-4 w-4 animate-spin mr-1" />
          ) : (
            <RefreshCw className="h-4 w-4 mr-1" />
          )}
          Refresh
        </Button>
      </div>
      
      <div className="space-y-3">
        {isLoading ? (
          <div className="flex items-center justify-center py-2">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            <span className="ml-2 text-sm text-muted-foreground">Checking status...</span>
          </div>
        ) : isError ? (
          <div className="py-2">
            <Badge variant="destructive">Error</Badge>
            <p className="text-sm mt-1 text-muted-foreground">
              {error instanceof Error ? error.message : "Failed to check database status"}
            </p>
          </div>
        ) : (
          <>
            <div className="flex items-center">
              <Badge variant={getStatusColor(data?.status)}>
                {data?.status === "healthy" ? "Healthy" : 
                 data?.status === "unhealthy" ? "Unhealthy" : 
                 data?.status === "error" ? "Error" : "Unknown"}
              </Badge>
              <span className="ml-2 text-sm">{data?.message}</span>
            </div>
            
            <div className="pt-1">
              <h4 className="text-sm font-medium mb-1">Database Connection</h4>
              <div className="flex items-center">
                <Badge variant={getStatusColor(data?.database.status)}>
                  {data?.database.status === "healthy" ? "Connected" : 
                   data?.database.status === "unhealthy" ? "Issues" : 
                   data?.database.status === "error" ? "Disconnected" : "Unknown"}
                </Badge>
                <span className="ml-2 text-sm">{data?.database.message}</span>
              </div>
              
              {data?.database.error && (
                <p className="text-xs text-red-500 mt-1">{data.database.error}</p>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}