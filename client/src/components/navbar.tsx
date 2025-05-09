import React from "react";
import { useModContext } from "@/context/mod-context";

export function Navbar() {
  return (
    <header className="bg-surface border-b border-gray-700 py-2 px-4 flex items-center justify-between">
      <div className="flex items-center">
        <button
          id="sidebar-toggle"
          className="md:hidden mr-3 text-white"
          onClick={() => {
            const sidebar = document.getElementById("sidebar");
            if (sidebar) {
              sidebar.classList.toggle("mobile-sidebar-hidden");
            }
          }}
        >
          <i className="mdi mdi-menu text-2xl"></i>
        </button>
        <div className="flex items-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="text-primary mr-2"
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M3 6h18"></path>
            <path d="M3 12h18"></path>
            <path d="M3 18h18"></path>
            <path d="M16 6l4 6-4 6"></path>
            <path d="M8 18l-4-6 4-6"></path>
            <path d="M13 12a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"></path>
          </svg>
          <h1 className="text-xl font-bold text-white">MinecraftAI Mod Creator</h1>
        </div>
      </div>
      <div className="flex items-center">
        <button 
          className="bg-primary hover:bg-opacity-80 text-white px-4 py-1 rounded-md flex items-center text-sm"
          onClick={() => {
            alert("GitHub integration coming soon!");
          }}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="mr-2"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"></path>
            <path d="M9 18c-4.51 2-5-2-7-2"></path>
          </svg>
          <span>Connect to GitHub</span>
        </button>
      </div>
    </header>
  );
}
