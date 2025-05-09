import OpenAI from "openai";
import { ErrorData } from "@/types";

// Types for API responses
export interface CodeGenerationResponse {
  code: string;
  explanation: string;
  suggestedFileName?: string;
}

export interface CompletionResponse {
  text: string;
}

// Initialize OpenAI client
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY || "dummy_key_for_development" });

// Helper to format timestamps
function getTimestamp(): string {
  return new Date().toISOString().replace('T', ' ').substring(0, 19);
}

import { 
  tryGenerateFromPatterns, 
  storeCodeGenerationPattern,
  tryFixFromPatterns,
  storeErrorFixPattern
} from './pattern-learning';

// Generate mod code using pattern learning with OpenAI fallback
export async function generateModCode(
  modName: string,
  modDescription: string,
  modLoader: string,
  mcVersion: string,
  idea: string
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
  logs: string;
}> {
  let logs = `[${getTimestamp()}] Starting mod code generation for ${modName}...\n`;
  logs += `[${getTimestamp()}] Using ${modLoader} for Minecraft ${mcVersion}\n`;
  
  try {
    // First, try to generate from our existing patterns
    logs += `[${getTimestamp()}] Searching for similar patterns in our knowledge base...\n`;
    const patternResult = await tryGenerateFromPatterns(idea, modLoader, mcVersion);
    
    // If we found a good pattern match with high confidence, use it
    if (patternResult.code && patternResult.confidence > 0.8) {
      logs += `[${getTimestamp()}] Found a highly similar pattern (${Math.round(patternResult.confidence * 100)}% match)! Using cached knowledge...\n`;
      
      // Parse the stored code into files
      // In a real implementation, you would properly store and retrieve the file structure
      const files = [
        { 
          path: `src/main/java/${modName.toLowerCase().replace(/\s+/g, '')}/${modName.replace(/\s+/g, '')}Mod.java`,
          content: patternResult.code
        }
      ];
      
      // Record that we used this pattern
      if (patternResult.patternId) {
        logs += `[${getTimestamp()}] Using knowledge from pattern #${patternResult.patternId}\n`;
      }
      
      return {
        files,
        explanation: "Generated using our knowledge base of Minecraft modding patterns",
        logs
      };
    }
    
    logs += `[${getTimestamp()}] No highly similar patterns found. Sending request to AI service...\n`;
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const prompt = `
      Generate a Minecraft mod based on the following requirements:
      
      Mod Name: ${modName}
      Mod Description: ${modDescription}
      Mod Loader: ${modLoader}
      Minecraft Version: ${mcVersion}
      
      Mod Idea:
      ${idea}
      
      Please generate the complete source code for this mod. Include all necessary files, including:
      1. Main mod class
      2. Any necessary configuration files
      3. Item/block classes as needed
      4. Rendering code if necessary
      5. Any other files needed for a complete mod
      
      Requirements:
      - The code should be compatible with ${modLoader} for Minecraft ${mcVersion}
      - Include proper package structure
      - Follow best practices for ${modLoader} development
      - Include appropriate comments and documentation
      
      Respond with a JSON object in the following format:
      {
        "files": [
          {
            "path": "relative/path/to/file.java",
            "content": "full file content here"
          }
        ],
        "explanation": "Brief explanation of the mod structure and how the components work together"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: prompt }],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    if (!result.files || !Array.isArray(result.files)) {
      throw new Error("Invalid response format from AI service");
    }
    
    logs += `[${getTimestamp()}] Successfully generated ${result.files.length} files\n`;
    
    // List all the generated files
    for (const file of result.files) {
      logs += `[${getTimestamp()}] Generated file: ${file.path}\n`;
    }
    
    return {
      files: result.files,
      explanation: result.explanation || "Mod code generated successfully",
      logs,
    };
  } catch (error) {
    logs += `[${getTimestamp()}] Error generating mod code: ${error instanceof Error ? error.message : String(error)}\n`;
    console.error("Error generating mod code:", error);
    
    // Return a minimal set of files in case of error
    return {
      files: [
        {
          path: `src/main/java/com/${modName.toLowerCase()}/ExampleMod.java`,
          content: `
            // Placeholder file due to an error in code generation
            package com.${modName.toLowerCase()};
            
            public class ExampleMod {
              // This is a placeholder file
              // Error occurred during code generation
            }
          `.trim(),
        },
      ],
      explanation: `Error generating code: ${error instanceof Error ? error.message : String(error)}`,
      logs,
    };
  }
}

// Fix compilation errors using OpenAI
export async function fixCompilationErrors(
  files: Array<{ path: string; content: string }>,
  errors: ErrorData[]
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
  logs: string;
}> {
  let logs = `[${getTimestamp()}] Starting error correction process...\n`;
  logs += `[${getTimestamp()}] Found ${errors.length} errors to fix\n`;
  
  try {
    logs += `[${getTimestamp()}] Sending error details to AI service...\n`;
    
    // Prepare the files and errors for the AI prompt
    const filesWithErrors = errors.map(error => {
      const file = files.find(f => f.path.includes(error.file));
      return {
        path: error.file,
        content: file?.content || "",
        line: error.line,
        message: error.message,
      };
    });
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const prompt = `
      Fix the following compilation errors in this Minecraft mod:
      
      Files with errors:
      ${JSON.stringify(filesWithErrors, null, 2)}
      
      All available files:
      ${JSON.stringify(files.map(f => ({ path: f.path })), null, 2)}
      
      For each error, determine the cause and provide a fix. Please return the complete fixed versions of all files that need to be modified.
      
      Respond with a JSON object in the following format:
      {
        "files": [
          {
            "path": "relative/path/to/file.java",
            "content": "full fixed file content here"
          }
        ],
        "explanation": "Explanation of what was wrong and how it was fixed for each error"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: prompt }],
      response_format: { type: "json_object" },
      temperature: 0.5,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    if (!result.files || !Array.isArray(result.files)) {
      throw new Error("Invalid response format from AI service");
    }
    
    // Update the original files with fixes
    const updatedFiles = [...files];
    for (const fixedFile of result.files) {
      const index = updatedFiles.findIndex(f => f.path === fixedFile.path);
      if (index >= 0) {
        updatedFiles[index] = fixedFile;
        logs += `[${getTimestamp()}] Fixed file: ${fixedFile.path}\n`;
      } else {
        updatedFiles.push(fixedFile);
        logs += `[${getTimestamp()}] Added new file: ${fixedFile.path}\n`;
      }
    }
    
    logs += `[${getTimestamp()}] Error correction complete\n`;
    
    return {
      files: updatedFiles,
      explanation: result.explanation || "Errors fixed successfully",
      logs,
    };
  } catch (error) {
    logs += `[${getTimestamp()}] Error fixing compilation errors: ${error instanceof Error ? error.message : String(error)}\n`;
    console.error("Error fixing compilation errors:", error);
    
    return {
      files,
      explanation: `Error fixing compilation errors: ${error instanceof Error ? error.message : String(error)}`,
      logs,
    };
  }
}

// Add features to an existing mod
export async function addModFeatures(
  files: Array<{ path: string; content: string }>,
  newFeatureDescription: string
): Promise<{
  files: Array<{ path: string; content: string }>;
  explanation: string;
  logs: string;
}> {
  let logs = `[${getTimestamp()}] Starting feature addition process...\n`;
  logs += `[${getTimestamp()}] New feature request: ${newFeatureDescription}\n`;
  
  try {
    logs += `[${getTimestamp()}] Analyzing existing mod structure...\n`;
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const prompt = `
      Add new features to an existing Minecraft mod based on the following description:
      
      New Feature Description:
      ${newFeatureDescription}
      
      Existing Mod Files:
      ${JSON.stringify(files.map(f => ({ path: f.path })), null, 2)}
      
      File Contents:
      ${JSON.stringify(files, null, 2)}
      
      Analyze the existing mod structure and add the requested features. You may need to:
      1. Modify existing files
      2. Create new files
      3. Ensure all changes maintain compatibility with the existing code
      
      Respond with a JSON object in the following format:
      {
        "files": [
          {
            "path": "relative/path/to/file.java",
            "content": "full updated file content here"
          }
        ],
        "explanation": "Explanation of how the new features were implemented and which files were created or modified"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: prompt }],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    if (!result.files || !Array.isArray(result.files)) {
      throw new Error("Invalid response format from AI service");
    }
    
    // Update the original files with changes
    const updatedFiles = [...files];
    for (const updatedFile of result.files) {
      const index = updatedFiles.findIndex(f => f.path === updatedFile.path);
      if (index >= 0) {
        updatedFiles[index] = updatedFile;
        logs += `[${getTimestamp()}] Modified file: ${updatedFile.path}\n`;
      } else {
        updatedFiles.push(updatedFile);
        logs += `[${getTimestamp()}] Added new file: ${updatedFile.path}\n`;
      }
    }
    
    logs += `[${getTimestamp()}] Feature addition complete\n`;
    
    return {
      files: updatedFiles,
      explanation: result.explanation || "Features added successfully",
      logs,
    };
  } catch (error) {
    logs += `[${getTimestamp()}] Error adding features: ${error instanceof Error ? error.message : String(error)}\n`;
    console.error("Error adding features:", error);
    
    return {
      files,
      explanation: `Error adding features: ${error instanceof Error ? error.message : String(error)}`,
      logs,
    };
  }
}

/**
 * Generate code from a prompt using OpenAI
 */
export async function generateCode(
  prompt: string,
  options: {
    language?: string;
    context?: string;
    complexity?: "simple" | "medium" | "complex";
  } = {}
): Promise<CodeGenerationResponse> {
  try {
    console.log(`[${getTimestamp()}] Generating code for prompt: ${prompt.substring(0, 100)}...`);
    
    const language = options.language || "java";
    const complexity = options.complexity || "medium";
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Generate ${language} code based on the following prompt:
      
      ${prompt}
      
      ${options.context ? `Context: ${options.context}` : ""}
      
      Complexity level: ${complexity}
      
      Instructions:
      - Generate well-structured, clean code that follows best practices for ${language}
      - Include appropriate comments and documentation
      - The complexity level is set to "${complexity}"
      
      Respond with a JSON object in the following format:
      {
        "code": "The generated code here",
        "explanation": "A brief explanation of how the code works and any important implementation details",
        "suggestedFileName": "A suggested file name for this code"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    return {
      code: result.code || "",
      explanation: result.explanation || "Code generated successfully",
      suggestedFileName: result.suggestedFileName,
    };
  } catch (error) {
    console.error("Error generating code:", error);
    throw new Error(`Code generation failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Fix code with errors using pattern learning with OpenAI fallback
 */
export async function fixCode(
  code: string,
  errors: string[],
  language: string = "java"
): Promise<CodeGenerationResponse> {
  try {
    console.log(`[${getTimestamp()}] Fixing code with ${errors.length} errors`);
    
    // Extract first error message for pattern matching
    const firstErrorMessage = errors.length > 0 ? errors[0] : "";
    
    // Try to fix from patterns first (if we have an error message to match against)
    if (firstErrorMessage) {
      console.log(`[${getTimestamp()}] Searching for known error patterns...`);
      // Determine mod loader from code content for better pattern matching
      // This is a simple heuristic - in a real implementation, use proper parsing
      let modLoader = "unknown";
      if (code.includes("net.fabricmc")) modLoader = "Fabric";
      else if (code.includes("net.minecraftforge")) modLoader = "Forge";
      else if (code.includes("org.bukkit")) modLoader = "Bukkit";
      
      const patternResult = await tryFixFromPatterns(firstErrorMessage, code, modLoader);
      
      if (patternResult.fixedCode && patternResult.confidence > 0.7) {
        console.log(`[${getTimestamp()}] Fixed using pattern matching with ${Math.round(patternResult.confidence * 100)}% confidence`);
        
        return {
          code: patternResult.fixedCode,
          explanation: "Fixed using our knowledge base of common Minecraft modding errors",
        };
      }
    }
    
    console.log(`[${getTimestamp()}] No matching pattern found, using AI service...`);
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Fix the following ${language} code that contains errors:
      
      \`\`\`${language}
      ${code}
      \`\`\`
      
      Errors:
      ${errors.map(err => `- ${err}`).join('\n')}
      
      Instructions:
      - Fix all the errors mentioned
      - Make minimal changes to the code to fix the issues
      - Keep the same structure and logic where possible
      - Include inline comments explaining the fixes
      
      Respond with a JSON object in the following format:
      {
        "code": "The fixed code here",
        "explanation": "An explanation of what was wrong and how it was fixed"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.5,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    // Store the successful fix pattern for future use
    if (result.code && firstErrorMessage) {
      console.log(`[${getTimestamp()}] Storing successful fix pattern`);
      
      // Determine error type from error message
      let errorType = "syntax";
      if (firstErrorMessage.includes("cannot find symbol")) errorType = "symbol_not_found";
      else if (firstErrorMessage.includes("incompatible types")) errorType = "type_mismatch";
      else if (firstErrorMessage.includes("is not accessible")) errorType = "access_error";
      
      // Determine mod loader if we couldn't earlier
      let modLoader = "unknown";
      if (code.includes("net.fabricmc")) modLoader = "Fabric";
      else if (code.includes("net.minecraftforge")) modLoader = "Forge";
      else if (code.includes("org.bukkit")) modLoader = "Bukkit";
      
      // Simplified fix strategy - in a real implementation, extract a proper pattern
      const fixStrategy = "Replaced error-causing code with corrected version";
      
      // Store the pattern
      await storeErrorFixPattern(
        firstErrorMessage,
        errorType,
        fixStrategy,
        modLoader
      );
    }
    
    return {
      code: result.code || code,
      explanation: result.explanation || "Code fixed successfully",
    };
  } catch (error) {
    console.error("Error fixing code:", error);
    throw new Error(`Code fixing failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Enhance code using OpenAI
 */
export async function enhanceCode(
  code: string,
  instructions: string,
  language: string = "java"
): Promise<CodeGenerationResponse> {
  try {
    console.log(`[${getTimestamp()}] Enhancing code with instructions: ${instructions.substring(0, 100)}...`);
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Enhance the following ${language} code according to these instructions:
      
      Instructions: ${instructions}
      
      Current code:
      \`\`\`${language}
      ${code}
      \`\`\`
      
      Respond with a JSON object in the following format:
      {
        "code": "The enhanced code here",
        "explanation": "An explanation of the enhancements made and why they improve the code"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    return {
      code: result.code || code,
      explanation: result.explanation || "Code enhanced successfully",
    };
  } catch (error) {
    console.error("Error enhancing code:", error);
    throw new Error(`Code enhancement failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Summarize code using OpenAI
 */
export async function summarizeCode(
  code: string,
  language: string = "java"
): Promise<CompletionResponse> {
  try {
    console.log(`[${getTimestamp()}] Summarizing code of length: ${code.length}`);
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Summarize the following ${language} code:
      
      \`\`\`${language}
      ${code}
      \`\`\`
      
      Provide a concise summary that explains:
      1. What the code does
      2. Key components and their purposes
      3. Any patterns or techniques used
      
      Respond with a JSON object in the following format:
      {
        "text": "The summary of the code"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.5,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    return {
      text: result.text || "No summary available",
    };
  } catch (error) {
    console.error("Error summarizing code:", error);
    throw new Error(`Code summarization failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Explain an error message using OpenAI
 */
export async function explainError(
  errorMessage: string,
  code?: string,
  language: string = "java"
): Promise<CompletionResponse> {
  try {
    console.log(`[${getTimestamp()}] Explaining error: ${errorMessage.substring(0, 100)}...`);
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Explain the following error message in ${language}:
      
      Error: ${errorMessage}
      
      ${code ? `Code context:\n\`\`\`${language}\n${code}\n\`\`\`\n` : ""}
      
      Provide a clear explanation that includes:
      1. What the error means
      2. Common causes of this error
      3. How to fix it
      
      Respond with a JSON object in the following format:
      {
        "text": "The explanation of the error"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.5,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    return {
      text: result.text || "No explanation available",
    };
  } catch (error) {
    console.error("Error explaining error:", error);
    throw new Error(`Error explanation failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Generate documentation for code using OpenAI
 */
export async function generateDocumentation(
  code: string,
  language: string = "java",
  style: "javadoc" | "markdown" | "inline" = "javadoc"
): Promise<CompletionResponse> {
  try {
    console.log(`[${getTimestamp()}] Generating ${style} documentation for ${language} code`);
    
    // The newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
    const promptText = `
      Generate ${style} documentation for the following ${language} code:
      
      \`\`\`${language}
      ${code}
      \`\`\`
      
      Documentation style: ${style}
      
      Instructions:
      - Create comprehensive documentation in ${style} format
      - Document classes, methods, parameters, and return values
      - Include examples where appropriate
      - Explain the purpose and functionality of the code
      
      Respond with a JSON object in the following format:
      {
        "text": "The generated documentation"
      }
    `;
    
    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: promptText }],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });
    
    const result = JSON.parse(response.choices[0].message.content || "{}");
    
    return {
      text: result.text || "No documentation available",
    };
  } catch (error) {
    console.error("Error generating documentation:", error);
    throw new Error(`Documentation generation failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}
