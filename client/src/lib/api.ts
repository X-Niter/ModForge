import { apiRequest } from "./queryClient";

/**
 * API client for ModForge services
 * A structured set of API functions for interacting with the backend
 */

// Types
export interface IdeaGenerationParams {
  theme?: string;
  complexity?: string;
  gameVersion?: string;
  modLoader?: string;
  keywords?: string[];
}

export interface UsageMetrics {
  totalRequests: number;
  patternMatches: number;
  apiCalls: number;
  estimatedTokensSaved: number;
  estimatedCostSaved: number;
  patternMatchRate?: string;
  apiCallRate?: string;
}

export interface TypeMetrics {
  patterns: number;
  matches: number;
  uses: number;
  successRate: number;
  avgConfidence: number;
}

export interface PatternMetrics {
  overall: {
    totalPatterns: number;
    totalUses: number;
    totalSuccesses: number;
    averageSuccessRate: number;
    estimatedTokensSaved: number;
    estimatedCostSaved: number;
  };
  ideas: TypeMetrics;
  code: TypeMetrics;
  fixes: TypeMetrics;
  features: TypeMetrics;
  documentation: TypeMetrics;
}

export interface SystemHealth {
  status: "healthy" | "unhealthy" | "error";
  message: string;
  databaseInfo?: {
    version?: string;
    connection?: string;
    uptime?: number;
  };
  timestamp?: string;
  error?: string;
}

export interface IdeaExpansionParams {
  title: string;
  description: string;
}

export interface ModCode {
  name: string;
  description: string;
  modLoader: string;
  minecraftVersion: string;
  idea?: string;
}

export interface CompilationError {
  file: string;
  line: number;
  message: string;
  code?: string;
}

// API Functions
export async function generateIdeas(params: IdeaGenerationParams) {
  const response = await apiRequest("POST", "/api/ai/generate-ideas", params);
  if (!response.ok) {
    throw new Error("Failed to generate ideas");
  }
  return response.json();
}

export async function expandIdea(params: IdeaExpansionParams) {
  const response = await apiRequest("POST", "/api/ai/expand-idea", params);
  if (!response.ok) {
    throw new Error("Failed to expand idea");
  }
  return response.json();
}

export async function generateModCode(params: ModCode) {
  const response = await apiRequest("POST", "/api/ai/generate-mod-code", params);
  if (!response.ok) {
    throw new Error("Failed to generate mod code");
  }
  return response.json();
}

export async function fixErrors(files: Array<{path: string, content: string}>, errors: CompilationError[]) {
  const response = await apiRequest("POST", "/api/ai/fix-errors", { files, errors });
  if (!response.ok) {
    throw new Error("Failed to fix errors");
  }
  return response.json();
}

export async function generateDocumentation(code: string, context?: string) {
  const response = await apiRequest("POST", "/api/ai/generate-documentation", { code, context });
  if (!response.ok) {
    throw new Error("Failed to generate documentation");
  }
  return response.json();
}

export async function addFeatures(files: Array<{path: string, content: string}>, description: string) {
  const response = await apiRequest("POST", "/api/ai/add-features", { files, description });
  if (!response.ok) {
    throw new Error("Failed to add features");
  }
  return response.json();
}

export async function generateCode(prompt: string, context?: string) {
  const response = await apiRequest("POST", "/api/ai/generate-code", { prompt, context });
  if (!response.ok) {
    throw new Error("Failed to generate code");
  }
  return response.json();
}

export async function generateGenericCode(prompt: string, context?: string) {
  const response = await apiRequest("POST", "/api/ai/generate-generic-code", { prompt, context });
  if (!response.ok) {
    throw new Error("Failed to generate generic code");
  }
  return response.json();
}

// GitHub Integration
export async function verifyGitHubToken(token: string) {
  const response = await apiRequest("POST", "/api/github/verify-token", { token });
  if (!response.ok) {
    throw new Error("Failed to verify GitHub token");
  }
  return response.json();
}

export async function pushToGitHub(modId: number, token: string) {
  const response = await apiRequest("POST", `/api/mods/${modId}/push-to-github`, { token });
  if (!response.ok) {
    throw new Error("Failed to push to GitHub");
  }
  return response.json();
}

// Metrics
export async function getUsageMetrics(): Promise<UsageMetrics> {
  const response = await apiRequest("GET", "/api/metrics/usage", null);
  if (!response.ok) {
    throw new Error("Failed to fetch usage metrics");
  }
  return response.json();
}

export async function getPatternLearningMetrics(): Promise<PatternMetrics> {
  const response = await apiRequest("GET", "/api/pattern-learning/metrics", null);
  if (!response.ok) {
    throw new Error("Failed to fetch pattern learning metrics");
  }
  return response.json();
}

// Health Check
export async function getSystemHealth(): Promise<SystemHealth> {
  const response = await apiRequest("GET", "/api/health", null);
  return response.json();
}