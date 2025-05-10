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
  return apiRequest("/api/ai/expand-idea", {
    method: "POST",
    data: params
  });
}

// Generate mod code structure
export async function generateModCode(params: {
  modName: string;
  modDescription: string;
  modLoader: string;
  mcVersion: string;
  idea: string;
}) {
  return apiRequest("/api/ai/generate-mod-code", {
    method: "POST",
    data: params
  });
}

// Fix compilation errors
export async function fixCompilationErrors(params: {
  files: Array<{ path: string; content: string }>;
  errors: string;
  modLoader: string;
}) {
  return apiRequest("/api/ai/fix-errors", {
    method: "POST",
    data: params
  });
}

// Generate documentation for code
export async function generateDocumentation(params: {
  code: string;
  language: string;
  style?: string;
}) {
  return apiRequest("/api/ai/generate-documentation", {
    method: "POST",
    data: params
  });
}

// Add features to an existing mod
export async function addFeatures(params: {
  files: Array<{ path: string; content: string }>;
  featureDescription: string;
  modLoader: string;
}) {
  return apiRequest("/api/ai/add-features", {
    method: "POST",
    data: params
  });
}

// Generic code generation
export async function generateGenericCode(params: {
  prompt: string;
  language: string;
  context?: string;
  complexity?: string;
}) {
  return apiRequest("/api/ai/generate-code", {
    method: "POST",
    data: params
  });
}