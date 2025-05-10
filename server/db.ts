import { Pool, neonConfig } from '@neondatabase/serverless';
import { drizzle } from 'drizzle-orm/neon-serverless';
import ws from "ws";
import * as schema from "@shared/schema";

// Configure Neon WebSockets for serverless environments
neonConfig.webSocketConstructor = ws;

// Ensure DATABASE_URL is provided
if (!process.env.DATABASE_URL) {
  throw new Error(
    "DATABASE_URL must be set. Did you forget to provision a database?",
  );
}

// Connection pool configuration with default values suitable for production
const poolConfig = {
  connectionString: process.env.DATABASE_URL,
  max: parseInt(process.env.DB_POOL_MAX || '10', 10),       // Maximum number of clients in the pool
  idleTimeoutMillis: 30000,                                 // How long a client is allowed to remain idle before being closed
  connectionTimeoutMillis: 5000,                            // How long to wait for a connection to become available
  allowExitOnIdle: false                                    // Allow the pool to exit if all clients are idle
};

// Create connection pool
export const pool = new Pool(poolConfig);

// Set up error handling for the pool
pool.on('error', (err, client) => {
  console.error('Unexpected error on idle database client', err instanceof Error ? err.message : String(err));
  // Log but don't exit - the error recovery system will handle reconnection
  // This prevents the application from crashing on transient database errors
});

// Create Drizzle ORM instance with our schemas
export const db = drizzle({ client: pool, schema });

// Connect to the database and test the connection
async function testConnection() {
  try {
    const client = await pool.connect();
    console.log('✅ Database connection successful');
    client.release();
    return true;
  } catch (error) {
    console.error('❌ Failed to connect to the database:', error instanceof Error ? error.message : String(error));
    return false;
  }
}

// Test connection on startup
testConnection().catch(console.error);

// Clean up pool on process exit (helpful for development)
process.on('SIGINT', () => {
  pool.end().then(() => {
    console.log('Database pool has shut down');
    process.exit(0);
  });
});