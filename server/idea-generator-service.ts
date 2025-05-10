import OpenAI from "openai";
import { z } from "zod";
import { storeIdeaGenerationPattern, tryGenerateIdeasFromPatterns, recordIdeaPatternResult } from "./idea-pattern-learning";

// Initialize OpenAI
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

// Schema for idea generation request
export const ideaGenerationRequestSchema = z.object({
  theme: z.string().optional().default(""),
  complexity: z.string().default("Moderate"),
  modLoader: z.string().default("forge"),
  minecraftVersion: z.string().optional().default("1.20.4"),
  keywords: z.array(z.string()).optional().default([]),
  count: z.number().optional().default(3),
});

export type IdeaGenerationRequest = z.infer<typeof ideaGenerationRequestSchema>;

// Schema for idea generation response
export interface ModIdea {
  title: string;
  description: string;
  features: string[];
  complexity: string;
  estimatedDevTime: string;
  suggestedModLoader: string;
  tags: string[];
  compatibilityNotes?: string;
}

export interface IdeaGenerationResponse {
  ideas: ModIdea[];
  inspirations: string[];
}

// Get current timestamp for logging
function getTimestamp(): string {
  return new Date().toISOString();
}

/**
 * Generate Minecraft mod ideas using OpenAI API
 * @param request Parameters to guide the idea generation
 * @returns A list of mod ideas and inspirations
 */
export async function generateModIdeas(request: IdeaGenerationRequest): Promise<IdeaGenerationResponse> {
  console.log(`${getTimestamp()} Generating mod ideas with parameters:`, request);
  
  try {
    // Build the prompt based on the request parameters
    let systemPrompt = `You are an expert Minecraft mod developer who specializes in creative mod ideas. 
Generate ${request.count || 3} unique, detailed, and innovative Minecraft mod ideas with ${request.complexity || "Moderate"} complexity.`;

    if (request.theme) {
      systemPrompt += ` The ideas should relate to the theme: "${request.theme}".`;
    }

    if (request.modLoader !== "forge") {
      systemPrompt += ` The ideas should be suitable for implementation using the ${request.modLoader} mod loader.`;
    }

    if (request.minecraftVersion) {
      systemPrompt += ` The ideas should be compatible with Minecraft version ${request.minecraftVersion}.`;
    }

    if (request.keywords && request.keywords.length > 0) {
      systemPrompt += ` The ideas should include concepts related to these keywords: ${request.keywords.join(", ")}.`;
    }

    systemPrompt += `
For each mod idea, provide:
1. A catchy title
2. A concise description
3. 3-5 key features
4. Complexity level (Simple, Medium, or Complex)
5. Estimated development time
6. Suggested mod loader
7. Relevant tags (gameplay, decoration, technology, etc.)
8. Compatibility notes (if applicable)

Additionally, provide 3 sources of inspiration that might help expand on these ideas.

Format your response as a JSON object with the following structure:
{
  "ideas": [
    {
      "title": "string",
      "description": "string",
      "features": ["string"],
      "complexity": "string",
      "estimatedDevTime": "string",
      "suggestedModLoader": "string",
      "tags": ["string"],
      "compatibilityNotes": "string" (optional)
    }
  ],
  "inspirations": ["string"]
}`;

    // First check if we have a similar pattern already saved
    const patternResult = await tryGenerateIdeasFromPatterns(
      systemPrompt, 
      request.theme || null,
      request.complexity,
      request.modLoader,
      request.minecraftVersion || null
    );
    
    // If we found a good pattern match, use it
    if (patternResult.ideas && patternResult.patternId) {
      console.log(`${getTimestamp()} Using existing pattern #${patternResult.patternId} with confidence ${patternResult.confidence.toFixed(2)}`);
      
      // Record the pattern usage
      recordIdeaPatternResult(patternResult.patternId, true);
      
      return patternResult.ideas;
    }
    
    console.log(`${getTimestamp()} No suitable pattern found, generating with OpenAI`);

    // Call OpenAI API
    const completion = await openai.chat.completions.create({
      model: "gpt-4o", // the newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: "Generate creative Minecraft mod ideas based on the specified parameters." }
      ],
      response_format: { type: "json_object" },
      temperature: 0.8,
    });

    // Parse and return the response
    const responseContent = completion.choices[0].message.content;
    console.log(`${getTimestamp()} Successfully generated mod ideas`);
    
    if (!responseContent) {
      throw new Error("Received empty response from OpenAI");
    }
    
    try {
      const parsedResponse = JSON.parse(responseContent) as IdeaGenerationResponse;
      
      // Store this successful generation in our pattern learning database
      await storeIdeaGenerationPattern(
        systemPrompt,
        request.theme || null,
        request.complexity,
        request.modLoader,
        request.minecraftVersion || null,
        parsedResponse
      );
      
      return parsedResponse;
    } catch (parseError) {
      console.error("Failed to parse OpenAI response as JSON:", parseError);
      throw new Error("Failed to parse AI response");
    }
  } catch (error) {
    console.error(`${getTimestamp()} Error generating mod ideas:`, error);
    throw error;
  }
}

/**
 * Expand a specific mod idea with more details
 * @param ideaTitle The title of the idea to expand
 * @param ideaDescription Brief description of the idea
 * @returns Detailed expansion of the mod idea
 */
export async function expandModIdea(title: string, description: string): Promise<{
  expandedIdea: {
    title: string;
    description: string;
    detailedFeatures: Array<{
      name: string;
      description: string;
      implementation: string;
    }>;
    technicalConsiderations: string[];
    developmentRoadmap: string[];
    potentialChallenges: string[];
    suggestedImplementationApproach: string;
  }
}> {
  console.log(`${getTimestamp()} Expanding mod idea: ${title}`);
  
  try {
    const systemPrompt = `You are an expert Minecraft mod developer with extensive knowledge of mod implementation, Java coding for Minecraft, and game design principles.
    
A user has requested more detailed information about their mod idea titled "${title}" with the following description: "${description}"

Expand this idea into a detailed mod concept. Include:

1. A refined title and description
2. 5-7 detailed features, including:
   - Feature name
   - Feature description
   - Implementation notes (code approach, game mechanics)
3. Technical considerations for development
4. A development roadmap with milestones
5. Potential challenges and solutions
6. Suggested implementation approach

Format your response as a JSON object with the following structure:
{
  "expandedIdea": {
    "title": "string",
    "description": "string",
    "detailedFeatures": [
      {
        "name": "string",
        "description": "string",
        "implementation": "string"
      }
    ],
    "technicalConsiderations": ["string"],
    "developmentRoadmap": ["string"],
    "potentialChallenges": ["string"],
    "suggestedImplementationApproach": "string"
  }
}`;

    // Call OpenAI API
    const completion = await openai.chat.completions.create({
      model: "gpt-4o", // the newest OpenAI model is "gpt-4o" which was released May 13, 2024. do not change this unless explicitly requested by the user
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: `Please expand the mod idea "${title}" with the description "${description}" into a detailed mod concept.` }
      ],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });

    // Parse and return the response
    const responseContent = completion.choices[0].message.content;
    console.log(`${getTimestamp()} Successfully expanded mod idea: ${title}`);
    
    if (!responseContent) {
      throw new Error("Received empty response from OpenAI");
    }
    
    try {
      return JSON.parse(responseContent);
    } catch (parseError) {
      console.error("Failed to parse OpenAI response as JSON:", parseError);
      throw new Error("Failed to parse AI response");
    }
  } catch (error) {
    console.error(`${getTimestamp()} Error expanding mod idea:`, error);
    throw error;
  }
}