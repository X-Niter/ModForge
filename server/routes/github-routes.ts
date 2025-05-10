/**
 * GitHub integration routes
 * Provides multiple authentication methods for GitHub
 */

import { Router } from 'express';
import axios from 'axios';
import { storage } from '../storage';

const router = Router();

// Verify a GitHub token
router.post('/verify-token', async (req, res) => {
  const { token } = req.body;
  
  if (!token) {
    return res.status(400).json({ 
      success: false, 
      message: 'GitHub token is required' 
    });
  }
  
  try {
    // Call GitHub API to verify token and get user info
    const response = await axios.get('https://api.github.com/user', {
      headers: {
        'Authorization': `token ${token}`,
        'Accept': 'application/vnd.github.v3+json'
      }
    });
    
    const userData = response.data;
    
    return res.json({ 
      success: true, 
      username: userData.login,
      avatar_url: userData.avatar_url,
      html_url: userData.html_url,
      name: userData.name,
      email: userData.email
    });
  } catch (error) {
    console.error('Error verifying GitHub token:', error);
    
    // Extract error message from GitHub API response if available
    let errorMessage = 'Failed to verify GitHub token';
    
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        errorMessage = 'Invalid or expired GitHub token';
      } else {
        errorMessage = error.response.data?.message || errorMessage;
      }
    }
    
    return res.status(401).json({ 
      success: false, 
      message: errorMessage 
    });
  }
});

// Save a verified GitHub token to user account
router.post('/save-token', async (req, res) => {
  const { token } = req.body;
  
  if (!token) {
    return res.status(400).json({ 
      success: false, 
      message: 'GitHub token is required' 
    });
  }
  
  try {
    // First verify the token
    const response = await axios.get('https://api.github.com/user', {
      headers: {
        'Authorization': `token ${token}`,
        'Accept': 'application/vnd.github.v3+json'
      }
    });
    
    const userData = response.data;
    
    // Check if user is logged in
    if (req.session && req.session.userId) {
      // Update user's GitHub info
      const user = await storage.getUser(req.session.userId);
      
      if (user) {
        const updatedMetadata = typeof user.metadata === 'object' && user.metadata !== null
          ? { ...user.metadata, github: {
              id: userData.id,
              login: userData.login,
              avatar_url: userData.avatar_url,
              html_url: userData.html_url,
              type: userData.type
            }}
          : { github: {
              id: userData.id,
              login: userData.login,
              avatar_url: userData.avatar_url,
              html_url: userData.html_url,
              type: userData.type
            }};
            
        await storage.updateUser(user.id, {
          githubToken: token,
          avatarUrl: userData.avatar_url,
          githubId: String(userData.id),
          metadata: updatedMetadata
        });
        
        return res.json({ 
          success: true, 
          message: 'GitHub token saved successfully' 
        });
      }
    }
    
    // If no user session, store token in local storage on client side
    // This branch will be taken for IDE plugin use cases
    return res.json({ 
      success: true, 
      message: 'GitHub token verified (use localStorage for local storage)',
      userData: {
        login: userData.login,
        id: userData.id,
        avatar_url: userData.avatar_url,
        html_url: userData.html_url,
      }
    });
  } catch (error) {
    console.error('Error saving GitHub token:', error);
    
    let errorMessage = 'Failed to save GitHub token';
    
    if (axios.isAxiosError(error) && error.response) {
      errorMessage = error.response.data?.message || errorMessage;
    }
    
    return res.status(500).json({ 
      success: false, 
      message: errorMessage 
    });
  }
});

// Get GitHub credentials from various sources
// Order of precedence:
// 1. Session token (from OAuth or saved token)
// 2. Token from request
// 3. Environment variable token
router.get('/get-credentials', async (req, res) => {
  const { token: requestToken } = req.body;
  let source = null;
  let token = null;
  
  // Check session first (most secure)
  if (req.session && req.session.userId) {
    const user = await storage.getUser(req.session.userId);
    if (user?.githubToken) {
      token = user.githubToken;
      source = 'session';
    }
  }
  
  // Fall back to request token
  if (!token && requestToken) {
    token = requestToken;
    source = 'request';
  }
  
  // Finally fall back to environment variable
  if (!token && process.env.GITHUB_TOKEN) {
    token = process.env.GITHUB_TOKEN;
    source = 'environment';
  }
  
  if (token) {
    try {
      // Verify the token
      const response = await axios.get('https://api.github.com/user', {
        headers: {
          'Authorization': `token ${token}`,
          'Accept': 'application/vnd.github.v3+json'
        }
      });
      
      const userData = response.data;
      
      return res.json({
        success: true,
        source,
        username: userData.login,
        avatar_url: userData.avatar_url,
        // Don't return the actual token for security
      });
    } catch (error) {
      // Token validation failed
      console.error('Error validating GitHub token:', error);
      return res.status(401).json({
        success: false,
        message: 'Invalid GitHub token',
        source
      });
    }
  }
  
  return res.status(404).json({
    success: false,
    message: 'No GitHub token found',
  });
});

export default router;