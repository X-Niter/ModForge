import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";

interface ModLoaderSelectProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  disabled?: boolean;
}

export function ModLoaderSelect({
  value,
  onChange,
  label = "Mod Loader",
  disabled = false,
}: ModLoaderSelectProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor="mod-loader">{label}</Label>
      <Select
        value={value}
        onValueChange={onChange}
        disabled={disabled}
      >
        <SelectTrigger id="mod-loader" className="w-full">
          <SelectValue placeholder="Select mod loader" />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            <SelectLabel>Mod Loaders</SelectLabel>
            <SelectItem value="forge">Forge</SelectItem>
            <SelectItem value="fabric">Fabric</SelectItem>
            <SelectItem value="quilt">Quilt</SelectItem>
            <SelectItem value="architectury">Architectury (Cross-loader)</SelectItem>
          </SelectGroup>
        </SelectContent>
      </Select>
    </div>
  );
}