import { useState } from "react";
import { apiRequest } from "@/lib/queryClient";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, Lightbulb, Zap, Clock, Tag, Layers, ArrowRight, Info, Cpu, Calendar } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

// Interface for mod idea data
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

// Interface for expanded idea data
interface ExpandedIdea {
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

interface IdeaCardProps {
  idea: ModIdea;
  onSelected?: (idea: ModIdea) => void;
}

export function IdeaCard({ idea, onSelected }: IdeaCardProps) {
  const [expanded, setExpanded] = useState<ExpandedIdea | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const { toast } = useToast();

  // Function to get the background color based on complexity
  const getComplexityColor = (complexity: string) => {
    const lowerComplexity = complexity.toLowerCase();
    if (lowerComplexity.includes("simple")) return "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100";
    if (lowerComplexity.includes("complex")) return "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-100";
    return "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-100";
  };

  // Function to expand the mod idea with more details
  const expandIdea = async () => {
    if (expanded) {
      setDialogOpen(true);
      return;
    }

    setIsLoading(true);
    try {
      const response = await apiRequest<{ expandedIdea: ExpandedIdea }>("/api/ai/expand-idea", {
        method: "POST",
        body: JSON.stringify({
          ideaTitle: idea.title,
          ideaDescription: idea.description
        }),
      });
      
      setExpanded(response.expandedIdea);
      setDialogOpen(true);
      
    } catch (error) {
      console.error("Error expanding idea:", error);
      toast({
        variant: "destructive",
        title: "Error",
        description: "Failed to expand mod idea. Please try again.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Card className="h-full flex flex-col hover:shadow-md transition-shadow duration-200">
        <CardHeader className="pb-2">
          <div className="flex justify-between items-start">
            <CardTitle className="text-lg">{idea.title}</CardTitle>
            <Badge variant="outline" className={getComplexityColor(idea.complexity)}>
              {idea.complexity}
            </Badge>
          </div>
          <CardDescription>{idea.description}</CardDescription>
        </CardHeader>
        <CardContent className="pb-2 flex-grow">
          <div className="space-y-4">
            <div>
              <Label className="flex items-center text-xs text-muted-foreground mb-1">
                <Lightbulb className="h-3 w-3 mr-1" />
                Key Features
              </Label>
              <ul className="text-sm pl-5 list-disc">
                {idea.features.map((feature, index) => (
                  <li key={index} className="mb-1">{feature}</li>
                ))}
              </ul>
            </div>
            
            <div className="grid grid-cols-2 gap-2 text-sm mt-2">
              <div>
                <Label className="flex items-center text-xs text-muted-foreground mb-1">
                  <Zap className="h-3 w-3 mr-1" />
                  Mod Loader
                </Label>
                <span>{idea.suggestedModLoader}</span>
              </div>
              <div>
                <Label className="flex items-center text-xs text-muted-foreground mb-1">
                  <Clock className="h-3 w-3 mr-1" />
                  Est. Time
                </Label>
                <span>{idea.estimatedDevTime}</span>
              </div>
            </div>
            
            {idea.tags && idea.tags.length > 0 && (
              <div>
                <Label className="flex items-center text-xs text-muted-foreground mb-1">
                  <Tag className="h-3 w-3 mr-1" />
                  Tags
                </Label>
                <div className="flex flex-wrap gap-1">
                  {idea.tags.map((tag, index) => (
                    <Badge key={index} variant="secondary" className="text-xs">
                      {tag}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </div>
        </CardContent>
        <CardFooter className="flex justify-between pt-2">
          <Button 
            variant="outline" 
            size="sm"
            disabled={isLoading}
            onClick={expandIdea}
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <>
                View Details
                <ArrowRight className="ml-1 h-3 w-3" />
              </>
            )}
          </Button>
          
          {onSelected && (
            <Button 
              size="sm"
              onClick={() => onSelected(idea)}
            >
              Use This Idea
            </Button>
          )}
        </CardFooter>
      </Card>

      {/* Expanded idea dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-xl">{expanded?.title || idea.title}</DialogTitle>
            <DialogDescription className="text-base">{expanded?.description || idea.description}</DialogDescription>
          </DialogHeader>
          
          {expanded ? (
            <Tabs defaultValue="features">
              <TabsList className="grid grid-cols-4 mb-4">
                <TabsTrigger value="features">Features</TabsTrigger>
                <TabsTrigger value="technical">Technical</TabsTrigger>
                <TabsTrigger value="roadmap">Roadmap</TabsTrigger>
                <TabsTrigger value="challenges">Challenges</TabsTrigger>
              </TabsList>
              
              <TabsContent value="features" className="space-y-4">
                <h3 className="text-lg font-medium flex items-center">
                  <Layers className="mr-2 h-5 w-5 text-primary" />
                  Detailed Features
                </h3>
                
                <Accordion type="single" collapsible className="w-full">
                  {expanded.detailedFeatures.map((feature, idx) => (
                    <AccordionItem key={idx} value={`feature-${idx}`}>
                      <AccordionTrigger className="text-base font-medium">
                        {feature.name}
                      </AccordionTrigger>
                      <AccordionContent className="space-y-2">
                        <div>
                          <p className="text-sm">{feature.description}</p>
                        </div>
                        <div>
                          <h4 className="text-sm font-semibold">Implementation Notes:</h4>
                          <p className="text-sm text-muted-foreground">{feature.implementation}</p>
                        </div>
                      </AccordionContent>
                    </AccordionItem>
                  ))}
                </Accordion>
              </TabsContent>
              
              <TabsContent value="technical" className="space-y-4">
                <h3 className="text-lg font-medium flex items-center">
                  <Cpu className="mr-2 h-5 w-5 text-primary" />
                  Technical Considerations
                </h3>
                <ul className="space-y-2">
                  {expanded.technicalConsiderations.map((consideration, idx) => (
                    <li key={idx} className="flex items-start">
                      <Info className="h-4 w-4 mr-2 mt-0.5 text-primary" />
                      <span className="text-sm">{consideration}</span>
                    </li>
                  ))}
                </ul>
                
                <h3 className="text-lg font-medium flex items-center mt-4">
                  <Info className="mr-2 h-5 w-5 text-primary" />
                  Suggested Implementation Approach
                </h3>
                <p className="text-sm">{expanded.suggestedImplementationApproach}</p>
              </TabsContent>
              
              <TabsContent value="roadmap" className="space-y-4">
                <h3 className="text-lg font-medium flex items-center">
                  <Calendar className="mr-2 h-5 w-5 text-primary" />
                  Development Roadmap
                </h3>
                <ol className="space-y-2">
                  {expanded.developmentRoadmap.map((step, idx) => (
                    <li key={idx} className="flex items-start">
                      <span className="font-medium mr-2">{idx + 1}.</span>
                      <span className="text-sm">{step}</span>
                    </li>
                  ))}
                </ol>
              </TabsContent>
              
              <TabsContent value="challenges" className="space-y-4">
                <h3 className="text-lg font-medium flex items-center">
                  <Info className="mr-2 h-5 w-5 text-primary" />
                  Potential Challenges
                </h3>
                <ul className="space-y-2">
                  {expanded.potentialChallenges.map((challenge, idx) => (
                    <li key={idx} className="flex items-start">
                      <Info className="h-4 w-4 mr-2 mt-0.5 text-red-500" />
                      <span className="text-sm">{challenge}</span>
                    </li>
                  ))}
                </ul>
              </TabsContent>
            </Tabs>
          ) : (
            <div className="flex justify-center items-center py-10">
              <Loader2 className="h-10 w-10 animate-spin text-muted-foreground" />
            </div>
          )}
          
          {onSelected && (
            <div className="flex justify-end mt-4">
              <Button 
                onClick={() => {
                  onSelected(idea);
                  setDialogOpen(false);
                }}
              >
                Use This Idea
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}