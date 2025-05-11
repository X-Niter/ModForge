# Using IntelliJ's Bundled Java 21 for Building

If you've downloaded Java 21 through IntelliJ IDEA, you can use that same Java installation to build the plugin. This guide will help you find and configure the JDK correctly.

## Finding IntelliJ's Bundled Java 21

When you download a JDK through IntelliJ, it's typically stored in one of these locations:

1. In the `.jdks` directory in your user home folder:
   ```
   C:\Users\<username>\.jdks\
   ```

2. Inside the JetBrains Toolbox app directory:
   ```
   %LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0\<version>\jbr
   ```

3. Directly inside the IntelliJ installation directory:
   ```
   %LOCALAPPDATA%\JetBrains\IntelliJIdea<version>\jbr
   ```

## How to Find It Automatically

Run the included `detect-java.bat` script to scan your system for all Java 21 installations:

```
detect-java.bat
```

This will list all Java 21 installations found on your system, including ones bundled with IntelliJ.

## Manual Method

1. Open IntelliJ IDEA
2. Go to File → Project Structure → Platform Settings → SDKs
3. Look for a JDK entry with version 21
4. Note the "JDK home path" value

## Configuring the Build

Once you have the path to your Java 21 installation, you can use it in one of these ways:

### Method 1: Edit gradle.properties

1. Open `gradle.properties`
2. Add or uncomment this line, replacing the path with your actual Java 21 path:
   ```
   org.gradle.java.home=C:/Users/yourusername/.jdks/jbr-21
   ```
   (Make sure to use forward slashes or escape backslashes)

### Method 2: Use the fix-build.bat Script

The included `fix-build.bat` script can automatically detect and configure Java 21:

```
fix-build.bat
```

### Method 3: Environment Variable

Set the `JAVA_HOME` environment variable to point to your Java 21 installation:

```
set JAVA_HOME=C:\Users\yourusername\.jdks\jbr-21
```

Then run gradle with this environment variable active.

## Bypassing Validation

If you're still having trouble, you can use the `simple-build.bat` script which skips validation and just builds the plugin:

```
simple-build.bat
```

## Need More Help?

If you're still having trouble, try running one of these commands to get detailed build information:

```
gradlew buildPlugin --info
gradlew buildPlugin --debug
gradlew buildPlugin --stacktrace
```

These will provide more detailed error messages that may help diagnose the problem.