import { CodeGenerator } from "@/components/code-generator";

export default function CodeGeneratorPage() {
  return (
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gradient-to-r from-blue-600 to-purple-600">
          AI Code Generator
        </h1>
        <p className="text-lg text-muted-foreground">
          Generate code for your Minecraft mod or any other programming needs
        </p>
      </div>
      
      <CodeGenerator />
    </div>
  );
}