import { useState, useEffect } from "react";
import { generateGenericCode, generateModCode, type ModCode } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Loader2, Code, CheckCircle2, AlertTriangle, BookOpen } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { useMutation } from "@tanstack/react-query";

export function CodeGenerator() {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState("generic");
  
  // Generic code generation
  const [prompt, setPrompt] = useState("");
  const [language, setLanguage] = useState("java");
  const [complexity, setComplexity] = useState<"simple" | "medium" | "complex">("medium");
  const [context, setContext] = useState("");
  
  // Minecraft mod generation
  const [modName, setModName] = useState("");
  const [modId, setModId] = useState("");
  const [modDescription, setModDescription] = useState("");
  const [modLoader, setModLoader] = useState("forge");
  const [mcVersion, setMcVersion] = useState("1.20.4");
  const [expandedIdeaData, setExpandedIdeaData] = useState<any>(null);
  const [ideaTitle, setIdeaTitle] = useState("");
  const [ideaDescription, setIdeaDescription] = useState("");
  
  // Shared state
  const [generatedCode, setGeneratedCode] = useState("");
  const [explanation, setExplanation] = useState("");
  const [error, setError] = useState<string | null>(null);
  
  // Load expanded idea from localStorage if coming from idea generator
  useEffect(() => {
    try {
      const savedExpandedIdea = localStorage.getItem('expandedIdea');
      const savedTitle = localStorage.getItem('ideaTitle');
      const savedDescription = localStorage.getItem('ideaDescription');
      
      if (savedExpandedIdea) {
        const expandedIdea = JSON.parse(savedExpandedIdea);
        setExpandedIdeaData(expandedIdea);
        
        // Automatically fill in the mod name and description from the expanded idea
        if (expandedIdea?.expandedIdea?.title) {
          setModName(expandedIdea.expandedIdea.title);
          // Generate a mod ID from the title (lowercase, no spaces, only alphanumeric and underscores)
          setModId(expandedIdea.expandedIdea.title
            .toLowerCase()
            .replace(/[^a-z0-9]/g, '_')
            .replace(/_+/g, '_')
            .replace(/^_|_$/g, '')
          );
        }
        
        if (expandedIdea?.expandedIdea?.description) {
          setModDescription(expandedIdea.expandedIdea.description);
        }
        
        if (savedTitle) {
          setIdeaTitle(savedTitle);
        }
        
        if (savedDescription) {
          setIdeaDescription(savedDescription);
        }
        
        // Switch to the Minecraft tab
        setActiveTab("minecraft");
        
        // Show a toast notification
        toast({
          title: "Expanded idea loaded",
          description: "Your mod idea has been loaded from the Idea Generator",
        });
        
        // Clear localStorage after loading to avoid reloading on future visits
        localStorage.removeItem('expandedIdea');
        localStorage.removeItem('ideaTitle');
        localStorage.removeItem('ideaDescription');
      }
    } catch (err) {
      console.error("Error loading expanded idea from localStorage:", err);
    }
  }, [toast]);
  
  // Generic code generation mutation
  const genericCodeMutation = useMutation({
    mutationFn: async () => {
      if (!prompt) {
        throw new Error("Please enter a description of the code you want to generate");
      }
      
      return generateGenericCode(prompt, context);
    },
    onSuccess: (data) => {
      setGeneratedCode(data.code);
      setExplanation(data.explanation);
      
      toast({
        title: "Code generated successfully",
        description: "Your code has been generated based on your specifications.",
      });
    },
    onError: (error) => {
      setError(error instanceof Error ? error.message : "An unknown error occurred");
      toast({
        title: "Error generating code",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    }
  });
  
  function handleGenerateGenericCode() {
    setError(null);
    genericCodeMutation.mutate();
  }
  
  // Minecraft mod generation mutation
  const modCodeMutation = useMutation({
    mutationFn: async () => {
      if (!modName || !modDescription) {
        throw new Error("Please provide a mod name and description");
      }
      
      // Build the idea string from either the expanded idea or the basic title/description
      let idea = "";
      
      if (expandedIdeaData && expandedIdeaData.expandedIdea) {
        // Format detailed idea from expanded idea data
        const expanded = expandedIdeaData.expandedIdea;
        
        idea = `Title: ${expanded.title}\n\nDescription: ${expanded.description}\n\n`;
        
        // Add detailed features
        if (expanded.detailedFeatures && expanded.detailedFeatures.length > 0) {
          idea += "Features:\n";
          expanded.detailedFeatures.forEach((feature: any, index: number) => {
            idea += `${index + 1}. ${feature.name}: ${feature.description}\n   Implementation: ${feature.implementation}\n`;
          });
          idea += "\n";
        }
        
        // Add technical considerations
        if (expanded.technicalConsiderations && expanded.technicalConsiderations.length > 0) {
          idea += "Technical Considerations:\n";
          expanded.technicalConsiderations.forEach((consideration: string, index: number) => {
            idea += `${index + 1}. ${consideration}\n`;
          });
          idea += "\n";
        }
        
        // Add suggested implementation approach
        if (expanded.suggestedImplementationApproach) {
          idea += `Implementation Approach: ${expanded.suggestedImplementationApproach}`;
        }
      } else if (ideaTitle && ideaDescription) {
        // Basic idea format
        idea = `Title: ${ideaTitle}\n\nDescription: ${ideaDescription}`;
      } else {
        // Use mod name and description as fallback
        idea = `Title: ${modName}\n\nDescription: ${modDescription}`;
      }
      
      return generateModCode({
        name: modName,
        description: modDescription,
        modLoader: modLoader,
        minecraftVersion: mcVersion,
        idea: idea
      });
    },
    onSuccess: (data) => {
      setGeneratedCode(data.code);
      setExplanation(data.explanation);
      
      toast({
        title: "Mod code generated successfully",
        description: "Your Minecraft mod code has been generated. You can now download or copy the code.",
      });
    },
    onError: (error) => {
      setError(error instanceof Error ? error.message : "An unknown error occurred");
      toast({
        title: "Error generating mod code",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    }
  });
  
  function handleGenerateModCode() {
    setError(null);
    modCodeMutation.mutate();
  }
  
  const handleCopyCode = () => {
    if (generatedCode) {
      navigator.clipboard.writeText(generatedCode);
      toast({
        title: "Code copied to clipboard",
        description: "The generated code has been copied to your clipboard.",
      });
    }
  };
  
  return (
    <div className="space-y-6">
      <Tabs 
        value={activeTab} 
        onValueChange={setActiveTab} 
        className="space-y-6"
      >
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="generic">
            <Code className="h-4 w-4 mr-2" />
            Generic Code
          </TabsTrigger>
          <TabsTrigger value="minecraft">
            <BookOpen className="h-4 w-4 mr-2" />
            Minecraft Mod
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="generic" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Generic Code Generator</CardTitle>
                <CardDescription>Generate code for any programming language</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="prompt">Prompt</Label>
                  <Textarea 
                    id="prompt"
                    placeholder="Describe the code you want to generate..." 
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    className="min-h-[120px]"
                  />
                </div>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="language">Language</Label>
                    <Select value={language} onValueChange={setLanguage}>
                      <SelectTrigger id="language">
                        <SelectValue placeholder="Select language" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="java">Java</SelectItem>
                        <SelectItem value="javascript">JavaScript</SelectItem>
                        <SelectItem value="typescript">TypeScript</SelectItem>
                        <SelectItem value="python">Python</SelectItem>
                        <SelectItem value="cpp">C++</SelectItem>
                        <SelectItem value="csharp">C#</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="complexity">Complexity</Label>
                    <Select 
                      value={complexity} 
                      onValueChange={(value) => setComplexity(value as "simple" | "medium" | "complex")}
                    >
                      <SelectTrigger id="complexity">
                        <SelectValue placeholder="Select complexity" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="simple">Simple</SelectItem>
                        <SelectItem value="medium">Medium</SelectItem>
                        <SelectItem value="complex">Complex</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="context">Context (Optional)</Label>
                  <Textarea 
                    id="context"
                    placeholder="Provide additional context or existing code..." 
                    value={context}
                    onChange={(e) => setContext(e.target.value)}
                    className="min-h-[80px]"
                  />
                </div>
              </CardContent>
              <CardFooter>
                <Button 
                  onClick={handleGenerateGenericCode} 
                  disabled={genericCodeMutation.isPending || !prompt} 
                  className="w-full"
                >
                  {genericCodeMutation.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Generating code...
                    </>
                  ) : (
                    <>
                      <Code className="mr-2 h-4 w-4" />
                      Generate Code
                    </>
                  )}
                </Button>
              </CardFooter>
            </Card>
            
            <OutputCard 
              error={error}
              generatedCode={generatedCode}
              explanation={explanation}
              onCopyCode={handleCopyCode}
            />
          </div>
        </TabsContent>
        
        <TabsContent value="minecraft" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Minecraft Mod Generator</CardTitle>
                <CardDescription>Generate code for your Minecraft mod</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="modName">Mod Name</Label>
                    <Input 
                      id="modName"
                      placeholder="My Awesome Mod" 
                      value={modName}
                      onChange={(e) => {
                        setModName(e.target.value);
                        // Automatically generate mod ID from name
                        if (!modId || modId === modName.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_').replace(/^_|_$/g, '')) {
                          setModId(e.target.value.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_').replace(/^_|_$/g, ''));
                        }
                      }}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="modId">Mod ID</Label>
                    <Input 
                      id="modId"
                      placeholder="myawesomemod" 
                      value={modId}
                      onChange={(e) => setModId(e.target.value)}
                    />
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="modDescription">Mod Description</Label>
                  <Textarea 
                    id="modDescription"
                    placeholder="Describe your mod..." 
                    value={modDescription}
                    onChange={(e) => setModDescription(e.target.value)}
                    className="min-h-[80px]"
                  />
                </div>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="modLoader">Mod Loader</Label>
                    <Select value={modLoader} onValueChange={setModLoader}>
                      <SelectTrigger id="modLoader">
                        <SelectValue placeholder="Select mod loader" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="forge">Forge</SelectItem>
                        <SelectItem value="fabric">Fabric</SelectItem>
                        <SelectItem value="quilt">Quilt</SelectItem>
                        <SelectItem value="architectury">Architectury (Multi-loader)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="mcVersion">Minecraft Version</Label>
                    <Select value={mcVersion} onValueChange={setMcVersion}>
                      <SelectTrigger id="mcVersion">
                        <SelectValue placeholder="Select Minecraft version" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="1.20.4">1.20.4</SelectItem>
                        <SelectItem value="1.19.4">1.19.4</SelectItem>
                        <SelectItem value="1.18.2">1.18.2</SelectItem>
                        <SelectItem value="1.16.5">1.16.5</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                
                {expandedIdeaData && (
                  <div className="p-4 bg-secondary/20 rounded-md">
                    <div className="font-medium mb-2">Loaded Idea: {expandedIdeaData.expandedIdea.title}</div>
                    <p className="text-sm text-muted-foreground mb-2">Expanded idea details will be used to generate your mod code.</p>
                    <div className="text-xs">
                      {expandedIdeaData.expandedIdea.detailedFeatures.length} features, {expandedIdeaData.expandedIdea.technicalConsiderations.length} technical considerations
                    </div>
                  </div>
                )}
              </CardContent>
              <CardFooter>
                <Button 
                  onClick={handleGenerateModCode} 
                  disabled={modCodeMutation.isPending || !modName || !modDescription} 
                  className="w-full"
                >
                  {modCodeMutation.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Generating mod code...
                    </>
                  ) : (
                    <>
                      <Code className="mr-2 h-4 w-4" />
                      Generate Mod Code
                    </>
                  )}
                </Button>
              </CardFooter>
            </Card>
            
            <OutputCard 
              error={error}
              generatedCode={generatedCode}
              explanation={explanation}
              onCopyCode={handleCopyCode}
            />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// Separate component for the output card
function OutputCard({ 
  error, 
  generatedCode, 
  explanation,
  onCopyCode
}: { 
  error: string | null; 
  generatedCode: string; 
  explanation: string;
  onCopyCode: () => void;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle>Generated Code</CardTitle>
          <CardDescription>AI-generated code based on your requirements</CardDescription>
        </div>
        {generatedCode && (
          <Button variant="outline" size="sm" onClick={onCopyCode}>
            <Code className="h-4 w-4 mr-2" />
            Copy Code
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        {error ? (
          <div className="bg-destructive/10 border border-destructive rounded-md p-4 text-sm">
            <div className="flex items-center">
              <AlertTriangle className="h-4 w-4 mr-2 text-destructive" />
              <div>Error: {error}</div>
            </div>
          </div>
        ) : generatedCode ? (
          <>
            <div className="bg-muted rounded-md p-4 font-mono text-sm overflow-auto max-h-[300px]">
              <pre className="whitespace-pre-wrap">{generatedCode}</pre>
            </div>
            
            {explanation && (
              <div className="bg-primary/5 rounded-md p-4 text-sm">
                <div className="font-semibold mb-2">Explanation:</div>
                <div>{explanation}</div>
              </div>
            )}
          </>
        ) : (
          <div className="flex items-center justify-center h-[300px] text-muted-foreground text-center flex-col">
            <Code className="h-12 w-12 mb-4 opacity-20" />
            <p>Generated code will appear here</p>
            <p className="text-sm mt-2">Fill out the form and click "Generate Code"</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}