import React, { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { useModContext } from "@/context/mod-context";
import axios from 'axios';

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
  const [isVerifying, setIsVerifying] = useState(false);
  const { toast } = useToast();
  const { currentMod } = useModContext();
  
  // Verify GitHub token
  const handleVerifyToken = async () => {
    if (!token) {
      toast({
        title: "Token Required",
        description: "Please enter a GitHub personal access token.",
        variant: "destructive"
      });
      return;
    }
    
    setIsVerifying(true);
    
    try {
      // Verify token by testing it
      const response = await axios.post<GitHubVerifyResponse>('/api/github/verify-token', { token });
      const data = response.data;
      
      if (data.valid) {
        toast({
          title: "Authentication Successful",
          description: `Connected to GitHub as ${data.username}`,
          variant: "default"
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
    } catch (error) {
      toast({
        title: "Authentication Error",
        description: error instanceof Error ? error.message : "Failed to verify GitHub token.",
        variant: "destructive"
      });
    } finally {
      setIsVerifying(false);
    }
  };
  
  // Handle push to GitHub when a mod is selected
  const handlePushToGitHub = async () => {
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
    
    setIsVerifying(true);
    
    try {
      const response = await axios.post<GitHubPushResponse>(
        `/api/mods/${currentMod.id}/push-to-github`, 
        { token }
      );
      const data = response.data;
      
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
                className="text-primary underline"
              >
                View Repository
              </a>
            </div>
          ),
          variant: "default"
        });
      } else {
        toast({
          title: "GitHub Push Failed",
          description: data.error || "Failed to push mod to GitHub.",
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "GitHub Push Error",
        description: error instanceof Error ? error.message : "An unknown error occurred.",
        variant: "destructive"
      });
    } finally {
      setIsVerifying(false);
    }
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
          disabled={!token || isVerifying}
        >
          {isVerifying ? (
            <>
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Verifying...
            </>
          ) : "Verify Token"}
        </Button>
        
        <Button 
          onClick={handlePushToGitHub}
          disabled={!token || !currentMod || isVerifying}
        >
          {isVerifying ? (
            <>
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
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