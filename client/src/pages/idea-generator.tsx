import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { apiRequest } from "@/lib/queryClient";
import { ModLoaderSelect } from "@/components/modloader-select";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Slider } from "@/components/ui/slider";
import { Lightbulb, LoaderCircle } from "lucide-react";

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

export default function IdeaGenerator() {
  const { toast } = useToast();
  const [preferredLoader, setPreferredLoader] = useState("forge");
  const [complexity, setComplexity] = useState(50);
  const [theme, setTheme] = useState("");
  const [mcVersion, setMcVersion] = useState("1.20.4");
  const [keywords, setKeywords] = useState("");
  const [generatedIdeas, setGeneratedIdeas] = useState<ModIdea[]>([]);
  const [expandedIdea, setExpandedIdea] = useState<ModIdea | null>(null);
  const [expandedDetails, setExpandedDetails] = useState<Record<string, any> | null>(null);

  // Generate ideas mutation
  const generateMutation = useMutation({
    mutationFn: async () => {
      const data = {
        modLoader: preferredLoader,
        complexity: complexityLabel(complexity),
        theme: theme,
        minecraftVersion: mcVersion,
        keywords: keywords.split(',').map(k => k.trim()).filter(Boolean)
      };
      
      const response = await apiRequest("POST", "/api/idea-generator/generate", data);
      
      if (!response.ok) {
        throw new Error("Failed to generate ideas");
      }
      
      return response.json();
    },
    onSuccess: (data) => {
      setGeneratedIdeas(data.ideas || []);
      toast({
        title: "Ideas Generated",
        description: "Successfully generated mod ideas",
      });
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message || "Failed to generate ideas",
        variant: "destructive",
      });
    }
  });

  // Expand idea mutation
  const expandMutation = useMutation({
    mutationFn: async (idea: ModIdea) => {
      const data = {
        title: idea.title,
        description: idea.description
      };
      
      const response = await apiRequest("POST", "/api/idea-generator/expand", data);
      
      if (!response.ok) {
        throw new Error("Failed to expand idea");
      }
      
      return response.json();
    },
    onSuccess: (data) => {
      setExpandedDetails(data);
      toast({
        title: "Idea Expanded",
        description: "Successfully expanded mod idea with additional details",
      });
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: error.message || "Failed to expand idea",
        variant: "destructive",
      });
    }
  });

  // Convert complexity number to label
  function complexityLabel(value: number): string {
    if (value < 25) return "Simple";
    if (value < 50) return "Moderate";
    if (value < 75) return "Advanced";
    return "Complex";
  }

  // Handle idea expansion
  const handleExpandIdea = (idea: ModIdea) => {
    setExpandedIdea(idea);
    expandMutation.mutate(idea);
  };

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    generateMutation.mutate();
  };

  return (
    <div className="container mx-auto py-6">
      <h1 className="text-3xl font-bold mb-8">Minecraft Mod Idea Generator</h1>
      
      {/* Generator form */}
      <Card className="mb-8">
        <CardHeader>
          <CardTitle>Generate New Ideas</CardTitle>
          <CardDescription>
            Specify the parameters for your ideal Minecraft mod
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="theme">Theme or Concept</Label>
                <Input
                  id="theme"
                  placeholder="e.g., Magic, Technology, Adventure"
                  value={theme}
                  onChange={(e) => setTheme(e.target.value)}
                />
              </div>
              
              <ModLoaderSelect
                value={preferredLoader}
                onChange={setPreferredLoader}
              />
              
              <div className="space-y-2">
                <Label htmlFor="mcVersion">Minecraft Version</Label>
                <Input
                  id="mcVersion"
                  placeholder="e.g., 1.20.4"
                  value={mcVersion}
                  onChange={(e) => setMcVersion(e.target.value)}
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="keywords">Keywords (comma separated)</Label>
                <Input
                  id="keywords"
                  placeholder="e.g., automation, farming, dimensions"
                  value={keywords}
                  onChange={(e) => setKeywords(e.target.value)}
                />
              </div>
              
              <div className="space-y-2 md:col-span-2">
                <div className="flex justify-between mb-2">
                  <Label htmlFor="complexity">Complexity: {complexityLabel(complexity)}</Label>
                  <span className="text-muted-foreground text-sm">{complexity}%</span>
                </div>
                <Slider
                  id="complexity"
                  min={0}
                  max={100}
                  step={1}
                  value={[complexity]}
                  onValueChange={(value) => setComplexity(value[0])}
                />
              </div>
            </div>
            
            <Button
              type="submit"
              className="w-full"
              disabled={generateMutation.isPending}
            >
              {generateMutation.isPending ? (
                <>
                  <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />
                  Generating Ideas...
                </>
              ) : (
                <>
                  <Lightbulb className="mr-2 h-4 w-4" />
                  Generate Ideas
                </>
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
      
      {/* Generated ideas */}
      {generatedIdeas.length > 0 && (
        <div className="space-y-6">
          <h2 className="text-2xl font-bold">Generated Ideas</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {generatedIdeas.map((idea, index) => (
              <Card key={index} className="flex flex-col">
                <CardHeader>
                  <CardTitle>{idea.title}</CardTitle>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {idea.tags.map((tag, i) => (
                      <Badge key={i} variant="secondary">{tag}</Badge>
                    ))}
                  </div>
                </CardHeader>
                <CardContent className="flex-1">
                  <p className="text-muted-foreground mb-4">{idea.description}</p>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="font-medium">Complexity:</span>
                      <span>{idea.complexity}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="font-medium">Mod Loader:</span>
                      <span>{idea.suggestedModLoader}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="font-medium">Dev Time:</span>
                      <span>{idea.estimatedDevTime}</span>
                    </div>
                  </div>
                </CardContent>
                <CardFooter>
                  <Button 
                    className="w-full" 
                    onClick={() => handleExpandIdea(idea)}
                    disabled={expandMutation.isPending && expandedIdea?.title === idea.title}
                  >
                    {expandMutation.isPending && expandedIdea?.title === idea.title ? (
                      <>
                        <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />
                        Expanding...
                      </>
                    ) : (
                      "Expand Idea"
                    )}
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        </div>
      )}
      
      {/* Expanded idea details */}
      {expandedIdea && expandedDetails && (
        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Expanded Idea: {expandedIdea.title}</CardTitle>
            <CardDescription>Detailed breakdown of the mod concept</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div>
              <h3 className="text-lg font-bold mb-2">Core Concept</h3>
              <p>{expandedDetails.coreConcept}</p>
            </div>
            
            <div>
              <h3 className="text-lg font-bold mb-2">Detailed Features</h3>
              <ul className="list-disc pl-5 space-y-1">
                {expandedDetails.detailedFeatures?.map((feature: string, i: number) => (
                  <li key={i}>{feature}</li>
                ))}
              </ul>
            </div>
            
            <div>
              <h3 className="text-lg font-bold mb-2">Technical Considerations</h3>
              <p>{expandedDetails.technicalConsiderations}</p>
            </div>
            
            <div>
              <h3 className="text-lg font-bold mb-2">Development Roadmap</h3>
              <ol className="list-decimal pl-5 space-y-1">
                {expandedDetails.developmentSteps?.map((step: string, i: number) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </div>
            
            {expandedDetails.potentialChallenges && (
              <div>
                <h3 className="text-lg font-bold mb-2">Potential Challenges</h3>
                <p>{expandedDetails.potentialChallenges}</p>
              </div>
            )}
            
            {expandedDetails.inspirations && (
              <div>
                <h3 className="text-lg font-bold mb-2">Inspirations & References</h3>
                <p>{expandedDetails.inspirations}</p>
              </div>
            )}
          </CardContent>
          <CardFooter>
            <Button 
              className="w-full"
              onClick={() => {
                // Create new mod with this idea
                toast({
                  title: "Feature Coming Soon",
                  description: "Creating a new mod from this idea will be available soon!",
                });
              }}
            >
              Create Mod From This Idea
            </Button>
          </CardFooter>
        </Card>
      )}
    </div>
  );
}