import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { GitHubOAuthButton } from "@/components/github-oauth-button";
import { useToast } from "@/hooks/use-toast";
import { apiRequest } from "@/lib/queryClient";
import { useAuth } from "@/hooks/use-auth";
import { Redirect } from "wouter";
import { Loader2 } from "lucide-react";

const LoginForm = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  const { loginMutation } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      toast({
        title: "Error",
        description: "Please fill in all fields",
        variant: "destructive"
      });
      return;
    }

    try {
      setLoading(true);
      await loginMutation.mutateAsync({ username, password });

      toast({
        title: "Success",
        description: "Logged in successfully",
      });
    } catch (error) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : "Failed to login";
      
      toast({
        title: "Login failed",
        description: errorMessage,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="username">Username</Label>
        <Input 
          id="username" 
          value={username} 
          onChange={(e) => setUsername(e.target.value)}
          placeholder="username" 
          disabled={loading}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <Input 
          id="password" 
          type="password" 
          value={password} 
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••" 
          disabled={loading}
        />
      </div>
      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Logging in...
          </>
        ) : (
          "Login"
        )}
      </Button>
    </form>
  );
};

const RegisterForm = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    displayName: ''
  });
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  const { registerMutation } = useAuth();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.username || !formData.password || !formData.confirmPassword) {
      toast({
        title: "Error",
        description: "Please fill in all required fields",
        variant: "destructive"
      });
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      toast({
        title: "Error",
        description: "Passwords do not match",
        variant: "destructive"
      });
      return;
    }

    try {
      setLoading(true);
      await registerMutation.mutateAsync(formData);

      toast({
        title: "Success",
        description: "Account created successfully",
      });
    } catch (error) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : "Failed to create account";
      
      toast({
        title: "Registration failed",
        description: errorMessage,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="username">Username *</Label>
        <Input 
          id="username" 
          name="username"
          value={formData.username} 
          onChange={handleChange}
          placeholder="username" 
          disabled={loading}
          required
        />
      </div>
      <div className="grid gap-4 grid-cols-1 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="password">Password *</Label>
          <Input 
            id="password" 
            name="password"
            type="password" 
            value={formData.password} 
            onChange={handleChange}
            placeholder="••••••••" 
            disabled={loading}
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirm Password *</Label>
          <Input 
            id="confirmPassword" 
            name="confirmPassword"
            type="password" 
            value={formData.confirmPassword} 
            onChange={handleChange}
            placeholder="••••••••" 
            disabled={loading}
            required
          />
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input 
          id="email" 
          name="email"
          type="email" 
          value={formData.email} 
          onChange={handleChange}
          placeholder="your@email.com" 
          disabled={loading}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="displayName">Display Name</Label>
        <Input 
          id="displayName" 
          name="displayName"
          value={formData.displayName} 
          onChange={handleChange}
          placeholder="Your Name" 
          disabled={loading}
        />
      </div>
      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Creating Account...
          </>
        ) : (
          "Create Account"
        )}
      </Button>
    </form>
  );
};

export default function AuthPage() {
  const { user, isLoading } = useAuth();
  
  // Redirect to home if already logged in
  if (!isLoading && user) {
    return <Redirect to="/" />;
  }

  return (
    <div className="container flex h-screen w-screen flex-col items-center justify-center">
      <div className="mx-auto flex w-full flex-col justify-center space-y-6 sm:w-[350px] md:w-[550px]">
        <div className="flex flex-col space-y-2 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">
            Welcome to ModForge
          </h1>
          <p className="text-sm text-muted-foreground">
            Sign in to your account or create a new one to get started
          </p>
        </div>

        <Tabs defaultValue="login" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="login">Login</TabsTrigger>
            <TabsTrigger value="register">Register</TabsTrigger>
          </TabsList>
          
          <Card>
            <TabsContent value="login" className="mt-0">
              <CardHeader>
                <CardTitle>Login</CardTitle>
                <CardDescription>
                  Enter your credentials to access your account
                </CardDescription>
              </CardHeader>
              <CardContent>
                <LoginForm />
              </CardContent>
              <CardFooter className="flex flex-col border-t pt-6">
                <div className="mt-4 flex items-center">
                  <div className="w-full border-t" />
                  <div className="mx-4 text-xs text-gray-500">OR</div>
                  <div className="w-full border-t" />
                </div>
                <div className="mt-4">
                  <GitHubOAuthButton />
                </div>
              </CardFooter>
            </TabsContent>
            
            <TabsContent value="register" className="mt-0">
              <CardHeader>
                <CardTitle>Create Account</CardTitle>
                <CardDescription>
                  Fill in your details to create a new account
                </CardDescription>
              </CardHeader>
              <CardContent>
                <RegisterForm />
              </CardContent>
              <CardFooter className="flex flex-col border-t pt-6">
                <div className="mt-4 flex items-center">
                  <div className="w-full border-t" />
                  <div className="mx-4 text-xs text-gray-500">OR</div>
                  <div className="w-full border-t" />
                </div>
                <div className="mt-4">
                  <GitHubOAuthButton />
                </div>
              </CardFooter>
            </TabsContent>
          </Card>
        </Tabs>
      </div>
    </div>
  );
}