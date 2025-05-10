import { useLocation, useNavigate } from "wouter";
import { 
  BarChart3, 
  Code2, 
  Cpu, 
  FileCode, 
  Github, 
  Lightbulb, 
  PlayCircle, 
  Settings, 
  Sparkles
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const navItems = [
  {
    title: "Home",
    href: "/",
    icon: <Cpu className="h-5 w-5" />,
    color: "text-blue-500"
  },
  {
    title: "Idea Generator",
    href: "/idea-generator",
    icon: <Lightbulb className="h-5 w-5" />,
    color: "text-yellow-500"
  },
  {
    title: "Code Generator",
    href: "/code-generator",
    icon: <Code2 className="h-5 w-5" />,
    color: "text-green-500"
  },
  {
    title: "Documentation",
    href: "/documentation",
    icon: <FileCode className="h-5 w-5" />,
    color: "text-purple-500"
  },
  {
    title: "Continuous Dev",
    href: "/continuous-development",
    icon: <PlayCircle className="h-5 w-5" />,
    color: "text-red-500"
  },
  {
    title: "Error Resolution",
    href: "/error-resolution",
    icon: <Sparkles className="h-5 w-5" />,
    color: "text-orange-500"
  },
  {
    title: "GitHub Integration",
    href: "/github-integration",
    icon: <Github className="h-5 w-5" />,
    color: "text-gray-500"
  },
  {
    title: "Metrics",
    href: "/metrics",
    icon: <BarChart3 className="h-5 w-5" />,
    color: "text-indigo-500"
  },
  {
    title: "Settings",
    href: "/settings",
    icon: <Settings className="h-5 w-5" />,
    color: "text-gray-500"
  }
];

export function MainNav() {
  const [location] = useLocation();
  const navigate = useNavigate();

  return (
    <nav className="flex flex-col space-y-1 p-2">
      {navItems.map((item) => (
        <Button
          key={item.href}
          variant="ghost"
          className={cn(
            "flex items-center justify-start gap-3 h-10 px-3 py-6 text-sm font-medium transition-all",
            location === item.href 
              ? "bg-accent" 
              : "opacity-70 hover:opacity-100"
          )}
          onClick={() => navigate(item.href)}
        >
          <span className={cn("", item.color)}>
            {item.icon}
          </span>
          <span>{item.title}</span>
        </Button>
      ))}
    </nav>
  );
}