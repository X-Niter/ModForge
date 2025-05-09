import React from "react";
import { useModContext } from "@/context/mod-context";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { BuildStatus } from "@/types";

export function StatusCard() {
  const { currentBuild, currentMod } = useModContext();
  
  // Default values when no build is available
  const status = currentBuild?.status || "No builds yet";
  const buildNumber = currentBuild?.buildNumber || 0;
  const completedTime = currentBuild?.completedAt 
    ? new Date(currentBuild.completedAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})
    : "";
  const errorsCount = currentBuild?.errorCount || 0;
  const warningsCount = currentBuild?.warningCount || 0;
  
  const statusColor = 
    status === BuildStatus.Success ? "bg-success" :
    status === BuildStatus.Failed ? "bg-error" :
    status === BuildStatus.InProgress ? "bg-accent" :
    "bg-gray-500";
  
  const statusText = 
    status === BuildStatus.Success ? "Build Successful" :
    status === BuildStatus.Failed ? "Build Failed" :
    status === BuildStatus.InProgress ? "Building..." :
    "No builds yet";
  
  const progress = 
    status === BuildStatus.Success ? 100 :
    status === BuildStatus.Failed ? 100 :
    status === BuildStatus.InProgress ? 60 :
    0;

  return (
    <div className="bg-surface rounded-lg shadow-lg p-5 mb-6">
      <h3 className="text-white font-medium mb-4 flex items-center">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className={`mr-2 ${status === BuildStatus.Success ? "text-success" : "text-primary"}`}
          width="20" 
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          {status === BuildStatus.Success ? (
            <>
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </>
          ) : (
            <>
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </>
          )}
        </svg>
        Current Status
      </h3>

      <div className="bg-background p-3 rounded-md mb-4 flex items-center">
        <div className="flex-1">
          <div className="flex items-center">
            <span className={`inline-block w-3 h-3 rounded-full ${statusColor} mr-2`}></span>
            <span className="font-medium">{statusText}</span>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            {completedTime 
              ? `Latest build completed at ${completedTime}`
              : status === BuildStatus.InProgress
                ? "Build in progress..."
                : "No builds completed yet"}
          </p>
        </div>
        <Button
          variant="secondary"
          className="text-white"
          disabled={!currentBuild}
        >
          View Details
        </Button>
      </div>

      <div className="mb-4">
        <div className="flex justify-between items-center mb-1">
          <span className="text-sm font-medium">Build Progress</span>
          <span className="text-xs">{progress}%</span>
        </div>
        <Progress value={progress} className="h-2.5 bg-background" />
      </div>

      <div className="grid grid-cols-2 gap-3 mb-3">
        <div className="bg-background p-2 rounded text-center">
          <div className={`text-2xl font-bold ${errorsCount > 0 ? "text-error" : "text-success"}`}>
            {errorsCount}
          </div>
          <div className="text-xs text-gray-400">Errors</div>
        </div>
        <div className="bg-background p-2 rounded text-center">
          <div className={`text-2xl font-bold ${warningsCount > 0 ? "text-accent" : "text-success"}`}>
            {warningsCount}
          </div>
          <div className="text-xs text-gray-400">Warnings</div>
        </div>
      </div>

      <Button 
        className="w-full bg-primary hover:bg-opacity-80 text-white"
        disabled={!currentBuild || status !== BuildStatus.Success}
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="mr-2"
          width="16" 
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Download Latest Build
      </Button>
    </div>
  );
}
