import { useState } from "react";
import { useToast } from "@/hooks/use-toast";
import { apiRequest } from "@/lib/queryClient";
import { useModContext } from "@/context/mod-context";
import { ModFormData, BuildStatus } from "@/types";
import { queryClient } from "@/lib/queryClient";
import { Build, Mod } from "@shared/schema";

export function useModGeneration() {
  const { toast } = useToast();
  const {
    setCurrentMod,
    addConsoleOutput,
    clearConsoleOutput,
    setIsGenerating,
    setCurrentBuild,
    setBuilds,
  } = useModContext();
  const [isLoading, setIsLoading] = useState(false);

  const generateMod = async (formData: ModFormData) => {
    setIsLoading(true);
    setIsGenerating(true);
    clearConsoleOutput();
    addConsoleOutput(`Starting generation of mod: ${formData.name}`);

    try {
      const response = await apiRequest("POST", "/api/mods", formData);
      const data = await response.json();
      
      setCurrentMod(data.mod);
      
      // Start polling for build status
      if (data.build) {
        setCurrentBuild(data.build);
        await pollBuildStatus(data.mod.id, data.build.id);
      }
      
      toast({
        title: "Mod generation started",
        description: "Your mod is being generated. Check the console for progress updates.",
      });
      
      // Invalidate mods query
      queryClient.invalidateQueries({ queryKey: ['/api/mods'] });
      
    } catch (error) {
      console.error("Error generating mod:", error);
      addConsoleOutput(`Error: ${error instanceof Error ? error.message : String(error)}`);
      toast({
        variant: "destructive",
        title: "Failed to generate mod",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    } finally {
      setIsLoading(false);
      setIsGenerating(false);
    }
  };

  const updateMod = async (id: number, formData: Partial<ModFormData>) => {
    setIsLoading(true);
    addConsoleOutput(`Updating mod: ${formData.name || ''}`);

    try {
      const response = await apiRequest("PATCH", `/api/mods/${id}`, formData);
      const data = await response.json();
      
      setCurrentMod(data.mod);
      
      toast({
        title: "Mod updated",
        description: "Your mod details have been updated.",
      });
      
      // Invalidate mod query
      queryClient.invalidateQueries({ queryKey: [`/api/mods/${id}`] });
      
    } catch (error) {
      console.error("Error updating mod:", error);
      addConsoleOutput(`Error: ${error instanceof Error ? error.message : String(error)}`);
      toast({
        variant: "destructive",
        title: "Failed to update mod",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const compileMod = async (modId: number) => {
    setIsLoading(true);
    setIsGenerating(true);
    addConsoleOutput("Starting compilation process...");

    try {
      const response = await apiRequest("POST", `/api/mods/${modId}/compile`, {});
      const data = await response.json();
      
      setCurrentBuild(data.build);
      
      // Start polling for build status
      await pollBuildStatus(modId, data.build.id);
      
      toast({
        title: "Compilation started",
        description: "Your mod is being compiled. Check the console for progress updates.",
      });
      
    } catch (error) {
      console.error("Error compiling mod:", error);
      addConsoleOutput(`Error: ${error instanceof Error ? error.message : String(error)}`);
      toast({
        variant: "destructive",
        title: "Failed to compile mod",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    } finally {
      setIsLoading(false);
      setIsGenerating(false);
    }
  };

  const pushToGithub = async (modId: number) => {
    setIsLoading(true);
    addConsoleOutput("Pushing to GitHub...");

    try {
      const response = await apiRequest("POST", `/api/mods/${modId}/github`, {});
      const data = await response.json();
      
      toast({
        title: "Success",
        description: `Successfully pushed to GitHub: ${data.repoUrl}`,
      });
      
      addConsoleOutput(`Successfully pushed to GitHub: ${data.repoUrl}`);
      
    } catch (error) {
      console.error("Error pushing to GitHub:", error);
      addConsoleOutput(`Error: ${error instanceof Error ? error.message : String(error)}`);
      toast({
        variant: "destructive",
        title: "Failed to push to GitHub",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const pollBuildStatus = async (modId: number, buildId: number) => {
    let status: BuildStatus = BuildStatus.InProgress;
    
    while (status === BuildStatus.InProgress) {
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      try {
        const response = await fetch(`/api/mods/${modId}/builds/${buildId}`, {
          credentials: 'include'
        });
        
        if (!response.ok) {
          throw new Error(`Server responded with ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        status = data.build.status;
        
        // Update the current build
        setCurrentBuild(data.build);
        
        // Add any new logs to the console
        if (data.build.logs) {
          data.build.logs.split('\n').forEach((line: string) => {
            if (line.trim()) addConsoleOutput(line);
          });
        }
        
        // If the build is complete, fetch all builds for the mod
        if (status !== BuildStatus.InProgress) {
          const buildsResponse = await fetch(`/api/mods/${modId}/builds`, {
            credentials: 'include'
          });
          
          if (buildsResponse.ok) {
            const buildsData = await buildsResponse.json();
            setBuilds(buildsData.builds);
          }
        }
      } catch (error) {
        console.error("Error polling build status:", error);
        addConsoleOutput(`Error polling build status: ${error instanceof Error ? error.message : String(error)}`);
        break;
      }
    }
  };

  return {
    generateMod,
    updateMod,
    compileMod,
    pushToGithub,
    isLoading,
  };
}
