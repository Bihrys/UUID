package bhw.bihrys.uuid.config;

import bhw.bihrys.uuid.Uuid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UuidConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Paths.get("config", "uuid-config.json");

	public static int playerSwapPermissionLevel = 3; // Default: OPs
	public static boolean transferPetsAutomatically = true;
	public static boolean enablePetOwnerSwap = true;

	public static void load() {
		try {
			File configFile = CONFIG_PATH.toFile();
			if (configFile.exists()) {
				try (FileReader reader = new FileReader(configFile)) {
					JsonObject json = GSON.fromJson(reader, JsonObject.class);
					playerSwapPermissionLevel = json.has("playerSwapPermissionLevel") ?
						json.get("playerSwapPermissionLevel").getAsInt() : 3;
					transferPetsAutomatically = json.has("transferPetsAutomatically") ?
						json.get("transferPetsAutomatically").getAsBoolean() : true;
					enablePetOwnerSwap = json.has("enablePetOwnerSwap") ?
						json.get("enablePetOwnerSwap").getAsBoolean() : true;
				}
				Uuid.LOGGER.info("Loaded UUID config from {}", CONFIG_PATH);
			} else {
				save();
				Uuid.LOGGER.info("Created default UUID config at {}", CONFIG_PATH);
			}
		} catch (IOException e) {
			Uuid.LOGGER.error("Failed to load UUID config", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("playerSwapPermissionLevel", playerSwapPermissionLevel);
			json.addProperty("transferPetsAutomatically", transferPetsAutomatically);
			json.addProperty("enablePetOwnerSwap", enablePetOwnerSwap);

			try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			Uuid.LOGGER.error("Failed to save UUID config", e);
		}
	}
}
