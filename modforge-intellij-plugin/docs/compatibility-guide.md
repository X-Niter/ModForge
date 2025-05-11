# ModForge IntelliJ Plugin Compatibility Guide

This guide provides information on how to maintain compatibility with IntelliJ IDEA 2025.1.1.1 and later versions.

## Compatibility Utilities

### Using CompatibilityUtil

The `CompatibilityUtil` class provides wrappers for IntelliJ APIs that have changed or been deprecated:

```java
// INSTEAD OF: project.getBaseDir()
VirtualFile projectDir = CompatibilityUtil.getProjectBaseDir(project);

// INSTEAD OF: PsiManager.getInstance(project).findFile(file)
PsiFile psiFile = CompatibilityUtil.getPsiFile(project, file);

// INSTEAD OF: FileEditorManager.getInstance(project).openFile(file, true)
CompatibilityUtil.openFileInEditor(project, file, true);

// INSTEAD OF: ApplicationManager.getApplication().runReadAction(...)
Document doc = CompatibilityUtil.runReadAction(() -> ...);

// INSTEAD OF: ApplicationManager.getApplication().invokeLater(...)
CompatibilityUtil.runOnUIThread(() -> ...);
```

### Using VirtualFileUtil

The `VirtualFileUtil` class provides methods for working with VirtualFile instances:

```java
// INSTEAD OF: LocalFileSystem.getInstance().findFileByPath(path.toString())
VirtualFile file = VirtualFileUtil.pathToVirtualFile(path);

// INSTEAD OF: LocalFileSystem.getInstance().findFileByIoFile(file)
VirtualFile vFile = VirtualFileUtil.fileToVirtualFile(file);
```

### Using DocumentListenerAdapter

The `DocumentListenerAdapter` class provides a compatible way to implement document listeners:

```java
// INSTEAD OF implementing DocumentListener directly
DocumentListener listener = new DocumentListenerAdapter() {
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // Handle document change
    }
};
```

## Common Gotchas

1. **Plugin XML**: Make sure your plugin.xml file has a proper `<name>` tag, not `<n>`. Use the `fix-plugin-xml.sh` script if needed.

2. **WebSocket Client**: Use `ModForgeWebSocketClient` instead of previous WebSocket client implementations.

3. **Document Events**: Always use `DocumentListenerAdapter` instead of directly implementing DocumentListener.

4. **UI Thread**: Always use `CompatibilityUtil.runOnUIThread()` to schedule work on the UI thread.

5. **Project Base Directory**: Never use `project.getBaseDir()`, always use `CompatibilityUtil.getProjectBaseDir(project)`.

## Testing Compatibility

Always test your changes with IntelliJ IDEA 2025.1.1.1 to ensure compatibility. Key areas to test:

1. Plugin installation and loading
2. File operations (opening, editing, saving)
3. PsiFile operations
4. UI interactions
5. WebSocket connectivity
6. Background tasks

## Build Process

When building the plugin, use the `build-with-fixed-plugin.sh` script to ensure the plugin.xml file is correctly formatted:

```
./build-with-fixed-plugin.sh
```

This will fix the plugin.xml file before building and ensure proper plugin metadata.