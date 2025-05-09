import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useModContext } from "@/context/mod-context";
import { ProgressCircle } from "@/components/ui/progress-circle";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { CheckCircle2, Clock, Sparkles, Brain, Code, Bug, AlertTriangle } from "lucide-react";

interface FeatureProgressItem {
  id: string;
  name: string;
  status: "planned" | "in_progress" | "completed" | "failed";
  progress: number;
  estimatedCompletion?: string;
  notes?: string;
}

interface FeatureProgressData {
  modId: number;
  features: FeatureProgressItem[];
  currentFeature?: string;
  aiSuggestions?: string[];
}

export function FeatureProgressTracker() {
  const { currentMod } = useModContext();
  const [activeTab, setActiveTab] = useState<string>("all");
  
  // In a real implementation, this would fetch from an API endpoint
  // For now, we'll simulate this with a static data object
  const { data, isLoading, error } = useQuery<FeatureProgressData>({
    queryKey: ['/api/mods', currentMod?.id, 'features', 'progress'],
    enabled: !!currentMod?.id,
    placeholderData: currentMod ? {
      modId: currentMod.id,
      features: [
        {
          id: "feature-1",
          name: "Basic mod structure and configuration",
          status: "completed",
          progress: 100,
          notes: "Initial setup complete with all required files"
        },
        {
          id: "feature-2",
          name: "Custom weapons implementation",
          status: "in_progress",
          progress: 65,
          estimatedCompletion: "~20 minutes",
          notes: "Currently implementing attack animations"
        },
        {
          id: "feature-3",
          name: "Stamina system",
          status: "planned",
          progress: 0,
          estimatedCompletion: "~45 minutes",
          notes: "Will start after weapons implementation"
        },
        {
          id: "feature-4",
          name: "Particle effects for combat actions",
          status: "planned",
          progress: 0,
          estimatedCompletion: "~30 minutes"
        }
      ],
      currentFeature: "feature-2",
      aiSuggestions: [
        "Consider adding special weapon effects for critical hits",
        "Combat balancing: adjust damage values based on weapon weight",
        "Add sound effects for each weapon type"
      ]
    } : undefined,
    refetchInterval: 15000 // Refresh every 15 seconds when active
  });
  
  const getFilteredFeatures = () => {
    if (!data) return [];
    
    if (activeTab === "all") {
      return data.features;
    }
    
    return data.features.filter(feature => feature.status === activeTab);
  };
  
  const getStatusColor = (status: string) => {
    switch (status) {
      case "completed": return "bg-green-500";
      case "in_progress": return "bg-blue-500";
      case "planned": return "bg-gray-400";
      case "failed": return "bg-red-500";
      default: return "bg-gray-400";
    }
  };
  
  const getStatusIcon = (status: string) => {
    switch (status) {
      case "completed": return <CheckCircle2 className="h-4 w-4 text-green-500" />;
      case "in_progress": return <Clock className="h-4 w-4 text-blue-500" />;
      case "planned": return <Sparkles className="h-4 w-4 text-gray-500" />;
      case "failed": return <AlertTriangle className="h-4 w-4 text-red-500" />;
      default: return <Sparkles className="h-4 w-4 text-gray-500" />;
    }
  };
  
  if (!currentMod) {
    return null;
  }
  
  return (
    <Card className="mb-6">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-center">
          <div>
            <CardTitle className="text-xl">Feature Progress</CardTitle>
            <CardDescription>
              Track the development progress of mod features
            </CardDescription>
          </div>
          <div className="flex items-center space-x-2">
            <Brain className="h-5 w-5 text-purple-500" />
            <span className="text-sm font-medium">AI-Driven Development</span>
          </div>
        </div>
      </CardHeader>
      
      <CardContent>
        <Tabs defaultValue="all" value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid grid-cols-4 mb-4">
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="in_progress">In Progress</TabsTrigger>
            <TabsTrigger value="completed">Completed</TabsTrigger>
            <TabsTrigger value="planned">Planned</TabsTrigger>
          </TabsList>
          
          <TabsContent value={activeTab} className="mt-0">
            <div className="space-y-4">
              {isLoading ? (
                <div className="text-center py-4 text-muted-foreground">Loading feature progress...</div>
              ) : error ? (
                <div className="text-center py-4 text-red-500">Error loading feature progress</div>
              ) : getFilteredFeatures().length === 0 ? (
                <div className="text-center py-4 text-muted-foreground">No features in this category</div>
              ) : (
                getFilteredFeatures().map(feature => (
                  <div 
                    key={feature.id} 
                    className={`p-4 rounded-lg border ${feature.id === data?.currentFeature ? 'border-blue-400 bg-blue-50 dark:bg-blue-950/20' : 'border-border'}`}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex-1">
                        <div className="flex items-center">
                          {getStatusIcon(feature.status)}
                          <h4 className="text-base font-medium ml-2">{feature.name}</h4>
                          {feature.id === data?.currentFeature && (
                            <Badge variant="secondary" className="ml-2 bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-100">
                              Current Focus
                            </Badge>
                          )}
                        </div>
                        {feature.notes && (
                          <p className="text-sm text-muted-foreground mt-1">{feature.notes}</p>
                        )}
                      </div>
                      
                      <div className="flex items-center space-x-3">
                        {feature.estimatedCompletion && feature.status !== 'completed' && (
                          <div className="text-xs text-muted-foreground flex items-center">
                            <Clock className="h-3 w-3 mr-1" />
                            {feature.estimatedCompletion}
                          </div>
                        )}
                        <div className="relative h-10 w-10">
                          <ProgressCircle 
                            value={feature.progress} 
                            size="md" 
                            className={getStatusColor(feature.status)}
                          />
                          <span className="absolute inset-0 flex items-center justify-center text-xs font-medium">
                            {feature.progress}%
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </TabsContent>
        </Tabs>
        
        {data?.aiSuggestions && data.aiSuggestions.length > 0 && (
          <div className="mt-6">
            <div className="flex items-center space-x-2 mb-3">
              <Sparkles className="h-4 w-4 text-purple-500" />
              <h4 className="font-medium text-sm">AI Feature Suggestions</h4>
            </div>
            <div className="bg-slate-50 dark:bg-slate-900 rounded-md p-3 border border-slate-200 dark:border-slate-800">
              <ul className="space-y-2">
                {data.aiSuggestions.map((suggestion, index) => (
                  <li key={index} className="text-sm flex items-start">
                    <Code className="h-4 w-4 mr-2 mt-0.5 text-slate-500" />
                    {suggestion}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}