import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { Link } from "wouter";
import { LucideCode, LucideFileCheck, LucideGithub, LucideRefreshCw } from "lucide-react";
import { DashboardOverview } from "@/components/dashboard-overview";

export default function HomePage() {
  const { toast } = useToast();

  const handleDownloadPlugin = () => {
    toast({
      title: "Plugin download started",
      description: "The IntelliJ plugin download has started.",
    });
  };

  return (
    <div className="container mx-auto py-10">
      <div className="flex flex-col md:flex-row gap-8 items-center mb-10">
        <div className="flex-1">
          <h1 className="text-4xl font-bold mb-4 bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-violet-600">
            ModForge
          </h1>
          <p className="text-xl text-muted-foreground mb-6">
            AI-powered Minecraft mod development platform for automated creation, continuous improvement, and seamless collaboration.
          </p>
          <div className="flex flex-wrap gap-3 mb-6">
            <Badge variant="outline" className="text-sm px-3 py-1 border-blue-400">AI-Driven Development</Badge>
            <Badge variant="outline" className="text-sm px-3 py-1 border-green-400">Multi-Loader Support</Badge>
            <Badge variant="outline" className="text-sm px-3 py-1 border-amber-400">GitHub Integration</Badge>
            <Badge variant="outline" className="text-sm px-3 py-1 border-violet-400">24/7 Automation</Badge>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link href="/idea-generator">
              <Button size="lg" className="bg-gradient-to-r from-blue-600 to-violet-600 hover:from-blue-700 hover:to-violet-700">
                Start Creating
              </Button>
            </Link>
            <Button size="lg" variant="outline" onClick={handleDownloadPlugin}>
              Download IntelliJ Plugin
            </Button>
            <a href="https://github.com/modforge-dev/modforge" target="_blank" rel="noopener noreferrer">
              <Button size="lg" variant="ghost">
                <LucideGithub className="mr-2 h-5 w-5" />
                GitHub
              </Button>
            </a>
          </div>
        </div>
        <div className="flex-1 rounded-md overflow-hidden shadow-xl">
          <img src="/img/hero-image.png" alt="ModForge Platform" className="w-full h-auto" />
        </div>
      </div>

      {/* Dashboard Overview for logged-in users */}
      <div className="mb-10">
        <DashboardOverview />
      </div>
      
      <Tabs defaultValue="features" className="w-full mb-10">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="features">Key Features</TabsTrigger>
          <TabsTrigger value="getting-started">Getting Started</TabsTrigger>
          <TabsTrigger value="integrations">Integrations</TabsTrigger>
          <TabsTrigger value="faq">FAQ</TabsTrigger>
        </TabsList>
        
        <TabsContent value="features" className="py-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <FeatureCard 
              icon={<LucideCode className="h-10 w-10 text-blue-500" />}
              title="AI-Powered Development"
              description="Generate mod code, fix errors, and create documentation using advanced AI models that understand Minecraft modding."
            />
            <FeatureCard 
              icon={<LucideRefreshCw className="h-10 w-10 text-green-500" />}
              title="Continuous Improvement"
              description="The system monitors your projects 24/7, automatically fixing issues and suggesting optimizations."
            />
            <FeatureCard 
              icon={<LucideGithub className="h-10 w-10 text-gray-700" />}
              title="GitHub Integration"
              description="Seamless GitHub workflow with automated PRs, issue responses, and version control management."
            />
            <FeatureCard 
              icon={<LucideFileCheck className="h-10 w-10 text-amber-500" />}
              title="Multi-Loader Support"
              description="Create mods for Forge, Fabric, and Quilt with a unified codebase using Architectury."
            />
            <FeatureCard 
              title="Idea Generation"
              description="Turn concepts into detailed mod specifications with AI-powered idea expansion and refinement."
            />
            <FeatureCard 
              title="IntelliJ Integration"
              description="Direct IDE integration with our IntelliJ plugin for seamless workflow between your IDE and ModForge."
            />
          </div>
        </TabsContent>
        
        <TabsContent value="getting-started" className="space-y-4">
          <h3 className="text-xl font-semibold">Getting Started with ModForge</h3>
          <ol className="list-decimal pl-5 space-y-3">
            <li>Create an account or sign in</li>
            <li>Use the Idea Generator to develop your mod concept</li>
            <li>Generate the initial codebase</li>
            <li>Download the IntelliJ plugin for direct IDE integration</li>
            <li>Push your project to GitHub for continuous development</li>
            <li>Watch as ModForge enhances your mod automatically</li>
          </ol>
          <Link href="/documentation">
            <Button className="mt-4">View Complete Documentation</Button>
          </Link>
        </TabsContent>
        
        <TabsContent value="integrations" className="space-y-4">
          <h3 className="text-xl font-semibold">ModForge Integrations</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle>IntelliJ IDEA</CardTitle>
              </CardHeader>
              <CardContent>
                <p>Our IntelliJ plugin provides direct access to ModForge features without leaving your IDE.</p>
              </CardContent>
              <CardFooter>
                <Button variant="outline" onClick={handleDownloadPlugin}>Download Plugin</Button>
              </CardFooter>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>GitHub</CardTitle>
              </CardHeader>
              <CardContent>
                <p>Automatic PR creation, issue management, and GitHub Actions integration.</p>
              </CardContent>
              <CardFooter>
                <Link href="/github-integration">
                  <Button variant="outline">Learn More</Button>
                </Link>
              </CardFooter>
            </Card>
          </div>
        </TabsContent>
        
        <TabsContent value="faq" className="space-y-6">
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold">Do I need to know programming to use ModForge?</h3>
              <p className="text-muted-foreground">No! ModForge is designed to be accessible to users with minimal programming experience. Our AI will generate code based on your descriptions.</p>
            </div>
            <div>
              <h3 className="text-lg font-semibold">Which Minecraft versions are supported?</h3>
              <p className="text-muted-foreground">ModForge currently supports Minecraft 1.16.5 through 1.19.4, with plans to expand to newer versions soon.</p>
            </div>
            <div>
              <h3 className="text-lg font-semibold">Is ModForge free to use?</h3>
              <p className="text-muted-foreground">ModForge offers both free and premium tiers. The free tier includes basic mod generation and GitHub integration, while premium unlocks continuous development and advanced features.</p>
            </div>
            <div>
              <h3 className="text-lg font-semibold">Can I use ModForge with existing projects?</h3>
              <p className="text-muted-foreground">Yes! You can import existing mod projects into ModForge, and our AI will analyze your code to understand your project structure.</p>
            </div>
          </div>
          <Link href="/faq">
            <Button variant="link">View All FAQs</Button>
          </Link>
        </TabsContent>
      </Tabs>

      <section className="my-12 py-8 px-6 bg-muted rounded-lg">
        <h2 className="text-2xl font-bold mb-4 text-center">Ready to revolutionize your Minecraft modding workflow?</h2>
        <p className="text-center text-muted-foreground mb-6">Create, improve, and publish your mods faster than ever before with AI-powered assistance.</p>
        <div className="flex justify-center">
          <Link href="/idea-generator">
            <Button size="lg" className="bg-gradient-to-r from-blue-600 to-violet-600 hover:from-blue-700 hover:to-violet-700">
              Get Started Now
            </Button>
          </Link>
        </div>
      </section>
    </div>
  );
}

function FeatureCard({ icon, title, description }: { icon?: React.ReactNode, title: string, description: string }) {
  return (
    <Card>
      <CardHeader>
        {icon && <div className="mb-2">{icon}</div>}
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <CardDescription className="text-sm">{description}</CardDescription>
      </CardContent>
    </Card>
  );
}