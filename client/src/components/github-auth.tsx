import React, { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { useModContext } from "@/context/mod-context";
import { apiRequest } from "@/lib/queryClient";
import { useMutation } from "@tanstack/react-query";

interface GitHubAuthProps {
  onSuccess?: (token: string) => void;
}

interface GitHubVerifyResponse {
  valid: boolean;
  username?: string;
  message?: string;
}

interface GitHubPushResponse {
  success: boolean;
  repoUrl?: string;
  error?: string;
  owner?: string;
}

export function GitHubAuth({ onSuccess }: GitHubAuthProps) {
  const [token, setToken] = useState('');
  const { toast } = useToast();
  const { currentMod } = useModContext();
  
  // Verify GitHub token mutation
  const verifyTokenMutation = useMutation({
    mutationFn: async (tokenToVerify: string) => {
      const response = await apiRequest("POST", "/api/github/verify-token", { token: tokenToVerify });
      
      if (!response.ok) {
        throw new Error("Failed to verify GitHub token");
      }
      
      return response.json() as Promise<GitHubVerifyResponse>;
    },
    onSuccess: (data) => {
      if (data.valid) {
        toast({
          title: "Authentication Successful",
          description: `Connected to GitHub as ${data.username}`,
        });
        
        // Pass token to parent component if needed
        if (onSuccess) {
          onSuccess(token);
        }
      } else {
        toast({
          title: "Authentication Failed",
          description: data.message || "Invalid GitHub token. Please check and try again.",
          variant: "destructive"
        });
      }
    },
    onError: (error) => {
      toast({
        title: "Authentication Error",
        description: error instanceof Error ? error.message : "Failed to verify GitHub token.",
        variant: "destructive"
      });
    }
  });
  
  // Push to GitHub mutation
  const pushToGitHubMutation = useMutation({
    mutationFn: async () => {
      if (!currentMod) {
        throw new Error("No mod selected");
      }
      
      const response = await apiRequest(
        "POST", 
        `/api/mods/${currentMod.id}/push-to-github`, 
        { token }
      );
      
      if (!response.ok) {
        throw new Error("Failed to push to GitHub");
      }
      
      return response.json() as Promise<GitHubPushResponse>;
    },
    onSuccess: (data) => {
      if (data.success && data.repoUrl) {
        toast({
          title: "Mod Pushed to GitHub",
          description: (
            <div>
              <p>Your mod has been successfully pushed to GitHub!</p>
              <a 
                href={data.repoUrl} 
                target="_blank" 
                rel="noopener noreferrer"
                className="underline"
              >
                View Repository
              </a>
            </div>
          )
        });
      } else {
        toast({
          title: "GitHub Push Failed",
          description: data.error || "Failed to push mod to GitHub.",
          variant: "destructive"
        });
      }
    },
    onError: (error) => {
      toast({
        title: "GitHub Push Error",
        description: error instanceof Error ? error.message : "An unknown error occurred.",
        variant: "destructive"
      });
    }
  });

  // Verify GitHub token handler
  const handleVerifyToken = () => {
    if (!token) {
      toast({
        title: "Token Required",
        description: "Please enter a GitHub personal access token.",
        variant: "destructive"
      });
      return;
    }
    
    verifyTokenMutation.mutate(token);
  };
  
  // Handle push to GitHub when a mod is selected
  const handlePushToGitHub = () => {
    if (!currentMod) {
      toast({
        title: "No Mod Selected",
        description: "Please select a mod to push to GitHub.",
        variant: "destructive"
      });
      return;
    }
    
    if (!token) {
      toast({
        title: "Token Required",
        description: "Please enter and verify your GitHub token first.",
        variant: "destructive"
      });
      return;
    }
    
    pushToGitHubMutation.mutate();
  };
  
  return (
    <Card className="bg-surface shadow-lg">
      <CardHeader>
        <CardTitle className="text-white flex items-center gap-2">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="text-primary"
            width="20" 
            height="20"
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
          GitHub Integration
        </CardTitle>
        <CardDescription>
          Connect your mod with GitHub to save and share your work
        </CardDescription>
      </CardHeader>
      
      <CardContent>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="github-token">GitHub Personal Access Token</Label>
            <Input
              id="github-token"
              type="password"
              placeholder="ghp_your_token_here"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              className="text-white bg-background border-gray-700"
            />
            <p className="text-xs text-gray-400">
              Create a token with 'repo' scope at{' '}
              <a 
                href="https://github.com/settings/tokens/new" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-primary underline"
              >
                GitHub Settings
              </a>
            </p>
          </div>
        </div>
      </CardContent>
      
      <CardFooter className="flex justify-between">
        <Button 
          variant="outline" 
          onClick={handleVerifyToken}
          disabled={!token || verifyTokenMutation.isPending}
        >
          {verifyTokenMutation.isPending ? (
            <>
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Verifying...
            </>
          ) : "Verify Token"}
        </Button>
        
        <Button 
          onClick={handlePushToGitHub}
          disabled={!token || !currentMod || pushToGitHubMutation.isPending}
        >
          {pushToGitHubMutation.isPending ? (
            <>
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Pushing...
            </>
          ) : "Push to GitHub"}
        </Button>
      </CardFooter>
    </Card>
  );
}