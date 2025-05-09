import React, { useEffect, useRef } from "react";
import { useModContext } from "@/context/mod-context";
import { Button } from "@/components/ui/button";

export function ConsoleOutput() {
  const { consoleOutput, clearConsoleOutput } = useModContext();
  const consoleRef = useRef<HTMLDivElement>(null);
  
  // Auto-scroll to bottom when new messages are added
  useEffect(() => {
    if (consoleRef.current) {
      consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }
  }, [consoleOutput]);
  
  // Function to determine the text color based on the message content
  const getMessageColor = (message: string) => {
    if (message.includes("Error") || message.includes("error") || message.includes("failed") || message.includes("Failed")) {
      return "text-error";
    } else if (message.includes("Warning") || message.includes("warning")) {
      return "text-accent";
    } else if (message.includes("Success") || message.includes("success") || message.includes("Successful") || message.includes("successful")) {
      return "text-success";
    } else {
      return "text-gray-400";
    }
  };

  return (
    <div className="mt-auto">
      <div className="flex items-center justify-between bg-surface px-4 py-2 border-t border-gray-700">
        <div className="flex items-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="mr-2 text-secondary"
            width="16" 
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="16 18 22 12 16 6"></polyline>
            <polyline points="8 6 2 12 8 18"></polyline>
          </svg>
          <h3 className="text-sm font-medium">Console Output</h3>
        </div>
        <div>
          <Button
            variant="ghost"
            size="sm"
            className="text-gray-400 hover:text-white"
            onClick={clearConsoleOutput}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16" 
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <path d="m15 9-6 6"></path>
              <path d="m9 9 6 6"></path>
            </svg>
          </Button>
        </div>
      </div>
      <div 
        ref={consoleRef} 
        className="console bg-background border-t border-gray-700 p-3 text-sm font-mono overflow-auto"
        style={{ minHeight: "150px", maxHeight: "250px" }}
      >
        {consoleOutput.length === 0 ? (
          <p className="text-gray-500">Console output will appear here...</p>
        ) : (
          consoleOutput.map((message, index) => (
            <p key={index} className={getMessageColor(message)}>
              {message}
            </p>
          ))
        )}
      </div>
    </div>
  );
}
