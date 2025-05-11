package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for working with IntelliJ services
 * Provides safe methods to get services with proper exception handling
 */
public class ServiceUtil {
    private static final Logger LOG = Logger.getInstance(ServiceUtil.class);
    
    /**
     * Safely get a service from the application
     * 
     * @param serviceClass The service class
     * @param <T> The service type
     * @return The service instance or null if not available
     */
    @Nullable
    public static <T> T getServiceFromApplication(@NotNull Class<T> serviceClass) {
        try {
            return ApplicationManager.getApplication().getService(serviceClass);
        } catch (Exception e) {
            LOG.warn("Failed to get application service: " + serviceClass.getName(), e);
            return null;
        }
    }
    
    /**
     * Safely get a service from a project
     * 
     * @param project The project
     * @param serviceClass The service class
     * @param <T> The service type
     * @return The service instance or null if not available
     */
    @Nullable
    public static <T> T getServiceFromProject(@NotNull Project project, @NotNull Class<T> serviceClass) {
        try {
            if (project.isDisposed()) {
                LOG.warn("Attempted to get service " + serviceClass.getName() + " from disposed project");
                return null;
            }
            return project.getService(serviceClass);
        } catch (Exception e) {
            LOG.warn("Failed to get project service: " + serviceClass.getName(), e);
            return null;
        }
    }
    
    /**
     * Check if a project service is available
     * 
     * @param project The project
     * @param serviceClass The service class
     * @return True if the service is available
     */
    public static boolean isProjectServiceAvailable(@NotNull Project project, @NotNull Class<?> serviceClass) {
        try {
            if (project.isDisposed()) {
                return false;
            }
            return project.getService(serviceClass) != null;
        } catch (Exception e) {
            LOG.debug("Service not available: " + serviceClass.getName(), e);
            return false;
        }
    }
    
    /**
     * Check if an application service is available
     * 
     * @param serviceClass The service class
     * @return True if the service is available
     */
    public static boolean isApplicationServiceAvailable(@NotNull Class<?> serviceClass) {
        try {
            return ApplicationManager.getApplication().getService(serviceClass) != null;
        } catch (Exception e) {
            LOG.debug("Application service not available: " + serviceClass.getName(), e);
            return false;
        }
    }
}