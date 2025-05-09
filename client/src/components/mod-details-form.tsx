import React from "react";
import { useModContext } from "@/context/mod-context";
import { useModGeneration } from "@/hooks/use-mod-generation";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";

export function ModDetailsForm({ formData, setFormData }: {
  formData: any;
  setFormData: (data: any) => void;
}) {
  const { currentMod } = useModContext();
  const data = currentMod || formData;
  
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };
  
  const handleSelectChange = (name: string, value: string) => {
    setFormData({ ...formData, [name]: value });
  };

  return (
    <div className="bg-surface rounded-lg shadow-lg p-5 mb-6">
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
          <circle cx="12" cy="12" r="10"></circle>
          <path d="M12 16v-4"></path>
          <path d="M12 8h.01"></path>
        </svg>
        Mod Details
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <Label className="text-gray-400 mb-1">Mod Name</Label>
          <Input
            type="text"
            name="name"
            value={data?.name || ""}
            onChange={handleChange}
            className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none"
          />
        </div>
        <div>
          <Label className="text-gray-400 mb-1">Mod ID</Label>
          <Input
            type="text"
            name="modId"
            value={data?.modId || ""}
            onChange={handleChange}
            className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none"
          />
        </div>
      </div>

      <div className="mb-4">
        <Label className="text-gray-400 mb-1">Mod Description</Label>
        <Textarea
          name="description"
          value={data?.description || ""}
          onChange={handleChange}
          className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none"
          rows={2}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <Label className="text-gray-400 mb-1">Version</Label>
          <Input
            type="text"
            name="version"
            value={data?.version || ""}
            onChange={handleChange}
            className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none"
          />
        </div>
        <div>
          <Label className="text-gray-400 mb-1">Minecraft Version</Label>
          <Input
            type="text"
            name="minecraftVersion"
            value={data?.minecraftVersion || ""}
            onChange={handleChange}
            className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none"
          />
        </div>
        <div>
          <Label className="text-gray-400 mb-1">License</Label>
          <Select
            defaultValue={data?.license || "MIT"}
            onValueChange={(value) => handleSelectChange("license", value)}
          >
            <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none">
              <SelectValue placeholder="Select a license" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="MIT">MIT</SelectItem>
              <SelectItem value="Apache 2.0">Apache 2.0</SelectItem>
              <SelectItem value="GPL 3.0">GPL 3.0</SelectItem>
              <SelectItem value="All Rights Reserved">All Rights Reserved</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>
  );
}
