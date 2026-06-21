package de.maxi.placeonpath.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Plain JSON config: the set of block ids (and optional #tags) that turn a path to dirt. */
public final class ConfigStore {
    private ConfigStore() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // CopyOnWriteArraySet: ordered, and safe to iterate (PathRules, server thread) while edited (screen).
    private static final Set<String> TURN_TO_DIRT = new CopyOnWriteArraySet<>();
    private static Path file;

    /** On-disk JSON shape. */
    private static final class Data {
        List<String> turnPathToDirt = new ArrayList<>();
    }

    public static void load() {
        file = Platform.getConfigFolder().resolve("placeonpath.json");
        TURN_TO_DIRT.clear();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                Data data = GSON.fromJson(r, Data.class);
                if (data != null && data.turnPathToDirt != null) {
                    TURN_TO_DIRT.addAll(data.turnPathToDirt);
                }
            } catch (Exception ignored) {
                // Corrupt/old file: start empty rather than crash.
            }
        }
    }

    public static void save() {
        if (file == null) load();
        Data data = new Data();
        data.turnPathToDirt = new ArrayList<>(TURN_TO_DIRT);
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(data, w);
            }
        } catch (IOException ignored) {
            // Best effort.
        }
    }

    public static Set<String> entries() { return TURN_TO_DIRT; }

    public static boolean isBlacklisted(String id) { return TURN_TO_DIRT.contains(id); }

    public static void set(String id, boolean blacklisted) {
        if (blacklisted) TURN_TO_DIRT.add(id);
        else TURN_TO_DIRT.remove(id);
    }

    public static void setAll(Collection<String> ids, boolean blacklisted) {
        if (blacklisted) TURN_TO_DIRT.addAll(ids);
        else TURN_TO_DIRT.removeAll(ids);
    }

    /** Replace the whole in-memory set (used to discard unsaved screen edits). Does not write to disk. */
    public static void replaceAll(Collection<String> ids) {
        TURN_TO_DIRT.clear();
        TURN_TO_DIRT.addAll(ids);
    }
}
