import React from "react";
import { useModContext } from "@/context/mod-context";
import { useModGeneration } from "@/hooks/use-mod-generation";
import { Button } from "@/components/ui/button";
import { BuildStatus } from "@/types";

export function ActionsCard({ formData }: { formData: any }) {
  const { currentMod, currentBuild } = useModContext();
  const { generateMod, compileMod, pushToGithub, isLoading } = useModGeneration();

  const handleGenerateMod = () => {
    if (!formData.name || !formData.idea) {
      alert("Please fill in at least the Mod Name and Mod Idea fields");
      return;
    }
    generateMod(formData);
  };

  const handleUpdateMod = () => {
    if (!currentMod) {
      alert("No mod selected to update");
      return;
    }
    
    // Prompt for additional features
    const newFeatures = prompt("Describe the new features or changes you want to add to your mod:");
    if (!newFeatures) return;
    
    const updatedFormData = {
      ...formData,
      idea: `${currentMod.idea}\n\nAdditional features: ${newFeatures}`
    };
    
    generateMod(updatedFormData);
  };

  const handleCompileMod = () => {
    if (!currentMod) {
      alert("No mod selected to compile");
      return;
    }
    compileMod(currentMod.id);
  };

  const handlePushToGithub = () => {
    if (!currentMod) {
      alert("No mod selected to push to GitHub");
      return;
    }
    pushToGithub(currentMod.id);
  };

  return (
    <div className="bg-surface rounded-lg shadow-lg p-5">
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
          <path d="m15 15-6-6m6 0-6 6"></path>
          <circle cx="12" cy="12" r="10"></circle>
        </svg>
        Actions
      </h3>

      <div className="space-y-3">
        <Button
          className="w-full bg-primary hover:bg-opacity-80 text-white"
          onClick={handleGenerateMod}
          disabled={isLoading}
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
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.29 7 12 12 20.71 7"></polyline>
            <line x1="12" y1="22" x2="12" y2="12"></line>
          </svg>
          <span>Generate Mod</span>
        </Button>

        <Button
          className="w-full bg-secondary hover:bg-opacity-80 text-white"
          onClick={handleUpdateMod}
          disabled={isLoading || !currentMod}
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
            <path d="M21 2v6h-6"></path>
            <path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path>
            <path d="M3 22v-6h6"></path>
            <path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path>
          </svg>
          <span>Update Existing Mod</span>
        </Button>

        <Button
          variant="outline"
          className="w-full bg-background hover:bg-gray-700 text-white"
          onClick={handleCompileMod}
          disabled={isLoading || !currentMod || (currentBuild?.status === BuildStatus.InProgress)}
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
            <path d="m15 5 4 4-4 4"></path>
            <path d="M9 9h10"></path>
            <path d="m5 15 4 4-4 4"></path>
            <path d="M9 19h10"></path>
          </svg>
          <span>Compile Mod</span>
        </Button>

        <Button
          variant="outline"
          className="w-full bg-background hover:bg-gray-700 text-white"
          onClick={handlePushToGithub}
          disabled={isLoading || !currentMod || (currentBuild?.status !== BuildStatus.Success)}
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
            <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"></path>
            <path d="M9 18c-4.51 2-5-2-7-2"></path>
          </svg>
          <span>Push to GitHub</span>
        </Button>

        <Button
          variant="outline"
          className="w-full bg-background hover:bg-gray-700 text-white"
          disabled={true}
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
            <path d="M17 17H22V12H17M2 7H7V2H2M12 22H17V17H12M12 12H17V7H12M2 22H7V17H2M2 12H7V7H2M12 7H17V2H12M2 2V7H7V2H2Z"></path>
          </svg>
          <span>Test In Game</span>
        </Button>
      </div>
    </div>
  );
}
