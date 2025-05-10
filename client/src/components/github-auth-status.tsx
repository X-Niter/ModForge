import React from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

interface GitHubAuthStatusProps {
  className?: string;
}

export function GitHubAuthStatus({ className }: GitHubAuthStatusProps) {
  // Interface for the auth response
  interface AuthResponse {
    authenticated: boolean;
    user?: {
      id: number;
      username: string;
      avatarUrl?: string;
      githubId?: string;
      email?: string;
    };
  }
  
  // Interface for GitHub status response
  interface GitHubStatusResponse {
    success: boolean;
    source?: string;
    username?: string;
    avatar_url?: string;
    message?: string;
  }
  
  // Check GitHub authentication status using both methods
  const { 
    data: authData, 
    isLoading: authLoading 
  } = useQuery<AuthResponse>({
    queryKey: ['/api/auth/me'],
    staleTime: 60 * 1000, // 1 minute
  });
  
  const { 
    data: githubStatus, 
    isLoading: githubLoading 
  } = useQuery<GitHubStatusResponse>({
    queryKey: ['/api/github/get-credentials'],
    staleTime: 60 * 1000, // 1 minute
  });
  
  // Handle logout
  const handleLogout = () => {
    window.location.href = '/api/auth/logout';
  };
  
  return (
    <Card className={`bg-surface shadow-lg ${className}`}>
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
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
            <line x1="9" y1="9" x2="9.01" y2="9"></line>
            <line x1="15" y1="9" x2="15.01" y2="9"></line>
          </svg>
          GitHub Account Status
        </CardTitle>
        <CardDescription>
          Your GitHub connection status
        </CardDescription>
      </CardHeader>
      <CardContent>
        {authLoading || githubLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-4 w-[250px]" />
            <Skeleton className="h-4 w-[200px]" />
            <Skeleton className="h-4 w-[150px]" />
          </div>
        ) : githubStatus?.success ? (
          // GitHub authenticated via any method (OAuth, token, or environment)
          <div className="space-y-4">
            <div className="flex items-center space-x-3">
              {githubStatus.avatar_url && (
                <img 
                  src={githubStatus.avatar_url} 
                  alt="GitHub Avatar" 
                  className="w-10 h-10 rounded-full border border-gray-600"
                />
              )}
              <div>
                <h4 className="text-white font-medium">{githubStatus.username || "GitHub User"}</h4>
                <p className="text-gray-400 text-sm">
                  {githubStatus.source === 'session' ? 'OAuth Authentication' : 
                   githubStatus.source === 'request' ? 'Token Authentication' : 
                   'Environment Authentication'}
                </p>
              </div>
            </div>
            
            <div className="flex items-center space-x-2 bg-green-950/30 p-3 rounded-md border border-green-800">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="text-green-500 h-5 w-5 flex-shrink-0"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <span className="text-green-400 text-sm">Successfully authenticated with GitHub</span>
            </div>
            
            {githubStatus.source === 'session' && (
              <Button 
                variant="outline" 
                className="w-full mt-4"
                onClick={handleLogout}
              >
                Sign Out from GitHub
              </Button>
            )}
          </div>
        ) : (
          // No GitHub authentication available
          <div className="space-y-4">
            <div className="flex items-center space-x-2 bg-amber-950/30 p-3 rounded-md border border-amber-800">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="text-amber-500 h-5 w-5 flex-shrink-0"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
              <span className="text-amber-400 text-sm">Not connected to GitHub</span>
            </div>
            
            <p className="text-gray-400 text-sm">
              Connect to GitHub to automate repository creation and updates. 
              You can use OAuth or a personal access token.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}