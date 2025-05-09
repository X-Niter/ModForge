import React, { useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { ModDetailsForm } from "@/components/mod-details-form";
import { ModRequirementsForm } from "@/components/mod-requirements-form";
import { BuildConfiguration } from "@/components/build-configuration";
import { StatusCard } from "@/components/status-card";
import { BuildHistory } from "@/components/build-history";
import { ActionsCard } from "@/components/actions-card";
import { ConsoleOutput } from "@/components/console-output";
import { modLoaders, compileFrequencies, autoFixLevels } from "@shared/schema";
import { ModFormData } from "@/types";

export default function Home() {
  const [formData, setFormData] = useState<ModFormData>({
    name: "EnhancedCombat",
    modId: "enhancedcombat",
    description: "Enhances Minecraft's combat system with new weapons, abilities, and combat mechanics.",
    version: "0.1.0",
    minecraftVersion: "1.19.2",
    license: "MIT",
    modLoader: "Forge",
    idea: `Create a combat enhancement mod with the following features:

1. Add 5 new weapon types: spear, battle axe, dagger, warhammer, and katana
2. Each weapon should have unique attack animations and effects
3. Implement a combo system that rewards consecutive hits
4. Add a stamina bar for balanced combat mechanics
5. Include special abilities for each weapon that can be activated with right-click
6. Create custom sounds and particle effects for combat actions
7. Balance all weapons to maintain vanilla-like gameplay feel`,
    featurePriority: "Balanced Features",
    codingStyle: "Well-Documented",
    compileFrequency: "Every 5 Minutes",
    autoFixLevel: "Balanced",
    autoPushToGithub: true,
    generateDocumentation: true,
  });

  // Initialize mobile sidebar state
  React.useEffect(() => {
    const sidebar = document.getElementById("sidebar");
    if (sidebar && window.innerWidth < 768) {
      sidebar.classList.add("mobile-sidebar-hidden");
    }

    // Close sidebar when clicking outside on mobile
    const handleClickOutside = (event: MouseEvent) => {
      const sidebarElement = document.getElementById("sidebar");
      const sidebarToggle = document.getElementById("sidebar-toggle");
      
      if (sidebarElement && sidebarToggle && window.innerWidth < 768) {
        const isClickInsideSidebar = sidebarElement.contains(event.target as Node);
        const isClickOnToggle = sidebarToggle.contains(event.target as Node);
        
        if (!isClickInsideSidebar && !isClickOnToggle) {
          sidebarElement.classList.add("mobile-sidebar-hidden");
        }
      }
    };

    // Handle window resize
    const handleResize = () => {
      const sidebarElement = document.getElementById("sidebar");
      if (sidebarElement && window.innerWidth >= 768) {
        sidebarElement.classList.remove("mobile-sidebar-hidden");
      }
    };
    
    document.addEventListener("click", handleClickOutside);
    window.addEventListener("resize", handleResize);
    
    return () => {
      document.removeEventListener("click", handleClickOutside);
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return (
    <div className="h-screen flex flex-col overflow-hidden bg-background text-foreground">
      <Navbar />
      
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        
        <main className="flex-1 overflow-hidden flex flex-col">
          <div className="flex flex-col h-full">
            <div className="bg-surface p-4 border-b border-gray-700 flex items-center justify-between">
              <div className="flex items-center">
                <h2 className="text-lg font-medium text-white">Mod Generator</h2>
              </div>
              <div className="flex items-center space-x-2">
                <button 
                  className="bg-background hover:bg-gray-700 text-white px-3 py-1 rounded text-sm flex items-center"
                  onClick={() => {
                    // Reset form to default values
                    setFormData({
                      name: "EnhancedCombat",
                      modId: "enhancedcombat",
                      description: "Enhances Minecraft's combat system with new weapons, abilities, and combat mechanics.",
                      version: "0.1.0",
                      minecraftVersion: "1.19.2",
                      license: "MIT",
                      modLoader: "Forge",
                      idea: `Create a combat enhancement mod with the following features:

1. Add 5 new weapon types: spear, battle axe, dagger, warhammer, and katana
2. Each weapon should have unique attack animations and effects
3. Implement a combo system that rewards consecutive hits
4. Add a stamina bar for balanced combat mechanics
5. Include special abilities for each weapon that can be activated with right-click
6. Create custom sounds and particle effects for combat actions
7. Balance all weapons to maintain vanilla-like gameplay feel`,
                      featurePriority: "Balanced Features",
                      codingStyle: "Well-Documented",
                      compileFrequency: "Every 5 Minutes",
                      autoFixLevel: "Balanced",
                      autoPushToGithub: true,
                      generateDocumentation: true,
                    });
                  }}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="mr-1"
                    width="16" 
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0"></path>
                    <path d="M14 8 8 14"></path>
                    <path d="m8 8 6 6"></path>
                  </svg>
                  <span>Reset</span>
                </button>
                <button 
                  className="bg-secondary hover:bg-opacity-80 text-white px-3 py-1 rounded text-sm flex items-center"
                  onClick={() => {
                    // Save the form data (would typically save to local storage or backend)
                    alert("Form data saved!");
                  }}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="mr-1"
                    width="16" 
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                  </svg>
                  <span>Save</span>
                </button>
              </div>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4">
              <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                <div className="lg:col-span-3">
                  <ModDetailsForm formData={formData} setFormData={setFormData} />
                  <ModRequirementsForm formData={formData} setFormData={setFormData} />
                  <BuildConfiguration formData={formData} setFormData={setFormData} />
                </div>
                
                <div className="lg:col-span-2">
                  <StatusCard />
                  <BuildHistory />
                  <ActionsCard formData={formData} />
                </div>
              </div>
            </div>
            
            <ConsoleOutput />
          </div>
        </main>
      </div>
    </div>
  );
}
