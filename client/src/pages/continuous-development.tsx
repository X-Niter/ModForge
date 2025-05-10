import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useModContext } from "@/context/mod-context";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { ContinuousDevelopmentControls } from "@/components/continuous-development-controls";
import { FeatureProgressTracker } from "@/components/feature-progress-tracker";
import { DevelopmentAnalytics } from "@/components/development-analytics";
import { ModSelector } from "@/components/mod-selector";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Brain, Code, Cpu, ArrowRight, Terminal, GitBranch, Settings } from "lucide-react";

export default function ContinuousDevelopment() {
  const { currentMod } = useModContext();
  const [activeTab, setActiveTab] = useState<string>("overview");
  
  // Get a list of mods for the selector
  const { data: modsData } = useQuery({
    queryKey: ['/api/mods'],
    refetchOnWindowFocus: false
  });
  
  return (
    <div className="h-screen flex flex-col overflow-hidden bg-background text-foreground">
      <Navbar />
      
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        
        <main className="flex-1 overflow-hidden flex flex-col">
          <div className="flex flex-col h-full">
            <div className="bg-surface p-4 border-b border-gray-700 flex items-center justify-between">
              <div className="flex items-center">
                <Brain className="h-5 w-5 text-purple-500 mr-2" />
                <h2 className="text-lg font-medium text-white">Continuous Development</h2>
              </div>
              
              <div className="flex items-center space-x-2">
                {currentMod && (
                  <Badge variant="outline" className="bg-primary/10 text-primary border-primary/20">
                    <Code className="h-3 w-3 mr-1" /> {currentMod.name}
                  </Badge>
                )}
                <ModSelector />
              </div>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4">
              {currentMod ? (
                <>
                  <div className="mb-6">
                    <ContinuousDevelopmentControls />
                  </div>
                  
                  <Tabs defaultValue="overview" value={activeTab} onValueChange={setActiveTab} className="w-full">
                    <TabsList className="grid w-full grid-cols-4 mb-6">
                      <TabsTrigger value="overview" className="flex items-center">
                        <Cpu className="h-4 w-4 mr-2" />
                        Overview
                      </TabsTrigger>
                      <TabsTrigger value="features" className="flex items-center">
                        <Code className="h-4 w-4 mr-2" />
                        Features
                      </TabsTrigger>
                      <TabsTrigger value="analytics" className="flex items-center">
                        <GitBranch className="h-4 w-4 mr-2" />
                        Analytics
                      </TabsTrigger>
                      <TabsTrigger value="logs" className="flex items-center">
                        <Terminal className="h-4 w-4 mr-2" />
                        Logs
                      </TabsTrigger>
                    </TabsList>
                    
                    <TabsContent value="overview" className="space-y-6">
                      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        <div className="lg:col-span-2">
                          <FeatureProgressTracker />
                        </div>
                        
                        <div className="lg:col-span-1">
                          <Card>
                            <CardHeader className="pb-3">
                              <CardTitle className="text-xl">System Status</CardTitle>
                              <CardDescription>Current operational metrics</CardDescription>
                            </CardHeader>
                            
                            <CardContent>
                              <div className="space-y-4">
                                <div className="flex justify-between items-center pb-2 border-b">
                                  <span className="text-muted-foreground">AI System</span>
                                  <Badge variant="outline" className="bg-primary/10 text-primary dark:bg-primary/5 dark:text-primary/90 border-primary/20">
                                    Operational
                                  </Badge>
                                </div>
                                
                                <div className="flex justify-between items-center pb-2 border-b">
                                  <span className="text-muted-foreground">Build System</span>
                                  <Badge variant="outline" className="bg-primary/10 text-primary dark:bg-primary/5 dark:text-primary/90 border-primary/20">
                                    Operational
                                  </Badge>
                                </div>
                                
                                <div className="flex justify-between items-center pb-2 border-b">
                                  <span className="text-muted-foreground">OpenAI API</span>
                                  <Badge variant="outline" className="bg-secondary/10 text-secondary dark:bg-secondary/5 dark:text-secondary/90 border-secondary/20">
                                    Connected
                                  </Badge>
                                </div>
                                
                                <div className="flex justify-between items-center pb-2 border-b">
                                  <span className="text-muted-foreground">Database</span>
                                  <Badge variant="outline" className="bg-primary/10 text-primary dark:bg-primary/5 dark:text-primary/90 border-primary/20">
                                    Healthy
                                  </Badge>
                                </div>
                                
                                <div className="flex justify-between items-center pb-2 border-b">
                                  <span className="text-muted-foreground">Continuous Integration</span>
                                  <Badge variant="outline" className="bg-accent/10 text-accent dark:bg-accent/5 dark:text-accent/90 border-accent/20">
                                    Active
                                  </Badge>
                                </div>
                                
                                <div className="flex justify-between items-center">
                                  <span className="text-muted-foreground">Last Check</span>
                                  <span className="text-sm">Just now</span>
                                </div>
                              </div>
                              
                              <div className="mt-6 flex justify-end">
                                <button className="text-sm text-blue-500 flex items-center">
                                  <Settings className="h-4 w-4 mr-1" /> Configure System
                                </button>
                              </div>
                            </CardContent>
                          </Card>
                        </div>
                      </div>
                    </TabsContent>
                    
                    <TabsContent value="features">
                      <FeatureProgressTracker />
                    </TabsContent>
                    
                    <TabsContent value="analytics">
                      <DevelopmentAnalytics />
                    </TabsContent>
                    
                    <TabsContent value="logs">
                      <Card>
                        <CardHeader className="pb-3">
                          <CardTitle className="text-xl">Development Logs</CardTitle>
                          <CardDescription>Real-time logs from the continuous development process</CardDescription>
                        </CardHeader>
                        
                        <CardContent>
                          <div className="bg-slate-900 text-slate-100 rounded-md p-4 font-mono text-sm h-[600px] overflow-y-auto">
                            <p className="text-green-400">[System] Continuous development started for {currentMod.name}</p>
                            <p className="text-blue-400">[AI] Analyzing mod structure...</p>
                            <p className="text-blue-400">[AI] Identifying feature implementation priorities...</p>
                            <p className="text-yellow-400">[Build] Starting build #1...</p>
                            <p className="text-yellow-400">[Build] Compiling Java sources...</p>
                            <p className="text-red-400">[Error] Build failed: 3 compilation errors</p>
                            <p className="text-blue-400">[AI] Analyzing error patterns...</p>
                            <p className="text-blue-400">[AI] Implementing fixes for syntax errors in WeaponType.java</p>
                            <p className="text-yellow-400">[Build] Starting build #2...</p>
                            <p className="text-yellow-400">[Build] Compiling Java sources...</p>
                            <p className="text-green-400">[Build] Build successful!</p>
                            <p className="text-blue-400">[AI] Implementing new feature: Special weapon abilities</p>
                            <p className="text-yellow-400">[Build] Starting build #3...</p>
                            <p className="text-yellow-400">[Build] Compiling Java sources...</p>
                            <p className="text-green-400">[Build] Build successful!</p>
                            <p className="text-blue-400">[AI] Optimizing combat mechanics...</p>
                            <p className="opacity-50">...</p>
                          </div>
                        </CardContent>
                      </Card>
                    </TabsContent>
                  </Tabs>
                </>
              ) : (
                <Card className="p-6 flex items-center justify-center">
                  <div className="text-center">
                    <Brain className="h-12 w-12 text-purple-500 mx-auto mb-4" />
                    <h3 className="text-xl font-medium mb-2">Select a Mod to Continue</h3>
                    <p className="text-muted-foreground mb-4">
                      Choose a mod to monitor and manage its continuous development
                    </p>
                    <ModSelector />
                  </div>
                </Card>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

// Using the ModSelector component imported from @/components/mod-selector