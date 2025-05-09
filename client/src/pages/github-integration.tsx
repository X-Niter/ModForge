import React from 'react';
import { GitHubAuth } from "@/components/github-auth";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
        <div className="md:col-span-2">
          <GitHubAuth />
        </div>
        
        <div className="space-y-6">
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