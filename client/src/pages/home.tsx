import { Link } from "wouter";
import { Layout } from "@/components/ui/layout";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  ArrowRight,
  Code,
  Lightbulb,
  GitBranch,
  RefreshCw,
  BarChart3,
  Cloud,
  Zap,
  Bot,
} from "lucide-react";

export default function Home() {
  return (
    <Layout>
      {/* Hero Section */}
      <section className="relative">
        {/* Background with gradient and pattern */}
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 z-0" />
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj48Y2lyY2xlIHN0cm9rZT0iIzgwODBmZiIgc3Ryb2tlLW9wYWNpdHk9Ii4xIiBjeD0iMSIgY3k9IjEiIHI9IjEiLz48L2c+PC9zdmc+')] z-0 opacity-40" />
        
        <div className="relative z-10 container px-4 py-16 md:py-24">
          <div className="max-w-4xl mx-auto text-center">
            <h1 className="text-4xl md:text-6xl font-bold tracking-tight mb-6">
              <span className="bg-gradient-to-r from-indigo-500 to-purple-600 bg-clip-text text-transparent">AI-Powered</span> Minecraft Mod Development
            </h1>
            <p className="text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              Create sophisticated Minecraft mods with continuous AI-driven development, zero manual coding required. From concept to deployment, automatically.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link href="/idea-generator">
                <Button size="lg" className="bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700">
                  Start Creating <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
              <Link href="/documentation">
                <Button size="lg" variant="outline">
                  Learn More
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Cards */}
      <section className="container px-4 py-16 relative z-10">
        <h2 className="text-3xl font-bold tracking-tight mb-8 text-center">
          Core Features
        </h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-amber-500 to-amber-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <Lightbulb className="h-6 w-6 text-white" />
              </div>
              <CardTitle>Idea Generation</CardTitle>
              <CardDescription>
                AI-powered brainstorming for mod concepts based on themes and complexity
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Generate unique mod ideas with detailed features, complexity estimates, and compatibility notes. Find inspiration with just a few clicks.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/idea-generator">
                <Button variant="ghost" className="text-amber-500 hover:text-amber-600 p-0">
                  Try Idea Generator <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>

          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-green-500 to-green-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <Code className="h-6 w-6 text-white" />
              </div>
              <CardTitle>Code Generation</CardTitle>
              <CardDescription>
                Automatically generate working mod code for all major mod loaders
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Transform ideas into functional code with smart architecture and best practices. Supports Forge, Fabric, and other mod loaders.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/code-generator">
                <Button variant="ghost" className="text-green-500 hover:text-green-600 p-0">
                  Generate Code <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>

          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-blue-500 to-blue-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <RefreshCw className="h-6 w-6 text-white" />
              </div>
              <CardTitle>Continuous Development</CardTitle>
              <CardDescription>
                24/7 automatic error correction, improvements and feature additions
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Set your mod to continuously evolve with AI-driven development that operates around the clock, even when you're not actively working.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/continuous-development">
                <Button variant="ghost" className="text-blue-500 hover:text-blue-600 p-0">
                  Enable Continuous Dev <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>

          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-purple-500 to-purple-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <GitBranch className="h-6 w-6 text-white" />
              </div>
              <CardTitle>GitHub Integration</CardTitle>
              <CardDescription>
                Seamless version control and collaboration
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Connect your GitHub account to automatically manage repositories, track changes, and collaborate with other developers.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/github-integration">
                <Button variant="ghost" className="text-purple-500 hover:text-purple-600 p-0">
                  Connect GitHub <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>

          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-rose-500 to-rose-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <Bot className="h-6 w-6 text-white" />
              </div>
              <CardTitle>Self-Learning System</CardTitle>
              <CardDescription>
                Pattern recognition that improves over time
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Our AI system becomes more efficient by learning from successful patterns, reducing API usage and becoming increasingly specialized for Minecraft.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/documentation#pattern-learning">
                <Button variant="ghost" className="text-rose-500 hover:text-rose-600 p-0">
                  Learn How It Works <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>

          <Card className="border-indigo-200 dark:border-indigo-900/40 bg-gradient-to-br from-background to-background/80">
            <CardHeader>
              <div className="bg-gradient-to-r from-cyan-500 to-cyan-600 w-12 h-12 rounded-lg flex items-center justify-center mb-2">
                <BarChart3 className="h-6 w-6 text-white" />
              </div>
              <CardTitle>Analytics Dashboard</CardTitle>
              <CardDescription>
                Insights into your mod's development and usage
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">
                Track development progress, monitor performance metrics, and gain insights into your mod's evolution with detailed analytics.
              </p>
            </CardContent>
            <CardFooter>
              <Link href="/continuous-development">
                <Button variant="ghost" className="text-cyan-500 hover:text-cyan-600 p-0">
                  View Analytics <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
              </Link>
            </CardFooter>
          </Card>
        </div>
      </section>

      {/* How It Works */}
      <section className="bg-muted/50 py-16">
        <div className="container px-4">
          <h2 className="text-3xl font-bold tracking-tight mb-8 text-center">
            How It Works
          </h2>
          
          <div className="grid gap-8 md:grid-cols-4">
            <div className="text-center">
              <div className="bg-gradient-to-r from-indigo-500 to-purple-600 w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="font-bold text-white text-lg">1</span>
              </div>
              <h3 className="font-semibold text-lg mb-2">Generate Ideas</h3>
              <p className="text-muted-foreground">
                Use our idea generator or input your own mod concept to begin
              </p>
            </div>
            
            <div className="text-center">
              <div className="bg-gradient-to-r from-indigo-500 to-purple-600 w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="font-bold text-white text-lg">2</span>
              </div>
              <h3 className="font-semibold text-lg mb-2">Configure Project</h3>
              <p className="text-muted-foreground">
                Select mod loader, set parameters, and define core features
              </p>
            </div>
            
            <div className="text-center">
              <div className="bg-gradient-to-r from-indigo-500 to-purple-600 w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="font-bold text-white text-lg">3</span>
              </div>
              <h3 className="font-semibold text-lg mb-2">Generate Code</h3>
              <p className="text-muted-foreground">
                Our AI creates all necessary code files for a working mod
              </p>
            </div>
            
            <div className="text-center">
              <div className="bg-gradient-to-r from-indigo-500 to-purple-600 w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="font-bold text-white text-lg">4</span>
              </div>
              <h3 className="font-semibold text-lg mb-2">Continuous Improvement</h3>
              <p className="text-muted-foreground">
                Enable 24/7 development to fix errors and add features automatically
              </p>
            </div>
          </div>
          
          <div className="mt-12 text-center">
            <Link href="/documentation">
              <Button className="bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700">
                View Complete Process
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container px-4 py-16">
        <div className="relative overflow-hidden rounded-2xl p-8 md:p-12 bg-gradient-to-br from-indigo-600 to-purple-700">
          {/* Decorative elements */}
          <div className="absolute top-0 left-0 w-full h-full opacity-20">
            <div className="absolute top-0 left-0 w-40 h-40 bg-white rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2"></div>
            <div className="absolute bottom-0 right-0 w-40 h-40 bg-purple-200 rounded-full blur-3xl translate-x-1/3 translate-y-1/3"></div>
          </div>
          
          <div className="relative z-10 text-center">
            <Zap className="h-12 w-12 mx-auto mb-6 text-white" />
            <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-white mb-4">
              Ready to Revolutionize Your Minecraft Modding?
            </h2>
            <p className="text-lg md:text-xl text-white/80 mb-8 max-w-2xl mx-auto">
              Join the next generation of mod development with AI that works for you 24/7, continuously improving your code.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link href="/idea-generator">
                <Button size="lg" className="bg-white text-indigo-700 hover:bg-white/90">
                  Start Creating Your Mod
                </Button>
              </Link>
              <Link href="/documentation">
                <Button size="lg" variant="outline" className="text-white border-white hover:bg-white/10">
                  Explore Documentation
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </Layout>
  );
}
