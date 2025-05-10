/**
 * GitHub OAuth configuration
 */

export const githubOAuth = {
  // These values will be set from environment variables
  clientId: process.env.GITHUB_CLIENT_ID || '',
  clientSecret: process.env.GITHUB_CLIENT_SECRET || '',
  callbackURL: process.env.GITHUB_CALLBACK_URL || 'http://localhost:5000/api/auth/github/callback',
  
  // GitHub OAuth API endpoints
  authorizationURL: 'https://github.com/login/oauth/authorize',
  tokenURL: 'https://github.com/login/oauth/access_token',
  userProfileURL: 'https://api.github.com/user',
  
  // Required scopes for our application
  // repo: Full control of private repositories
  // user:email: Access to user's email
  scopes: ['repo', 'user:email'],
  
  // Returns the full authorization URL
  getAuthorizationURL(): string {
    const url = new URL(this.authorizationURL);
    url.searchParams.append('client_id', this.clientId);
    url.searchParams.append('redirect_uri', this.callbackURL);
    url.searchParams.append('scope', this.scopes.join(' '));
    url.searchParams.append('state', Math.random().toString(36).substring(7));
    
    return url.toString();
  }
};