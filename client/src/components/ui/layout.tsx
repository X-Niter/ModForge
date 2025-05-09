import { ReactNode, useState, useEffect } from "react";
import { Link, useLocation } from "wouter";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Code,
  Home,
  Lightbulb,
  Github,
  Settings,
  RefreshCw,
  FileText,
  Scale,
  BookOpen,
  Moon,
  Sun,
  BarChart3,
  Brain,
  Bug,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

// Simple theme toggle component
const ThemeToggle = () => {
  const [theme, setThemeState] = useState<"dark" | "light" | "system">(
    () => (localStorage.getItem('theme') as any) || 'dark'
  );

  // Update the theme when the component mounts and when the theme changes
  useEffect(() => {
    const root = window.document.documentElement;
    root.classList.remove("light", "dark");

    if (theme === "system") {
      const systemTheme = window.matchMedia("(prefers-color-scheme: dark)")
        .matches
        ? "dark"
        : "light";
      root.classList.add(systemTheme);
      return;
    }

    root.classList.add(theme);
  }, [theme]);

  const setTheme = (theme: "dark" | "light" | "system") => {
    localStorage.setItem('theme', theme);
    setThemeState(theme);
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon">
          <Sun className="h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
          <Moon className="absolute h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          <span className="sr-only">Toggle theme</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => setTheme("light")}>
          Light
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme("dark")}>
          Dark
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme("system")}>
          System
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const [location] = useLocation();

  const navigationItems = [
    {
      name: "Home",
      href: "/",
      icon: <Home className="w-5 h-5 mr-2" />,
    },
    {
      name: "Idea Generator",
      href: "/idea-generator",
      icon: <Lightbulb className="w-5 h-5 mr-2" />,
    },
    {
      name: "Code Generator",
      href: "/code-generator",
      icon: <Code className="w-5 h-5 mr-2" />,
    },
    {
      name: "Continuous Development",
      href: "/continuous-development",
      icon: <RefreshCw className="w-5 h-5 mr-2" />,
    },
    {
      name: "Error Resolution",
      href: "/error-resolution",
      icon: <Brain className="w-5 h-5 mr-2" />,
    },
    {
      name: "GitHub Integration",
      href: "/github-integration",
      icon: <Github className="w-5 h-5 mr-2" />,
    },
    {
      name: "AI Metrics",
      href: "/metrics",
      icon: <BarChart3 className="w-5 h-5 mr-2" />,
    },
    {
      name: "Documentation",
      href: "/documentation",
      icon: <BookOpen className="w-5 h-5 mr-2" />,
    },
    {
      name: "Settings",
      href: "/settings",
      icon: <Settings className="w-5 h-5 mr-2" />,
    },
  ];

  const footerLinks = [
    {
      name: "Terms of Service",
      href: "/terms",
      icon: <FileText className="w-4 h-4 mr-1" />,
    },
    {
      name: "License",
      href: "/license",
      icon: <Scale className="w-4 h-4 mr-1" />,
    },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Header with gradient border bottom */}
      <header className="sticky top-0 z-40 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between px-4">
          <div className="flex items-center">
            <Link href="/">
              <div className="flex items-center space-x-2 cursor-pointer">
                <div className="relative w-8 h-8 overflow-hidden rounded-md bg-gradient-to-br from-indigo-500 to-purple-600">
                  <div className="absolute inset-0 flex items-center justify-center text-white font-bold text-lg">M</div>
                </div>
                <span className="font-bold text-xl bg-gradient-to-r from-indigo-500 to-purple-600 bg-clip-text text-transparent">
                  ModForge AI
                </span>
              </div>
            </Link>
          </div>
          <div className="flex items-center space-x-4">
            <ThemeToggle />
          </div>
        </div>
      </header>

      {/* Content area with sidebar and main content */}
      <div className="flex-1 flex flex-col md:flex-row">
        {/* Sidebar */}
        <aside className="border-r border-border/40 w-full md:w-64 md:flex-shrink-0 pt-6 pb-4 hidden md:block">
          <nav className="flex flex-col px-4 space-y-1">
            {navigationItems.map((item) => (
              <Link key={item.href} href={item.href}>
                <Button
                  variant={location === item.href ? "default" : "ghost"}
                  className={cn(
                    "justify-start w-full",
                    location === item.href
                      ? "bg-gradient-to-r from-indigo-500 to-purple-600 text-white"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {item.icon}
                  {item.name}
                </Button>
              </Link>
            ))}
          </nav>

          <div className="mt-auto pt-6 px-4">
            <div className="bg-muted/40 rounded-lg p-4 text-sm">
              <h4 className="font-medium mb-2">API Usage Stats</h4>
              <div className="text-xs text-muted-foreground space-y-1">
                <div className="flex justify-between">
                  <span>Pattern Matches:</span>
                  <span className="font-medium">76%</span>
                </div>
                <div className="flex justify-between">
                  <span>API Calls:</span>
                  <span className="font-medium">24%</span>
                </div>
                <div className="flex justify-between">
                  <span>Est. Savings:</span>
                  <span className="font-medium text-green-500">$27.84</span>
                </div>
              </div>
            </div>
          </div>
        </aside>

        {/* Mobile navigation */}
        <div className="md:hidden border-b border-border/40 p-2">
          <nav className="flex overflow-x-auto pb-2 space-x-2 scrollbar-hide">
            {navigationItems.map((item) => (
              <Link key={item.href} href={item.href}>
                <Button
                  variant={location === item.href ? "default" : "outline"}
                  size="sm"
                  className={cn(
                    "whitespace-nowrap",
                    location === item.href && "bg-gradient-to-r from-indigo-500 to-purple-600 text-white"
                  )}
                >
                  {item.icon}
                  <span className="text-xs">{item.name}</span>
                </Button>
              </Link>
            ))}
          </nav>
        </div>

        {/* Main content area */}
        <main className="flex-1 p-4 md:p-6 overflow-auto">
          {children}
        </main>
      </div>

      {/* Footer */}
      <footer className="border-t border-border/40 py-4 bg-background/95">
        <div className="container px-4">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <div className="flex space-x-4 mb-4 md:mb-0">
              {footerLinks.map((link) => (
                <Link key={link.href} href={link.href}>
                  <Button variant="ghost" size="sm" className="flex items-center text-muted-foreground hover:text-foreground">
                    {link.icon}
                    {link.name}
                  </Button>
                </Link>
              ))}
            </div>
            <div className="text-sm text-muted-foreground">
              &copy; {new Date().getFullYear()} X_Niter & Knoxhack. All rights reserved.
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}