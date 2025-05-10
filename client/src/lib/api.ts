import { apiRequest } from "@/lib/queryClient";

// Generate mod ideas
export async function generateModIdeas(params: {
  theme?: string;
  complexity: string;
  category?: string;
  modLoader: string;
  mcVersion?: string;
  includeItems: boolean;
  includeBlocks: boolean;
  includeEntities: boolean;
  includeWorldGen: boolean;
  includeStructures: boolean;
  includeGameplayMechanics: boolean;
  additionalNotes?: string;
}) {
  return apiRequest("/api/ai/generate-ideas", {
    method: "POST",
    data: params
  });
}

// Expand a mod idea with more details
export async function expandModIdea(params: {
  title: string;
  description: string;
}) {
  const response = await apiRequest("POST", "/api/ai/expand-idea", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to expand mod idea");
  }
  return response.json();
}

// Generate mod code structure
export async function generateModCode(params: {
  modName: string;
  modDescription: string;
  modLoader: string;
  mcVersion: string;
  idea: string;
}) {
  const response = await apiRequest("POST", "/api/ai/generate-mod-code", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to generate mod code");
  }
  return response.json();
}

// Fix compilation errors
export async function fixCompilationErrors(params: {
  files: Array<{ path: string; content: string }>;
  errors: string;
  modLoader: string;
}) {
  const response = await apiRequest("POST", "/api/ai/fix-errors", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to fix compilation errors");
  }
  return response.json();
}

// Generate documentation for code
export async function generateDocumentation(params: {
  code: string;
  language: string;
  style?: string;
}) {
  const response = await apiRequest("POST", "/api/ai/generate-documentation", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to generate documentation");
  }
  return response.json();
}

// Add features to an existing mod
export async function addFeatures(params: {
  files: Array<{ path: string; content: string }>;
  featureDescription: string;
  modLoader: string;
}) {
  const response = await apiRequest("POST", "/api/ai/add-features", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to add features");
  }
  return response.json();
}

// Generic code generation
export async function generateGenericCode(params: {
  prompt: string;
  language: string;
  context?: string;
  complexity?: string;
}) {
  const response = await apiRequest("POST", "/api/ai/generate-code", params);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Failed to generate code");
  }
  return response.json();
}