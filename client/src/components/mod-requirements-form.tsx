import React from "react";
import { useModContext } from "@/context/mod-context";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";

export function ModRequirementsForm({ formData, setFormData }: {
  formData: any;
  setFormData: (data: any) => void;
}) {
  const { currentMod } = useModContext();
  const data = currentMod || formData;
  
  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
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
          <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <path d="M12 18v-6"></path>
          <path d="M8 18v-1"></path>
          <path d="M16 18v-3"></path>
        </svg>
        Mod Requirements
      </h3>

      <div className="mb-4">
        <Label className="text-gray-400 mb-1">
          Describe your mod idea in detail
        </Label>
        <Textarea
          name="idea"
          value={data?.idea || ""}
          onChange={handleChange}
          className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none code-editor"
          rows={6}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label className="text-gray-400 mb-1">Feature Priority</Label>
          <Select
            defaultValue={data?.featurePriority || "Balanced Features"}
            onValueChange={(value) => handleSelectChange("featurePriority", value)}
          >
            <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none">
              <SelectValue placeholder="Select priority" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Code Correctness">Code Correctness</SelectItem>
              <SelectItem value="Balanced Features">Balanced Features</SelectItem>
              <SelectItem value="Performance">Performance</SelectItem>
              <SelectItem value="Compatibility">Compatibility</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div>
          <Label className="text-gray-400 mb-1">Coding Style</Label>
          <Select
            defaultValue={data?.codingStyle || "Well-Documented"}
            onValueChange={(value) => handleSelectChange("codingStyle", value)}
          >
            <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none">
              <SelectValue placeholder="Select coding style" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Concise">Concise</SelectItem>
              <SelectItem value="Well-Documented">Well-Documented</SelectItem>
              <SelectItem value="Highly Optimized">Highly Optimized</SelectItem>
              <SelectItem value="Beginner Friendly">Beginner Friendly</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>
  );
}
