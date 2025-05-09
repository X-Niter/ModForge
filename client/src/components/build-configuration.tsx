import React from "react";
import { useModContext } from "@/context/mod-context";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";
import { compileFrequencies, autoFixLevels } from "@shared/schema";

export function BuildConfiguration({ formData, setFormData }: {
  formData: any;
  setFormData: (data: any) => void;
}) {
  const { currentMod } = useModContext();
  const data = currentMod || formData;
  
  const handleSelectChange = (name: string, value: string) => {
    setFormData({ ...formData, [name]: value });
  };
  
  const handleCheckboxChange = (name: string, checked: boolean) => {
    setFormData({ ...formData, [name]: checked });
  };

  return (
    <div className="bg-surface rounded-lg shadow-lg p-5">
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
          <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
          <circle cx="12" cy="12" r="3"></circle>
        </svg>
        Build Configuration
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <Label className="text-gray-400 mb-1">Auto-compile Frequency</Label>
          <Select
            defaultValue={data?.compileFrequency || "Every 5 Minutes"}
            onValueChange={(value) => handleSelectChange("compileFrequency", value)}
          >
            <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none">
              <SelectValue placeholder="Select frequency" />
            </SelectTrigger>
            <SelectContent>
              {compileFrequencies.map((freq) => (
                <SelectItem key={freq} value={freq}>
                  {freq}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div>
          <Label className="text-gray-400 mb-1">Auto-fix Level</Label>
          <Select
            defaultValue={data?.autoFixLevel || "Balanced"}
            onValueChange={(value) => handleSelectChange("autoFixLevel", value)}
          >
            <SelectTrigger className="w-full bg-background border border-gray-700 rounded py-2 px-3 text-white focus:border-primary focus:outline-none">
              <SelectValue placeholder="Select auto-fix level" />
            </SelectTrigger>
            <SelectContent>
              {autoFixLevels.map((level) => (
                <SelectItem key={level} value={level}>
                  {level} {level === "Conservative" && "(Only Syntax)"}
                  {level === "Balanced" && "(Syntax + Common Issues)"}
                  {level === "Aggressive" && "(Full Automated Fixing)"}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-2">
          <Checkbox
            id="autoPushToGithub"
            checked={data?.autoPushToGithub || false}
            onCheckedChange={(checked) => 
              handleCheckboxChange("autoPushToGithub", checked as boolean)
            }
            className="bg-background border-gray-700 text-primary rounded"
          />
          <Label
            htmlFor="autoPushToGithub"
            className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
          >
            Auto-push to GitHub when build successful
          </Label>
        </div>
        
        <div className="flex items-center space-x-2">
          <Checkbox
            id="generateDocumentation"
            checked={data?.generateDocumentation || false}
            onCheckedChange={(checked) => 
              handleCheckboxChange("generateDocumentation", checked as boolean)
            }
            className="bg-background border-gray-700 text-primary rounded"
          />
          <Label
            htmlFor="generateDocumentation"
            className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
          >
            Generate documentation
          </Label>
        </div>
      </div>
    </div>
  );
}
