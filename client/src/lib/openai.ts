import OpenAI from "openai";
import { apiRequest } from "@/lib/queryClient";

// Custom type for code generation responses
export interface CodeGenerationResponse {
  code: string;
  explanation: string;
  suggestedFileName?: string;
}

// Custom type for completion responses
export interface CompletionResponse {
  text: string;
}

// Custom type for Minecraft mod generation response
export interface ModGenerationResponse {
  files: Array<{ path: string; content: string }>;
  explanation: string;
  logs: string;
}

/**
 * Client-side function to request Minecraft mod code generation through our backend API
 * This forwards the request to the server which uses the OpenAI API
 */
export async function generateModCode(
  modName: string,
  modDescription: string,
  modLoader: string,
  mcVersion: string,
  idea: string
): Promise<ModGenerationResponse> {
  try {
    const response = await apiRequest<ModGenerationResponse>("/api/ai/generate-mod-code", {
      method: "POST",
      body: JSON.stringify({
        modName,
        modDescription,
        modLoader,
        mcVersion,
        idea
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to generate mod code:", error);
    throw new Error("Mod code generation failed. Please try again.");
  }
}

/**
 * Client-side function to request general code generation through our backend API
 * This forwards the request to the server which uses the OpenAI API
 */
export async function generateCode(
  prompt: string,
  options?: {
    language?: string;
    context?: string;
    complexity?: "simple" | "medium" | "complex";
  }
): Promise<CodeGenerationResponse> {
  try {
    const response = await apiRequest<CodeGenerationResponse>("/api/ai/generate-code", {
      method: "POST",
      body: JSON.stringify({
        prompt,
        language: options?.language || "java",
        context: options?.context,
        complexity: options?.complexity || "medium"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to generate code:", error);
    throw new Error("Code generation failed. Please try again.");
  }
}

/**
 * Client-side function to request code fixing through our backend API
 */
export async function fixCode(
  code: string,
  errors: string[],
  language?: string
): Promise<CodeGenerationResponse> {
  try {
    const response = await apiRequest<CodeGenerationResponse>("/api/ai/fix-code", {
      method: "POST",
      body: JSON.stringify({
        code,
        errors,
        language: language || "java"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to fix code:", error);
    throw new Error("Code fixing failed. Please try again.");
  }
}

/**
 * Client-side function for AI-based code enhancement
 */
export async function enhanceCode(
  code: string,
  instructions: string,
  language?: string
): Promise<CodeGenerationResponse> {
  try {
    const response = await apiRequest<CodeGenerationResponse>("/api/ai/enhance-code", {
      method: "POST",
      body: JSON.stringify({
        code,
        instructions,
        language: language || "java"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to enhance code:", error);
    throw new Error("Code enhancement failed. Please try again.");
  }
}

/**
 * Client-side function to summarize code
 */
export async function summarizeCode(
  code: string,
  language?: string
): Promise<CompletionResponse> {
  try {
    const response = await apiRequest<CompletionResponse>("/api/ai/summarize-code", {
      method: "POST",
      body: JSON.stringify({
        code,
        language: language || "java"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to summarize code:", error);
    throw new Error("Code summarization failed. Please try again.");
  }
}

/**
 * Client-side function to explain error messages
 */
export async function explainError(
  errorMessage: string,
  code?: string,
  language?: string
): Promise<CompletionResponse> {
  try {
    const response = await apiRequest<CompletionResponse>("/api/ai/explain-error", {
      method: "POST",
      body: JSON.stringify({
        errorMessage,
        code,
        language: language || "java"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to explain error:", error);
    throw new Error("Error explanation failed. Please try again.");
  }
}

/**
 * Client-side function to generate code documentation
 */
export async function generateDocumentation(
  code: string,
  language?: string,
  style?: "javadoc" | "markdown" | "inline"
): Promise<CompletionResponse> {
  try {
    const response = await apiRequest<CompletionResponse>("/api/ai/generate-documentation", {
      method: "POST",
      body: JSON.stringify({
        code,
        language: language || "java",
        style: style || "javadoc"
      })
    });
    
    return response;
  } catch (error) {
    console.error("Failed to generate documentation:", error);
    throw new Error("Documentation generation failed. Please try again.");
  }
}