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

    /**
     * Minecraft 1.21.1 permission levels:
     * 0 = everyone, 1-4 = vanilla OP levels (4 = full OP).
     */
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
                save();
                Uuid.LOGGER.info("Loaded config from {}", CONFIG_PATH);
            } else {
                INSTANCE = new UuidConfig();
                save();
                Uuid.LOGGER.info("Created default config at {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            Uuid.LOGGER.error("Failed to load config. Falling back to defaults.", e);
            INSTANCE = new UuidConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            Uuid.LOGGER.error("Failed to save config", e);
        }
    }

    private static void sanitize() {
        INSTANCE.playerSwapPermissionLevel = clampPermission(INSTANCE.playerSwapPermissionLevel);
        INSTANCE.petOwnerSwapPermissionLevel = clampPermission(INSTANCE.petOwnerSwapPermissionLevel);
    }

    private static int clampPermission(int value) {
        return Math.max(0, Math.min(4, value));
    }
}
