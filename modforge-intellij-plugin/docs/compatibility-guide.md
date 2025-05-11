# IntelliJ IDEA 2025.1.1.1 Compatibility Guide

This guide provides essential information for maintaining compatibility with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

## Common API Changes

### Deprecated APIs and Replacements

| Deprecated API | Replacement | Notes |
|----------------|-------------|-------|
| `Project.getBaseDir()` | `CompatibilityUtil.getProjectBaseDir(project)` | Use our utility method for consistent behavior |
| `FileEditorManager.getSelectedTextEditor()` | `CompatibilityUtil.getSelectedTextEditor(project)` | Handles null checks and version differences |
| `CacheUpdater`, `CacheUpdaterFacade` | `CompatibilityUtil.refreshAll(project)` | Removed in newer versions |
| `ApplicationManager.getApplication().runReadAction()` | `CompatibilityUtil.runReadAction()` | More consistent behavior |
| `ApplicationManager.getApplication().runWriteAction()` | `CompatibilityUtil.runWriteAction()` | More consistent behavior |
| `PsiManager.getInstance(project).findFile()` | `CompatibilityUtil.findPsiFile(project, file)` | More robust against nulls |

### Java 21 Features

IntelliJ IDEA 2025.1.1.1 runs on Java 21, allowing us to use its features:

1. **Virtual Threads**: Use `ThreadUtils.createVirtualThreadExecutor()` for better performance with network operations.
2. **String Templates**: Available for more concise string formatting.
3. **Pattern Matching for Switch**: Use for more concise and safer type checking.
4. **Record Patterns**: Use for destructuring records.

## Service Registration

Services must be properly registered in the plugin.xml file. Use the appropriate service level:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Application-level service -->
    <applicationService serviceImplementation="com.modforge.intellij.plugin.services.ModAuthenticationManager"/>
    
    <!-- Project-level service -->
    <projectService serviceImplementation="com.modforge.intellij.plugin.services.ModForgeNotificationService"/>
</extensions>
```

For modern service implementation, use the `@Service` annotation:

```java
@Service(Service.Level.PROJECT)
public final class MyService {
    // Service implementation
}
```

## Plugin.xml Compatibility

The plugin.xml file must include the correct IDs and dependency declarations:

1. Always use proper `id` elements - never use shorthand notations
2. Specify version ranges: `sinceBuild="233"` and `untilBuild="251.25410.129"`
3. Include only essential plugin dependencies

## Build System

Our Gradle build is configured to:

1. Build with Java 17 compatibility (required for IntelliJ 2023.3.6)
2. Use a Java 21 toolchain for development
3. Validate plugin structure and versioning

Always run the `validatePluginForProduction` task before releasing.

## Troubleshooting

### Common Issues

1. **NoSuchMethodError**: API method may be renamed or removed. Check the IntelliJ API changelog.
2. **ClassNotFoundException**: Class moved to a different package or removed. Use `CompatibilityUtil`.
3. **NullPointerException in IDE core**: Check parameter null-safety with `@NotNull` and `@Nullable`.

### Debugging Steps

1. Check API changes in the [IntelliJ Platform API Changes Log](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html)
2. Use our `CompatibilityUtil` class for API version differences
3. Test with both 2023.3.6 (build target) and 2025.1.1.1 (runtime target)

## Best Practices

1. Always use `@Service` annotation for services
2. Prefer constructor injection over field injection
3. Use `ThreadUtils` for thread management
4. Follow IntelliJ Platform Coding Guidelines
5. Test with different IntelliJ versions

## Resources

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Plugin Repository](https://plugins.jetbrains.com/)
- [ModForge Support Channels](https://modforge.dev/support)