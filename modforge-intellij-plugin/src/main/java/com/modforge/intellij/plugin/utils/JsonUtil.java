package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Utility for JSON operations.
 * This class provides utility methods for working with JSON data.
 */
public final class JsonUtil {
    private static final Logger LOG = Logger.getInstance(JsonUtil.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    
    /**
     * Private constructor to prevent instantiation.
     */
    private JsonUtil() {
        // Utility class
    }
    
    /**
     * Writes an object to a JSON string.
     *
     * @param obj The object to write
     * @return The JSON string
     */
    @NotNull
    public static String writeToString(@NotNull Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * Reads a JSON string to an object.
     *
     * @param json The JSON string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object, or null if an error occurs
     */
    @Nullable
    public static <T> T readFromString(@NotNull String json, @NotNull Class<T> clazz) {
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            LOG.warn("Failed to read JSON string to object of class " + clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * Reads a JSON string to a map.
     *
     * @param json The JSON string
     * @return The map, or null if an error occurs
     */
    @Nullable
    public static Map<String, Object> readMapFromString(@NotNull String json) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return GSON.fromJson(json, type);
        } catch (Exception e) {
            LOG.warn("Failed to read JSON string to map", e);
            return null;
        }
    }
    
    /**
     * Reads a JSON stream to a map.
     *
     * @param stream The input stream
     * @return The map, or null if an error occurs
     */
    @Nullable
    public static Map<String, Object> readMapFromStream(@NotNull InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return GSON.fromJson(reader, type);
        } catch (IOException e) {
            LOG.warn("Failed to read JSON stream to map", e);
            return null;
        }
    }
    
    /**
     * Reads a JSON string to an array.
     *
     * @param json The JSON string
     * @param clazz The class of the array elements
     * @param <T> The type of the array elements
     * @return The array, or null if an error occurs
     */
    @Nullable
    public static <T> T[] readArrayFromString(@NotNull String json, @NotNull Class<T[]> clazz) {
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            LOG.warn("Failed to read JSON string to array of class " + clazz.getComponentType().getName(), e);
            return null;
        }
    }
}