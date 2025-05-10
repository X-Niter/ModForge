import passport from "passport";
import { Strategy as LocalStrategy } from "passport-local";
import { Strategy as BearerStrategy } from "passport-http-bearer";
import { Express, Request, Response, NextFunction } from "express";
import session from "express-session";
import { scrypt, randomBytes, timingSafeEqual } from "crypto";
import { promisify } from "util";
import jwt from "jsonwebtoken";
// Define a simplified version of the JWT sign function to avoid type issues
function signJwt(payload: any, secret: string, options: { expiresIn: string | number }): string {
  return jwt.sign(payload, secret, options as any);
}
import { storage } from "./storage";
import { InsertUser } from "@shared/schema";
import { z } from "zod";
import { db } from "./db";
import { users } from "@shared/schema";
import { eq } from "drizzle-orm";

// Extend express-session with custom properties
declare module 'express-session' {
  interface SessionData {
    loginAttempts?: Record<string, { count: number, lastAttempt: number }>;
  }
}

// Type guard to check if a value is a Record<string, unknown>
function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object';
}

// Define a utility function to ensure metadata is the correct type
function ensureMetadataType<T extends { metadata?: any }>(user: T): T & { metadata: Record<string, unknown> } {
  if (user) {
    // Make sure metadata is always a proper object
    if (!isRecord(user.metadata)) {
      user.metadata = {};
    }
  }
  return user as T & { metadata: Record<string, unknown> };
}

// Extend Express to include User type
declare global {
  namespace Express {
    interface User {
      id: number;
      username: string;
      email: string | null;
      displayName: string | null;
      avatarUrl: string | null;
      githubId: string | null;
      githubToken: string | null;
      stripeCustomerId: string | null;
      stripeSubscriptionId: string | null;
      password: string;
      isAdmin: boolean;
      metadata: Record<string, unknown>;
      createdAt: Date;
      updatedAt: Date;
    }
  }
}

// Create async version of scrypt
const scryptAsync = promisify(scrypt);

/**
 * Hash a password for storage
 * @param password Plain text password
 * @returns Hashed password with salt
 */
export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16).toString("hex");
  const buf = (await scryptAsync(password, salt, 64)) as Buffer;
  return `${buf.toString("hex")}.${salt}`;
}

/**
 * Compare a supplied password with a stored hashed password
 * @param supplied Plain text password from login attempt
 * @param stored Hashed password from database
 * @returns Whether passwords match
 */
export async function comparePasswords(supplied: string, stored: string): Promise<boolean> {
  const [hashed, salt] = stored.split(".");
  const hashedBuf = Buffer.from(hashed, "hex");
  const suppliedBuf = (await scryptAsync(supplied, salt, 64)) as Buffer;
  return timingSafeEqual(hashedBuf, suppliedBuf);
}

// Login and registration validation schemas
export const loginSchema = z.object({
  username: z.string().min(3, { message: "Username must be at least 3 characters" }),
  password: z.string().min(6, { message: "Password must be at least 6 characters" }),
});

export type LoginData = z.infer<typeof loginSchema>;

/**
 * Set up authentication for the application
 * @param app Express application
 */
export function setupAuth(app: Express) {
  // Configure sessions
  const sessionSecret = process.env.SESSION_SECRET || randomBytes(32).toString("hex");
  
  const sessionSettings: session.SessionOptions = {
    secret: sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: {
      secure: process.env.NODE_ENV === "production",
      maxAge: 30 * 24 * 60 * 60 * 1000, // 30 days
    },
    // We'll use the storage.sessionStore here if using database storage
  };
  
  app.use(session(sessionSettings));
  app.use(passport.initialize());
  app.use(passport.session());

  // Set up the local strategy
  passport.use(
    new LocalStrategy(async (username, password, done) => {
      try {
        // Security: always validate input length to prevent excessive load
        if (username.length > 100 || password.length > 100) {
          return done(null, false, { message: "Invalid credentials format" });
        }
        
        const [user] = await db.select().from(users).where(eq(users.username, username));
        
        if (!user) {
          return done(null, false, { message: "Invalid username or password" });
        }
        
        const passwordMatches = await comparePasswords(password, user.password);
        if (!passwordMatches) {
          return done(null, false, { message: "Invalid username or password" });
        }
        
        // Use our utility to ensure metadata is valid
        return done(null, ensureMetadataType(user));
      } catch (error) {
        return done(error);
      }
    })
  );

  // Serialize and deserialize user
  passport.serializeUser((user, done) => {
    done(null, user.id);
  });

  passport.deserializeUser(async (id: number, done) => {
    try {
      const [user] = await db.select().from(users).where(eq(users.id, id));
      if (!user) {
        return done(null, false);
      }
      
      // Use our utility to ensure metadata is valid
      done(null, ensureMetadataType(user));
    } catch (error) {
      done(error);
    }
  });
  
  // Set up JWT Bearer strategy for token-based authentication (IDE plugin)
  passport.use(
    new BearerStrategy(async (token, done) => {
      try {
        // Verify the token
        const decoded = jwt.verify(
          token, 
          process.env.JWT_SECRET || "fallback-secret-change-in-production"
        ) as { sub: string, username: string };
        
        // Get the user
        const userId = parseInt(decoded.sub, 10);
        const [user] = await db.select().from(users).where(eq(users.id, userId));
        
        if (!user) {
          return done(null, false, "User not found");
        }
        
        // Use our utility to ensure metadata is valid
        return done(null, ensureMetadataType(user));
      } catch (error) {
        // Token verification failed
        return done(null, false, "Invalid token");
      }
    })
  );

  // Registration endpoint
  app.post("/api/register", async (req: Request, res: Response) => {
    try {
      const validatedData = loginSchema.parse(req.body);
      
      // Check if user already exists
      const [existingUser] = await db.select().from(users).where(eq(users.username, validatedData.username));
      if (existingUser) {
        return res.status(400).json({ message: "Username already exists" });
      }
      
      // Hash password
      const hashedPassword = await hashPassword(validatedData.password);
      
      // Create user with sanitized inputs
      const [newUser] = await db.insert(users)
        .values({
          username: validatedData.username.trim(),
          password: hashedPassword,
          email: req.body.email ? req.body.email.trim() : null,
          displayName: req.body.displayName ? req.body.displayName.trim() : null,
          metadata: {} as Record<string, unknown>,
        })
        .returning();
      
      // Log in the new user (ensure the metadata is properly typed)
      req.login(ensureMetadataType(newUser), (err) => {
        if (err) {
          return res.status(500).json({ message: "Login failed after registration" });
        }
        
        // Generate a token for API access with proper permissions
        const token = generateToken(ensureMetadataType(newUser), {
          purpose: 'registration'
        });
        
        // Return user without password, including token
        const { password, ...userWithoutPassword } = newUser;
        return res.status(201).json({ ...userWithoutPassword, token });
      });
      
    } catch (error: any) {
      if (error.name === "ZodError") {
        return res.status(400).json({ message: "Validation failed", errors: error.errors });
      }
      return res.status(500).json({ message: "Registration failed", error: error.message });
    }
  });

  // Login endpoint with enhanced security and rate limiting
  app.post("/api/login", (req: Request, res: Response, next: NextFunction) => {
    // Validate login input first using Zod
    const validationResult = loginSchema.safeParse(req.body);
    if (!validationResult.success) {
      return res.status(400).json({ 
        message: "Invalid login credentials format", 
        errors: validationResult.error.errors 
      });
    }

    // Rate limiting check
    const ipAddress = req.ip || req.socket.remoteAddress || 'unknown';
    const loginAttemptsStore: Record<string, { count: number, lastAttempt: number }> = 
      req.session.loginAttempts || {};
    
    const now = Date.now();
    const attempts = loginAttemptsStore[ipAddress] || { count: 0, lastAttempt: 0 };
    
    // Implement increasing delays for repeated failed attempts
    if (attempts.count >= 5) {
      const timeElapsed = now - attempts.lastAttempt;
      const requiredDelay = Math.min(30000, Math.pow(2, attempts.count - 5) * 1000);
      
      if (timeElapsed < requiredDelay) {
        return res.status(429).json({ 
          message: `Too many login attempts. Please try again in ${Math.ceil((requiredDelay - timeElapsed) / 1000)} seconds.` 
        });
      }
    }
    
    // Authenticate user with passport
    passport.authenticate("local", (err: Error, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      // Failed login
      if (!user) {
        // Track failed attempts
        attempts.count += 1;
        attempts.lastAttempt = now;
        loginAttemptsStore[ipAddress] = attempts;
        req.session.loginAttempts = loginAttemptsStore;
        
        return res.status(401).json({ message: info?.message || "Invalid username or password" });
      }
      
      // Successful login - reset attempts counter
      delete loginAttemptsStore[ipAddress];
      req.session.loginAttempts = loginAttemptsStore;
      
      req.login(user, (loginErr) => {
        if (loginErr) {
          return next(loginErr);
        }
        
        // Generate a JSON Web Token for API access
        const token = signJwt(
          { sub: user.id.toString(), username: user.username },
          process.env.JWT_SECRET || "fallback-secret-change-in-production",
          { expiresIn: '30d' }
        );
        
        // Return user without password, including token
        const { password, ...userWithoutPassword } = user;
        return res.status(200).json({ ...userWithoutPassword, token });
      });
    })(req, res, next);
  });

  // Generate a token with customizable expiration
  function generateToken(
    user: Express.User, 
    tokenOptions: { 
      expiresIn?: string | number,
      isAdmin?: boolean,
      scope?: string,
      purpose?: string 
    } = {}
  ): string {
    const expiry = tokenOptions.expiresIn || '30d';
    
    // Create payload with optional fields for specific purposes
    const payload: Record<string, any> = {
      sub: user.id.toString(),
      username: user.username,
      iat: Math.floor(Date.now() / 1000)
    };
    
    // Add optional fields
    if (tokenOptions.isAdmin) {
      payload.isAdmin = true;
    }
    
    if (tokenOptions.scope) {
      payload.scope = tokenOptions.scope;
    }
    
    if (tokenOptions.purpose) {
      payload.purpose = tokenOptions.purpose;
    }
    
    const jwtSecret = process.env.JWT_SECRET || "fallback-secret-change-in-production";
    const expiryOption = typeof expiry === 'string' ? expiry : `${expiry}s`;
    
    return signJwt(payload, jwtSecret, { expiresIn: expiryOption });
  }
  
  // Generate a short-lived JIT (Just-In-Time) token for automated operations
  function generateJitToken(userId: number, username: string, purpose: string): string {
    // Short-lived token (1 hour) for automation purposes
    const jwtSecret = process.env.JWT_SECRET || "fallback-secret-change-in-production";
    const payload = { 
      sub: userId.toString(), 
      username,
      purpose,
      jit: true,
      iat: Math.floor(Date.now() / 1000)
    };
    return signJwt(payload, jwtSecret, { expiresIn: '1h' });
  }

  // Token generation endpoint for IDE plugin
  app.post("/api/token", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate("local", (err: Error, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      if (!user) {
        return res.status(401).json({ message: info?.message || "Invalid credentials" });
      }
      
      // Generate token
      const token = generateToken(user);
      
      return res.status(200).json({
        token,
        userId: user.id,
        username: user.username,
        expiresIn: 30 * 24 * 60 * 60 // 30 days in seconds
      });
    })(req, res, next);
  });

  // Logout endpoint
  app.post("/api/logout", (req: Request, res: Response) => {
    req.logout((err) => {
      if (err) {
        return res.status(500).json({ message: "Logout failed" });
      }
      req.session.destroy((err) => {
        return res.status(200).json({ message: "Logged out successfully" });
      });
    });
  });

  // User info endpoint - supports both session and token auth
  app.get("/api/user", (req: Request, res: Response, next: NextFunction) => {
    // Try session auth first
    if (req.isAuthenticated()) {
      const { password, ...userWithoutPassword } = req.user as Express.User;
      return res.status(200).json(userWithoutPassword);
    }
    
    // Fall back to token auth
    passport.authenticate('bearer', { session: false }, 
      (err: Error | null, user: Express.User | false | null, info: string | undefined) => {
        if (err) {
          return next(err);
        }
        if (!user) {
          return res.status(401).json({ message: "Not authenticated" });
        }
        
        const { password, ...userWithoutPassword } = user;
        return res.status(200).json(userWithoutPassword);
      }
    )(req, res, next);
  });
  
  // Token verification endpoint
  app.get("/api/auth/verify", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate('bearer', { session: false }, 
      (err: Error | null, user: Express.User | false | null, info: string | undefined) => {
        if (err) {
          return next(err);
        }
        if (!user) {
          return res.status(401).json({ 
            authenticated: false,
            message: info || "Token verification failed"
          });
        }
        
        return res.status(200).json({
          authenticated: true,
          message: "Authentication valid (token)",
          userId: user.id,
          username: user.username
        });
      }
    )(req, res, next);
  });
  
  // Verification endpoint that accepts username/password directly
  app.post("/api/auth/verify", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate("local", (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ 
          authenticated: false,
          message: info?.message || "Invalid credentials" 
        });
      }
      
      return res.status(200).json({
        authenticated: true,
        message: "Authentication valid (credentials)",
        userId: user.id,
        username: user.username
      });
    })(req, res, next);
  });

  // Authentication check middleware - supports both session and token auth
  return function requireAuth(req: Request, res: Response, next: NextFunction) {
    // First check session auth
    if (req.isAuthenticated()) {
      return next();
    }
    
    // Then try token auth
    passport.authenticate('bearer', { session: false }, (err: Error | null, user: Express.User | false | null, info: string | undefined) => {
      if (err) {
        return res.status(500).json({ message: "Authentication error", error: err.message });
      }
      
      if (!user) {
        return res.status(401).json({ message: "Authentication required" });
      }
      
      // Set user manually since we're not using sessions with bearer auth
      req.user = user;
      return next();
    })(req, res, next);
  };
}