import OpenAI from "openai";

// the newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
const OPENAI_MODEL = "gpt-4o";

// Initialize OpenAI client - client side implementation
// Note: We're just defining the interface on the client side
// Actual API calls will be handled through server endpoints
export const openai = {
  // This is just a placeholder for type compatibility
};

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
    // Create a structured prompt for the AI
    const featureTypes = [];
    if (params.includeItems) featureTypes.push("Items");
    if (params.includeBlocks) featureTypes.push("Blocks");
    if (params.includeEntities) featureTypes.push("Entities");
    if (params.includeWorldGen) featureTypes.push("World Generation");
    if (params.includeStructures) featureTypes.push("Structures");
    if (params.includeGameplayMechanics) featureTypes.push("Gameplay Mechanics");

    const modLoaderSpecific = params.modLoader !== "any" 
      ? `The mod should be developed for ${params.modLoader} specifically.` 
      : "The mod can be developed for any mod loader (Forge, Fabric, or Quilt).";

    const mcVersionSpecific = params.mcVersion 
      ? `The mod should target Minecraft version ${params.mcVersion}.` 
      : "The mod can target recent Minecraft versions.";

    const prompt = `Generate 5 unique and creative Minecraft mod ideas with the following criteria:
Theme: ${params.theme || "Any theme"}
Complexity: ${params.complexity}
Category: ${params.category || "Any category"}
${modLoaderSpecific}
${mcVersionSpecific}
Feature Types to Include: ${featureTypes.join(", ")}
Additional Notes: ${params.additionalNotes || "None"}

For each mod idea, include:
1. A catchy title
2. A brief description
3. A list of 4-6 key features
4. Complexity level (Simple, Medium, Complex)
5. Estimated development time
6. Suggested mod loader
7. 3-5 tags that describe the mod
8. Optional compatibility notes

Also include a list of 5-7 existing Minecraft mods that could serve as inspiration for these ideas.

Format the response as a JSON object with the following structure:
{
  "ideas": [
    {
      "title": "Mod Title",
      "description": "Description",
      "features": ["Feature 1", "Feature 2", ...],
      "complexity": "Medium",
      "estimatedDevTime": "2 weeks",
      "suggestedModLoader": "Forge",
      "tags": ["magic", "combat", ...],
      "compatibilityNotes": "Works well with XYZ mod"
    },
    ...
  ],
  "inspirations": [
    "Mod 1 - Brief description",
    ...
  ]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are a Minecraft modding expert who specializes in generating creative and feasible mod ideas. Your ideas should be original but implementable using current modding techniques."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to generate mod ideas: Empty response");
    }

    return JSON.parse(content);
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
    const prompt = `Expand on the following Minecraft mod idea with detailed implementation guidance:
Title: ${params.title}
Description: ${params.description}

Provide a comprehensive expansion that includes:

1. A detailed description explaining the mod's purpose, gameplay elements, and unique aspects
2. A list of specific features (6-8), each with:
   - Feature name
   - Detailed description
   - Implementation guidance for developers
3. Technical details:
   - Required dependencies or libraries
   - Recommended mod loader
   - Compatible Minecraft versions
   - Difficulty assessment
   - Estimated development time
4. Compatibility analysis:
   - Mods it would work well with
   - Potential conflict points with other mods
   - General compatibility notes
5. A suggested file structure for the mod
6. Next steps for beginning development

Format your response as a JSON object with the following structure:
{
  "title": "The mod title",
  "description": "Brief description",
  "detailedDescription": "Extended multi-paragraph explanation",
  "features": [
    {
      "name": "Feature name",
      "description": "What the feature does",
      "implementation": "Technical guidance for implementation"
    },
    ...
  ],
  "technicalDetails": {
    "requiredDependencies": ["Dependency 1", ...],
    "suggestedModLoader": "Forge/Fabric/Quilt",
    "recommendedMcVersions": ["1.19.4", ...],
    "difficulty": "Beginner/Intermediate/Advanced",
    "estimatedDevTime": "Time estimate"
  },
  "compatibility": {
    "compatibleWith": ["Mod 1", ...],
    "potentialConflicts": ["Mod 2", ...],
    "notes": "General compatibility notes"
  },
  "fileStructure": [
    "src/main/java/com/modname/ModMain.java",
    ...
  ],
  "nextSteps": [
    "Step 1",
    ...
  ]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are a senior Minecraft mod developer with extensive knowledge of mod development practices, Java programming, and the Minecraft modding ecosystem. You provide detailed, technically accurate expansion of mod concepts."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to expand mod idea: Empty response");
    }

    return JSON.parse(content);
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
    const prompt = `Generate the initial code for a Minecraft mod with the following details:
Mod Name: ${params.modName}
Description: ${params.modDescription}
Mod Loader: ${params.modLoader}
Minecraft Version: ${params.mcVersion}
Idea: ${params.idea}

Provide a complete implementation with the necessary files for a functioning mod. For each file, include:
1. The file path (relative to the project root)
2. The complete file content with proper comments
3. An explanation of what the file does

Format your response as a JSON object with:
{
  "files": [
    {
      "path": "src/main/java/com/example/ExampleMod.java",
      "content": "// Full code content here",
      "explanation": "This is the main mod class that initializes..."
    },
    ...
  ],
  "setupInstructions": "Step-by-step instructions to set up the mod",
  "nextSteps": [
    "Add more features like...",
    ...
  ]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are an expert Minecraft mod developer specializing in Java programming and mod creation. You generate complete, compilable code for Minecraft mods that follows best practices for the specified mod loader."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to generate mod code: Empty response");
    }

    return JSON.parse(content);
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
    const filesContent = params.files.map(file => 
      `File: ${file.path}\n\`\`\`java\n${file.content}\n\`\`\``
    ).join('\n\n');

    const prompt = `Fix the following compilation errors in a Minecraft mod:

Mod Loader: ${params.modLoader}

Errors:
${params.errors}

Current Files:
${filesContent}

Analyze the errors and provide fixed versions of the affected files. For each file that needs changes:
1. Identify the root cause of the error
2. Provide the complete fixed file content
3. Explain what was changed and why

Format your response as a JSON object:
{
  "analysis": "Overall analysis of the errors and their causes",
  "fixes": [
    {
      "path": "src/main/java/com/example/ExampleMod.java",
      "fixedContent": "// Complete fixed file content",
      "explanation": "Changed X to Y because..."
    },
    ...
  ]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are an expert Minecraft mod developer specializing in fixing compilation errors. You provide precise, targeted fixes that address the root causes of problems while maintaining the original functionality of the code."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to fix compilation errors: Empty response");
    }

    return JSON.parse(content);
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
    const style = params.style || "standard";
    
    const prompt = `Generate comprehensive documentation for the following code:

\`\`\`${params.language}
${params.code}
\`\`\`

Documentation style: ${style}

For this code, please provide:
1. Overall description of what the code does
2. Detailed documentation for each class, method, and field
3. Parameter descriptions
4. Return value descriptions
5. Examples of usage where appropriate

Format your response as a JSON object:
{
  "overview": "High-level description of the code",
  "documentation": {
    "main": "Documentation for the entire file/module",
    "classes": [
      {
        "name": "ClassName",
        "description": "Class description",
        "methods": [
          {
            "name": "methodName",
            "description": "Method description",
            "parameters": [{"name": "param1", "description": "Parameter description"}],
            "returnValue": "Description of return value",
            "examples": ["Example usage code"]
          }
        ],
        "fields": [
          {
            "name": "fieldName",
            "description": "Field description"
          }
        ]
      }
    ]
  },
  "formattedDocumentation": "The documentation in the appropriate format for the language (JavaDoc, JSDoc, etc.)"
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are a technical documentation expert specializing in creating clear, comprehensive documentation for code. You understand programming concepts deeply and can explain them in a way that's helpful for developers."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to generate documentation: Empty response");
    }

    return JSON.parse(content);
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
    const filesContent = params.files.map(file => 
      `File: ${file.path}\n\`\`\`java\n${file.content}\n\`\`\``
    ).join('\n\n');

    const prompt = `Add the following feature to an existing Minecraft mod:

Mod Loader: ${params.modLoader}
Feature Description: ${params.featureDescription}

Current Files:
${filesContent}

Analyze the current mod structure and implement the requested feature. Provide:
1. Any new files that need to be created
2. Changes to existing files
3. Explanation of your implementation approach

Format your response as a JSON object:
{
  "approach": "Explanation of your implementation strategy",
  "newFiles": [
    {
      "path": "src/main/java/com/example/NewFile.java",
      "content": "// Complete file content",
      "explanation": "This file is needed to implement..."
    }
  ],
  "modifiedFiles": [
    {
      "path": "src/main/java/com/example/ExistingFile.java",
      "content": "// Complete modified file content",
      "explanation": "Changed to add the new feature by..."
    }
  ]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are an expert Minecraft mod developer specializing in feature implementation. You can analyze existing code and extend it with new functionality while maintaining the original design patterns and code style."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to add features: Empty response");
    }

    return JSON.parse(content);
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
    const complexityLevel = params.complexity || "medium";
    const context = params.context || "No additional context provided";

    const prompt = `Generate code based on the following request:

Request: ${params.prompt}
Language: ${params.language}
Complexity Level: ${complexityLevel}
Additional Context: ${context}

Provide:
1. Complete, functional code that implements the request
2. Explanation of how the code works
3. Any assumptions made during implementation
4. Suggestions for further improvements or alternatives

Format your response as a JSON object:
{
  "code": "// Complete code implementation",
  "explanation": "Detailed explanation of how the code works",
  "assumptions": ["Assumption 1", "Assumption 2", ...],
  "improvements": ["Potential improvement 1", ...]
}`;

    const response = await openai.chat.completions.create({
      model: OPENAI_MODEL,
      messages: [
        {
          role: "system",
          content: "You are an expert programmer who can generate high-quality, functional code in any language. You provide clean, efficient solutions with thorough explanations."
        },
        {
          role: "user",
          content: prompt
        }
      ],
      response_format: { type: "json_object" }
    });

    const content = response.choices[0].message.content;
    if (!content) {
      throw new Error("Failed to generate code: Empty response");
    }

    return JSON.parse(content);
  } catch (error) {
    console.error("Error in generateGenericCode:", error);
    throw error;
  }
}