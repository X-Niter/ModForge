import React, { createContext, useState, useContext, ReactNode } from "react";
import { ModContextType } from "@/types";
import { Mod, Build, ModFile } from "@shared/schema";

const ModContext = createContext<ModContextType | undefined>(undefined);

export function ModProvider({ children }: { children: ReactNode }) {
  const [currentMod, setCurrentMod] = useState<Mod | null>(null);
  const [builds, setBuilds] = useState<Build[]>([]);
  const [currentBuild, setCurrentBuild] = useState<Build | null>(null);
  const [consoleOutput, setConsoleOutput] = useState<string[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [modFiles, setModFiles] = useState<ModFile[]>([]);

  const addConsoleOutput = (message: string) => {
    const timestamp = new Date().toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    });
    setConsoleOutput((prev) => [...prev, `[${timestamp}] ${message}`]);
  };

  const clearConsoleOutput = () => {
    setConsoleOutput([]);
  };

  return (
    <ModContext.Provider
      value={{
        currentMod,
        setCurrentMod,
        builds,
        setBuilds,
        currentBuild,
        setCurrentBuild,
        consoleOutput,
        addConsoleOutput,
        clearConsoleOutput,
        isGenerating,
        setIsGenerating,
        modFiles,
        setModFiles,
      }}
    >
      {children}
    </ModContext.Provider>
  );
}

export function useModContext() {
  const context = useContext(ModContext);
  if (context === undefined) {
    throw new Error("useModContext must be used within a ModProvider");
  }
  return context;
}
