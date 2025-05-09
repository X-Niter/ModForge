
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { 
  BookOpen, 
  Code, 
  GitBranch, 
  Play, 
  RefreshCw, 
  Server, 
  Lightbulb,
  Settings
} from "lucide-react";

export default function DocumentationPage() {
  return (
      <div className="container mx-auto">
        <div className="mb-6">
          <h1 className="text-3xl font-bold tracking-tight mb-2">Documentation</h1>
          <p className="text-muted-foreground">Learn how to use ModForge AI's features effectively.</p>
        </div>

        <Tabs defaultValue="overview" className="space-y-4">
          <TabsList className="w-full md:w-auto">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="idea-generator">Idea Generator</TabsTrigger>
            <TabsTrigger value="code-generator">Code Generator</TabsTrigger>
            <TabsTrigger value="continuous-dev">Continuous Development</TabsTrigger>
            <TabsTrigger value="github">GitHub Integration</TabsTrigger>
            <TabsTrigger value="api">API Usage</TabsTrigger>
          </TabsList>
          
          <TabsContent value="overview" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <BookOpen className="w-5 h-5 mr-2 text-indigo-500" />
                  ModForge AI Platform Overview
                </CardTitle>
                <CardDescription>
                  Understand the core components of the ModForge AI platform
                </CardDescription>
              </CardHeader>
              <CardContent className="prose prose-slate dark:prose-invert max-w-none">
                <p>
                  ModForge AI is an advanced platform for Minecraft mod development that uses AI to automate and enhance the entire mod creation process.
                  It combines idea generation, code development, continuous improvement, and GitHub integration into a seamless workflow.
                </p>
                
                <h3>Core Features</h3>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 not-prose">
                  <Card className="border-indigo-200 dark:border-indigo-900">
                    <CardHeader className="pb-2">
                      <CardTitle className="text-md flex items-center">
                        <Lightbulb className="w-4 h-4 mr-2 text-amber-500" />
                        Idea Generation
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0 text-sm">
                      AI-powered brainstorming for mod concepts based on themes, complexity, and mod loaders.
                    </CardContent>
                  </Card>
                  
                  <Card className="border-indigo-200 dark:border-indigo-900">
                    <CardHeader className="pb-2">
                      <CardTitle className="text-md flex items-center">
                        <Code className="w-4 h-4 mr-2 text-green-500" />
                        Code Generation
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0 text-sm">
                      Produces functional mod code with intelligent architecture and best practices.
                    </CardContent>
                  </Card>
                  
                  <Card className="border-indigo-200 dark:border-indigo-900">
                    <CardHeader className="pb-2">
                      <CardTitle className="text-md flex items-center">
                        <RefreshCw className="w-4 h-4 mr-2 text-blue-500" />
                        Continuous Development
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0 text-sm">
                      Automatic error correction, code improvement, and feature addition over time.
                    </CardContent>
                  </Card>
                  
                  <Card className="border-indigo-200 dark:border-indigo-900">
                    <CardHeader className="pb-2">
                      <CardTitle className="text-md flex items-center">
                        <GitBranch className="w-4 h-4 mr-2 text-purple-500" />
                        GitHub Integration
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0 text-sm">
                      Seamless repository management for version control and sharing.
                    </CardContent>
                  </Card>
                </div>
                
                <h3>Pattern Learning System</h3>
                <p>
                  One of ModForge AI's most powerful features is its pattern learning system. As the platform processes more mod development tasks, it becomes increasingly self-reliant by:
                </p>
                <ul>
                  <li>Storing successful code patterns and solutions</li>
                  <li>Matching new requests against previous patterns</li>
                  <li>Reducing dependency on external AI services over time</li>
                  <li>Becoming more specialized for Minecraft mod development</li>
                </ul>
                
                <p>
                  This approach makes the platform faster, more cost-effective, and increasingly tailored to the specific needs of mod developers.
                </p>
                
                <h3>Getting Started</h3>
                <p>
                  To begin using ModForge AI:
                </p>
                <ol>
                  <li>Generate a mod idea or enter your own concept</li>
                  <li>Choose your preferred mod loader (Forge, Fabric, etc.)</li>
                  <li>Configure the initial project settings</li>
                  <li>Let the platform generate the initial code structure</li>
                  <li>Enable continuous development for ongoing improvements</li>
                  <li>Optionally connect to GitHub for version control</li>
                </ol>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="idea-generator">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Lightbulb className="w-5 h-5 mr-2 text-amber-500" />
                  Idea Generator
                </CardTitle>
                <CardDescription>
                  How to use the AI-powered mod idea generation system
                </CardDescription>
              </CardHeader>
              <CardContent className="prose prose-slate dark:prose-invert max-w-none">
                <p>
                  The Idea Generator helps you brainstorm unique Minecraft mod concepts based on your preferences and requirements.
                </p>
                
                <h3>Using the Idea Generator</h3>
                <ol>
                  <li>Navigate to the Idea Generator page from the main menu</li>
                  <li>Select the complexity level (Simple, Medium, or Complex)</li>
                  <li>Choose your preferred mod loader</li>
                  <li>Optionally provide a theme or specific Minecraft version</li>
                  <li>Add any existing ideas you want the system to build upon</li>
                  <li>Click "Generate Ideas" to receive multiple mod concepts</li>
                </ol>
                
                <h3>Understanding the Results</h3>
                <p>
                  Each generated idea includes:
                </p>
                <ul>
                  <li><strong>Title:</strong> A concise name for the mod</li>
                  <li><strong>Description:</strong> Overview of the mod's concept and purpose</li>
                  <li><strong>Features:</strong> Key functionality the mod would include</li>
                  <li><strong>Complexity:</strong> Estimated development difficulty</li>
                  <li><strong>Development Time:</strong> Rough estimate for implementation</li>
                  <li><strong>Suggested Mod Loader:</strong> Best platform for this mod type</li>
                  <li><strong>Tags:</strong> Categories and themes for the mod</li>
                  <li><strong>Compatibility Notes:</strong> Potential interactions with other mods</li>
                </ul>
                
                <h3>Expanding Ideas</h3>
                <p>
                  You can click the "Expand" button on any idea to get a more detailed breakdown, including:
                </p>
                <ul>
                  <li>Implementation suggestions</li>
                  <li>Technical considerations</li>
                  <li>Potential gameplay mechanics</li>
                  <li>User experience recommendations</li>
                </ul>
                
                <h3>Starting Development</h3>
                <p>
                  Once you find an idea you like, click "Start Development" to:
                </p>
                <ul>
                  <li>Create a new mod project based on the idea</li>
                  <li>Configure initial project parameters</li>
                  <li>Generate the baseline code structure</li>
                  <li>Begin the development process</li>
                </ul>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="code-generator">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Code className="w-5 h-5 mr-2 text-green-500" />
                  Code Generator
                </CardTitle>
                <CardDescription>
                  How to generate, customize, and manage mod code
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="prose prose-slate dark:prose-invert max-w-none">
                  <p>
                    The Code Generator transforms mod concepts into functional code, handling the boilerplate and complex technical details.
                  </p>
                  
                  <h3>Code Generation Process</h3>
                  <ol>
                    <li>Start with an idea from the Idea Generator or input your own concept</li>
                    <li>Select a mod loader (Forge, Fabric, Quilt, etc.)</li>
                    <li>Configure basic project parameters (mod ID, name, version, etc.)</li>
                    <li>Specify additional features you want to include</li>
                    <li>Click "Generate Code" to create the mod structure</li>
                  </ol>
                  
                  <h3>Understanding Generated Code</h3>
                  <p>
                    The code generator creates:
                  </p>
                  <ul>
                    <li>Basic mod infrastructure (main class, registration systems)</li>
                    <li>Config files and project structure</li>
                    <li>Feature implementations based on the mod concept</li>
                    <li>Required dependencies and build configuration</li>
                    <li>Documentation for key components</li>
                  </ul>
                  
                  <h3>Editing and Customizing</h3>
                  <p>
                    You can modify the generated code directly in the platform:
                  </p>
                  <ul>
                    <li>Use the integrated code editor with syntax highlighting</li>
                    <li>Make manual changes to any file</li>
                    <li>Add or modify features with natural language descriptions</li>
                    <li>Get explanations of any code section using the AI assistant</li>
                  </ul>
                  
                  <h3>Compiling and Testing</h3>
                  <p>
                    Test your mod's compile-readiness:
                  </p>
                  <ul>
                    <li>Click "Compile" to check for errors</li>
                    <li>Review error messages with AI-enhanced explanations</li>
                    <li>Use the "Fix Errors" feature to automatically resolve issues</li>
                    <li>Test changes incrementally as you develop</li>
                  </ul>
                  
                  <div className="bg-muted p-4 rounded-md not-prose mt-4">
                    <h4 className="font-medium text-sm mb-2">Pro Tip: Code Enhancement</h4>
                    <p className="text-sm text-muted-foreground">
                      Use the "Enhance Code" feature to improve specific implementations, optimize for performance, or add better documentation to existing code. Just highlight the section and click "Enhance".
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="continuous-dev">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <RefreshCw className="w-5 h-5 mr-2 text-blue-500" />
                  Continuous Development
                </CardTitle>
                <CardDescription>
                  Automated mod improvement and maintenance
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="prose prose-slate dark:prose-invert max-w-none">
                  <p>
                    The Continuous Development system actively improves your mod by fixing errors, adding features, and optimizing code even when you're not actively working on it.
                  </p>
                  
                  <h3>Starting Continuous Development</h3>
                  <ol>
                    <li>Navigate to the Continuous Development page</li>
                    <li>Select a mod project from your list</li>
                    <li>Configure development parameters:
                      <ul>
                        <li>Development frequency (how often changes are made)</li>
                        <li>Feature focus areas (what aspects to improve)</li>
                        <li>Complexity limits (how ambitious changes can be)</li>
                      </ul>
                    </li>
                    <li>Click "Start Continuous Development" to activate the system</li>
                  </ol>
                  
                  <h3>Monitoring Progress</h3>
                  <p>
                    Track your mod's development:
                  </p>
                  <ul>
                    <li>View the development timeline with all changes</li>
                    <li>Check compilation status and error correction history</li>
                    <li>Review added or enhanced features</li>
                    <li>See statistics on development activity and progress</li>
                  </ul>
                  
                  <h3>Managing Development</h3>
                  <p>
                    Control the development process:
                  </p>
                  <ul>
                    <li>Pause development at any time</li>
                    <li>Adjust focus areas as your priorities change</li>
                    <li>Set constraints to guide the AI's decisions</li>
                    <li>Approve or reject proposed changes</li>
                    <li>Add specific feature requests to the development queue</li>
                  </ul>
                  
                  <h3>System Intelligence</h3>
                  <p>
                    The continuous development system becomes more effective over time through:
                  </p>
                  <ul>
                    <li>Learning from successful code patterns and fixes</li>
                    <li>Adapting to your preferences based on approvals/rejections</li>
                    <li>Building specialized knowledge about your mod's architecture</li>
                    <li>Optimizing its approach based on compilation results</li>
                  </ul>
                  
                  <div className="bg-muted p-4 rounded-md not-prose mt-4">
                    <h4 className="font-medium text-sm mb-2">Best Practice</h4>
                    <p className="text-sm text-muted-foreground">
                      For optimal results, regularly review the changes made by the continuous development system. This helps the AI understand your preferences better and improves the quality of future changes.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="github">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <GitBranch className="w-5 h-5 mr-2 text-purple-500" />
                  GitHub Integration
                </CardTitle>
                <CardDescription>
                  Version control and collaboration features
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="prose prose-slate dark:prose-invert max-w-none">
                  <p>
                    ModForge AI integrates with GitHub to provide version control, collaboration, and backup features for your mod projects.
                  </p>
                  
                  <h3>Connecting Your GitHub Account</h3>
                  <ol>
                    <li>Navigate to the GitHub Integration page</li>
                    <li>Click "Connect to GitHub"</li>
                    <li>Authorize the ModForge AI application</li>
                    <li>Set your default repository preferences</li>
                  </ol>
                  
                  <h3>Creating Repositories</h3>
                  <p>
                    For each mod project, you can:
                  </p>
                  <ul>
                    <li>Create a new GitHub repository with proper Minecraft mod structure</li>
                    <li>Configure repository visibility (public or private)</li>
                    <li>Set up initial README, .gitignore, and license files</li>
                    <li>Add standard mod documentation</li>
                  </ul>
                  
                  <h3>Synchronizing Changes</h3>
                  <p>
                    Keep your project in sync:
                  </p>
                  <ul>
                    <li>Manual synchronization with the "Push to GitHub" button</li>
                    <li>Automatic synchronization after continuous development cycles</li>
                    <li>Scheduled commits based on your preferences</li>
                    <li>Configurable commit message formats</li>
                  </ul>
                  
                  <h3>Managing Repositories</h3>
                  <p>
                    Additional repository management features:
                  </p>
                  <ul>
                    <li>View commit history directly in the ModForge AI interface</li>
                    <li>Create and manage branches for different development tracks</li>
                    <li>Set up GitHub Actions workflows for automated testing</li>
                    <li>Configure release processes for mod versions</li>
                  </ul>
                  
                  <h3>Collaboration</h3>
                  <p>
                    Work with other developers:
                  </p>
                  <ul>
                    <li>Share repository access with team members</li>
                    <li>Review changes made by others</li>
                    <li>Merge contributions into the main codebase</li>
                    <li>Manage issues and feature requests</li>
                  </ul>
                  
                  <div className="bg-muted p-4 rounded-md not-prose mt-4">
                    <h4 className="font-medium text-sm mb-2">Security Note</h4>
                    <p className="text-sm text-muted-foreground">
                      ModForge AI requires only the minimum necessary GitHub permissions to manage your mod repositories. You can review and adjust these permissions at any time through your GitHub account settings.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="api">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Server className="w-5 h-5 mr-2 text-red-500" />
                  API Usage &amp; Analytics
                </CardTitle>
                <CardDescription>
                  Understanding the platform's AI efficiency
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="prose prose-slate dark:prose-invert max-w-none">
                  <p>
                    ModForge AI uses a sophisticated pattern learning system to reduce API usage and become more self-reliant over time. This section helps you understand how this works and track efficiency metrics.
                  </p>
                  
                  <h3>How Pattern Learning Works</h3>
                  <p>
                    The pattern learning system:
                  </p>
                  <ul>
                    <li>Stores successful code generation and problem-solving patterns</li>
                    <li>Categorizes patterns by type (code generation, error fixes, features, etc.)</li>
                    <li>Matches new requests against the pattern database</li>
                    <li>Uses direct pattern matches when possible instead of external API calls</li>
                    <li>Continuously updates success rates for each pattern</li>
                  </ul>
                  
                  <h3>Pattern Categories</h3>
                  <p>
                    The system maintains separate pattern databases for:
                  </p>
                  <ul>
                    <li><strong>Code Patterns:</strong> General code generation templates</li>
                    <li><strong>Error Patterns:</strong> Solutions for specific compilation errors</li>
                    <li><strong>Idea Patterns:</strong> Mod concept generation frameworks</li>
                    <li><strong>Feature Patterns:</strong> Implementation approaches for specific features</li>
                    <li><strong>Documentation Patterns:</strong> Templates for code documentation</li>
                  </ul>
                  
                  <h3>Viewing Usage Analytics</h3>
                  <p>
                    Monitor system efficiency:
                  </p>
                  <ul>
                    <li>Overall pattern match rate vs. API calls</li>
                    <li>Estimated cost savings from pattern matches</li>
                    <li>Pattern learning efficiency over time</li>
                    <li>Most frequently used pattern categories</li>
                    <li>Areas with potential for improvement</li>
                  </ul>
                  
                  <h3>System Growth</h3>
                  <p>
                    The pattern learning system becomes more effective as:
                  </p>
                  <ul>
                    <li>More users create mods on the platform</li>
                    <li>More code patterns are successfully compiled</li>
                    <li>More error resolution strategies are validated</li>
                    <li>The variety of mod types and features increases</li>
                  </ul>
                  
                  <div className="bg-muted p-4 rounded-md not-prose mt-4">
                    <h4 className="font-medium text-sm mb-2">System Benefits</h4>
                    <p className="text-sm text-muted-foreground">
                      As the pattern learning system matures, you'll notice faster response times, more consistent results, and greater specialization for Minecraft-specific development patterns. This represents the system becoming more efficient and self-reliant.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
  );
}