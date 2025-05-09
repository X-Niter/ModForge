import React from "react";
import { useModContext } from "@/context/mod-context";

export function CodeEditor() {
  const { modFiles } = useModContext();
  
  // For now, this is a simple placeholder for a code editor component
  // In a full implementation, you'd use something like CodeMirror or Monaco Editor
  
  return (
    <div className="bg-surface rounded-lg shadow-lg p-5 h-full">
      <h3 className="text-white font-medium mb-4 flex items-center">
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
      
      {modFiles.length === 0 ? (
        <div className="h-64 flex items-center justify-center bg-background rounded-md">
          <p className="text-gray-500">
            No code files have been generated yet. Start by generating a mod!
          </p>
        </div>
      ) : (
        <div className="bg-background rounded-md p-4 h-64 overflow-auto font-mono text-sm">
          <pre className="text-white">
            {/* Just show the first file for now */}
            {modFiles[0]?.content || ''}
          </pre>
        </div>
      )}
    </div>
  );
}
