import { Switch, Route } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ModProvider } from "@/context/mod-context";
import Home from "@/pages/home";
import NotFound from "@/pages/not-found";
import GitHubIntegration from "@/pages/github-integration";
import Settings from "@/pages/settings";
import ContinuousDevelopment from "@/pages/continuous-development";
import IdeaGenerator from "@/pages/idea-generator";

function Router() {
  return (
    <Switch>
      <Route path="/" component={Home}/>
      <Route path="/github-integration" component={GitHubIntegration}/>
      <Route path="/settings" component={Settings}/>
      <Route path="/continuous-development" component={ContinuousDevelopment}/>
      <Route path="/idea-generator" component={IdeaGenerator}/>
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <ModProvider>
          <Toaster />
          <Router />
        </ModProvider>
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
