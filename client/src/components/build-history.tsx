import React from "react";
import { useModContext } from "@/context/mod-context";
import { Button } from "@/components/ui/button";
import { BuildStatus } from "@/types";

export function BuildHistory() {
  const { builds, setCurrentBuild } = useModContext();
  
  // Show up to 3 most recent builds
  const recentBuilds = builds?.slice(0, 3) || [];
  
  const getStatusColor = (status: string) => {
    switch (status) {
      case BuildStatus.Success:
        return "border-success";
      case BuildStatus.Failed:
        return "border-error";
      case BuildStatus.InProgress:
        return "border-accent";
      default:
        return "border-gray-500";
    }
  };
  
  const getStatusBadge = (status: string) => {
    switch (status) {
      case BuildStatus.Success:
        return <span className="px-2 py-0.5 text-xs rounded bg-success text-white">Success</span>;
      case BuildStatus.Failed:
        return <span className="px-2 py-0.5 text-xs rounded bg-error text-white">Failed</span>;
      case BuildStatus.InProgress:
        return <span className="px-2 py-0.5 text-xs rounded bg-accent text-white">In Progress</span>;
      default:
        return <span className="px-2 py-0.5 text-xs rounded bg-gray-500 text-white">Unknown</span>;
    }
  };

  return (
    <div className="bg-surface rounded-lg shadow-lg p-5 mb-6">
      <h3 className="text-white font-medium mb-4 flex items-center">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="mr-2 text-primary"
          width="20" 
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0"></path>
          <path d="M12 8v4l2 2"></path>
        </svg>
        Build History
      </h3>

      {recentBuilds.length > 0 ? (
        recentBuilds.map((build) => (
          <div key={build.id} className={`border-l-4 ${getStatusColor(build.status)} pl-3 mb-3`}>
            <div className="flex justify-between items-start">
              <div>
                <p className="text-sm font-medium">Build #{build.buildNumber}</p>
                <p className="text-xs text-gray-400">
                  {build.createdAt ? new Date(build.createdAt).toLocaleString() : "Unknown time"}
                </p>
              </div>
              {getStatusBadge(build.status)}
            </div>
          </div>
        ))
      ) : (
        <div className="text-center py-4 text-gray-400">
          No build history available
        </div>
      )}

      <Button
        variant="outline"
        className="w-full bg-background hover:bg-gray-700 text-white py-2 rounded"
        disabled={recentBuilds.length === 0}
      >
        View All Builds
      </Button>
    </div>
  );
}
