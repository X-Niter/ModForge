import React from "react";
import { useModContext } from "@/context/mod-context";
import { ModLoaderSelect } from "./modloader-select";
import { useLocation } from "wouter";
import { NavLink } from "./nav-link";

export function Sidebar() {
  const { currentMod, currentBuild } = useModContext();
  const [location] = useLocation();
  
  const status = currentBuild?.status || "No builds yet";
  const statusColor = status === "success" ? "bg-success" : 
                     status === "failed" ? "bg-error" : 
                     "bg-accent";
  
  const statusText = status === "success" ? "Compilation Successful" :
                    status === "failed" ? "Compilation Failed" :
                    status === "in_progress" ? "Compiling..." :
                    "No builds yet";

  return (
    <aside
      id="sidebar"
      className="w-64 bg-surface border-r border-gray-700 flex-shrink-0 transition-transform duration-300 md:translate-x-0"
    >
      <div className="flex flex-col h-full">
        <div className="p-4 border-b border-gray-700">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-bold text-white">Project Info</h2>
            <span className="px-2 py-1 bg-accent text-xs rounded-full text-white">
              Active
            </span>
          </div>

          <div className="mb-3">
            <p className="text-xs text-gray-400 mb-1">Current Project</p>
            <p className="text-sm font-medium text-white">
              {currentMod?.name || "No project selected"}
            </p>
          </div>

          <div className="mb-3">
            <p className="text-xs text-gray-400 mb-1">Mod Loader</p>
            <ModLoaderSelect />
          </div>

          <div>
            <p className="text-xs text-gray-400 mb-1">Status</p>
            <div className="flex items-center">
              <span className={`inline-block w-2 h-2 rounded-full ${statusColor} mr-2`}></span>
              <span className="text-sm">{statusText}</span>
            </div>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto hide-scroll py-2">
          <NavLink href="/" 
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
                width="20" 
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12 2c-5.5 0-10 4-10 9a9.6 9.6 0 0 0 2.3 6.4L2 22l5-2a9.3 9.3 0 0 0 5 1c5.5 0 10-4 10-9s-4.5-9-10-9Z"></path>
                <path d="M16 10c-.3-.3-.7-.3-1 0l-3 3-1-1c-.3-.3-.7-.3-1 0"></path>
              </svg>
            }
          >
            Mod Generator
          </NavLink>
          
          <NavLink href="/code-explorer"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
                width="20" 
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <path d="m10 13-2 2 2 2"></path>
                <path d="m14 17 2-2-2-2"></path>
              </svg>
            }
          >
            Code Explorer
          </NavLink>
          
          <NavLink href="/compilation"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
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
            }
          >
            Compilation Status
          </NavLink>
          
          <NavLink href="/build-history"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
                width="20" 
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0"></path>
                <path d="M12 8v4l2 2"></path>
              </svg>
            }
          >
            Build History
          </NavLink>
          
          <NavLink href="/github-integration"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
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
            }
          >
            GitHub Integration
          </NavLink>
          
          <NavLink href="/settings"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="mr-3"
                width="20" 
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            }
          >
            Settings
          </NavLink>
        </nav>

        <div className="p-4 border-t border-gray-700">
          <div className="flex items-center">
            <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="text-white"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div className="ml-2">
              <p className="text-sm font-medium text-white">User</p>
              <p className="text-xs text-gray-400">Free Tier</p>
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}