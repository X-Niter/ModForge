import React from "react";
import { useModContext } from "@/context/mod-context";
import { useModGeneration } from "@/hooks/use-mod-generation";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { modLoaders } from "@shared/schema";

export function ModLoaderSelect() {
  const { currentMod } = useModContext();
  const { updateMod, isLoading } = useModGeneration();

  const handleModLoaderChange = (value: string) => {
    if (currentMod) {
      updateMod(currentMod.id, { modLoader: value as any });
    }
  };

  return (
    <div className="relative">
      <Select
        defaultValue={currentMod?.modLoader || "Forge"}
        onValueChange={handleModLoaderChange}
        disabled={isLoading || !currentMod}
      >
        <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-1 px-2 text-sm">
          <SelectValue placeholder="Select mod loader" />
        </SelectTrigger>
        <SelectContent>
          {modLoaders.map((loader) => (
            <SelectItem key={loader} value={loader}>
              {loader} 1.19.2
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
