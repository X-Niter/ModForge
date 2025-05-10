import { useLocation, Link } from "wouter";
import { Button } from "@/components/ui/button";
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
} from "@/components/ui/navigation-menu";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  SheetClose,
} from "@/components/ui/sheet";
import { 
  LucideGithub, 
  LucideMenu, 
  LucideSettings, 
  LucideLogIn, 
  LucideLogOut, 
  LucideUser,
  LucideCode,
  LucideFileText,
  LucideBrain,
  LucideActivity,
  LucidePackageOpen
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useState } from "react";

// Mock authentication for now
const useAuth = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  return {
    isLoggedIn,
    login: () => setIsLoggedIn(true),
    logout: () => setIsLoggedIn(false),
    user: isLoggedIn ? { name: "Demo User" } : null
  };
};

export function Navbar() {
  const [location] = useLocation();
  const { isLoggedIn, login, logout, user } = useAuth();
  
  const navItems = [
    {
      title: "Features",
      href: "#",
      description: "Core features of the ModForge platform",
      items: [
        {
          title: "Idea Generator",
          href: "/idea-generator",
          description: "Generate and expand mod ideas with AI",
          icon: <LucideBrain className="h-4 w-4 mr-2" />
        },
        {
          title: "Code Generator",
          href: "/code-generator",
          description: "Create mod code from natural language",
          icon: <LucideCode className="h-4 w-4 mr-2" />
        },
        {
          title: "Continuous Development",
          href: "/continuous-development",
          description: "Automated code improvement and error fixing",
          icon: <LucideActivity className="h-4 w-4 mr-2" />
        },
        {
          title: "JAR Analyzer",
          href: "/jar-analyzer",
          description: "Analyze existing mods and extract features",
          icon: <LucidePackageOpen className="h-4 w-4 mr-2" />
        },
      ],
    },
    {
      title: "Integrations",
      href: "/github-integration",
      description: "Integration with development tools",
      icon: <LucideGithub className="h-4 w-4 mr-2" />
    },
    {
      title: "Documentation",
      href: "/documentation",
      description: "Guides and documentation",
      icon: <LucideFileText className="h-4 w-4 mr-2" />
    },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center">
        <div className="mr-4 flex">
          <Link href="/" className="mr-6 flex items-center space-x-2">
            <span className="hidden font-bold sm:inline-block text-xl bg-gradient-to-r from-blue-600 to-violet-600 bg-clip-text text-transparent">
              ModForge
            </span>
          </Link>
          
          {/* Desktop Navigation */}
          <NavigationMenu className="hidden md:flex">
            <NavigationMenuList>
              {navItems.map((item) => (
                <NavigationMenuItem key={item.title}>
                  {item.items ? (
                    <>
                      <NavigationMenuTrigger>{item.title}</NavigationMenuTrigger>
                      <NavigationMenuContent>
                        <ul className="grid w-[400px] gap-3 p-4 md:w-[500px] md:grid-cols-2 lg:w-[600px]">
                          {item.items.map((subItem) => (
                            <ListItem
                              key={subItem.title}
                              title={subItem.title}
                              href={subItem.href}
                              className={cn(location === subItem.href && "bg-muted")}
                            >
                              <div className="flex items-center">
                                {subItem.icon}
                                {subItem.description}
                              </div>
                            </ListItem>
                          ))}
                        </ul>
                      </NavigationMenuContent>
                    </>
                  ) : (
                    <Link href={item.href}>
                      <NavigationMenuLink
                        className={cn(
                          "group inline-flex h-10 w-max items-center justify-center rounded-md bg-background px-4 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground focus:outline-none disabled:pointer-events-none disabled:opacity-50 data-[active]:bg-accent/50 data-[state=open]:bg-accent/50",
                          location === item.href && "bg-accent/50"
                        )}
                      >
                        <div className="flex items-center">
                          {item.icon}
                          {item.title}
                        </div>
                      </NavigationMenuLink>
                    </Link>
                  )}
                </NavigationMenuItem>
              ))}
            </NavigationMenuList>
          </NavigationMenu>
        </div>

        <div className="flex-1"></div>
        
        {/* Authentication */}
        <div className="flex items-center gap-2">
          {isLoggedIn ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-8 w-8 rounded-full">
                  <LucideUser className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>My Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link href="/settings">
                    <LucideSettings className="mr-2 h-4 w-4" />
                    <span>Settings</span>
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem onClick={logout}>
                  <LucideLogOut className="mr-2 h-4 w-4" />
                  <span>Log out</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Button variant="outline" size="sm" onClick={login}>
              <LucideLogIn className="mr-2 h-4 w-4" />
              Log in
            </Button>
          )}
        </div>
        
        {/* Mobile Navigation */}
        <div className="md:hidden ml-2">
          <Sheet>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon">
                <LucideMenu className="h-5 w-5" />
                <span className="sr-only">Toggle navigation menu</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="right">
              <SheetHeader>
                <SheetTitle>ModForge</SheetTitle>
                <SheetDescription>
                  AI-powered Minecraft mod development
                </SheetDescription>
              </SheetHeader>
              <div className="grid gap-2 py-6">
                {/* Flatten navigation for mobile */}
                <SheetClose asChild>
                  <Link href="/">
                    <Button variant="ghost" className="w-full justify-start">Home</Button>
                  </Link>
                </SheetClose>
                
                {navItems.map((item) => (
                  <div key={item.title}>
                    {item.items ? (
                      <>
                        <div className="px-4 py-2 font-medium">{item.title}</div>
                        {item.items.map((subItem) => (
                          <SheetClose key={subItem.title} asChild>
                            <Link href={subItem.href}>
                              <Button variant="ghost" className="w-full justify-start pl-8 mb-1">
                                {subItem.icon}
                                {subItem.title}
                              </Button>
                            </Link>
                          </SheetClose>
                        ))}
                      </>
                    ) : (
                      <SheetClose asChild>
                        <Link href={item.href}>
                          <Button variant="ghost" className="w-full justify-start">
                            {item.icon}
                            {item.title}
                          </Button>
                        </Link>
                      </SheetClose>
                    )}
                  </div>
                ))}
                
                {/* Authentication section for mobile */}
                <div className="border-t pt-4 mt-4">
                  {isLoggedIn ? (
                    <>
                      <div className="px-4 py-2 text-sm text-muted-foreground">
                        Signed in as <span className="font-medium">{user?.name}</span>
                      </div>
                      <SheetClose asChild>
                        <Link href="/settings">
                          <Button variant="ghost" className="w-full justify-start">
                            <LucideSettings className="mr-2 h-4 w-4" />
                            Settings
                          </Button>
                        </Link>
                      </SheetClose>
                      <Button variant="ghost" className="w-full justify-start" onClick={logout}>
                        <LucideLogOut className="mr-2 h-4 w-4" />
                        Log out
                      </Button>
                    </>
                  ) : (
                    <Button className="w-full" onClick={login}>
                      <LucideLogIn className="mr-2 h-4 w-4" />
                      Log in
                    </Button>
                  )}
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}

const ListItem = ({ className, title, children, href, ...props }: {
  className?: string,
  title: string,
  children?: React.ReactNode,
  href: string,
  [key: string]: any
}) => {
  return (
    <li>
      <Link href={href}>
        <NavigationMenuLink
          className={cn(
            "block select-none space-y-1 rounded-md p-3 leading-none no-underline outline-none transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground",
            className
          )}
          {...props}
        >
          <div className="text-sm font-medium leading-none">{title}</div>
          <div className="line-clamp-2 text-sm leading-snug text-muted-foreground">
            {children}
          </div>
        </NavigationMenuLink>
      </Link>
    </li>
  );
};