import { useState } from "react";
import { useLocation } from "wouter";
import { IdeaGeneratorForm } from "@/components/idea-generator-form";
import { IdeaCard } from "@/components/idea-card";
import { insertModSchema, type Mod } from "@shared/schema";
import { apiRequest } from "@/lib/queryClient";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Separator } from "@/components/ui/separator";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Lightbulb, RefreshCw, Sparkles } from "lucide-react";

interface ModIdea {
  title: string;
  description: string;
  features: string[];
  complexity: string;
  estimatedDevTime: string;
  suggestedModLoader: string;
  tags: string[];
  compatibilityNotes?: string;
}

export default function IdeaGeneratorPage() {
  const [generatedIdeas, setGeneratedIdeas] = useState<ModIdea[]>([]);
  const [inspirations, setInspirations] = useState<string[]>([]);
  const [selectedIdea, setSelectedIdea] = useState<ModIdea | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [, setLocation] = useLocation();
  const { toast } = useToast();

  const handleGenerationSuccess = (response: { ideas: ModIdea[], inspirations: string[] }) => {
    setGeneratedIdeas(response.ideas);
    setInspirations(response.inspirations || []);
    setSelectedIdea(null);
  };

  const selectIdea = (idea: ModIdea) => {
    setSelectedIdea(idea);
  };

  const createModFromIdea = async () => {
    if (!selectedIdea) return;
    
    setIsCreating(true);
    try {
      // Extract features into a string
      const featuresStr = selectedIdea.features.join("\n• ");
      
      // Create a mod from the selected idea
      const modData = {
        name: selectedIdea.title,
        description: selectedIdea.description,
        idea: `${selectedIdea.description}\n\nFeatures:\n• ${featuresStr}`,
        modLoader: selectedIdea.suggestedModLoader === "Any" ? "Forge" : selectedIdea.suggestedModLoader,
        minecraftVersion: "1.20.4", // Default to latest
        autoFixLevel: "Balanced",
        compileFrequency: "Every 5 Minutes",
      };
      
      // Validate the mod data
      const validationResult = insertModSchema.safeParse(modData);
      if (!validationResult.success) {
        throw new Error("Invalid mod data: " + JSON.stringify(validationResult.error.errors));
      }
      
      // Create the mod
      const response = await apiRequest<Mod>("/api/mods", {
        method: "POST", 
        body: JSON.stringify(modData),
      });
      
      toast({
        title: "Mod Created",
        description: `Successfully created mod: ${selectedIdea.title}`,
      });
      
      // Navigate to the home page or the mod details page
      setLocation(`/`);
      
    } catch (error) {
      console.error("Error creating mod:", error);
      toast({
        variant: "destructive",
        title: "Error",
        description: "Failed to create mod from idea. Please try again.",
      });
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gradient-to-r from-blue-600 to-purple-600">Mod Idea Generator</h1>
          <p className="text-lg text-muted-foreground">
            Let AI help you brainstorm your next Minecraft mod
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <IdeaGeneratorForm onSuccess={handleGenerationSuccess} />
          
          {selectedIdea && (
            <div className="mt-6 space-y-4">
              <Alert>
                <Sparkles className="h-4 w-4" />
                <AlertTitle>Idea Selected</AlertTitle>
                <AlertDescription>
                  You selected: <strong>{selectedIdea.title}</strong>
                </AlertDescription>
              </Alert>
              
              <Button 
                className="w-full"
                disabled={isCreating}
                onClick={createModFromIdea}
              >
                {isCreating ? (
                  <>
                    <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                    Creating Mod...
                  </>
                ) : (
                  <>
                    Create Mod From This Idea
                  </>
                )}
              </Button>
            </div>
          )}
          
          {inspirations.length > 0 && (
            <div className="mt-6">
              <h3 className="text-lg font-semibold mb-2 flex items-center">
                <Lightbulb className="mr-2 h-5 w-5 text-yellow-500" />
                Additional Inspirations
              </h3>
              <ul className="space-y-2">
                {inspirations.map((inspiration, index) => (
                  <li key={index} className="text-sm bg-slate-50 dark:bg-slate-800 p-3 rounded-md">
                    {inspiration}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
        
        <div className="lg:col-span-2">
          {generatedIdeas.length > 0 ? (
            <>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold">Generated Ideas</h2>
                <span className="text-sm text-muted-foreground">
                  {generatedIdeas.length} ideas generated
                </span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {generatedIdeas.map((idea, index) => (
                  <IdeaCard 
                    key={index} 
                    idea={idea}
                    onSelected={selectIdea}
                  />
                ))}
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center justify-center h-full min-h-[400px] bg-slate-50 dark:bg-slate-800 rounded-lg">
              <Sparkles className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-xl font-medium mb-2">No Ideas Generated Yet</h3>
              <p className="text-muted-foreground text-center max-w-md">
                Fill out the form to the left and click "Generate Mod Ideas" to have AI suggest some creative mod concepts for you.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}