import OpenAI from "openai";
import { z } from "zod";

// Initialize OpenAI
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

// Schema for idea generation request
export const ideaGenerationRequestSchema = z.object({
  theme: z.string().optional(),
  complexity: z.enum(["simple", "medium", "complex"]).default("medium"),
  preferredModLoader: z.enum(["Any", "Forge", "Fabric", "Bukkit", "Spigot", "Paper"]).default("Any"),
  gameVersion: z.string().optional(),
  existingIdeas: z.array(z.string()).optional(),
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
Generate ${request.complexity === "simple" ? "3" : request.complexity === "medium" ? "4" : "5"} unique, detailed, and innovative Minecraft mod ideas.`;

    if (request.theme) {
      systemPrompt += ` The ideas should relate to the theme: "${request.theme}".`;
    }

    if (request.preferredModLoader !== "Any") {
      systemPrompt += ` The ideas should be suitable for implementation using the ${request.preferredModLoader} mod loader.`;
    }

    if (request.gameVersion) {
      systemPrompt += ` The ideas should be compatible with Minecraft version ${request.gameVersion}.`;
    }

    if (request.existingIdeas && request.existingIdeas.length > 0) {
      systemPrompt += ` Avoid similar concepts to these existing ideas: ${request.existingIdeas.join(", ")}.`;
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
export async function expandModIdea(ideaTitle: string, ideaDescription: string): Promise<{
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
  console.log(`${getTimestamp()} Expanding mod idea: ${ideaTitle}`);
  
  try {
    const systemPrompt = `You are an expert Minecraft mod developer with extensive knowledge of mod implementation, Java coding for Minecraft, and game design principles.
    
A user has requested more detailed information about their mod idea titled "${ideaTitle}" with the following description: "${ideaDescription}"

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
        { role: "user", content: `Please expand the mod idea "${ideaTitle}" with the description "${ideaDescription}" into a detailed mod concept.` }
      ],
      response_format: { type: "json_object" },
      temperature: 0.7,
    });

    // Parse and return the response
    const responseContent = completion.choices[0].message.content;
    console.log(`${getTimestamp()} Successfully expanded mod idea: ${ideaTitle}`);
    
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