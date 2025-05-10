import OpenAI from "openai";

// the newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
const OPENAI_MODEL = "gpt-4o";

// Note: We're not using direct OpenAI client on the client side
// All API calls go through our server endpoints for security
// This is just a stub to inform developers that OpenAI calls should be made server-side

import { apiRequest } from "./queryClient";

// These functions serve as an interface to the server-side OpenAI functionality
// They call our server API endpoints rather than OpenAI directly

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
  try {
    // Call our server API endpoint instead of OpenAI directly
    const response = await apiRequest("POST", "/api/ai/generate-ideas", params);
    return response.json();
  } catch (error) {
    console.error("Error in generateModIdeas:", error);
    throw error;
  }
}

// Expand a mod idea with more details
export async function expandModIdea(params: {
  title: string;
  description: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/expand-idea", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in expandModIdea:", error);
    throw error;
  }
}

// Generate mod code structure
export async function generateModCode(params: {
  modName: string;
  modDescription: string;
  modLoader: string;
  mcVersion: string;
  idea: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/generate-code", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in generateModCode:", error);
    throw error;
  }
}

// Fix compilation errors
export async function fixCompilationErrors(params: {
  files: Array<{ path: string; content: string }>;
  errors: string;
  modLoader: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/fix-errors", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in fixCompilationErrors:", error);
    throw error;
  }
}

// Generate documentation for code
export async function generateDocumentation(params: {
  code: string;
  language: string;
  style?: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/generate-docs", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in generateDocumentation:", error);
    throw error;
  }
}

// Add features to an existing mod
export async function addFeatures(params: {
  files: Array<{ path: string; content: string }>;
  featureDescription: string;
  modLoader: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/add-features", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in addFeatures:", error);
    throw error;
  }
}

// Generic code generation
export async function generateGenericCode(params: {
  prompt: string;
  language: string;
  context?: string;
  complexity?: string;
}) {
  try {
    // Call our server API endpoint instead of OpenAI directly
    return apiRequest("/api/ai/generate-generic-code", {
      method: "POST",
      data: params
    });
  } catch (error) {
    console.error("Error in generateGenericCode:", error);
    throw error;
  }
}