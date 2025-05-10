import { scrypt, randomBytes, timingSafeEqual } from 'crypto';
import { promisify } from 'util';

// Convert callback-based scrypt to Promise-based
const scryptAsync = promisify(scrypt);

/**
 * Hash a password for secure storage
 * Returns a string in the format: `${hashedPassword}.${salt}`
 * 
 * @param password The plaintext password to hash
 * @returns Hashed password with salt in the format "hashedPassword.salt"
 */
export async function hashPassword(password: string): Promise<string> {
  // Generate a random salt
  const salt = randomBytes(16).toString('hex');
  
  // Hash the password with the salt
  const hashBuffer = (await scryptAsync(password, salt, 64)) as Buffer;
  
  // Return the hashed password and salt joined with a dot
  return `${hashBuffer.toString('hex')}.${salt}`;
}

/**
 * Verify a password against a stored hash
 * 
 * @param suppliedPassword The password to verify
 * @param storedHash The stored hash from the database
 * @returns True if the password matches, false otherwise
 */
export async function verifyPassword(suppliedPassword: string, storedHash: string): Promise<boolean> {
  // Split the stored hash into the hash and salt
  const [hashedPassword, salt] = storedHash.split('.');
  
  // If the stored hash doesn't have the expected format, return false
  if (!hashedPassword || !salt) {
    return false;
  }
  
  // Hash the supplied password with the same salt
  const suppliedHashBuffer = (await scryptAsync(suppliedPassword, salt, 64)) as Buffer;
  
  // Convert the stored hash to a buffer for comparison
  const storedHashBuffer = Buffer.from(hashedPassword, 'hex');
  
  // Use timing-safe comparison to prevent timing attacks
  return timingSafeEqual(storedHashBuffer, suppliedHashBuffer);
}