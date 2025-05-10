import { Switch, Route } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ModProvider } from "@/context/mod-context";
import { AuthProvider } from "@/hooks/use-auth";
import { ProtectedRoute } from "@/components/protected-route";
import { Layout } from "@/components/layout/layout";
import Home from "@/pages/home";
import NotFound from "@/pages/not-found";
import GitHubIntegration from "@/pages/github-integration";
import Settings from "@/pages/settings";
import ContinuousDevelopment from "@/pages/continuous-development";
import IdeaGenerator from "@/pages/idea-generator";
import CodeGeneratorPage from "@/pages/code-generator";
import Documentation from "@/pages/documentation";
import TermsOfService from "@/pages/terms";
import LicensePage from "@/pages/license";
import MetricsPage from "@/pages/metrics";
import ErrorResolutionPage from "@/pages/error-resolution";
import WebExplorerPage from "@/pages/web-explorer";
import JarAnalyzerPage from "@/pages/jar-analyzer";
import AuthPage from "@/pages/auth-page";

function Router() {
  return (
    <Switch>
      {/* Auth page doesn't use the Layout component */}
      <Route path="/auth" component={AuthPage}/>
      
      {/* All other pages use the Layout component */}
      <Route>
        <Layout>
          <Switch>
            <Route path="/" component={Home}/>
            
            {/* Protected routes that require authentication */}
            <ProtectedRoute path="/github-integration" component={GitHubIntegration}/>
            <ProtectedRoute path="/settings" component={Settings}/>
            <ProtectedRoute path="/continuous-development" component={ContinuousDevelopment}/>
            <ProtectedRoute path="/idea-generator" component={IdeaGenerator}/>
            <ProtectedRoute path="/code-generator" component={CodeGeneratorPage}/>
            <ProtectedRoute path="/documentation" component={Documentation}/>
            <ProtectedRoute path="/metrics" component={MetricsPage}/>
            <ProtectedRoute path="/error-resolution" component={ErrorResolutionPage}/>
            <ProtectedRoute path="/web-explorer" component={WebExplorerPage}/>
            <ProtectedRoute path="/jar-analyzer" component={JarAnalyzerPage}/>
            
            {/* Public routes */}
            <Route path="/terms" component={TermsOfService}/>
            <Route path="/license" component={LicensePage}/>
            <Route component={NotFound} />
          </Switch>
        </Layout>
      </Route>
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <AuthProvider>
          <ModProvider>
            <Toaster />
            <Router />
          </ModProvider>
        </AuthProvider>
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
