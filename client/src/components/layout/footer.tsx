import { Link } from "wouter";
import { LucideGithub, LucideTwitter, LucideYoutube, LucideHeart } from "lucide-react";

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="w-full border-t bg-background py-6">
      <div className="container flex flex-col md:flex-row items-center justify-between">
        <div className="flex flex-col items-center md:items-start mb-4 md:mb-0">
          <h3 className="text-lg font-bold bg-gradient-to-r from-blue-600 to-violet-600 bg-clip-text text-transparent">
            ModForge
          </h3>
          <p className="text-sm text-muted-foreground mt-1">
            AI-powered Minecraft mod development platform
          </p>
          <p className="text-xs text-muted-foreground mt-2">
            &copy; {currentYear} ModForge. All rights reserved.
          </p>
        </div>
        
        <div className="grid grid-cols-2 md:grid-cols-3 gap-8 md:gap-20">
          <FooterSection 
            title="Product" 
            links={[
              { name: "Features", href: "/#features" },
              { name: "Idea Generator", href: "/idea-generator" },
              { name: "Code Generator", href: "/code-generator" },
              { name: "Continuous Development", href: "/continuous-development" },
              { name: "JAR Analyzer", href: "/jar-analyzer" },
            ]} 
          />
          
          <FooterSection 
            title="Resources" 
            links={[
              { name: "Documentation", href: "/documentation" },
              { name: "API Reference", href: "/documentation#api" },
              { name: "GitHub Integration", href: "/github-integration" },
              { name: "IntelliJ Plugin", href: "/intellij-plugin" },
              { name: "Metrics", href: "/metrics" },
            ]} 
          />
          
          <FooterSection 
            title="Company" 
            links={[
              { name: "About Us", href: "/about" },
              { name: "Terms of Service", href: "/terms" },
              { name: "Privacy Policy", href: "/privacy" },
              { name: "License", href: "/license" },
              { name: "Contact", href: "/contact" },
            ]} 
          />
        </div>
        
        <div className="mt-8 md:mt-0 flex flex-col items-center md:items-end">
          <div className="flex space-x-4 mb-4">
            <a href="https://github.com/modforge-dev/modforge" target="_blank" rel="noopener noreferrer" className="text-muted-foreground hover:text-foreground transition-colors">
              <LucideGithub className="h-5 w-5" />
              <span className="sr-only">GitHub</span>
            </a>
            <a href="https://twitter.com/modforge" target="_blank" rel="noopener noreferrer" className="text-muted-foreground hover:text-foreground transition-colors">
              <LucideTwitter className="h-5 w-5" />
              <span className="sr-only">Twitter</span>
            </a>
            <a href="https://youtube.com/modforge" target="_blank" rel="noopener noreferrer" className="text-muted-foreground hover:text-foreground transition-colors">
              <LucideYoutube className="h-5 w-5" />
              <span className="sr-only">YouTube</span>
            </a>
          </div>
          <div className="text-xs text-muted-foreground flex items-center">
            <span>Made with</span> 
            <LucideHeart className="h-3 w-3 mx-1 text-red-500" />
            <span>by developers for developers</span>
          </div>
        </div>
      </div>
    </footer>
  );
}

function FooterSection({ title, links }: { title: string, links: { name: string, href: string }[] }) {
  return (
    <div className="flex flex-col">
      <h4 className="font-medium text-sm mb-3">{title}</h4>
      <ul className="space-y-2">
        {links.map((link) => (
          <li key={link.name} className="text-sm">
            <Link href={link.href}>
              <a className="text-muted-foreground hover:text-foreground transition-colors">
                {link.name}
              </a>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}