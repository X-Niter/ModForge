import React, { useState, useEffect } from 'react';
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { useQuery } from "@tanstack/react-query";

interface GitHubOAuthButtonProps {
  onSuccess?: () => void;
  className?: string;
}

export function GitHubOAuthButton({ onSuccess, className }: GitHubOAuthButtonProps) {
  const [isLoading, setIsLoading] = useState(false);
  const { toast } = useToast();
  
  // Check if user is already authenticated
  const { data: authData, isLoading: isAuthCheckLoading } = useQuery({
    queryKey: ['/api/auth/me'],
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
  
  useEffect(() => {
    // If authentication check completes and user is authenticated
    if (authData?.authenticated && onSuccess) {
      onSuccess();
    }
  }, [authData, onSuccess]);
  
  const handleLoginClick = () => {
    setIsLoading(true);
    
    // Redirect to GitHub OAuth login
    window.location.href = '/api/auth/github';
  };
  
  return (
    <div className={className}>
      {authData?.authenticated ? (
        <div className="flex items-center space-x-2">
          {authData.user?.avatarUrl && (
            <img 
              src={authData.user.avatarUrl} 
              alt="GitHub Avatar" 
              className="w-8 h-8 rounded-full"
            />
          )}
          <span className="text-sm text-gray-300">
            Connected as {authData.user?.username || 'GitHub User'}
          </span>
        </div>
      ) : (
        <Button 
          onClick={handleLoginClick}
          disabled={isLoading || isAuthCheckLoading}
          className="flex items-center space-x-2 bg-slate-800 hover:bg-slate-700"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="20" 
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="text-white"
          >
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
          </svg>
          <span>Login with GitHub</span>
          {isLoading && (
            <span className="animate-spin ml-2">◌</span>
          )}
        </Button>
      )}
    </div>
  );
}