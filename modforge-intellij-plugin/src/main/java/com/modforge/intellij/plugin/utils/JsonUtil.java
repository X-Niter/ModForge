package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for JSON operations.
 * This class provides methods for serializing and deserializing JSON data.
 */
public final class JsonUtil {
    private static final Logger LOG = Logger.getInstance(JsonUtil.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Writes an object to a JSON string.
     * 
     * @param object The object to serialize
     * @return The JSON string
     */
    @NotNull
    public static String writeToString(@NotNull Object object) {
        return GSON.toJson(object);
    }

    /**
     * Reads a JSON string into an object.
     * 
     * @param json The JSON string
     * @param classOfT The class of T
     * @param <T> The type to deserialize to
     * @return The deserialized object
     */
    @Nullable
    public static <T> T readFromString(@NotNull String json, @NotNull Class<T> classOfT) {
        try {
            return GSON.fromJson(json, classOfT);
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reads a JSON string into a map.
     * 
     * @param json The JSON string
     * @return The deserialized map
     */
    @Nullable
    public static Map<String, Object> readMapFromString(@NotNull String json) {
        try {
            return GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON to map: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reads a JSON input stream into an object.
     * 
     * @param inputStream The input stream
     * @param classOfT The class of T
     * @param <T> The type to deserialize to
     * @return The deserialized object
     * @throws IOException If an IO error occurs
     */
    @Nullable
    public static <T> T readFromStream(@NotNull InputStream inputStream, @NotNull Class<T> classOfT) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, classOfT);
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON from stream: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reads a JSON input stream into a map.
     * 
     * @param inputStream The input stream
     * @return The deserialized map
     * @throws IOException If an IO error occurs
     */
    @Nullable
    public static Map<String, Object> readMapFromStream(@NotNull InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON from stream to map: " + e.getMessage(), e);
            return null;
        }
    }
}