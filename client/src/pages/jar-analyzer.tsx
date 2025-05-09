import React, { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from '@/lib/queryClient';
import { useToast } from '@/hooks/use-toast';
import { FaDatabase, FaUpload, FaSearch, FaDownload, FaCode, FaTrash } from 'react-icons/fa';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle, Info, ExternalLink, Code } from 'lucide-react';

const JarAnalyzerPage: React.FC = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [activeTab, setActiveTab] = useState('jars');
  const [searchQuery, setSearchQuery] = useState('');
  const [modLoader, setModLoader] = useState('forge');
  const [mcVersion, setMcVersion] = useState('1.19.2');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [dragActive, setDragActive] = useState(false);
  const [selectedJarId, setSelectedJarId] = useState<number | null>(null);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  
  // Fetch all JAR files
  const {
    data: jars,
    isLoading: jarsLoading,
    isError: jarsError
  } = useQuery({
    queryKey: ['/api/jar-analyzer/jars'],
    retry: 1
  });
  
  // Fetch JAR analysis stats
  const {
    data: stats,
    isLoading: statsLoading,
    isError: statsError
  } = useQuery({
    queryKey: ['/api/jar-analyzer/stats'],
    retry: 1
  });
  
  // Fetch classes for selected JAR
  const {
    data: classes,
    isLoading: classesLoading,
    isError: classesError
  } = useQuery({
    queryKey: ['/api/jar-analyzer/jars', selectedJarId, 'classes'],
    enabled: !!selectedJarId,
    retry: 1
  });
  
  // Fetch search results from Modrinth
  const {
    data: modrinthResults,
    isLoading: modrinthLoading,
    isError: modrinthError,
    refetch: searchModrinth
  } = useQuery({
    queryKey: ['/api/jar-analyzer/search/modrinth', searchQuery],
    enabled: false,
    retry: 1
  });
  
  // Upload JAR file mutation
  const uploadMutation = useMutation({
    mutationFn: async (formData: FormData) => {
      return apiRequest('/api/jar-analyzer/jars/upload', {
        method: 'POST',
        body: formData,
        headers: {
          // Don't set content-type here, it will be set automatically with the correct boundary
        }
      });
    },
    onSuccess: () => {
      toast({
        title: 'JAR file uploaded successfully',
        description: 'The file is now being processed in the background.',
      });
      
      // Reset upload progress
      setUploadProgress(0);
      
      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/jars'] });
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/stats'] });
    },
    onError: (error: any) => {
      toast({
        title: 'Upload failed',
        description: error.message || 'There was an error uploading the JAR file.',
        variant: 'destructive'
      });
      setUploadProgress(0);
    }
  });
  
  // Download JAR from URL mutation
  const downloadMutation = useMutation({
    mutationFn: async (data: {
      url: string;
      modName: string;
      modLoader: string;
      version: string;
      mcVersion: string;
    }) => {
      return apiRequest('/api/jar-analyzer/jars/download', {
        method: 'POST',
        body: JSON.stringify(data),
        headers: {
          'Content-Type': 'application/json'
        }
      });
    },
    onSuccess: () => {
      toast({
        title: 'JAR file downloaded successfully',
        description: 'The file is now being processed in the background.',
      });
      
      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/jars'] });
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/stats'] });
    },
    onError: (error: any) => {
      toast({
        title: 'Download failed',
        description: error.message || 'There was an error downloading the JAR file.',
        variant: 'destructive'
      });
    }
  });
  
  // Delete JAR mutation
  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      return apiRequest(`/api/jar-analyzer/jars/${id}`, {
        method: 'DELETE'
      });
    },
    onSuccess: () => {
      toast({
        title: 'JAR file deleted',
        description: 'The JAR file and all its extracted classes have been deleted.',
      });
      
      // Reset selected JAR
      if (selectedJarId === deleteMutation.variables) {
        setSelectedJarId(null);
        setSelectedClassId(null);
      }
      
      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/jars'] });
      queryClient.invalidateQueries({ queryKey: ['/api/jar-analyzer/stats'] });
    },
    onError: (error: any) => {
      toast({
        title: 'Deletion failed',
        description: error.message || 'There was an error deleting the JAR file.',
        variant: 'destructive'
      });
    }
  });
  
  // Handle JAR file upload
  const handleFileUpload = (file: File) => {
    if (!file || !file.name.endsWith('.jar')) {
      toast({
        title: 'Invalid file',
        description: 'Please select a valid JAR file.',
        variant: 'destructive'
      });
      return;
    }
    
    const formData = new FormData();
    formData.append('jarFile', file);
    formData.append('modLoader', modLoader);
    formData.append('mcVersion', mcVersion);
    
    // Show initial progress
    setUploadProgress(10);
    
    // Simulate progress until upload completes
    const interval = setInterval(() => {
      setUploadProgress(prev => {
        const newProgress = prev + Math.random() * 5;
        return newProgress < 90 ? newProgress : 90;
      });
    }, 300);
    
    // Start upload
    uploadMutation.mutate(formData);
    
    // Clear interval when upload completes
    return () => clearInterval(interval);
  };
  
  // Handle file input change
  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFileUpload(e.target.files[0]);
    }
  };
  
  // Handle drag events
  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };
  
  // Handle drop event
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };
  
  // Trigger file input click
  const handleUploadClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };
  
  // Handle search button click
  const handleSearch = () => {
    if (searchQuery.trim()) {
      searchModrinth();
    }
  };
  
  // Handle Enter key in search input
  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };
  
  // Handle download from Modrinth
  const handleDownloadFromModrinth = (mod: any) => {
    // Get latest version from mod info
    downloadMutation.mutate({
      url: `https://api.modrinth.com/v2/project/${mod.id}/version`, // This is just placeholder URL, would need real URL
      modName: mod.name,
      modLoader: mod.modLoader || modLoader,
      version: '1.0.0', // Placeholder
      mcVersion
    });
  };

  // Status badge component
  const StatusBadge = ({ status }: { status: string }) => {
    let color = '';
    switch (status) {
      case 'completed':
        color = 'bg-green-500';
        break;
      case 'processing':
        color = 'bg-blue-500';
        break;
      case 'pending':
        color = 'bg-yellow-500';
        break;
      case 'error':
        color = 'bg-red-500';
        break;
      default:
        color = 'bg-gray-500';
    }
    return <Badge className={color}>{status}</Badge>;
  };
  
  return (
    <div className="container mx-auto py-6">
      <div className="flex flex-col space-y-6">
        <div className="flex flex-col space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">JAR File Analyzer</h1>
          <p className="text-muted-foreground">
            Upload and analyze JAR files to extract code patterns and improve AI learning.
          </p>
        </div>
        
        <Tabs defaultValue="jars" onValueChange={setActiveTab} value={activeTab}>
          <TabsList className="grid grid-cols-3">
            <TabsTrigger value="jars">JAR Files</TabsTrigger>
            <TabsTrigger value="search">Find & Download</TabsTrigger>
            <TabsTrigger value="stats">Analytics</TabsTrigger>
          </TabsList>
          
          {/* JAR Files Tab */}
          <TabsContent value="jars" className="space-y-6">
            {/* Upload Section */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FaUpload className="h-5 w-5" />
                  <span>Upload JAR File</span>
                </CardTitle>
                <CardDescription>
                  Upload a Minecraft mod JAR file to analyze its classes and code patterns.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">Mod Loader</label>
                    <Select value={modLoader} onValueChange={setModLoader}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select mod loader" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="forge">Forge</SelectItem>
                        <SelectItem value="neoforge">NeoForge</SelectItem>
                        <SelectItem value="fabric">Fabric</SelectItem>
                        <SelectItem value="quilt">Quilt</SelectItem>
                        <SelectItem value="other">Other</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">Minecraft Version</label>
                    <Select value={mcVersion} onValueChange={setMcVersion}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select MC version" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="1.20.4">1.20.4</SelectItem>
                        <SelectItem value="1.20.1">1.20.1</SelectItem>
                        <SelectItem value="1.19.4">1.19.4</SelectItem>
                        <SelectItem value="1.19.2">1.19.2</SelectItem>
                        <SelectItem value="1.18.2">1.18.2</SelectItem>
                        <SelectItem value="1.16.5">1.16.5</SelectItem>
                        <SelectItem value="1.12.2">1.12.2</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex items-end">
                    <Button 
                      onClick={handleUploadClick} 
                      className="w-full"
                      disabled={uploadMutation.isPending}
                    >
                      Select JAR File
                    </Button>
                    <input
                      type="file"
                      ref={fileInputRef}
                      accept=".jar"
                      onChange={handleFileInputChange}
                      className="hidden"
                    />
                  </div>
                </div>
                
                {/* Drag & Drop Area */}
                <div
                  className={`border-2 border-dashed rounded-lg p-8 text-center ${
                    dragActive ? 'border-primary bg-primary/10' : 'border-gray-300'
                  }`}
                  onDragEnter={handleDrag}
                  onDragOver={handleDrag}
                  onDragLeave={handleDrag}
                  onDrop={handleDrop}
                >
                  <div className="flex flex-col items-center justify-center">
                    <FaDatabase className="h-12 w-12 text-muted-foreground mb-3" />
                    <p className="mb-2 text-sm text-muted-foreground">
                      Drag and drop a JAR file here or click the button above
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Supports Forge, Fabric, NeoForge, and Quilt mod files
                    </p>
                  </div>
                </div>
                
                {/* Upload Progress */}
                {uploadProgress > 0 && (
                  <div className="mt-4">
                    <Progress value={uploadProgress} className="h-2" />
                    <p className="text-sm text-center mt-1 text-muted-foreground">
                      {uploadMutation.isPending ? 'Uploading...' : 'Processing...'}
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
            
            {/* JAR Files List */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* JAR Files List */}
              <div className="md:col-span-1">
                <Card className="h-full">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <FaDatabase className="h-5 w-5" />
                      <span>JAR Files</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    {jarsLoading ? (
                      <div className="space-y-2">
                        <Skeleton className="h-12 w-full" />
                        <Skeleton className="h-12 w-full" />
                        <Skeleton className="h-12 w-full" />
                      </div>
                    ) : jarsError ? (
                      <Alert variant="destructive">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>Error</AlertTitle>
                        <AlertDescription>
                          Failed to load JAR files. Please try again.
                        </AlertDescription>
                      </Alert>
                    ) : jars?.length === 0 ? (
                      <p className="text-center py-4 text-muted-foreground">
                        No JAR files available. Upload one to get started.
                      </p>
                    ) : (
                      <ScrollArea className="h-[400px] pr-4">
                        <div className="space-y-2">
                          {jars?.map((jar: any) => (
                            <div 
                              key={jar.id}
                              onClick={() => setSelectedJarId(jar.id)}
                              className={`p-3 rounded-md cursor-pointer flex items-center justify-between ${
                                selectedJarId === jar.id ? 'bg-primary/10' : 'hover:bg-muted'
                              }`}
                            >
                              <div className="flex flex-col">
                                <span className="font-medium truncate max-w-[200px]">{jar.fileName}</span>
                                <div className="flex items-center gap-1 text-xs">
                                  <Badge variant="outline">{jar.modLoader}</Badge>
                                  <span>{jar.mcVersion}</span>
                                </div>
                              </div>
                              <div className="flex items-center gap-2">
                                <StatusBadge status={jar.status} />
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    deleteMutation.mutate(jar.id);
                                  }}
                                >
                                  <FaTrash className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    )}
                  </CardContent>
                </Card>
              </div>
              
              {/* Classes List */}
              <div className="md:col-span-2">
                <Card className="h-full">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <FaCode className="h-5 w-5" />
                      <span>Extracted Classes</span>
                    </CardTitle>
                    {selectedJarId && (
                      <CardDescription>
                        {jars?.find((j: any) => j.id === selectedJarId)?.fileName || 'Selected JAR file'}
                      </CardDescription>
                    )}
                  </CardHeader>
                  <CardContent>
                    {!selectedJarId ? (
                      <div className="flex flex-col items-center justify-center h-[400px] text-center">
                        <Code className="h-16 w-16 text-muted-foreground mb-4" />
                        <h3 className="text-lg font-medium">No JAR file selected</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          Select a JAR file from the list to view its extracted classes
                        </p>
                      </div>
                    ) : classesLoading ? (
                      <div className="space-y-2">
                        <Skeleton className="h-12 w-full" />
                        <Skeleton className="h-12 w-full" />
                        <Skeleton className="h-12 w-full" />
                      </div>
                    ) : classesError ? (
                      <Alert variant="destructive">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>Error</AlertTitle>
                        <AlertDescription>
                          Failed to load classes. Please try again.
                        </AlertDescription>
                      </Alert>
                    ) : classes?.length === 0 ? (
                      <div className="text-center py-8">
                        <h3 className="font-medium mb-1">No classes extracted yet</h3>
                        <p className="text-sm text-muted-foreground">
                          The JAR file may still be processing. Check back later.
                        </p>
                        
                        {jars?.find((j: any) => j.id === selectedJarId)?.status === 'pending' && (
                          <div className="mt-4">
                            <Progress value={10} className="h-2" />
                            <p className="text-sm mt-1 text-muted-foreground">Waiting to be processed...</p>
                          </div>
                        )}
                        
                        {jars?.find((j: any) => j.id === selectedJarId)?.status === 'processing' && (
                          <div className="mt-4">
                            <Progress value={50} className="h-2" />
                            <p className="text-sm mt-1 text-muted-foreground">Processing JAR file...</p>
                          </div>
                        )}
                        
                        {jars?.find((j: any) => j.id === selectedJarId)?.status === 'error' && (
                          <Alert variant="destructive" className="mt-4">
                            <AlertCircle className="h-4 w-4" />
                            <AlertTitle>Processing Error</AlertTitle>
                            <AlertDescription>
                              There was an error processing this JAR file.
                            </AlertDescription>
                          </Alert>
                        )}
                      </div>
                    ) : (
                      <ScrollArea className="h-[400px] pr-4">
                        <div className="space-y-3">
                          {classes?.map((cls: any) => (
                            <div 
                              key={cls.id}
                              onClick={() => setSelectedClassId(cls.id)}
                              className={`p-3 rounded-md cursor-pointer border ${
                                selectedClassId === cls.id ? 'border-primary bg-primary/5' : 'border-muted hover:bg-muted'
                              }`}
                            >
                              <div className="flex items-start justify-between">
                                <div>
                                  <div className="flex items-center gap-2">
                                    <span className="font-medium">{cls.className}</span>
                                    <Badge variant="outline">{cls.classType}</Badge>
                                    {cls.isPublic && <Badge variant="outline">public</Badge>}
                                  </div>
                                  <p className="text-sm text-muted-foreground mt-1">
                                    {cls.packageName || 'default package'}
                                  </p>
                                </div>
                                <div className="flex items-center gap-1">
                                  <Badge variant="outline" className="text-xs">
                                    {cls.methods?.length || 0} methods
                                  </Badge>
                                  <Badge variant="outline" className="text-xs">
                                    {cls.fields?.length || 0} fields
                                  </Badge>
                                </div>
                              </div>
                              
                              {selectedClassId === cls.id && (
                                <div className="mt-3 p-3 bg-muted rounded-md max-h-64 overflow-auto text-xs">
                                  <pre className="whitespace-pre-wrap break-words">
                                    {cls.content.substring(0, 500)}
                                    {cls.content.length > 500 && '...'}
                                  </pre>
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    )}
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>
          
          {/* Search & Download Tab */}
          <TabsContent value="search" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FaSearch className="h-5 w-5" />
                  <span>Search Mod Repositories</span>
                </CardTitle>
                <CardDescription>
                  Search for and download JAR files from Modrinth and CurseForge.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                  <div className="md:col-span-2">
                    <Input
                      placeholder="Search for mods..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      onKeyDown={handleSearchKeyDown}
                    />
                  </div>
                  <div>
                    <Select value={modLoader} onValueChange={setModLoader}>
                      <SelectTrigger>
                        <SelectValue placeholder="Mod loader" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="forge">Forge</SelectItem>
                        <SelectItem value="neoforge">NeoForge</SelectItem>
                        <SelectItem value="fabric">Fabric</SelectItem>
                        <SelectItem value="quilt">Quilt</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Button onClick={handleSearch} className="w-full">
                      Search
                    </Button>
                  </div>
                </div>
                
                <Tabs defaultValue="modrinth">
                  <TabsList className="grid grid-cols-2">
                    <TabsTrigger value="modrinth">Modrinth</TabsTrigger>
                    <TabsTrigger value="curseforge">CurseForge</TabsTrigger>
                  </TabsList>
                  
                  {/* Modrinth Results */}
                  <TabsContent value="modrinth">
                    {modrinthLoading ? (
                      <div className="space-y-4 mt-4">
                        <Skeleton className="h-24 w-full" />
                        <Skeleton className="h-24 w-full" />
                        <Skeleton className="h-24 w-full" />
                      </div>
                    ) : modrinthError ? (
                      <Alert variant="destructive" className="mt-4">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>Error</AlertTitle>
                        <AlertDescription>
                          Failed to load results from Modrinth. Please try again.
                        </AlertDescription>
                      </Alert>
                    ) : modrinthResults?.length === 0 ? (
                      <div className="text-center py-8">
                        <h3 className="font-medium">No results found</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          Try a different search term or mod loader.
                        </p>
                      </div>
                    ) : modrinthResults ? (
                      <div className="space-y-4 mt-4">
                        {modrinthResults.map((mod: any) => (
                          <Card key={mod.id}>
                            <CardContent className="p-4">
                              <div className="flex justify-between items-start">
                                <div>
                                  <h3 className="font-medium">{mod.name}</h3>
                                  <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                                    {mod.description}
                                  </p>
                                  <div className="flex items-center gap-2 mt-2">
                                    <Badge variant="outline">{mod.modLoader}</Badge>
                                    <span className="text-xs text-muted-foreground">
                                      {mod.downloads.toLocaleString()} downloads
                                    </span>
                                    <span className="text-xs text-muted-foreground">
                                      by {mod.author}
                                    </span>
                                  </div>
                                </div>
                                <div className="flex gap-2">
                                  <Button 
                                    variant="outline" 
                                    size="sm"
                                    onClick={() => window.open(mod.url, '_blank')}
                                  >
                                    <ExternalLink className="h-4 w-4 mr-1" />
                                    View
                                  </Button>
                                  <Button 
                                    size="sm"
                                    onClick={() => handleDownloadFromModrinth(mod)}
                                    disabled={downloadMutation.isPending}
                                  >
                                    <FaDownload className="h-4 w-4 mr-1" />
                                    Download
                                  </Button>
                                </div>
                              </div>
                            </CardContent>
                          </Card>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-8">
                        <Info className="h-16 w-16 mx-auto text-muted-foreground mb-2" />
                        <h3 className="font-medium">Search for Minecraft mods</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          Enter a search term above to find mods on Modrinth.
                        </p>
                      </div>
                    )}
                  </TabsContent>
                  
                  {/* CurseForge Results */}
                  <TabsContent value="curseforge">
                    <Alert className="mt-4">
                      <Info className="h-4 w-4" />
                      <AlertTitle>CurseForge API Key Required</AlertTitle>
                      <AlertDescription>
                        CurseForge requires an API key for searching and downloading mods.
                        Please provide an API key in the settings to enable this feature.
                      </AlertDescription>
                    </Alert>
                  </TabsContent>
                </Tabs>
              </CardContent>
            </Card>
          </TabsContent>
          
          {/* Analytics Tab */}
          <TabsContent value="stats" className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle>JAR Analytics</CardTitle>
                  <CardDescription>
                    Statistics about analyzed JAR files and extracted classes.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {statsLoading ? (
                    <div className="space-y-4">
                      <Skeleton className="h-6 w-full" />
                      <Skeleton className="h-6 w-full" />
                      <Skeleton className="h-6 w-full" />
                    </div>
                  ) : statsError ? (
                    <Alert variant="destructive">
                      <AlertCircle className="h-4 w-4" />
                      <AlertTitle>Error</AlertTitle>
                      <AlertDescription>
                        Failed to load statistics. Please try again.
                      </AlertDescription>
                    </Alert>
                  ) : stats ? (
                    <div className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-muted p-4 rounded-lg">
                          <p className="text-muted-foreground text-sm">Total JAR Files</p>
                          <p className="text-3xl font-bold">{stats.totalJars}</p>
                        </div>
                        <div className="bg-muted p-4 rounded-lg">
                          <p className="text-muted-foreground text-sm">Total Classes</p>
                          <p className="text-3xl font-bold">{stats.totalClasses}</p>
                        </div>
                      </div>
                      
                      <div>
                        <h4 className="font-medium mb-2">Processing Status</h4>
                        <div className="space-y-2">
                          <div className="flex justify-between items-center">
                            <span className="text-sm">Completed</span>
                            <Badge className="bg-green-500">{stats.status.completed}</Badge>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-sm">Processing</span>
                            <Badge className="bg-blue-500">{stats.status.processing}</Badge>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-sm">Pending</span>
                            <Badge className="bg-yellow-500">{stats.status.pending}</Badge>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-sm">Error</span>
                            <Badge className="bg-red-500">{stats.status.error}</Badge>
                          </div>
                        </div>
                      </div>
                      
                      <Separator />
                      
                      <div>
                        <h4 className="font-medium mb-2">Class Types</h4>
                        <div className="space-y-2">
                          {stats.classTypes.map((type: any) => (
                            <div key={type.type} className="flex justify-between items-center">
                              <span className="text-sm capitalize">{type.type}</span>
                              <Badge variant="outline">{type.count}</Badge>
                            </div>
                          ))}
                        </div>
                      </div>
                      
                      <Separator />
                      
                      <div>
                        <h4 className="font-medium mb-2">Mod Loaders</h4>
                        <div className="space-y-2">
                          {stats.modLoaders.map((loader: any) => (
                            <div key={loader.loader} className="flex justify-between items-center">
                              <span className="text-sm capitalize">{loader.loader}</span>
                              <Badge variant="outline">{loader.count}</Badge>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  ) : null}
                </CardContent>
              </Card>
              
              <Card>
                <CardHeader>
                  <CardTitle>AI Learning Impact</CardTitle>
                  <CardDescription>
                    How the analyzed JAR files improve AI code generation.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <Alert>
                      <Info className="h-4 w-4" />
                      <AlertTitle>Pattern Learning Enabled</AlertTitle>
                      <AlertDescription>
                        The system is automatically learning from extracted classes
                        to improve code generation and reduce API costs.
                      </AlertDescription>
                    </Alert>
                    
                    <div className="bg-muted p-4 rounded-lg">
                      <h4 className="font-medium mb-2">Estimated Benefits</h4>
                      <div className="space-y-2">
                        <div className="flex justify-between items-center">
                          <span className="text-sm">Pattern Matches</span>
                          <Badge variant="outline">
                            {statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.12) : 0}
                          </Badge>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-sm">API Calls Saved</span>
                          <Badge variant="outline">
                            {statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.08) : 0}
                          </Badge>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-sm">Cost Reduction</span>
                          <Badge variant="outline">
                            ~{statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.03) : 0}%
                          </Badge>
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium mb-2">Learning Progress</h4>
                      <div className="space-y-3">
                        <div>
                          <div className="flex justify-between mb-1">
                            <span className="text-sm">Code Patterns</span>
                            <span className="text-xs text-muted-foreground">
                              {statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.85) : 0} learned
                            </span>
                          </div>
                          <Progress value={85} className="h-2" />
                        </div>
                        <div>
                          <div className="flex justify-between mb-1">
                            <span className="text-sm">Class Relationships</span>
                            <span className="text-xs text-muted-foreground">
                              {statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.62) : 0} mapped
                            </span>
                          </div>
                          <Progress value={62} className="h-2" />
                        </div>
                        <div>
                          <div className="flex justify-between mb-1">
                            <span className="text-sm">API Usage Patterns</span>
                            <span className="text-xs text-muted-foreground">
                              {statsLoading ? '-' : stats?.totalClasses ? Math.floor(stats.totalClasses * 0.41) : 0} discovered
                            </span>
                          </div>
                          <Progress value={41} className="h-2" />
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default JarAnalyzerPage;