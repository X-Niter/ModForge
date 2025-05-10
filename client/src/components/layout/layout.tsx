import { Navbar } from "./navbar";
import { Footer } from "./footer";
import { MainNav } from "../main-nav";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Menu } from "lucide-react";

interface LayoutProps {
  children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      
      <div className="flex flex-1">
        {/* Sidebar - collapsible on mobile */}
        <div className="relative">
          <Button 
            variant="ghost" 
            size="icon" 
            className="absolute -right-10 top-4 md:hidden"
            onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            <Menu />
          </Button>
          
          <aside 
            className={`
              border-r bg-background z-30 h-[calc(100vh-4rem)] w-64
              transition-all duration-300 
              ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
              md:translate-x-0
            `}
          >
            <MainNav />
          </aside>
        </div>
        
        {/* Main content */}
        <main className="flex-1 p-6 md:p-8">{children}</main>
      </div>
      
      <Footer />
    </div>
  );
}