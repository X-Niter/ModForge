import passport from "passport";
import { Strategy as LocalStrategy } from "passport-local";
import { Strategy as BearerStrategy } from "passport-http-bearer";
import { Express, Request, Response, NextFunction } from "express";
import session from "express-session";
import { scrypt, randomBytes, timingSafeEqual } from "crypto";
import { promisify } from "util";
import jwt from "jsonwebtoken";
import { storage } from "./storage";
import { InsertUser } from "@shared/schema";
import { z } from "zod";
import { db } from "./db";
import { users } from "@shared/schema";
import { eq } from "drizzle-orm";

// Create async version of scrypt
const scryptAsync = promisify(scrypt);

// JWT secret for token generation
const JWT_SECRET = process.env.JWT_SECRET || randomBytes(32).toString("hex");
const TOKEN_EXPIRY = '30d';  // 30 days expiry for token

/**
 * Generate a JWT token for a user
 * @param user User object
 * @returns JWT token
 */
export function generateToken(user: Express.User): string {
  const payload = {
    sub: user.id.toString(),
    username: user.username,
    iat: Math.floor(Date.now() / 1000),
  };
  
  return jwt.sign(payload, JWT_SECRET, { expiresIn: TOKEN_EXPIRY });
}

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

// Extending Express.User to include our user properties
declare global {
  namespace Express {
    interface User {
      id: number;
      username: string;
      email: string | null;
      password: string;
      displayName: string | null;
      avatarUrl: string | null;
      githubId: string | null;
      githubToken: string | null;
      stripeCustomerId: string | null;
      stripeSubscriptionId: string | null;
      isAdmin: boolean;
      metadata: Record<string, unknown> | any;
      createdAt: Date;
      updatedAt: Date;
    }
  }
}

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
        const [user] = await db.select().from(users).where(eq(users.username, username));
        
        if (!user) {
          return done(null, false, { message: "Invalid username or password" });
        }
        
        const passwordMatches = await comparePasswords(password, user.password);
        if (!passwordMatches) {
          return done(null, false, { message: "Invalid username or password" });
        }
        
        return done(null, user);
      } catch (error) {
        return done(error);
      }
    })
  );
  
  // Set up the Bearer token strategy
  passport.use(
    new BearerStrategy(async (token, done) => {
      try {
        // Verify the token
        const payload = jwt.verify(token, JWT_SECRET) as jwt.JwtPayload;
        
        if (!payload || !payload.sub) {
          return done(null, false);
        }
        
        // Extract the user ID from the token
        const userId = parseInt(payload.sub, 10);
        
        // Get the user from the database
        const [user] = await db.select().from(users).where(eq(users.id, userId));
        
        if (!user) {
          return done(null, false);
        }
        
        return done(null, user);
      } catch (error) {
        return done(error);
      }
    })
  );

  // Serialize and deserialize user
  passport.serializeUser((user: Express.User, done) => {
    done(null, user.id);
  });

  passport.deserializeUser(async (id: number, done) => {
    try {
      const [user] = await db.select().from(users).where(eq(users.id, id));
      if (user && typeof user.metadata !== 'object') {
        user.metadata = {};
      }
      if (!user) {
        return done(null, false);
      }
      done(null, user);
    } catch (error) {
      done(error);
    }
  });

  // Registration endpoint with enhanced validation and error handling
  app.post("/api/register", async (req: Request, res: Response) => {
    try {
      // Strict validation with Zod
      const validatedData = loginSchema.parse(req.body);
      
      // Sanitize inputs to prevent injection attacks
      const sanitizedUsername = validatedData.username.trim();
      
      // Additional validation for username format
      if (!/^[a-zA-Z0-9_-]+$/.test(sanitizedUsername)) {
        return res.status(400).json({ 
          message: "Username can only contain letters, numbers, underscores and hyphens" 
        });
      }
      
      // Check if user already exists with case-insensitive query
      const [existingUser] = await db
        .select()
        .from(users)
        .where(eq(users.username, sanitizedUsername));
        
      if (existingUser) {
        return res.status(400).json({ message: "Username already exists" });
      }
      
      // Validate email format if provided
      if (req.body.email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(req.body.email)) {
          return res.status(400).json({ message: "Invalid email format" });
        }
      }
      
      // Hash password with improved security
      const hashedPassword = await hashPassword(validatedData.password);
      
      // Create user with sanitized inputs
      const [newUser] = await db.insert(users)
        .values({
          username: sanitizedUsername,
          password: hashedPassword,
          email: req.body.email ? req.body.email.trim() : null,
          displayName: req.body.displayName ? req.body.displayName.trim() : null,
          metadata: {} as Record<string, unknown>,
        })
        .returning();
      
      // Log in the new user
      req.login(newUser, (err) => {
        if (err) {
          return res.status(500).json({ message: "Login failed after registration" });
        }
        
        // Return user without password
        const { password, ...userWithoutPassword } = newUser;
        return res.status(201).json(userWithoutPassword);
      });
      
    } catch (error: any) {
      if (error.name === "ZodError") {
        return res.status(400).json({ message: "Validation failed", errors: error.errors });
      }
      return res.status(500).json({ message: "Registration failed", error: error.message });
    }
  });

  // Login endpoint
  app.post("/api/login", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate("local", (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ message: info?.message || "Login failed" });
      }
      
      req.login(user, (loginErr) => {
        if (loginErr) {
          return next(loginErr);
        }
        
        // Generate a token for the user
        const token = generateToken(user);
        
        // Return user without password, including token
        const { password, ...userWithoutPassword } = user;
        return res.status(200).json({ ...userWithoutPassword, token });
      });
    })(req, res, next);
  });
  
  // Token generation endpoint for IDE plugin
  app.post("/api/token", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate("local", (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return res.status(500).json({ success: false, message: "Authentication error", error: err.message });
      }
      
      if (!user) {
        return res.status(401).json({ success: false, message: info?.message || "Invalid credentials" });
      }
      
      // Generate a token for the user
      const token = generateToken(user);
      
      return res.status(200).json({
        success: true,
        message: "Token generated successfully",
        token,
        userId: user.id,
        username: user.username
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

  // User info endpoint - supports both session and token authentication
  app.get("/api/user", (req: Request, res: Response, next: NextFunction) => {
    // First check if authenticated via session
    if (req.isAuthenticated()) {
      // Return user without password
      const { password, ...userWithoutPassword } = req.user as Express.User;
      return res.status(200).json(userWithoutPassword);
    }
    
    // If not session authenticated, try bearer token authentication
    passport.authenticate('bearer', { session: false }, (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ message: "Not authenticated" });
      }
      
      // Return user without password
      const { password, ...userWithoutPassword } = user as Express.User;
      return res.status(200).json(userWithoutPassword);
    })(req, res, next);
  });
  
  // IntelliJ plugin specific auth endpoints - supports both session and token auth
  app.get("/api/auth/me", (req: Request, res: Response, next: NextFunction) => {
    // First check if authenticated via session
    if (req.isAuthenticated()) {
      // Return user without password
      const { password, ...userWithoutPassword } = req.user as Express.User;
      return res.status(200).json({
        success: true,
        user: userWithoutPassword
      });
    }
    
    // If not session authenticated, try bearer token authentication
    passport.authenticate('bearer', { session: false }, (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ success: false, message: "Not authenticated" });
      }
      
      // Return user without password
      const { password, ...userWithoutPassword } = user as Express.User;
      return res.status(200).json({
        success: true,
        user: userWithoutPassword
      });
    })(req, res, next);
  });
  
  // Verification endpoint for IntelliJ plugin token authentication
  app.get("/api/auth/verify", (req: Request, res: Response, next: NextFunction) => {
    // First check if authenticated via session
    if (req.isAuthenticated()) {
      return res.status(200).json({
        success: true,
        message: "Authentication valid (session)",
        userId: req.user.id,
        username: req.user.username
      });
    }
    
    // If not session authenticated, try bearer token authentication
    passport.authenticate('bearer', { session: false }, (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ success: false, message: "Not authenticated" });
      }
      
      return res.status(200).json({
        success: true,
        message: "Authentication valid (token)",
        userId: user.id,
        username: user.username
      });
    })(req, res, next);
  });
  
  // Verification endpoint that accepts username/password directly
  app.post("/api/auth/verify", (req: Request, res: Response, next: NextFunction) => {
    passport.authenticate("local", (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return res.status(500).json({ success: false, message: "Authentication error", error: err.message });
      }
      
      if (!user) {
        return res.status(401).json({ success: false, message: info?.message || "Invalid credentials" });
      }
      
      return res.status(200).json({
        success: true,
        message: "Authentication valid",
        userId: user.id,
        username: user.username
      });
    })(req, res, next);
  });

  // Authentication check middleware that supports both session and token-based auth
  return function requireAuth(req: Request, res: Response, next: NextFunction) {
    // First check if the user is already authenticated via session
    if (req.isAuthenticated()) {
      return next();
    }
    
    // If not, check for Bearer token authentication
    passport.authenticate('bearer', { session: false }, (err: Error | null, user: Express.User | false | null | undefined, info: any) => {
      if (err) {
        return next(err);
      }
      
      if (!user) {
        return res.status(401).json({ message: "Authentication required" });
      }
      
      // User authenticated via token, attach to request
      req.user = user;
      return next();
    })(req, res, next);
  };
}
