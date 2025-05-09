import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { apiRequest } from "@/lib/queryClient";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useToast } from "@/hooks/use-toast";
import { Loader2, Sparkles } from "lucide-react";
import { modLoaders } from "@shared/schema";

// Idea generator request schema
const formSchema = z.object({
  theme: z.string().optional(),
  complexity: z.enum(["simple", "medium", "complex"]).default("medium"),
  preferredModLoader: z.enum(["Any", ...modLoaders]).default("Any"),
  gameVersion: z.string().optional(),
  existingIdeas: z.string().optional().transform(val => 
    val ? val.split(',').map(s => s.trim()).filter(Boolean) : []
  ),
});

// Type for the form data
type FormData = z.infer<typeof formSchema>;

interface IdeaGeneratorFormProps {
  onSuccess: (ideas: any) => void;
}

export function IdeaGeneratorForm({ onSuccess }: IdeaGeneratorFormProps) {
  const [isLoading, setIsLoading] = useState(false);
  const { toast } = useToast();

  // Define form with react-hook-form and zod validation
  const form = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      theme: "",
      complexity: "medium",
      preferredModLoader: "Any",
      gameVersion: "1.20.4",
      existingIdeas: "",
    },
  });

  // Function to handle form submission
  async function onSubmit(data: FormData) {
    setIsLoading(true);
    try {
      const response = await apiRequest<{ ideas: any[], inspirations: string[] }>("/api/ai/generate-ideas", {
        method: "POST",
        body: JSON.stringify(data),
      });
      
      onSuccess(response);
      
      toast({
        title: "Ideas Generated",
        description: `Generated ${response.ideas.length} mod ideas`,
      });
    } catch (error) {
      console.error("Error generating ideas:", error);
      toast({
        variant: "destructive",
        title: "Error",
        description: "Failed to generate mod ideas. Please try again.",
      });
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="space-y-6 bg-white dark:bg-slate-900 p-6 rounded-lg shadow-md">
      <div className="mb-4">
        <h3 className="text-lg font-semibold mb-1 flex items-center">
          <Sparkles className="mr-2 h-5 w-5 text-primary" />
          AI Mod Idea Generator
        </h3>
        <p className="text-sm text-muted-foreground">
          Let AI help you brainstorm creative Minecraft mod ideas. Provide some parameters to guide the generation.
        </p>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
            name="theme"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Theme (Optional)</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. Fantasy, Technology, Exploration" {...field} />
                </FormControl>
                <FormDescription>
                  Provide a theme to focus the generated ideas
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="complexity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Complexity</FormLabel>
                  <Select 
                    onValueChange={field.onChange} 
                    defaultValue={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select complexity" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="simple">Simple</SelectItem>
                      <SelectItem value="medium">Medium</SelectItem>
                      <SelectItem value="complex">Complex</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormDescription>
                    How complex should the mod ideas be?
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="preferredModLoader"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Preferred Mod Loader</FormLabel>
                  <Select 
                    onValueChange={field.onChange} 
                    defaultValue={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select mod loader" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="Any">Any</SelectItem>
                      {modLoaders.map(loader => (
                        <SelectItem key={loader} value={loader}>{loader}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormDescription>
                    Select a specific mod loader or "Any"
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <FormField
            control={form.control}
            name="gameVersion"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Game Version (Optional)</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. 1.20.4" {...field} />
                </FormControl>
                <FormDescription>
                  Specify a Minecraft version
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="existingIdeas"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Ideas to Avoid (Optional)</FormLabel>
                <FormControl>
                  <Textarea 
                    placeholder="Enter ideas to avoid, separated by commas" 
                    className="h-20"
                    {...field} 
                  />
                </FormControl>
                <FormDescription>
                  List any existing ideas you want to avoid
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <Button 
            type="submit" 
            className="w-full" 
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Generating Ideas...
              </>
            ) : (
              <>
                <Sparkles className="mr-2 h-4 w-4" />
                Generate Mod Ideas
              </>
            )}
          </Button>
        </form>
      </Form>
    </div>
  );
}