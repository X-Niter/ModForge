import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { DatabaseStatus } from "@/components/database-status";
import { Button } from "@/components/ui/button";
import { apiRequest } from "@/lib/queryClient";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AlertCircle, AlertTriangle, RefreshCcw, Database, Save, Server, ShieldCheck } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useToast } from "@/hooks/use-toast";

interface DatabaseStats {
  tables: {
    name: string;
    rowCount: number;
  }[];
  totalRows: number;
  databaseSize: string;
  lastBackup?: string;
}

export default function Settings() {
  const [activeTab, setActiveTab] = useState("general");
  const { toast } = useToast();
  const [isRunningCleanup, setIsRunningCleanup] = useState(false);
  const [isRunningMigration, setIsRunningMigration] = useState(false);

  // Mock database stats - in a real implementation this would come from the API
  const stats: DatabaseStats = {
    tables: [
      { name: "users", rowCount: 1 },
      { name: "mods", rowCount: 0 },
      { name: "builds", rowCount: 0 },
      { name: "mod_files", rowCount: 0 }
    ],
    totalRows: 1,
    databaseSize: "< 1 MB",
    lastBackup: undefined
  };

  const runDatabaseCleanup = async () => {
    try {
      setIsRunningCleanup(true);
      toast({
        title: "Database Cleanup",
        description: "Starting database cleanup...",
      });
      
      // This would be a real API call in production
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      toast({
        title: "Database Cleanup Complete",
        description: "All non-essential data has been cleaned up.",
        variant: "default",
      });
    } catch (error) {
      toast({
        title: "Database Cleanup Failed",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    } finally {
      setIsRunningCleanup(false);
    }
  };

  const runDatabaseMigration = async () => {
    try {
      setIsRunningMigration(true);
      toast({
        title: "Database Migration",
        description: "Starting database migration...",
      });
      
      // This would be a real API call in production
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      toast({
        title: "Database Migration Complete",
        description: "Database schema is up to date.",
        variant: "default",
      });
    } catch (error) {
      toast({
        title: "Database Migration Failed",
        description: error instanceof Error ? error.message : "An unknown error occurred",
        variant: "destructive",
      });
    } finally {
      setIsRunningMigration(false);
    }
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Settings</h1>
        <p className="text-muted-foreground">
          Configure your Minecraft mod development environment
        </p>
      </div>

      <Tabs
        defaultValue="general"
        value={activeTab}
        onValueChange={setActiveTab}
        className="space-y-6"
      >
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="database">Database</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>General Settings</CardTitle>
              <CardDescription>
                Configure basic settings for your mod development environment.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <h3 className="text-lg font-medium">Environment Status</h3>
                <div className="grid gap-2">
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <Server className="h-5 w-5 mr-2 text-muted-foreground" />
                      <span>Application Server</span>
                    </div>
                    <Badge variant="secondary">Running</Badge>
                  </div>
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <Database className="h-5 w-5 mr-2 text-muted-foreground" />
                      <span>Database Connection</span>
                    </div>
                    <Badge variant="secondary">Connected</Badge>
                  </div>
                </div>
              </div>
            </CardContent>
            <CardFooter className="flex justify-between border-t pt-5">
              <Button variant="outline">Reset Defaults</Button>
              <Button>Save Changes</Button>
            </CardFooter>
          </Card>
        </TabsContent>

        <TabsContent value="database" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Database Management</CardTitle>
              <CardDescription>
                Monitor and manage your database connection and data.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <DatabaseStatus />

              <div className="space-y-2 mt-6">
                <h3 className="text-lg font-medium">Database Information</h3>
                <div className="grid gap-2 mt-2">
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <span className="text-sm text-muted-foreground w-32">Total Tables</span>
                    </div>
                    <span>{stats.tables.length}</span>
                  </div>
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <span className="text-sm text-muted-foreground w-32">Total Records</span>
                    </div>
                    <span>{stats.totalRows}</span>
                  </div>
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <span className="text-sm text-muted-foreground w-32">Database Size</span>
                    </div>
                    <span>{stats.databaseSize}</span>
                  </div>
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <span className="text-sm text-muted-foreground w-32">Last Backup</span>
                    </div>
                    <span>{stats.lastBackup || "Never"}</span>
                  </div>
                </div>
              </div>

              <div className="space-y-2 mt-6">
                <h3 className="text-lg font-medium">Table Data</h3>
                <div className="grid gap-2 mt-2">
                  {stats.tables.map(table => (
                    <div key={table.name} className="flex items-center justify-between border p-3 rounded-md">
                      <div className="flex items-center">
                        <span className="text-sm font-medium">{table.name}</span>
                      </div>
                      <span className="text-sm">{table.rowCount} records</span>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
            <CardFooter className="flex justify-between border-t pt-5">
              <Button 
                variant="outline" 
                onClick={runDatabaseCleanup}
                disabled={isRunningCleanup}
              >
                {isRunningCleanup ? (
                  <>
                    <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />
                    Running Cleanup...
                  </>
                ) : (
                  <>
                    <AlertTriangle className="mr-2 h-4 w-4" />
                    Cleanup Database
                  </>
                )}
              </Button>
              <Button
                onClick={runDatabaseMigration}
                disabled={isRunningMigration}
              >
                {isRunningMigration ? (
                  <>
                    <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />
                    Running Migration...
                  </>
                ) : (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    Run Migration
                  </>
                )}
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

        <TabsContent value="security" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Security Settings</CardTitle>
              <CardDescription>
                Manage your security settings and API credentials.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <h3 className="text-lg font-medium">API Credentials</h3>
                <div className="grid gap-2">
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <ShieldCheck className="h-5 w-5 mr-2 text-muted-foreground" />
                      <span>OpenAI API</span>
                    </div>
                    <Badge>Configured</Badge>
                  </div>
                  <div className="flex items-center justify-between border p-3 rounded-md">
                    <div className="flex items-center">
                      <ShieldCheck className="h-5 w-5 mr-2 text-muted-foreground" />
                      <span>GitHub API</span>
                    </div>
                    <Badge variant="outline">Not Configured</Badge>
                  </div>
                </div>
              </div>

              <Separator className="my-4" />

              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <AlertCircle className="h-5 w-5 text-amber-500" />
                  <h3 className="text-lg font-medium">Security Warnings</h3>
                </div>
                <p className="text-sm text-muted-foreground">
                  All API keys are stored securely and are not exposed to client-side code.
                </p>
              </div>
            </CardContent>
            <CardFooter className="border-t pt-5">
              <Button className="w-full">
                <ShieldCheck className="mr-2 h-4 w-4" />
                Update Security Settings
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}