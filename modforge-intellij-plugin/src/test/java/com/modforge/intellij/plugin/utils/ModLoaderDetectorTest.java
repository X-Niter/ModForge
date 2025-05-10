package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Tests for the ModLoaderDetector class.
 */
public class ModLoaderDetectorTest extends BasePlatformTestCase {
    
    /**
     * Test detecting Forge from mods.toml.
     */
    @Test
    public void testDetectForgeFromModsToml() {
        // Create a mock project
        Project mockProject = mock(Project.class);
        
        // Create a mock virtual file for mods.toml
        VirtualFile mockFile = mock(VirtualFile.class);
        when(mockFile.getName()).thenReturn("mods.toml");
        
        // Mock the content of mods.toml
        String content = "modLoader=\"javafml\"\n" +
                "loaderVersion=\"[36,)\"\n" +
                "license=\"MIT\"\n" +
                "[[mods]]\n" +
                "modId=\"examplemod\"\n" +
                "version=\"${file.jarVersion}\"\n" +
                "displayName=\"Example Mod\"\n" +
                "authors=\"Forge Developer\"\n" +
                "description='''This is an example mod for Minecraft Forge.'''";
        
        try {
            // Mock the ModLoaderDetector to find our mock file
            ModLoaderDetector.findFileInProject = (project, fileName) -> {
                if (fileName.equals("mods.toml")) {
                    return mockFile;
                }
                return null;
            };
            
            // Mock the file content
            when(mockFile.contentsToByteArray()).thenReturn(content.getBytes());
            
            // Call the method under test
            ModLoaderDetector.ModLoader loader = ModLoaderDetector.detectModLoader(mockProject);
            
            // Verify the result
            assertEquals(ModLoaderDetector.ModLoader.FORGE, loader);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
    
    /**
     * Test detecting Fabric from fabric.mod.json.
     */
    @Test
    public void testDetectFabricFromModJson() {
        // Create a mock project
        Project mockProject = mock(Project.class);
        
        // Create a mock virtual file for fabric.mod.json
        VirtualFile mockFile = mock(VirtualFile.class);
        when(mockFile.getName()).thenReturn("fabric.mod.json");
        
        try {
            // Mock the ModLoaderDetector to find our mock file
            ModLoaderDetector.findFileInProject = (project, fileName) -> {
                if (fileName.equals("fabric.mod.json")) {
                    return mockFile;
                }
                return null;
            };
            
            // Call the method under test
            ModLoaderDetector.ModLoader loader = ModLoaderDetector.detectModLoader(mockProject);
            
            // Verify the result
            assertEquals(ModLoaderDetector.ModLoader.FABRIC, loader);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
    
    /**
     * Test detecting Quilt from quilt.mod.json.
     */
    @Test
    public void testDetectQuiltFromModJson() {
        // Create a mock project
        Project mockProject = mock(Project.class);
        
        // Create a mock virtual file for quilt.mod.json
        VirtualFile mockFile = mock(VirtualFile.class);
        when(mockFile.getName()).thenReturn("quilt.mod.json");
        
        try {
            // Mock the ModLoaderDetector to find our mock file
            ModLoaderDetector.findFileInProject = (project, fileName) -> {
                if (fileName.equals("quilt.mod.json")) {
                    return mockFile;
                }
                return null;
            };
            
            // Call the method under test
            ModLoaderDetector.ModLoader loader = ModLoaderDetector.detectModLoader(mockProject);
            
            // Verify the result
            assertEquals(ModLoaderDetector.ModLoader.QUILT, loader);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
    
    /**
     * Test detecting Architectury from build.gradle.
     */
    @Test
    public void testDetectArchitecturyFromBuildGradle() {
        // Create a mock project
        Project mockProject = mock(Project.class);
        
        // Create a mock virtual file for build.gradle
        VirtualFile mockFile = mock(VirtualFile.class);
        when(mockFile.getName()).thenReturn("build.gradle");
        
        // Mock the content of build.gradle
        String content = "plugins {\n" +
                "    id 'fabric-loom' version '0.12.22' apply false\n" +
                "    id 'dev.architectury.loom' version '0.11.0.1' apply false\n" +
                "    id 'architectury-plugin' version '3.4.131' apply false\n" +
                "}\n" +
                "\n" +
                "subprojects {\n" +
                "    apply plugin: 'dev.architectury.loom'\n" +
                "}\n";
        
        try {
            // Mock the ModLoaderDetector to find our mock file
            ModLoaderDetector.findFileInProject = (project, fileName) -> {
                if (fileName.equals("build.gradle")) {
                    return mockFile;
                }
                return null;
            };
            
            // Mock the file content
            when(mockFile.contentsToByteArray()).thenReturn(content.getBytes());
            
            // Also mock the findDirectoryInProject to find common, fabric, and forge folders
            VirtualFile mockCommonDir = mock(VirtualFile.class);
            when(mockCommonDir.getName()).thenReturn("common");
            VirtualFile mockFabricDir = mock(VirtualFile.class);
            when(mockFabricDir.getName()).thenReturn("fabric");
            VirtualFile mockForgeDir = mock(VirtualFile.class);
            when(mockForgeDir.getName()).thenReturn("forge");
            
            ModLoaderDetector.findDirectoryInProject = (project, dirName) -> {
                if (dirName.equals("common")) {
                    return mockCommonDir;
                } else if (dirName.equals("fabric")) {
                    return mockFabricDir;
                } else if (dirName.equals("forge")) {
                    return mockForgeDir;
                }
                return null;
            };
            
            // Call the method under test
            ModLoaderDetector.ModLoader loader = ModLoaderDetector.detectModLoader(mockProject);
            
            // Verify the result
            assertEquals(ModLoaderDetector.ModLoader.ARCHITECTURY, loader);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
    
    /**
     * Test detecting unknown mod loader.
     */
    @Test
    public void testDetectUnknownModLoader() {
        // Create a mock project
        Project mockProject = mock(Project.class);
        
        try {
            // Mock the ModLoaderDetector to not find any files
            ModLoaderDetector.findFileInProject = (project, fileName) -> null;
            ModLoaderDetector.findDirectoryInProject = (project, dirName) -> null;
            
            // Call the method under test
            ModLoaderDetector.ModLoader loader = ModLoaderDetector.detectModLoader(mockProject);
            
            // Verify the result
            assertEquals(ModLoaderDetector.ModLoader.UNKNOWN, loader);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
}