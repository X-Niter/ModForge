import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useModContext } from "@/context/mod-context";
import { Badge } from "@/components/ui/badge";
import { ArrowRight } from "lucide-react";
import { Mod } from "@/types";

export function ModSelector() {
  const { currentMod, setCurrentMod } = useModContext();
  const [isOpen, setIsOpen] = useState(false);
  
  // Get list of mods
  const { data } = useQuery<{mods: Mod[]}>({
    queryKey: ['/api/mods'],
    refetchOnWindowFocus: false
  });
  
  const mods = data?.mods || [];
  
  return (
    <div className="relative">
      <button 
        className="bg-primary/90 hover:bg-primary text-white px-3 py-1 rounded text-sm flex items-center"
        onClick={() => setIsOpen(!isOpen)}
      >
        {currentMod ? "Change Mod" : "Select Mod"}
        <ArrowRight className="h-4 w-4 ml-1" />
      </button>
      
      {isOpen && (
        <div className="absolute right-0 mt-1 w-60 bg-white dark:bg-slate-800 rounded-md shadow-lg z-10 py-1">
          {mods.length === 0 ? (
            <div className="px-4 py-2 text-sm text-muted-foreground">
              No mods available
            </div>
          ) : (
            mods.map(mod => (
              <button
                key={mod.id}
                className="w-full text-left px-4 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center justify-between"
                onClick={() => {
                  setCurrentMod(mod);
                  setIsOpen(false);
                }}
              >
                <span>{mod.name}</span>
                <Badge variant="outline" className="text-xs">
                  {mod.modLoader}
                </Badge>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}