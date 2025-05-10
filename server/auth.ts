import { Express, Request, Response, NextFunction } from 'express';
import session from 'express-session';
import { storage } from './storage';
import { insertUserSchema, User } from '@shared/schema';
import { hashPassword, verifyPassword } from './utils/password';
import { z } from 'zod';

// Custom types for user registration and login
export const registerSchema = insertUserSchema
  .extend({
    password: z.string().min(6, 'Password must be at least 6 characters'),
    confirmPassword: z.string()
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword']
  });

export const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required')
});

// Express session extension
declare module 'express-session' {
  interface SessionData {
    userId: number;
    isAuthenticated: boolean;
  }
}

// Type declaration for Express Request
declare global {
  namespace Express {
    interface Request {
      user?: User;
    }
  }
}

/**
 * Set up the authentication system for the application
 */
export function setupAuth(app: Express) {
  // Authentication middleware - populates req.user if logged in
  app.use(async (req: Request, res: Response, next: NextFunction) => {
    if (req.session && req.session.userId) {
      try {
        const user = await storage.getUser(req.session.userId);
        if (user) {
          req.user = user;
        }
      } catch (error) {
        console.error('Error fetching user in auth middleware:', error);
      }
    }
    next();
  });

  // User registration endpoint
  app.post('/api/auth/register', async (req: Request, res: Response) => {
    try {
      // Validate the request data using Zod
      const validationResult = registerSchema.safeParse(req.body);
      
      if (!validationResult.success) {
        return res.status(400).json({
          success: false,
          errors: validationResult.error.format()
        });
      }
      
      const { username, email, password, displayName } = validationResult.data;
      
      // Check if the username already exists
      const existingUser = await storage.getUserByUsername(username);
      if (existingUser) {
        return res.status(400).json({
          success: false,
          message: 'Username already taken'
        });
      }
      
      // Hash the password
      const hashedPassword = await hashPassword(password);
      
      // Create the user
      const user = await storage.createUser({
        username,
        email,
        password: hashedPassword,
        displayName,
        metadata: {}
      });
      
      // Set up the session
      if (req.session) {
        req.session.userId = user.id;
        req.session.isAuthenticated = true;
      }
      
      // Return the user without the password
      const { password: _, ...safeUser } = user;
      return res.status(201).json({
        success: true,
        user: safeUser
      });
    } catch (error) {
      console.error('Error during registration:', error);
      return res.status(500).json({
        success: false,
        message: 'An error occurred during registration'
      });
    }
  });
  
  // User login endpoint
  app.post('/api/auth/login', async (req: Request, res: Response) => {
    try {
      // Validate the request data
      const validationResult = loginSchema.safeParse(req.body);
      
      if (!validationResult.success) {
        return res.status(400).json({
          success: false,
          errors: validationResult.error.format()
        });
      }
      
      const { username, password } = validationResult.data;
      
      // Get the user by username
      const user = await storage.getUserByUsername(username);
      if (!user) {
        return res.status(400).json({
          success: false,
          message: 'Invalid username or password'
        });
      }
      
      // Verify the password
      const isValidPassword = await verifyPassword(password, user.password);
      if (!isValidPassword) {
        return res.status(400).json({
          success: false,
          message: 'Invalid username or password'
        });
      }
      
      // Set up the session
      if (req.session) {
        req.session.userId = user.id;
        req.session.isAuthenticated = true;
      }
      
      // Return the user without the password
      const { password: _, ...safeUser } = user;
      return res.json({
        success: true,
        user: safeUser
      });
    } catch (error) {
      console.error('Error during login:', error);
      return res.status(500).json({
        success: false,
        message: 'An error occurred during login'
      });
    }
  });
  
  // User logout endpoint
  app.post('/api/auth/logout', (req: Request, res: Response) => {
    if (req.session) {
      req.session.destroy((err) => {
        if (err) {
          console.error('Error destroying session:', err);
          return res.status(500).json({
            success: false,
            message: 'Error logging out'
          });
        }
        return res.json({
          success: true,
          message: 'Logged out successfully'
        });
      });
    } else {
      return res.json({
        success: true,
        message: 'Already logged out'
      });
    }
  });
  
  // Check authentication status
  app.get('/api/auth/me', (req: Request, res: Response) => {
    if (req.user) {
      const { password, ...safeUser } = req.user;
      return res.json({
        authenticated: true,
        user: safeUser
      });
    }
    
    return res.json({
      authenticated: false
    });
  });
  
  // Middleware to require authentication
  return function requireAuth(req: Request, res: Response, next: NextFunction) {
    if (req.user) {
      return next();
    }
    
    return res.status(401).json({
      success: false,
      message: 'Authentication required'
    });
  };
}