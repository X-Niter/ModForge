import { useState } from "react";
import { generateCode } from "@/lib/openai";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Loader2, Code, CheckCircle2, AlertTriangle } from "lucide-react";

export function CodeGenerator() {
  const [prompt, setPrompt] = useState("");
  const [language, setLanguage] = useState("java");
  const [complexity, setComplexity] = useState<"simple" | "medium" | "complex">("medium");
  const [context, setContext] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedCode, setGeneratedCode] = useState("");
  const [explanation, setExplanation] = useState("");
  const [error, setError] = useState<string | null>(null);
  
  async function handleGenerateCode() {
    if (!prompt) return;
    
    setIsGenerating(true);
    setError(null);
    
    try {
      const result = await generateCode(prompt, {
        language,
        complexity,
        context: context || undefined
      });
      
      setGeneratedCode(result.code);
      setExplanation(result.explanation);
    } catch (err) {
      setError(err instanceof Error ? err.message : "An unknown error occurred");
    } finally {
      setIsGenerating(false);
    }
  }
  
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Code Generator</CardTitle>
          <CardDescription>Generate code using AI</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="prompt">Prompt</Label>
            <Textarea 
              id="prompt"
              placeholder="Describe the code you want to generate..." 
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              className="min-h-[120px]"
            />
          </div>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="language">Language</Label>
              <Select value={language} onValueChange={setLanguage}>
                <SelectTrigger id="language">
                  <SelectValue placeholder="Select language" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="java">Java</SelectItem>
                  <SelectItem value="javascript">JavaScript</SelectItem>
                  <SelectItem value="typescript">TypeScript</SelectItem>
                  <SelectItem value="python">Python</SelectItem>
                  <SelectItem value="cpp">C++</SelectItem>
                  <SelectItem value="csharp">C#</SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="complexity">Complexity</Label>
              <Select 
                value={complexity} 
                onValueChange={(value) => setComplexity(value as "simple" | "medium" | "complex")}
              >
                <SelectTrigger id="complexity">
                  <SelectValue placeholder="Select complexity" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="simple">Simple</SelectItem>
                  <SelectItem value="medium">Medium</SelectItem>
                  <SelectItem value="complex">Complex</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="context">Context (Optional)</Label>
            <Textarea 
              id="context"
              placeholder="Provide additional context or existing code..." 
              value={context}
              onChange={(e) => setContext(e.target.value)}
              className="min-h-[80px]"
            />
          </div>
        </CardContent>
        <CardFooter>
          <Button 
            onClick={handleGenerateCode} 
            disabled={isGenerating || !prompt} 
            className="w-full"
          >
            {isGenerating ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Generating code...
              </>
            ) : (
              <>
                <Code className="mr-2 h-4 w-4" />
                Generate Code
              </>
            )}
          </Button>
        </CardFooter>
      </Card>
      
      <Card>
        <CardHeader>
          <CardTitle>Generated Code</CardTitle>
          <CardDescription>AI-generated code based on your requirements</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {error ? (
            <div className="bg-destructive/10 border border-destructive rounded-md p-4 text-sm">
              <div className="flex items-center">
                <AlertTriangle className="h-4 w-4 mr-2 text-destructive" />
                <div>Error: {error}</div>
              </div>
            </div>
          ) : generatedCode ? (
            <>
              <div className="bg-muted rounded-md p-4 font-mono text-sm overflow-auto max-h-[300px]">
                <pre className="whitespace-pre-wrap">{generatedCode}</pre>
              </div>
              
              {explanation && (
                <div className="bg-primary/5 rounded-md p-4 text-sm">
                  <div className="font-semibold mb-2">Explanation:</div>
                  <div>{explanation}</div>
                </div>
              )}
            </>
          ) : (
            <div className="flex items-center justify-center h-[300px] text-muted-foreground text-center flex-col">
              <Code className="h-12 w-12 mb-4 opacity-20" />
              <p>Generated code will appear here</p>
              <p className="text-sm mt-2">Enter a prompt and click "Generate Code"</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}