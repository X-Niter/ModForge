/**
 * Authentication routes for the ModForge application
 * Handles GitHub OAuth integration
 */

import express, { Router, Request, Response } from 'express';
import axios from 'axios';
import { githubOAuth } from '../config/github-oauth';
import { storage } from '../storage';
import { z } from 'zod';
import 'express-session';

// Extend the Express.Session interface to include our custom properties
declare module 'express-session' {
  interface SessionData {
    userId: number;
    isAuthenticated: boolean;
  }
}

const router = Router();

// GitHub OAuth login
router.get('/github', (req, res) => {
  // Check if the GitHub OAuth client ID is configured
  if (!githubOAuth.clientId) {
    return res.status(500).json({
      error: 'GitHub OAuth not configured',
      message: 'GitHub client ID is missing. Ask the administrator to configure GitHub OAuth.'
    });
  }
  
  // Redirect to GitHub authorization URL
  const authUrl = githubOAuth.getAuthorizationURL();
  res.redirect(authUrl);
});

// GitHub OAuth callback
router.get('/github/callback', async (req, res) => {
  try {
    const { code, state } = req.query;
    
    if (!code) {
      return res.status(400).json({
        error: 'Invalid request',
        message: 'Authorization code is missing'
      });
    }
    
    // Exchange code for access token
    const tokenResponse = await axios.post(
      githubOAuth.tokenURL,
      {
        client_id: githubOAuth.clientId,
        client_secret: githubOAuth.clientSecret,
        code,
        redirect_uri: githubOAuth.callbackURL
      },
      {
        headers: {
          Accept: 'application/json'
        }
      }
    );
    
    if (!tokenResponse.data.access_token) {
      return res.status(400).json({
        error: 'Authentication failed',
        message: 'Could not retrieve access token'
      });
    }
    
    const accessToken = tokenResponse.data.access_token;
    
    // Get user profile with the access token
    const userResponse = await axios.get(githubOAuth.userProfileURL, {
      headers: {
        Authorization: `token ${accessToken}`,
        Accept: 'application/vnd.github.v3+json'
      }
    });
    
    if (!userResponse.data) {
      return res.status(400).json({
        error: 'Authentication failed',
        message: 'Could not retrieve user profile'
      });
    }
    
    const githubUser = userResponse.data;
    
    // Check if user exists, create if not
    let user = await storage.getUserByUsername(githubUser.login);
    
    // Prepare GitHub metadata
    const githubMetadata = {
      id: githubUser.id,
      login: githubUser.login,
      avatar_url: githubUser.avatar_url,
      html_url: githubUser.html_url,
      type: githubUser.type
    };
    
    if (!user) {
      // Create a new user
      user = await storage.createUser({
        username: githubUser.login,
        email: githubUser.email || `${githubUser.login}@github.user`,
        password: '', // No password for OAuth users
        avatarUrl: githubUser.avatar_url,
        githubId: String(githubUser.id),
        githubToken: accessToken,
        metadata: {
          github: githubMetadata
        }
      });
    } else {
      // Update existing user with new token
      const updatedMetadata = typeof user.metadata === 'object' && user.metadata !== null
        ? { ...user.metadata, github: githubMetadata }
        : { github: githubMetadata };
        
      user = await storage.updateUser(user.id, {
        githubToken: accessToken,
        avatarUrl: githubUser.avatar_url,
        githubId: String(githubUser.id),
        metadata: updatedMetadata
      });
    }
    
    // Set user session
    if (req.session && user) {
      req.session.userId = user.id;
      req.session.isAuthenticated = true;
    }
    
    // Redirect to GitHub integration page
    res.redirect('/github-integration');
  } catch (error) {
    console.error('GitHub OAuth error:', error);
    res.status(500).json({
      error: 'Authentication failed',
      message: error instanceof Error ? error.message : 'Unknown error during authentication'
    });
  }
});

// Logout
router.get('/logout', (req, res) => {
  if (req.session) {
    req.session.destroy((err) => {
      if (err) {
        console.error('Error destroying session:', err);
      }
      res.redirect('/');
    });
  } else {
    res.redirect('/');
  }
});

// Check if user is authenticated
router.get('/me', async (req, res) => {
  if (req.session && req.session.isAuthenticated && req.session.userId) {
    try {
      const user = await storage.getUser(req.session.userId);
      
      if (user) {
        // Don't send sensitive information to the client
        const { password, ...safeUser } = user;
        return res.json({ 
          authenticated: true,
          user: safeUser
        });
      }
    } catch (error) {
      console.error('Error retrieving user:', error);
    }
  }
  
  res.json({ authenticated: false });
});

export default router;