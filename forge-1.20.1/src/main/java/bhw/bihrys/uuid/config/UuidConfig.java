package bhw.bihrys.uuid.config;

import bhw.bihrys.uuid.Uuid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UuidConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "uuid-config.json");

    public static UuidConfig INSTANCE = new UuidConfig();

    public int playerSwapPermissionLevel = 4;
    public int petOwnerSwapPermissionLevel = 4;
    public boolean backupBeforeOverwrite = true;
    public boolean transferPetsAutomatically = true;
    public boolean enablePetOwnerSwap = true;

    private UuidConfig() {
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    UuidConfig loaded = GSON.fromJson(reader, UuidConfig.class);
                    INSTANCE = loaded == null ? new UuidConfig() : loaded;
                }
                sanitize();
            }
            save();
        } catch (Exception exception) {
            Uuid.LOGGER.error("Failed to load UUID Swap config; using defaults", exception);
            INSTANCE = new UuidConfig();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception exception) {
            Uuid.LOGGER.error("Failed to save UUID Swap config", exception);
        }
    }

    private static void sanitize() {
        INSTANCE.playerSwapPermissionLevel = clamp(INSTANCE.playerSwapPermissionLevel);
        INSTANCE.petOwnerSwapPermissionLevel = clamp(INSTANCE.petOwnerSwapPermissionLevel);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(4, value));
    }
}
