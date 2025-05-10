import React from 'react';
import { GitHubAuth } from "@/components/github-auth";
import { GitHubOAuthButton } from "@/components/github-oauth-button";
import { GitHubAuthStatus } from "@/components/github-auth-status";
import { GitHubTokenInput } from "@/components/github-token-input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { useToast } from "@/hooks/use-toast";
import { useModContext } from "@/context/mod-context";

export default function GitHubIntegration() {
  const { currentMod } = useModContext();
  
  return (
    <div className="flex-1 p-6 overflow-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-white mb-2">GitHub Integration</h1>
        <p className="text-gray-400">
          Connect your Minecraft mod to GitHub for version control and collaboration
        </p>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
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
                  <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
                </svg>
                GitHub Authentication
              </CardTitle>
              <CardDescription>
                Connect your ModForge account to GitHub
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <Alert className="bg-amber-950/30 border-amber-700 text-amber-500">
                <svg 
                  xmlns="http://www.w3.org/2000/svg" 
                  width="16" 
                  height="16" 
                  viewBox="0 0 24 24" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2" 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  className="mr-2"
                >
                  <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"></path>
                  <path d="M12 9v4"></path>
                  <path d="M12 17h.01"></path>
                </svg>
                <AlertTitle>New Authentication Method Available</AlertTitle>
                <AlertDescription>
                  You can now authenticate directly with GitHub instead of using a token. Try the new method below for a more secure experience.
                </AlertDescription>
              </Alert>
              
              <div className="space-y-6">
                <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700">
                  <h3 className="text-white font-medium mb-3">OAuth Authentication (Recommended)</h3>
                  <p className="text-gray-400 text-sm mb-4">
                    Securely connect to GitHub with one click - no need to create or manage tokens.
                  </p>
                  <GitHubOAuthButton />
                </div>
                
                <div className="flex items-center gap-4 my-4">
                  <div className="flex-1 h-px bg-slate-700"></div>
                  <div className="text-slate-500 text-xs font-medium">OR</div>
                  <div className="flex-1 h-px bg-slate-700"></div>
                </div>
                
                <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700">
                  <h3 className="text-white font-medium mb-3">Personal Access Token</h3>
                  <p className="text-gray-400 text-sm mb-4">
                    For local development, IDE plugin use, or when OAuth is not available.
                  </p>
                  <GitHubTokenInput />
                </div>
              </div>
            </CardContent>
            <CardFooter>
              <p className="text-gray-500 text-xs">Your GitHub account permissions will only be used for pushing mods and accessing repositories.</p>
            </CardFooter>
          </Card>
          
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
                  <rect width="18" height="11" x="3" y="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
                Legacy Token Authentication
              </CardTitle>
              <CardDescription>
                Use a personal access token for GitHub authentication
              </CardDescription>
            </CardHeader>
            <CardContent>
              <GitHubAuth />
            </CardContent>
          </Card>
        </div>
        
        <div className="space-y-6">
          <GitHubAuthStatus />
          
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
                  <circle cx="12" cy="12" r="10"></circle>
                  <path d="M12 16v-4"></path>
                  <path d="M12 8h.01"></path>
                </svg>
                Repository Info
              </CardTitle>
              <CardDescription>
                Current GitHub repository information
              </CardDescription>
            </CardHeader>
            <CardContent>
              {currentMod?.githubRepo ? (
                <div className="space-y-3">
                  <div>
                    <h4 className="text-xs text-gray-400 mb-1">Repository URL</h4>
                    <p className="text-sm text-white break-all">
                      <a 
                        href={currentMod.githubRepo} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="text-primary hover:underline"
                      >
                        {currentMod.githubRepo}
                      </a>
                    </p>
                  </div>
                  <div>
                    <h4 className="text-xs text-gray-400 mb-1">Last Pushed</h4>
                    <p className="text-sm text-white">
                      {new Date(currentMod.updatedAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="text-center py-6">
                  <svg 
                    xmlns="http://www.w3.org/2000/svg" 
                    className="h-12 w-12 mx-auto text-gray-500 mb-3"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <p className="text-gray-400">
                    No repository connected
                  </p>
                  <p className="text-sm text-gray-500 mt-1">
                    Connect your mod using the GitHub integration
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
          
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
                  <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                </svg>
                How it Works
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex gap-2">
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-white">1</span>
                  </div>
                  <p className="text-sm text-gray-300">
                    Generate a personal access token on GitHub
                  </p>
                </div>
                <div className="flex gap-2">
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-white">2</span>
                  </div>
                  <p className="text-sm text-gray-300">
                    Verify your token to connect to GitHub
                  </p>
                </div>
                <div className="flex gap-2">
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-white">3</span>
                  </div>
                  <p className="text-sm text-gray-300">
                    Push your mod files to a new or existing repository
                  </p>
                </div>
                <div className="flex gap-2">
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-white">4</span>
                  </div>
                  <p className="text-sm text-gray-300">
                    Share your repository with other mod developers
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}