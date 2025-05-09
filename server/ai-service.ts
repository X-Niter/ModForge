import OpenAI from "openai";
import { ErrorData } from "@/types";

// Initialize OpenAI client
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY || "dummy_key_for_development" });

// Helper to format timestamps
function getTimestamp(): string {
  return new Date().toISOString().replace('T', ' ').substring(0, 19);
}

// Generate mod code using OpenAI
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
    logs += `[${getTimestamp()}] Sending request to AI service...\n`;
    
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
