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
  Globe,
  Database,
  Menu,
  X,
  Zap,
  Code2,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";

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

interface NavItemProps {
  icon: ReactNode;
  href: string;
  name: string;
  isActive: boolean;
  badge?: string;
}

// Navigation item component
const NavItem = ({ icon, href, name, isActive, badge }: NavItemProps) => {
  return (
    <Link href={href}>
      <div
        className={cn(
          "group flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-all",
          isActive
            ? "bg-gradient-to-r from-indigo-500 to-purple-600 text-white"
            : "text-muted-foreground hover:bg-muted hover:text-foreground"
        )}
      >
        <div className="flex items-center gap-2 flex-1">
          {icon}
          <span>{name}</span>
        </div>
        {badge && (
          <Badge variant="outline" className="bg-background/20">{badge}</Badge>
        )}
      </div>
    </Link>
  );
};

export function Layout({ children }: LayoutProps) {
  const [location] = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const toggleMobileMenu = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  // Group navigation items into categories
  const mainNavItems = [
    {
      name: "Home",
      href: "/",
      icon: <Home className="w-5 h-5" />,
    },
    {
      name: "Idea Generator",
      href: "/idea-generator",
      icon: <Lightbulb className="w-5 h-5" />,
      badge: "AI"
    },
    {
      name: "Code Generator",
      href: "/code-generator",
      icon: <Code2 className="w-5 h-5" />,
      badge: "AI"
    },
  ];
  
  const developmentItems = [
    {
      name: "Continuous Development",
      href: "/continuous-development",
      icon: <RefreshCw className="w-5 h-5" />,
    },
    {
      name: "Error Resolution",
      href: "/error-resolution",
      icon: <Brain className="w-5 h-5" />,
      badge: "ML"
    },
  ];
  
  const toolsItems = [
    {
      name: "Web Explorer",
      href: "/web-explorer",
      icon: <Globe className="w-5 h-5" />,
    },
    {
      name: "JAR Analyzer",
      href: "/jar-analyzer",
      icon: <Database className="w-5 h-5" />,
    },
    {
      name: "GitHub Integration",
      href: "/github-integration",
      icon: <Github className="w-5 h-5" />,
    },
  ];
  
  const otherItems = [
    {
      name: "AI Metrics",
      href: "/metrics",
      icon: <BarChart3 className="w-5 h-5" />,
    },
    {
      name: "Documentation",
      href: "/documentation",
      icon: <BookOpen className="w-5 h-5" />,
    },
    {
      name: "Settings",
      href: "/settings",
      icon: <Settings className="w-5 h-5" />,
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
  
  // API usage stats (mock data for UI purposes)
  const apiStats = {
    patternMatches: 76,
    apiCalls: 24,
    savings: 27.84,
    totalQueries: 523
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between px-4">
          <div className="flex items-center">
            <Button variant="ghost" size="icon" onClick={toggleMobileMenu} className="md:hidden mr-2">
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </Button>
            <Link href="/">
              <div className="flex items-center space-x-2 cursor-pointer">
                <div className="relative w-9 h-9 overflow-hidden rounded-md bg-gradient-to-br from-indigo-500 via-purple-600 to-pink-500">
                  <div className="absolute inset-0 flex items-center justify-center text-white font-bold text-lg">
                    <Zap className="h-5 w-5" />
                  </div>
                </div>
                <span className="font-bold text-xl bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 bg-clip-text text-transparent">
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

      {/* Mobile menu overlay */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-40 bg-background/80 backdrop-blur-sm md:hidden" onClick={toggleMobileMenu}>
          <div className="fixed inset-y-0 left-0 z-50 w-3/4 max-w-xs bg-background border-r border-border/40 p-4" onClick={(e) => e.stopPropagation()}>
            <div className="flex flex-col h-full">
              <div className="mb-6 mt-2">
                <h2 className="text-lg font-semibold mb-4">Navigation</h2>
                <div className="space-y-1">
                  {mainNavItems.map((item) => (
                    <NavItem 
                      key={item.href} 
                      {...item} 
                      isActive={location === item.href}
                    />
                  ))}
                </div>
                
                <h3 className="text-xs uppercase tracking-wider text-muted-foreground mt-6 mb-2 px-2">Development</h3>
                <div className="space-y-1">
                  {developmentItems.map((item) => (
                    <NavItem 
                      key={item.href} 
                      {...item} 
                      isActive={location === item.href}
                    />
                  ))}
                </div>
                
                <h3 className="text-xs uppercase tracking-wider text-muted-foreground mt-6 mb-2 px-2">Tools</h3>
                <div className="space-y-1">
                  {toolsItems.map((item) => (
                    <NavItem 
                      key={item.href} 
                      {...item} 
                      isActive={location === item.href}
                    />
                  ))}
                </div>
                
                <h3 className="text-xs uppercase tracking-wider text-muted-foreground mt-6 mb-2 px-2">Other</h3>
                <div className="space-y-1">
                  {otherItems.map((item) => (
                    <NavItem 
                      key={item.href} 
                      {...item} 
                      isActive={location === item.href}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Content area with sidebar and main content */}
      <div className="flex-1 flex flex-col md:flex-row">
        {/* Desktop Sidebar */}
        <aside className="border-r border-border/40 w-full md:w-64 md:flex-shrink-0 pt-4 pb-4 hidden md:block">
          <div className="px-3 py-2">
            <div className="space-y-1 mb-6">
              {mainNavItems.map((item) => (
                <NavItem 
                  key={item.href} 
                  {...item} 
                  isActive={location === item.href} 
                />
              ))}
            </div>
            
            <div className="space-y-0.5">
              <h3 className="text-xs uppercase tracking-wider text-muted-foreground mb-1 px-3">Development</h3>
              {developmentItems.map((item) => (
                <NavItem 
                  key={item.href} 
                  {...item} 
                  isActive={location === item.href}
                />
              ))}
            </div>
            
            <div className="space-y-0.5 mt-4">
              <h3 className="text-xs uppercase tracking-wider text-muted-foreground mb-1 px-3">Tools</h3>
              {toolsItems.map((item) => (
                <NavItem 
                  key={item.href} 
                  {...item} 
                  isActive={location === item.href}
                />
              ))}
            </div>
            
            <div className="space-y-0.5 mt-4">
              <h3 className="text-xs uppercase tracking-wider text-muted-foreground mb-1 px-3">System</h3>
              {otherItems.map((item) => (
                <NavItem 
                  key={item.href} 
                  {...item} 
                  isActive={location === item.href}
                />
              ))}
            </div>

            <div className="mt-6">
              <div className="bg-muted/30 rounded-xl p-4 text-sm border border-border/30">
                <h4 className="font-medium flex items-center mb-3">
                  <BarChart3 className="h-4 w-4 mr-2 text-indigo-400" />
                  AI Performance Metrics
                </h4>
                <div className="space-y-3">
                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-muted-foreground">Pattern Matches</span>
                      <span className="font-medium">{apiStats.patternMatches}%</span>
                    </div>
                    <Progress value={apiStats.patternMatches} className="h-1.5" />
                  </div>
                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-muted-foreground">API Calls</span>
                      <span className="font-medium">{apiStats.apiCalls}%</span>
                    </div>
                    <Progress value={apiStats.apiCalls} className="h-1.5" />
                  </div>
                  <div className="flex justify-between text-xs pt-2 border-t border-border/40">
                    <span className="text-muted-foreground">Estimated Savings</span>
                    <span className="font-medium text-green-500">${apiStats.savings.toFixed(2)}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        {/* Main content area */}
        <main className="flex-1 overflow-auto">
          <div className="container mx-auto p-4 md:p-6 max-w-6xl">
            {children}
          </div>
        </main>
      </div>

      {/* Footer */}
      <footer className="border-t border-border/40 py-4 bg-background/95">
        <div className="container px-4 max-w-6xl mx-auto">
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
            <div className="flex items-center">
              <div className="text-sm text-muted-foreground">
                &copy; {new Date().getFullYear()} X_Niter & Knoxhack. All rights reserved.
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}