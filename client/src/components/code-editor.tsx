import React, { useState, useEffect } from "react";
import { useModContext } from "@/context/mod-context";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ScrollArea } from "@/components/ui/scroll-area";

export function CodeEditor() {
  const { modFiles } = useModContext();
  const [activeFile, setActiveFile] = useState<string | null>(null);
  const [filesByFolder, setFilesByFolder] = useState<Record<string, Array<{ path: string; content: string }>>>({});
  
  // Organize files by folder for better navigation
  useEffect(() => {
    if (modFiles.length > 0) {
      const newFilesByFolder: Record<string, Array<{ path: string; content: string }>> = {};
      
      modFiles.forEach(file => {
        const pathParts = file.path.split('/');
        const folderPath = pathParts.length > 1 ? pathParts.slice(0, -1).join('/') : 'root';
        
        if (!newFilesByFolder[folderPath]) {
          newFilesByFolder[folderPath] = [];
        }
        
        newFilesByFolder[folderPath].push({
          path: file.path,
          content: file.content
        });
      });
      
      setFilesByFolder(newFilesByFolder);
      
      // Set active file to the first one if none is selected
      if (!activeFile && modFiles.length > 0) {
        setActiveFile(modFiles[0].path);
      }
    }
  }, [modFiles, activeFile]);
  
  // Get the content of the active file
  const getActiveFileContent = () => {
    if (!activeFile) return '';
    const file = modFiles.find(f => f.path === activeFile);
    return file ? file.content : '';
  };
  
  // Syntax highlighting helper (simple version)
  const highlightSyntax = (code: string) => {
    // Java-specific keywords
    const javaKeywords = [
      'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
      'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
      'for', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
      'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super',
      'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while',
      // Minecraft/Forge specific
      'Mod', 'EventHandler', 'SidedProxy'
    ];
    
    // Very simple syntax highlighting (would use a real library in production)
    const lines = code.split('\n');
    return lines.map((line, i) => {
      // Handle comments
      if (line.trim().startsWith('//')) {
        return `<span class="text-gray-400">${line}</span>`;
      }
      
      // Handle strings
      let highlightedLine = line.replace(/"([^"]*)"/g, '<span class="text-accent">"$1"</span>');
      
      // Highlight keywords
      javaKeywords.forEach(keyword => {
        // This is a simple approach - a real implementation would use regex with word boundaries
        const regex = new RegExp(`\\b${keyword}\\b`, 'g');
        highlightedLine = highlightedLine.replace(regex, `<span class="text-secondary font-medium">${keyword}</span>`);
      });
      
      // Highlight annotations
      highlightedLine = highlightedLine.replace(/@\w+/g, '<span class="text-primary font-medium">$&</span>');
      
      return highlightedLine;
    }).join('\n');
  };
  
  return (
    <div className="bg-surface rounded-lg shadow-lg p-5 flex flex-col h-full">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-white font-medium flex items-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="mr-2 text-primary"
            width="20" 
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="m18 16 4-4-4-4"></path>
            <path d="m6 8-4 4 4 4"></path>
            <path d="m14.5 4-5 16"></path>
          </svg>
          Code Editor
        </h3>
        <div className="flex space-x-2">
          <Button 
            variant="outline"
            size="sm"
            className="text-xs"
            disabled={!activeFile}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="mr-1"
              width="12" 
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3Z"></path>
            </svg>
            Edit
          </Button>
          <Button 
            variant="outline"
            size="sm"
            className="text-xs"
            disabled={modFiles.length === 0}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="mr-1"
              width="12" 
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Download
          </Button>
        </div>
      </div>
      
      {modFiles.length === 0 ? (
        <div className="flex-1 flex items-center justify-center bg-background rounded-md">
          <div className="text-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="mx-auto mb-4 text-gray-500"
              width="40" 
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            <p className="text-gray-500">
              No code files have been generated yet.<br />
              Start by generating a mod!
            </p>
          </div>
        </div>
      ) : (
        <div className="flex-1 grid grid-cols-5 gap-4 h-full">
          {/* File browser */}
          <div className="col-span-1 bg-background rounded-md p-2 overflow-hidden flex flex-col">
            <h4 className="text-xs font-medium uppercase text-gray-400 px-2 pb-2">Files</h4>
            <ScrollArea className="flex-1">
              <div className="space-y-1.5">
                {Object.entries(filesByFolder).map(([folder, files]) => (
                  <div key={folder} className="mb-3">
                    <div className="flex items-center px-2 py-1 text-xs text-gray-400">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="mr-1"
                        width="12" 
                        height="12"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M3 8a2 2 0 0 1 2-2h4.586a1 1 0 0 1 .707.293L12 8h9a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                      </svg>
                      {folder === 'root' ? 'Root' : folder}
                    </div>
                    <div className="mt-1 pl-3">
                      {files.map(file => {
                        // Get just the filename from the path
                        const filename = file.path.split('/').pop() || file.path;
                        return (
                          <button
                            key={file.path}
                            className={`w-full text-left px-2 py-1 text-xs rounded mb-0.5 hover:bg-gray-700 ${activeFile === file.path ? 'bg-primary bg-opacity-20 text-primary' : 'text-gray-200'}`}
                            onClick={() => setActiveFile(file.path)}
                          >
                            <div className="flex items-center">
                              <svg
                                xmlns="http://www.w3.org/2000/svg"
                                className="mr-1 flex-shrink-0"
                                width="10" 
                                height="10"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                              >
                                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
                              </svg>
                              <span className="truncate">{filename}</span>
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </div>
          
          {/* Code viewer */}
          <div className="col-span-4 bg-background rounded-md flex flex-col overflow-hidden">
            {activeFile && (
              <>
                <div className="border-b border-gray-700 px-4 py-2 text-sm font-medium text-gray-300 flex justify-between items-center">
                  <div className="flex items-center">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="mr-2 text-gray-400"
                      width="14" 
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
                    </svg>
                    {activeFile}
                  </div>
                  <div className="text-xs text-gray-400">
                    {modFiles.find(f => f.path === activeFile)?.content.split('\n').length || 0} lines
                  </div>
                </div>
                <ScrollArea className="flex-1 h-full">
                  <div className="p-4 font-mono text-sm">
                    <div 
                      className="text-white"
                      dangerouslySetInnerHTML={{ __html: highlightSyntax(getActiveFileContent()) }}
                    />
                  </div>
                </ScrollArea>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
