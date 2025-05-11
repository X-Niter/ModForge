# IntelliJ IDEA 2025.1.1.1 Compatibility

This project has been updated to ensure compatibility with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

## Summary of Changes

1. **API Compatibility Utilities**
   - Created CompatibilityUtil class to handle deprecated APIs
   - Added VirtualFileUtil for VirtualFile operations
   - Created DocumentListenerAdapter for document events

2. **Plugin Metadata Fix**
   - Fixed plugin.xml to use proper `<name>` tag instead of `<n>`
   - Added build scripts to ensure correct plugin metadata

3. **Class Updates**
   - Updated ModForgeWebSocketClient constructor to match newer API requirements
   - Enhanced ModLoaderDetector with better display name handling
   - Fixed dependencies and imports across the codebase

4. **Documentation**
   - Added compatibility guide for developers
   - Created compatibility checklist for testing

## How to Build

Use the provided build script to ensure proper plugin metadata:

```bash
./build-with-fixed-plugin.sh
```

## Required Java Version

Java 21.0.6+9-b895.109 or higher is required for development.

## Testing

The plugin has been tested with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

## Developer Notes

When making changes to this codebase, please refer to the compatibility guide at `docs/compatibility-guide.md` to ensure your changes maintain compatibility with IntelliJ IDEA 2025.1.1.1 and newer versions.