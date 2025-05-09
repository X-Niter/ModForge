import { Mod, Build, ModFile, ModLoader, AutoFixLevel, CompileFrequency } from "@shared/schema";

export interface ModContextType {
  currentMod: Mod | null;
  setCurrentMod: (mod: Mod | null) => void;
  builds: Build[];
  setBuilds: (builds: Build[]) => void;
  currentBuild: Build | null;
  setCurrentBuild: (build: Build | null) => void;
  consoleOutput: string[];
  addConsoleOutput: (message: string) => void;
  clearConsoleOutput: () => void;
  isGenerating: boolean;
  setIsGenerating: (isGenerating: boolean) => void;
  modFiles: ModFile[];
  setModFiles: (files: ModFile[]) => void;
}

export interface ModFormData {
  name: string;
  modId: string;
  description: string;
  version: string;
  minecraftVersion: string;
  license: string;
  modLoader: ModLoader;
  idea: string;
  featurePriority: string;
  codingStyle: string;
  compileFrequency: CompileFrequency;
  autoFixLevel: AutoFixLevel;
  autoPushToGithub: boolean;
  generateDocumentation: boolean;
}

export enum BuildStatus {
  Success = "success",
  Failed = "failed",
  InProgress = "in_progress",
}

export interface ErrorData {
  line: number;
  message: string;
  file: string;
  code?: string;
  suggestion?: string;
}
