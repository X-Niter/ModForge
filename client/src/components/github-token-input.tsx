import React, { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { apiRequest } from "@/lib/queryClient";
import { Loader2 } from "lucide-react";

export function GitHubTokenInput() {
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token.trim()) {
      toast({
        title: "Token required",
        description: "Please enter a GitHub personal access token",
        variant: "destructive"
      });
      return;
    }
    
    try {
      setLoading(true);
      const response = await apiRequest("POST", "/api/github/save-token", { token });
      
      if (response.ok) {
        toast({
          title: "Success",
          description: "GitHub token saved successfully",
        });
        // Force a refresh to update the authentication status
        window.location.reload();
      } else {
        const error = await response.json();
        throw new Error(error.message || "Failed to save GitHub token");
      }
    } catch (error) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : "An error occurred while saving the token";
        
      toast({
        title: "Error",
        description: errorMessage,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="space-y-2">
        <Label htmlFor="github-token" className="text-white">GitHub Token</Label>
        <div className="flex gap-2">
          <Input
            id="github-token"
            type="password"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="ghp_xxxxxxxxxxxxxxxxx"
            className="flex-1"
            disabled={loading}
          />
          <Button type="submit" disabled={loading}>
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Saving
              </>
            ) : (
              "Save Token"
            )}
          </Button>
        </div>
        <p className="text-xs text-gray-400 mt-1">
          Create a token with <code>repo</code> scope at{" "}
          <a 
            href="https://github.com/settings/tokens/new" 
            target="_blank" 
            rel="noopener noreferrer"
            className="text-blue-400 hover:underline"
          >
            github.com/settings/tokens/new
          </a>
        </p>
      </div>
    </form>
  );
}