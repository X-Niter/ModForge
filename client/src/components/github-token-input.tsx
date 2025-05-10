import React, { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { apiRequest } from "@/lib/queryClient";
import {
  AlertCircle,
  CheckCircle2,
  Github,
  Key,
  Lock
} from "lucide-react";

interface GitHubTokenInputProps {
  onSuccess?: () => void;
  className?: string;
}

export function GitHubTokenInput({ onSuccess, className }: GitHubTokenInputProps) {
  const [token, setToken] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [isVerified, setIsVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setToken(e.target.value);
    if (isVerified) setIsVerified(false);
    if (error) setError(null);
  };

  const handleVerify = async () => {
    if (!token.trim()) {
      setError('Please enter a GitHub token');
      return;
    }

    setIsVerifying(true);
    setError(null);

    try {
      // Verify the token by calling GitHub API
      const response = await apiRequest('POST', '/api/github/verify-token', { token });
      
      if (response.ok) {
        const data = await response.json();
        setIsVerified(true);
        toast({
          title: "Token Verified",
          description: `Connected as ${data.username}`,
        });
        
        if (onSuccess) {
          onSuccess();
        }
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to verify token');
      }
    } catch (err) {
      setError('Error verifying token. Please try again.');
      console.error('Error verifying token:', err);
    } finally {
      setIsVerifying(false);
    }
  };

  const handleSave = async () => {
    if (!token.trim() || !isVerified) {
      return;
    }

    try {
      // Save the verified token
      const response = await apiRequest('POST', '/api/github/save-token', { token });
      
      if (response.ok) {
        toast({
          title: "Token Saved",
          description: "Your GitHub token has been securely stored",
          variant: "success",
        });
        
        if (onSuccess) {
          onSuccess();
        }
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to save token');
      }
    } catch (err) {
      setError('Error saving token. Please try again.');
      console.error('Error saving token:', err);
    }
  };

  return (
    <Card className={`bg-surface shadow-lg ${className}`}>
      <CardHeader>
        <CardTitle className="text-white flex items-center gap-2">
          <Key className="h-5 w-5 text-primary" />
          GitHub Personal Access Token
        </CardTitle>
        <CardDescription>
          Connect to GitHub using a personal access token
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="text-sm text-gray-400 mb-1">
            Enter your GitHub Personal Access Token:
          </div>
          <div className="flex gap-2">
            <Input
              type="password"
              value={token}
              onChange={handleChange}
              placeholder="ghp_xxxxxxxxxxxxxxxx"
              className="flex-1 bg-background"
            />
            <Button 
              onClick={handleVerify}
              disabled={isVerifying || !token.trim()}
              variant="secondary"
              size="sm"
            >
              {isVerifying ? "Verifying..." : "Verify"}
            </Button>
          </div>
        </div>

        {error && (
          <div className="text-red-500 text-sm flex items-center gap-1.5">
            <AlertCircle className="h-4 w-4" />
            <span>{error}</span>
          </div>
        )}

        {isVerified && (
          <div className="text-green-500 text-sm flex items-center gap-1.5">
            <CheckCircle2 className="h-4 w-4" />
            <span>Token verified successfully</span>
          </div>
        )}

        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700 text-xs text-gray-400 space-y-2">
          <p className="flex items-center gap-1.5">
            <Lock className="h-3.5 w-3.5 text-amber-500" />
            <span>Your token is stored securely and only used for GitHub operations</span>
          </p>
          <div>
            <div className="font-medium text-gray-300 mb-1">How to create a Personal Access Token:</div>
            <ol className="list-decimal pl-5 space-y-1">
              <li>Go to your GitHub Settings &gt; Developer settings &gt; Personal access tokens</li>
              <li>Click "Generate new token" (classic)</li>
              <li>Add a note like "ModForge"</li>
              <li>Select scopes: <span className="text-primary">repo</span> and <span className="text-primary">workflow</span></li>
              <li>Generate and copy your token</li>
            </ol>
          </div>
        </div>
      </CardContent>
      <CardFooter>
        <Button 
          className="w-full flex items-center gap-1.5"
          onClick={handleSave}
          disabled={!isVerified}
        >
          <Github className="h-4 w-4" />
          <span>Save Token & Connect</span>
        </Button>
      </CardFooter>
    </Card>
  );
}