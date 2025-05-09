import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "../lib/queryClient";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Progress } from "@/components/ui/progress";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Globe, Search, Plus, RefreshCw, Code, BookOpen, AlertTriangle, Trash2, ArrowUpRight } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

// Define the source type
interface WebSource {
  id: number;
  url: string;
  title: string;
  description: string | null;
  lastScraped: string;
  status: string;
  contentType: string;
  discoveredPages: number;
  extractedPatterns: number;
  tags: string[];
  createdAt: string;
}

// Define the stats type
interface WebScrapingStats {
  totalSources: number;
  totalPages: number;
  totalPatterns: number;
  codeSnippets: number;
  errorExamples: number;
  lastUpdated: string;
  activeScrapes: number;
  pendingSources: number;
  failedSources: number;
}

// Define the form schema for validation
const addSourceSchema = z.object({
  url: z.string().url({ message: "Please enter a valid URL" }),
  description: z.string().optional(),
  contentType: z.string({ required_error: "Please select a content type" }),
  tags: z.string().optional()
});

// Define the form type for React Hook Form
type SourceFormValues = z.infer<typeof addSourceSchema>;

// Type for the transformed data sent to the API
type SourceApiValues = {
  url: string;
  description?: string;
  contentType: string;
  tags: string[];
};

// WebExplorer component
export default function WebExplorerPage() {
  const [selectedTabId, setSelectedTabId] = useState<number | null>(null);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Query to get all sources
  const { data: sources = [], isLoading: sourcesLoading } = useQuery<WebSource[]>({
    queryKey: ['/api/web-explorer/sources'],
    retry: 1
  });

  // Query to get statistics with default empty object
  const { data: stats = {} as WebScrapingStats, isLoading: statsLoading } = useQuery<WebScrapingStats>({
    queryKey: ['/api/web-explorer/stats'],
    retry: 1
  });

  // Mutation for adding a new source
  const addSourceMutation = useMutation({
    mutationFn: (newSource: SourceApiValues) => {
      return apiRequest('/api/web-explorer/sources', {
        method: 'POST',
        data: newSource
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/web-explorer/sources'] });
      toast({
        title: "Source added",
        description: "The web source has been added successfully.",
      });
      form.reset();
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: "Failed to add web source. Please try again.",
        variant: "destructive",
      });
      console.error("Failed to add source:", error);
    }
  });

  // Mutation for deleting a source
  const deleteSourceMutation = useMutation({
    mutationFn: (id: number) => {
      return apiRequest(`/api/web-explorer/sources/${id}`, {
        method: 'DELETE'
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/api/web-explorer/sources'] });
      toast({
        title: "Source deleted",
        description: "The web source has been deleted successfully.",
      });
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: "Failed to delete web source. Please try again.",
        variant: "destructive",
      });
      console.error("Failed to delete source:", error);
    }
  });

  // Mutation for triggering web scraping
  const triggerScrapingMutation = useMutation({
    mutationFn: () => {
      return apiRequest('/api/web-explorer/trigger', {
        method: 'POST'
      });
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['/api/web-explorer/sources'] });
      queryClient.invalidateQueries({ queryKey: ['/api/web-explorer/stats'] });
      toast({
        title: "Scraping triggered",
        description: data.message || "Web scraping has been triggered successfully.",
      });
    },
    onError: (error) => {
      toast({
        title: "Error",
        description: "Failed to trigger web scraping. Please try again.",
        variant: "destructive",
      });
      console.error("Failed to trigger scraping:", error);
    }
  });

  // Form setup for adding a new source
  const form = useForm<SourceFormValues>({
    resolver: zodResolver(addSourceSchema),
    defaultValues: {
      url: "",
      description: "",
      contentType: "documentation",
      tags: ""
    }
  });

  const onSubmit = (values: SourceFormValues) => {
    // Transform the form values to the API format
    const apiValues: SourceApiValues = {
      url: values.url,
      contentType: values.contentType,
      tags: values.tags ? values.tags.split(',').map(tag => tag.trim()) : [],
    };
    
    // Add description only if it's provided
    if (values.description) {
      apiValues.description = values.description;
    }
    
    addSourceMutation.mutate(apiValues);
  };

  // Function to get status badge color
  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return "bg-green-500";
      case 'pending':
        return "bg-yellow-500";
      case 'error':
        return "bg-red-500";
      case 'completed':
        return "bg-blue-500";
      default:
        return "bg-gray-500";
    }
  };

  return (
    <div className="container mx-auto space-y-6">
      <div className="flex flex-col space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Web Explorer</h1>
        <p className="text-muted-foreground">
          Explore and gather knowledge from Minecraft modding websites to enhance the system's self-learning capabilities.
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Total Sources</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statsLoading ? "..." : stats?.totalSources || 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Extracted Patterns</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statsLoading ? "..." : stats?.totalPatterns || 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Code Snippets</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statsLoading ? "..." : stats?.codeSnippets || 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Error Examples</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statsLoading ? "..." : stats?.errorExamples || 0}</div>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="sources" className="w-full">
        <TabsList className="mb-4">
          <TabsTrigger value="sources">
            <Globe className="mr-2 h-4 w-4" />
            Sources
          </TabsTrigger>
          <TabsTrigger value="add">
            <Plus className="mr-2 h-4 w-4" />
            Add Source
          </TabsTrigger>
          <TabsTrigger value="statistics">
            <RefreshCw className="mr-2 h-4 w-4" />
            Statistics
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="sources" className="space-y-4">
          <div className="flex justify-between mb-4">
            <h2 className="text-xl font-semibold">Web Sources</h2>
            <Button 
              onClick={() => triggerScrapingMutation.mutate()}
              disabled={triggerScrapingMutation.isPending}
            >
              {triggerScrapingMutation.isPending ? (
                <>
                  <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                  Processing...
                </>
              ) : (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Trigger Scraping
                </>
              )}
            </Button>
          </div>

          {sourcesLoading ? (
            <div className="flex justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin" />
            </div>
          ) : sources && sources.length > 0 ? (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {sources.map((source: WebSource) => (
                <Card key={source.id} className="overflow-hidden">
                  <CardHeader className="pb-2">
                    <div className="flex justify-between items-start">
                      <CardTitle className="text-lg font-semibold truncate">{source.title}</CardTitle>
                      <Badge className={getStatusColor(source.status)}>
                        {source.status}
                      </Badge>
                    </div>
                    <CardDescription className="truncate">
                      <a href={source.url} target="_blank" rel="noopener noreferrer" className="flex items-center hover:underline">
                        {source.url}
                        <ArrowUpRight className="ml-1 h-3 w-3" />
                      </a>
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="pb-2">
                    <div className="space-y-2">
                      <p className="text-sm text-muted-foreground line-clamp-2">
                        {source.description || "No description provided."}
                      </p>
                      <div className="flex items-center text-sm">
                        <div className="flex items-center mr-4">
                          <BookOpen className="mr-1 h-4 w-4 text-muted-foreground" />
                          <span>{source.discoveredPages} pages</span>
                        </div>
                        <div className="flex items-center">
                          <Code className="mr-1 h-4 w-4 text-muted-foreground" />
                          <span>{source.extractedPatterns} patterns</span>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-1 mt-2">
                        {source.tags && source.tags.map((tag, index) => (
                          <Badge key={index} variant="outline" className="text-xs">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                  <CardFooter className="pt-2 flex justify-between">
                    <div className="text-xs text-muted-foreground">
                      Last updated: {new Date(source.lastScraped).toLocaleDateString()}
                    </div>
                    <Button 
                      variant="ghost" 
                      size="icon"
                      onClick={() => {
                        if (confirm("Are you sure you want to delete this source?")) {
                          deleteSourceMutation.mutate(source.id);
                        }
                      }}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </CardFooter>
                </Card>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <Globe className="mx-auto h-12 w-12 text-muted-foreground/50" />
              <h3 className="mt-4 text-lg font-semibold">No sources found</h3>
              <p className="text-muted-foreground">Add your first web source to start gathering modding knowledge.</p>
            </div>
          )}
        </TabsContent>
        
        <TabsContent value="add">
          <Card>
            <CardHeader>
              <CardTitle>Add Web Source</CardTitle>
              <CardDescription>
                Add a website containing Minecraft modding documentation, tutorials, or code examples.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                  <FormField
                    control={form.control}
                    name="url"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>URL</FormLabel>
                        <FormControl>
                          <Input placeholder="https://example.com/modding-docs" {...field} />
                        </FormControl>
                        <FormDescription>
                          Enter the website URL to scrape for modding knowledge.
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="contentType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Content Type</FormLabel>
                        <Select 
                          onValueChange={field.onChange} 
                          defaultValue={field.value}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select content type" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="documentation">Documentation</SelectItem>
                            <SelectItem value="tutorial">Tutorials</SelectItem>
                            <SelectItem value="api">API Reference</SelectItem>
                            <SelectItem value="forum">Forums</SelectItem>
                            <SelectItem value="example">Code Examples</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          Select the primary type of content on this website.
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Description (Optional)</FormLabel>
                        <FormControl>
                          <Textarea 
                            placeholder="Brief description of what this site contains..." 
                            className="resize-none"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="tags"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Tags (Optional)</FormLabel>
                        <FormControl>
                          <Input 
                            placeholder="forge, fabric, rendering, entities, etc. (comma-separated)" 
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>
                          Add comma-separated tags to categorize this source.
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <Button 
                    type="submit" 
                    className="w-full"
                    disabled={addSourceMutation.isPending}
                  >
                    {addSourceMutation.isPending ? (
                      <>
                        <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                        Adding Source...
                      </>
                    ) : (
                      <>
                        <Plus className="mr-2 h-4 w-4" />
                        Add Source
                      </>
                    )}
                  </Button>
                </form>
              </Form>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="statistics">
          <Card>
            <CardHeader>
              <CardTitle>Web Scraping Statistics</CardTitle>
              <CardDescription>
                Overview of gathered data and web scraping performance.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {statsLoading ? (
                <div className="flex justify-center py-8">
                  <RefreshCw className="h-6 w-6 animate-spin" />
                </div>
              ) : stats ? (
                <>
                  <div className="grid gap-6 md:grid-cols-2">
                    <div className="space-y-4">
                      <div>
                        <div className="flex justify-between mb-1">
                          <span className="text-sm font-medium">Total Pages</span>
                          <span className="text-sm">{stats.totalPages}</span>
                        </div>
                        <Progress value={stats.totalPages > 0 ? 100 : 0} className="h-2" />
                      </div>
                      
                      <div>
                        <div className="flex justify-between mb-1">
                          <span className="text-sm font-medium">Patterns Extracted</span>
                          <span className="text-sm">{stats.totalPatterns}</span>
                        </div>
                        <Progress value={stats.totalPatterns > 0 ? 100 : 0} className="h-2" />
                      </div>
                      
                      <div>
                        <div className="flex justify-between mb-1">
                          <span className="text-sm font-medium">Code Snippets</span>
                          <span className="text-sm">{stats.codeSnippets}</span>
                        </div>
                        <Progress 
                          value={stats.totalPatterns > 0 ? (stats.codeSnippets / stats.totalPatterns) * 100 : 0} 
                          className="h-2" 
                        />
                      </div>
                      
                      <div>
                        <div className="flex justify-between mb-1">
                          <span className="text-sm font-medium">Error Examples</span>
                          <span className="text-sm">{stats.errorExamples}</span>
                        </div>
                        <Progress 
                          value={stats.totalPatterns > 0 ? (stats.errorExamples / stats.totalPatterns) * 100 : 0} 
                          className="h-2" 
                        />
                      </div>
                    </div>
                    
                    <div className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <Card>
                          <CardHeader className="p-4 pb-2">
                            <CardTitle className="text-sm">Active Scrapes</CardTitle>
                          </CardHeader>
                          <CardContent className="p-4 pt-0">
                            <div className="text-2xl font-bold">{stats.activeScrapes}</div>
                          </CardContent>
                        </Card>
                        
                        <Card>
                          <CardHeader className="p-4 pb-2">
                            <CardTitle className="text-sm">Pending Sources</CardTitle>
                          </CardHeader>
                          <CardContent className="p-4 pt-0">
                            <div className="text-2xl font-bold">{stats.pendingSources}</div>
                          </CardContent>
                        </Card>
                        
                        <Card>
                          <CardHeader className="p-4 pb-2">
                            <CardTitle className="text-sm">Failed Sources</CardTitle>
                          </CardHeader>
                          <CardContent className="p-4 pt-0">
                            <div className="text-2xl font-bold">{stats.failedSources}</div>
                          </CardContent>
                        </Card>
                        
                        <Card>
                          <CardHeader className="p-4 pb-2">
                            <CardTitle className="text-sm">Last Updated</CardTitle>
                          </CardHeader>
                          <CardContent className="p-4 pt-0 text-xs">
                            {new Date(stats.lastUpdated).toLocaleString()}
                          </CardContent>
                        </Card>
                      </div>
                      
                      <Card>
                        <CardHeader className="p-4 pb-2">
                          <CardTitle className="text-sm">Source Status Distribution</CardTitle>
                        </CardHeader>
                        <CardContent className="p-4 pt-0">
                          <div className="flex h-4 mb-2">
                            {stats.activeScrapes > 0 && (
                              <div 
                                className="bg-green-500 h-full" 
                                style={{ 
                                  width: `${(stats.activeScrapes / (stats.totalSources || 1)) * 100}%` 
                                }}
                              />
                            )}
                            {stats.pendingSources > 0 && (
                              <div 
                                className="bg-yellow-500 h-full" 
                                style={{ 
                                  width: `${(stats.pendingSources / (stats.totalSources || 1)) * 100}%` 
                                }}
                              />
                            )}
                            {stats.failedSources > 0 && (
                              <div 
                                className="bg-red-500 h-full" 
                                style={{ 
                                  width: `${(stats.failedSources / (stats.totalSources || 1)) * 100}%` 
                                }}
                              />
                            )}
                            {(stats.totalSources - stats.activeScrapes - stats.pendingSources - stats.failedSources) > 0 && (
                              <div 
                                className="bg-blue-500 h-full" 
                                style={{ 
                                  width: `${((stats.totalSources - stats.activeScrapes - stats.pendingSources - stats.failedSources) / (stats.totalSources || 1)) * 100}%` 
                                }}
                              />
                            )}
                          </div>
                          <div className="grid grid-cols-4 text-xs">
                            <div className="flex items-center">
                              <div className="w-2 h-2 rounded-full bg-green-500 mr-1" />
                              <span>Active</span>
                            </div>
                            <div className="flex items-center">
                              <div className="w-2 h-2 rounded-full bg-yellow-500 mr-1" />
                              <span>Pending</span>
                            </div>
                            <div className="flex items-center">
                              <div className="w-2 h-2 rounded-full bg-red-500 mr-1" />
                              <span>Failed</span>
                            </div>
                            <div className="flex items-center">
                              <div className="w-2 h-2 rounded-full bg-blue-500 mr-1" />
                              <span>Completed</span>
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    </div>
                  </div>
                </>
              ) : (
                <div className="text-center py-8">
                  <AlertTriangle className="mx-auto h-12 w-12 text-muted-foreground/50" />
                  <h3 className="mt-4 text-lg font-semibold">No statistics available</h3>
                  <p className="text-muted-foreground">Add web sources and trigger scraping to generate statistics.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}