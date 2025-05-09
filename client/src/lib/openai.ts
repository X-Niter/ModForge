import { apiRequest } from "./queryClient";

// Function to generate mod code using the backend OpenAI service
export async function generateModCode(
  modName: string,
  modDescription: string,
  modLoader: string,
  mcVersion: string,
  idea: string
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
}> {
  try {
    const response = await apiRequest("POST", "/api/ai/generate-code", {
      modName,
      modDescription,
      modLoader,
      mcVersion,
      idea,
    });

    return await response.json();
  } catch (error) {
    console.error("Error generating mod code:", error);
    throw new Error(
      `Failed to generate mod code: ${
        error instanceof Error ? error.message : "Unknown error"
      }`
    );
  }
}

// Function to fix compilation errors
export async function fixCompilationErrors(
  files: Array<{ path: string; content: string }>,
  errors: Array<{
    file: string;
    line: number;
    message: string;
    code?: string;
  }>
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
}> {
  try {
    const response = await apiRequest("POST", "/api/ai/fix-errors", {
      files,
      errors,
    });

    return await response.json();
  } catch (error) {
    console.error("Error fixing compilation errors:", error);
    throw new Error(
      `Failed to fix compilation errors: ${
        error instanceof Error ? error.message : "Unknown error"
      }`
    );
  }
}

// Function to add features to an existing mod
export async function addModFeatures(
  files: Array<{ path: string; content: string }>,
  newFeatureDescription: string
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
}> {
  try {
    const response = await apiRequest("POST", "/api/ai/add-features", {
      files,
      newFeatureDescription,
    });

    return await response.json();
  } catch (error) {
    console.error("Error adding mod features:", error);
    throw new Error(
      `Failed to add mod features: ${
        error instanceof Error ? error.message : "Unknown error"
      }`
    );
  }
}
