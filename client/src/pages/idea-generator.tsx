import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useLocation, useRoute, navigate } from "wouter";
import { apiRequest } from "@/lib/queryClient";
import { useToast } from "@/hooks/use-toast";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { 
  LucideBrain, 
  LucideSparkles, 
  LucideLightbulb, 
  LucideChevronRight, 
  LucideArrowRight, 
  LucideLoader2, 
  LucideDownload 
} from "lucide-react";

const ideaGenerationFormSchema = z.object({
  theme: z.string().optional(),
  complexity: z.enum(["simple", "medium", "complex"]).default("medium"),
  category: z.string().optional(),
  modLoader: z.enum(["forge", "fabric", "quilt", "any"]).default("any"),
  mcVersion: z.string().optional(),
  includeItems: z.boolean().default(true),
  includeBlocks: z.boolean().default(true),
  includeEntities: z.boolean().default(false),
  includeWorldGen: z.boolean().default(false),
  includeStructures: z.boolean().default(false),
  includeGameplayMechanics: z.boolean().default(true),
  additionalNotes: z.string().optional(),
});

type IdeaGenerationFormValues = z.infer<typeof ideaGenerationFormSchema>;

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

interface GenerateIdeasResponse {
  ideas: ModIdea[];
  inspirations: string[];
}

interface ExpandedIdeaResponse {
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
}

export default function IdeaGeneratorPage() {
  const { toast } = useToast();
  const [selectedIdea, setSelectedIdea] = useState<ModIdea | null>(null);
  const [expandedIdea, setExpandedIdea] = useState<ExpandedIdeaResponse | null>(null);
  const [activeTab, setActiveTab] = useState("form");

  const form = useForm<IdeaGenerationFormValues>({
    resolver: zodResolver(ideaGenerationFormSchema),
    defaultValues: {
      complexity: "medium",
      modLoader: "any",
      includeItems: true,
      includeBlocks: true,
      includeEntities: false,
      includeWorldGen: false,
      includeStructures: false,
      includeGameplayMechanics: true,
    },
  });

  const generateIdeasMutation = useMutation({
    mutationFn: async (data: IdeaGenerationFormValues) => {
      return apiRequest("/api/ai/generate-ideas", {
        method: "POST",
        data
      }) as Promise<GenerateIdeasResponse>;
    },
    onSuccess: (data) => {
      setSelectedIdea(null);
      setExpandedIdea(null);
      setActiveTab("ideas");
      toast({
        title: "Ideas generated successfully",
        description: `Generated ${data.ideas.length} mod ideas based on your criteria.`
      });
    },
    onError: (error) => {
      toast({
        title: "Failed to generate ideas",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
        variant: "destructive",
      });
    },
  });

  const expandIdeaMutation = useMutation({
    mutationFn: async ({ title, description }: { title: string; description: string }) => {
      return apiRequest("/api/ai/expand-idea", {
        method: "POST",
        data: { title, description }
      }) as Promise<ExpandedIdeaResponse>;
    },
    onSuccess: (data) => {
      setExpandedIdea(data);
      setActiveTab("expanded");
      toast({
        title: "Idea expanded successfully",
        description: "Your mod idea has been expanded with detailed features and implementation notes.",
      });
    },
    onError: (error) => {
      toast({
        title: "Failed to expand idea",
        description: error instanceof Error ? error.message : "An unexpected error occurred",
        variant: "destructive",
      });
    },
  });

  const onSubmit = (data: IdeaGenerationFormValues) => {
    generateIdeasMutation.mutate(data);
  };

  const handleSelectIdea = (idea: ModIdea) => {
    setSelectedIdea(idea);
  };

  const handleExpandIdea = () => {
    if (selectedIdea) {
      expandIdeaMutation.mutate({
        title: selectedIdea.title,
        description: selectedIdea.description,
      });
    }
  };

  const handleCreateMod = () => {
    if (expandedIdea) {
      toast({
        title: "Redirecting to mod creation",
        description: "Taking you to the code generator to create your mod.",
      });
      
      // Navigate to the code generator with the expanded idea data
      navigate("/code-generator", { 
        state: { 
          expandedIdea: expandedIdea,
          ideaTitle: selectedIdea?.title || "", 
          ideaDescription: selectedIdea?.description || "" 
        } 
      });
    }
  };

  return (
    <div className="container mx-auto py-10">
      <div className="flex items-center mb-6">
        <LucideBrain className="h-8 w-8 mr-3 text-blue-500" />
        <div>
          <h1 className="text-3xl font-bold">Mod Idea Generator</h1>
          <p className="text-muted-foreground">Generate Minecraft mod ideas with AI assistance</p>
        </div>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList className="grid grid-cols-3 w-full">
          <TabsTrigger value="form" disabled={generateIdeasMutation.isPending}>
            <LucideSparkles className="h-4 w-4 mr-2" />
            Generate Ideas
          </TabsTrigger>
          <TabsTrigger 
            value="ideas" 
            disabled={!generateIdeasMutation.data || generateIdeasMutation.isPending}
          >
            <LucideLightbulb className="h-4 w-4 mr-2" />
            Browse Ideas
          </TabsTrigger>
          <TabsTrigger 
            value="expanded" 
            disabled={!expandedIdea || expandIdeaMutation.isPending}
          >
            <LucideChevronRight className="h-4 w-4 mr-2" />
            Expanded Idea
          </TabsTrigger>
        </TabsList>

        <TabsContent value="form" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="md:col-span-2">
              <Card>
                <CardHeader>
                  <CardTitle>Generate Mod Ideas</CardTitle>
                  <CardDescription>
                    Fill out the form below to generate custom Minecraft mod ideas
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                      <FormField
                        control={form.control}
                        name="theme"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Theme</FormLabel>
                            <FormControl>
                              <Input
                                placeholder="e.g., Magic, Technology, Nature, Adventure"
                                {...field}
                              />
                            </FormControl>
                            <FormDescription>
                              Enter a theme or leave blank for random ideas
                            </FormDescription>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <FormField
                          control={form.control}
                          name="complexity"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Complexity</FormLabel>
                              <Select
                                onValueChange={field.onChange}
                                defaultValue={field.value}
                              >
                                <FormControl>
                                  <SelectTrigger>
                                    <SelectValue placeholder="Select complexity" />
                                  </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                  <SelectItem value="simple">Simple</SelectItem>
                                  <SelectItem value="medium">Medium</SelectItem>
                                  <SelectItem value="complex">Complex</SelectItem>
                                </SelectContent>
                              </Select>
                              <FormDescription>
                                How complex should the mod be?
                              </FormDescription>
                              <FormMessage />
                            </FormItem>
                          )}
                        />

                        <FormField
                          control={form.control}
                          name="modLoader"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Mod Loader</FormLabel>
                              <Select
                                onValueChange={field.onChange}
                                defaultValue={field.value}
                              >
                                <FormControl>
                                  <SelectTrigger>
                                    <SelectValue placeholder="Select mod loader" />
                                  </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                  <SelectItem value="any">Any</SelectItem>
                                  <SelectItem value="forge">Forge</SelectItem>
                                  <SelectItem value="fabric">Fabric</SelectItem>
                                  <SelectItem value="quilt">Quilt</SelectItem>
                                </SelectContent>
                              </Select>
                              <FormDescription>
                                Preferred mod loader
                              </FormDescription>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>

                      <FormField
                        control={form.control}
                        name="mcVersion"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Minecraft Version</FormLabel>
                            <FormControl>
                              <Input
                                placeholder="e.g., 1.19.4, 1.18.2"
                                {...field}
                              />
                            </FormControl>
                            <FormDescription>
                              Target Minecraft version (optional)
                            </FormDescription>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <div>
                        <h3 className="text-sm font-medium mb-2">Include Features</h3>
                        <div className="grid grid-cols-2 gap-2">
                          <FormField
                            control={form.control}
                            name="includeItems"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">Items</FormLabel>
                              </FormItem>
                            )}
                          />
                          <FormField
                            control={form.control}
                            name="includeBlocks"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">Blocks</FormLabel>
                              </FormItem>
                            )}
                          />
                          <FormField
                            control={form.control}
                            name="includeEntities"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">Entities</FormLabel>
                              </FormItem>
                            )}
                          />
                          <FormField
                            control={form.control}
                            name="includeWorldGen"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">World Generation</FormLabel>
                              </FormItem>
                            )}
                          />
                          <FormField
                            control={form.control}
                            name="includeStructures"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">Structures</FormLabel>
                              </FormItem>
                            )}
                          />
                          <FormField
                            control={form.control}
                            name="includeGameplayMechanics"
                            render={({ field }) => (
                              <FormItem className="flex items-center space-x-2 space-y-0">
                                <FormControl>
                                  <Checkbox
                                    checked={field.value}
                                    onCheckedChange={field.onChange}
                                  />
                                </FormControl>
                                <FormLabel className="text-sm font-normal">Gameplay Mechanics</FormLabel>
                              </FormItem>
                            )}
                          />
                        </div>
                      </div>

                      <FormField
                        control={form.control}
                        name="additionalNotes"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Additional Notes</FormLabel>
                            <FormControl>
                              <Textarea
                                placeholder="Any additional requirements or ideas..."
                                className="min-h-[100px]"
                                {...field}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <Button 
                        type="submit" 
                        className="w-full bg-gradient-to-r from-blue-600 to-violet-600"
                        disabled={generateIdeasMutation.isPending}
                      >
                        {generateIdeasMutation.isPending ? (
                          <>
                            <LucideLoader2 className="mr-2 h-4 w-4 animate-spin" />
                            Generating Ideas...
                          </>
                        ) : (
                          <>
                            <LucideSparkles className="mr-2 h-4 w-4" />
                            Generate Ideas
                          </>
                        )}
                      </Button>
                    </form>
                  </Form>
                </CardContent>
              </Card>
            </div>

            <div>
              <Card>
                <CardHeader>
                  <CardTitle>Tips</CardTitle>
                  <CardDescription>
                    How to get the best results
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <h3 className="text-sm font-medium">Be Specific</h3>
                    <p className="text-sm text-muted-foreground">
                      The more specific your theme, the more focused your results will be.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-sm font-medium">Complexity Matters</h3>
                    <p className="text-sm text-muted-foreground">
                      Choose a complexity level that matches your development experience.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-sm font-medium">Include Features</h3>
                    <p className="text-sm text-muted-foreground">
                      Select the types of features you're interested in implementing.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-sm font-medium">Additional Notes</h3>
                    <p className="text-sm text-muted-foreground">
                      Add specific requirements like "compatible with mod X" or "focus on peaceful gameplay".
                    </p>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="ideas" className="space-y-6">
          {generateIdeasMutation.data && (
            <div className="grid grid-cols-1 gap-6">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {generateIdeasMutation.data.ideas.map((idea, index) => (
                  <Card
                    key={index}
                    className={`cursor-pointer hover:shadow-md transition-shadow ${
                      selectedIdea === idea ? "ring-2 ring-blue-500" : ""
                    }`}
                    onClick={() => handleSelectIdea(idea)}
                  >
                    <CardHeader className="pb-2">
                      <CardTitle className="text-lg">{idea.title}</CardTitle>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {idea.tags.slice(0, 3).map((tag, tagIndex) => (
                          <Badge key={tagIndex} variant="outline" className="text-xs">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    </CardHeader>
                    <CardContent className="pb-2">
                      <p className="text-sm text-muted-foreground line-clamp-3">
                        {idea.description}
                      </p>
                    </CardContent>
                    <CardFooter className="flex justify-between text-xs text-muted-foreground pt-0">
                      <span>Complexity: {idea.complexity}</span>
                      <span>{idea.suggestedModLoader}</span>
                    </CardFooter>
                  </Card>
                ))}
              </div>

              {selectedIdea && (
                <Card>
                  <CardHeader>
                    <div className="flex justify-between items-start">
                      <CardTitle>{selectedIdea.title}</CardTitle>
                      <div className="flex gap-1">
                        {selectedIdea.tags.map((tag, index) => (
                          <Badge key={index} variant="outline">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    </div>
                    <CardDescription>
                      Complexity: {selectedIdea.complexity} • Dev Time: {selectedIdea.estimatedDevTime} • 
                      Mod Loader: {selectedIdea.suggestedModLoader}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <h3 className="text-sm font-medium mb-2">Description</h3>
                      <p className="text-sm text-muted-foreground">{selectedIdea.description}</p>
                    </div>
                    <div>
                      <h3 className="text-sm font-medium mb-2">Features</h3>
                      <ul className="list-disc pl-5 space-y-1">
                        {selectedIdea.features.map((feature, index) => (
                          <li key={index} className="text-sm text-muted-foreground">
                            {feature}
                          </li>
                        ))}
                      </ul>
                    </div>
                    {selectedIdea.compatibilityNotes && (
                      <div>
                        <h3 className="text-sm font-medium mb-2">Compatibility Notes</h3>
                        <p className="text-sm text-muted-foreground">{selectedIdea.compatibilityNotes}</p>
                      </div>
                    )}
                  </CardContent>
                  <CardFooter>
                    <Button
                      className="w-full bg-gradient-to-r from-blue-600 to-violet-600"
                      onClick={handleExpandIdea}
                      disabled={expandIdeaMutation.isPending}
                    >
                      {expandIdeaMutation.isPending ? (
                        <>
                          <LucideLoader2 className="mr-2 h-4 w-4 animate-spin" />
                          Expanding Idea...
                        </>
                      ) : (
                        <>
                          <LucideChevronRight className="mr-2 h-4 w-4" />
                          Expand This Idea
                        </>
                      )}
                    </Button>
                  </CardFooter>
                </Card>
              )}

              {generateIdeasMutation.data.inspirations.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Inspirations</CardTitle>
                    <CardDescription>
                      Other mods that might inspire your project
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ul className="list-disc pl-5 space-y-1">
                      {generateIdeasMutation.data.inspirations.map((inspiration, index) => (
                        <li key={index} className="text-sm">
                          {inspiration}
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>
              )}
            </div>
          )}
        </TabsContent>

        <TabsContent value="expanded" className="space-y-6">
          {expandedIdea && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2 space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>{expandedIdea.expandedIdea.title}</CardTitle>
                    <CardDescription>
                      {expandedIdea.expandedIdea.description}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <h3 className="text-sm font-medium mb-4">Features</h3>
                      <div className="space-y-4">
                        {expandedIdea.expandedIdea.detailedFeatures.map((feature, index) => (
                          <div key={index} className="space-y-2">
                            <h4 className="font-medium">{feature.name}</h4>
                            <p className="text-sm text-muted-foreground">{feature.description}</p>
                            <div className="text-xs bg-muted rounded-md p-3">
                              <p className="font-medium mb-1">Implementation Notes:</p>
                              <p className="text-muted-foreground">{feature.implementation}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Implementation Approach</CardTitle>
                    <CardDescription>
                      Suggested approach for development
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="text-sm text-muted-foreground space-y-2">
                      <p>{expandedIdea.expandedIdea.suggestedImplementationApproach}</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Development Roadmap</CardTitle>
                    <CardDescription>
                      Recommended milestones for implementation
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ol className="list-decimal pl-5 space-y-2">
                      {expandedIdea.expandedIdea.developmentRoadmap.map((step, index) => (
                        <li key={index} className="text-sm">
                          {step}
                        </li>
                      ))}
                    </ol>
                  </CardContent>
                  <CardFooter>
                    <Button 
                      className="w-full bg-gradient-to-r from-blue-600 to-violet-600"
                      onClick={handleCreateMod}
                    >
                      <LucideArrowRight className="mr-2 h-4 w-4" />
                      Create This Mod
                    </Button>
                  </CardFooter>
                </Card>
              </div>

              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Technical Considerations</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ul className="list-disc pl-5 space-y-2">
                      {expandedIdea.expandedIdea.technicalConsiderations.map((item, idx) => (
                        <li key={idx} className="text-sm text-muted-foreground">{item}</li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">Potential Challenges</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ul className="list-disc pl-5 space-y-2">
                      {expandedIdea.expandedIdea.potentialChallenges.map((challenge, idx) => (
                        <li key={idx} className="text-sm text-muted-foreground">{challenge}</li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>

                <div className="flex gap-4">
                  <Button variant="outline" className="flex-1">
                    <LucideDownload className="mr-2 h-4 w-4" />
                    Export as JSON
                  </Button>
                </div>
              </div>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}