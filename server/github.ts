import axios from 'axios';
import { storage } from "./storage";
import { githubLogger } from "./logger";

// GitHub API helper class
class GitHubClient {
  private baseUrl = 'https://api.github.com';
  private token: string;
  
  constructor(token: string) {
    this.token = token;
  }
  
  /**
   * Create a GitHubClient from the user's session if available
   * @param req Express request with session
   * @returns GitHubClient instance or null if no token available
   */
  static async fromSession(req: any): Promise<GitHubClient | null> {
    // Check if there's a session with a logged in user
    if (req.session && req.session.userId) {
      try {
        // Get the user from storage
        const user = await storage.getUser(req.session.userId);
        
        // If user exists and has a GitHub token, create a client
        if (user && user.githubToken) {
          return new GitHubClient(user.githubToken);
        }
      } catch (error) {
        console.error('Error creating GitHub client from session:', error);
      }
    }
    
    return null;
  }
  
  // Generic request method
  private async request<T>(method: string, endpoint: string, data?: any): Promise<T> {
    try {
      const url = `${this.baseUrl}${endpoint}`;
      const response = await axios({
        method,
        url,
        data,
        headers: {
          'Authorization': `token ${this.token}`,
          'Accept': 'application/vnd.github.v3+json',
          'Content-Type': 'application/json'
        }
      });
      
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        throw new Error(`GitHub API error: ${error.response.status} - ${JSON.stringify(error.response.data)}`);
      }
      throw error;
    }
  }
  
  // Check if repository exists
  async checkRepoExists(owner: string, repo: string): Promise<boolean> {
    try {
      await this.request('GET', `/repos/${owner}/${repo}`);
      return true;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response && error.response.status === 404) {
        return false;
      }
      throw error;
    }
  }
  
  // Get authenticated user info
  async getAuthenticatedUser(): Promise<{ login: string }> {
    return this.request<{ login: string }>('GET', '/user');
  }
  
  // Create a new repository
  async createRepo(name: string, description: string): Promise<{ html_url: string, clone_url: string }> {
    const data = {
      name,
      description,
      private: false,
      auto_init: true
    };
    
    return this.request<{ html_url: string, clone_url: string }>('POST', '/user/repos', data);
  }
  
  // Create or update a file in a repository
  async createOrUpdateFile(
    owner: string, 
    repo: string, 
    path: string, 
    content: string, 
    message: string
  ): Promise<void> {
    // First check if file exists
    let sha: string | undefined;
    try {
      const response = await this.request<{sha: string}>('GET', `/repos/${owner}/${repo}/contents/${path}`);
      sha = response.sha;
    } catch (error) {
      // File doesn't exist, that's fine
    }
    
    // Create or update the file
    const data: any = {
      message,
      content: Buffer.from(content).toString('base64'),
      branch: 'main' // Or use 'master' depending on what the repo uses
    };
    
    if (sha) {
      data.sha = sha;
    }
    
    await this.request('PUT', `/repos/${owner}/${repo}/contents/${path}`, data);
  }
}

// Function to push a mod to GitHub
export async function pushModToGitHub(
  modId: number,
  req?: any,
  githubToken?: string
): Promise<{
  success: boolean;
  repoUrl?: string;
  owner?: string;
  error?: string;
  logs?: string;
}> {
  let logs = '';
  
  const addLog = (message: string) => {
    logs += `${message}\n`;
    githubLogger.info(message);
  };
  
  try {
    // Get the mod from storage
    const mod = await storage.getMod(modId);
    if (!mod) {
      throw new Error(`Mod with ID ${modId} not found`);
    }
    
    // Try to create GitHub client from session first
    let github: GitHubClient | null = null;
    
    if (req) {
      github = await GitHubClient.fromSession(req);
      if (github) {
        addLog('Using GitHub authentication from your account');
      }
    }
    
    // Fall back to token if session doesn't have one
    if (!github) {
      const token = githubToken || process.env.GITHUB_TOKEN;
      if (!token) {
        return {
          success: false,
          error: "GitHub authentication failed. Please log in with GitHub or provide a GitHub token.",
          logs
        };
      }
      
      github = new GitHubClient(token);
    }
    
    addLog(`Starting GitHub integration for mod: ${mod.name}`);
    
    // Get authenticated user
    addLog('Authenticating with GitHub...');
    const user = await github.getAuthenticatedUser();
    const owner = user.login;
    
    addLog(`Authenticated as: ${owner}`);
    
    // Create repository name - sanitize the mod name for GitHub (alphanumeric and dashes only)
    const modName = mod.name || 'minecraft-mod';
    const repoName = `minecraft-${modName.toLowerCase()}-mod`.replace(/[^a-z0-9-]/g, '-');
    
    // Check if repo exists
    const repoExists = await github.checkRepoExists(owner, repoName);
    
    let repoUrl = '';
    if (repoExists) {
      addLog(`Repository ${owner}/${repoName} already exists. Updating files...`);
      repoUrl = `https://github.com/${owner}/${repoName}`;
    } else {
      // Create the repository
      addLog(`Creating new repository: ${repoName}...`);
      const repo = await github.createRepo(repoName, mod.description || `${mod.name} - A Minecraft mod`);
      repoUrl = repo.html_url;
      addLog(`Repository created: ${repoUrl}`);
    }
    
    // Get all mod files
    const modFiles = await storage.getModFilesByModId(modId);
    if (modFiles.length === 0) {
      addLog('No files found for this mod.');
      return {
        success: false,
        error: "No files found for this mod.",
        logs
      };
    }
    
    // Add README.md if it doesn't exist
    const hasReadme = modFiles.some(f => f.path.toLowerCase() === 'readme.md');
    if (!hasReadme) {
      addLog('Creating README.md...');
      
      // Get metadata for license and features if available
      const license = mod.metadata && typeof mod.metadata === 'object' && 'license' in mod.metadata 
        ? mod.metadata.license
        : 'MIT License';
      const features = mod.metadata && typeof mod.metadata === 'object' && 'features' in mod.metadata
        ? mod.metadata.features
        : mod.description;
        
      const readmeContent = `# ${mod.name}

${mod.description || ''}

## Minecraft Version
${mod.minecraftVersion}

## Mod Loader
${mod.modLoader}

## License
${license}

## Features
${features}
`;
      
      await github.createOrUpdateFile(
        owner,
        repoName,
        'README.md',
        readmeContent,
        'Add README.md'
      );
    }
    
    // Push all files to the repository
    addLog(`Pushing ${modFiles.length} files to repository...`);
    
    // We'll push files in small batches to avoid rate limiting
    const batchSize = 5;
    for (let i = 0; i < modFiles.length; i += batchSize) {
      const batch = modFiles.slice(i, i + batchSize);
      await Promise.all(batch.map(async (file) => {
        addLog(`Pushing file: ${file.path}`);
        await github.createOrUpdateFile(
          owner,
          repoName,
          file.path,
          file.content,
          `Update ${file.path}`
        );
      }));
    }
    
    // Update the mod with the repo URL
    await storage.updateMod(modId, {
      githubRepo: repoUrl,
    });
    
    addLog('GitHub push completed successfully!');
    
    return {
      success: true,
      repoUrl,
      owner,
      logs
    };
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    addLog(`Error pushing to GitHub: ${errorMessage}`);
    
    return {
      success: false,
      error: errorMessage,
      logs
    };
  }
}
