/**
 * Shared types and enums for ModForge
 */

/**
 * ModLoader enum
 * Defines the supported Minecraft mod loaders
 */
export enum ModLoader {
  Forge = "forge",
  Fabric = "fabric",
  Quilt = "quilt",
  Architectury = "architectury"
}

/**
 * AutoFixLevel enum
 * Defines how aggressively the system should try to fix compilation errors
 */
export enum AutoFixLevel {
  None = "none",
  Conservative = "conservative",
  Balanced = "balanced",
  Aggressive = "aggressive"
}

/**
 * CompileFrequency enum
 * Defines how often compilation should be attempted
 */
export enum CompileFrequency {
  Manual = "manual",
  OnChange = "on_change",
  Continuous = "continuous",
  Daily = "daily"
}

/**
 * Array of mod loaders for UI dropdowns
 */
export const modLoaders = Object.values(ModLoader);

/**
 * Array of auto-fix levels for UI dropdowns
 */
export const autoFixLevels = Object.values(AutoFixLevel);

/**
 * Array of compile frequencies for UI dropdowns
 */
export const compileFrequencies = Object.values(CompileFrequency);