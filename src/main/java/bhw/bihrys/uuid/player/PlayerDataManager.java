package bhw.bihrys.uuid.player;

import bhw.bihrys.uuid.Uuid;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataManager {

	public static void swapPlayerData(ServerPlayerEntity sourcePlayer, UUID targetUuid, MinecraftServer server) {
		try {
			// Load target player's data from disk
			NbtCompound targetData = loadPlayerData(targetUuid, server);
			if (targetData == null) {
				Uuid.LOGGER.warn("Could not load data for player {}", targetUuid);
				return;
			}

			// Save source player's current state before swap
			sourcePlayer.getAdvancementTracker().save();
			sourcePlayer.getStatHandler().save();

			// Copy target data onto source player
			copyPlayerData(sourcePlayer, targetData, server);

			// Transfer target's pets to source player if enabled
			if (bhw.bihrys.uuid.config.UuidConfig.transferPetsAutomatically) {
				transferPets(targetUuid, sourcePlayer.getUuid(), server);
			}

			// Resync player
			resyncPlayer(sourcePlayer, server);

			Uuid.LOGGER.info("Swapped player {} with data from {}", sourcePlayer.getName().getString(), targetUuid);
		} catch (Exception e) {
			Uuid.LOGGER.error("Error swapping player data", e);
		}
	}

	private static NbtCompound loadPlayerData(UUID uuid, MinecraftServer server) {
		try {
			Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
			Path playerFile = playerDataDir.resolve(uuid + ".dat");

			if (!Files.exists(playerFile)) {
				return null;
			}

			return NbtIo.read(playerFile);
		} catch (Exception e) {
			Uuid.LOGGER.error("Failed to load player data for {}", uuid, e);
			return null;
		}
	}

	private static void copyPlayerData(ServerPlayerEntity player, NbtCompound data, MinecraftServer server) {
		try {
			ErrorReporter reporter = new ErrorReporter.Logging(Uuid.LOGGER);
			var readView = NbtReadView.create(reporter, server.getRegistryManager(), data);

			// Load player data using the new ReadView API
			player.readData(readView);

			// Reset position to last known location (don't teleport mid-air)
			if (data.contains("Pos")) {
				NbtList posList = data.getListOrEmpty("Pos");
				if (posList.size() == 3) {
					double x = posList.getDouble(0, 0.0);
					double y = posList.getDouble(1, 0.0);
					double z = posList.getDouble(2, 0.0);
					player.setPosition(x, y, z);
				}
			}

			// Copy experience and health
			if (data.contains("XpLevel")) {
				player.setExperienceLevel(data.getInt("XpLevel", 0));
			}
			if (data.contains("XpP")) {
				player.experienceProgress = data.getFloat("XpP", 0.0f);
			}
			if (data.contains("Health")) {
				player.setHealth(data.getFloat("Health", 20.0f));
			}

		} catch (Exception e) {
			Uuid.LOGGER.error("Failed to copy player data", e);
		}
	}

	private static void resyncPlayer(ServerPlayerEntity player, MinecraftServer server) {
		try {
			// Resync stats
			player.getStatHandler().sendStats(player);

			// Resync advancements
			player.getAdvancementTracker().reload(server.getAdvancementLoader());

			// Resync abilities
			player.sendAbilitiesUpdate();

			// Mark for full update
			player.markHealthDirty();
		} catch (Exception e) {
			Uuid.LOGGER.error("Failed to resync player", e);
		}
	}

	private static void transferPets(UUID oldOwner, UUID newOwner, MinecraftServer server) {
		try {
			for (ServerWorld world : server.getWorlds()) {
				world.iterateEntities().forEach(entity -> {
					if (entity instanceof TameableEntity tameable) {
						UUID ownerUuid = tameable.getOwnerReference().getUuid();
						if (ownerUuid.equals(oldOwner)) {
							// Find the new owner player entity
							var newOwnerPlayer = server.getPlayerManager().getPlayer(newOwner);
							if (newOwnerPlayer != null) {
								tameable.setOwner(newOwnerPlayer);
								Uuid.LOGGER.info("Transferred pet {} from {} to {}",
									entity.getType().toString(), oldOwner, newOwner);
							}
						}
					}
				});
			}
		} catch (Exception e) {
			Uuid.LOGGER.error("Failed to transfer pets", e);
		}
	}

	public static List<PlayerInfo> getAllPlayers(MinecraftServer server) {
		List<PlayerInfo> players = new ArrayList<>();

		// Add online players
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			players.add(new PlayerInfo(player.getUuid(), player.getName().getString()));
		}

		// Add offline players from playerdata directory
		try {
			Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
			if (Files.exists(playerDataDir)) {
				Files.list(playerDataDir)
					.filter(p -> p.toString().endsWith(".dat"))
					.forEach(p -> {
						try {
							String fileName = p.getFileName().toString();
							UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));

							// Skip if already in online players
							if (server.getPlayerManager().getPlayer(uuid) == null) {
								// Try to get player name from name-to-id cache
								var configEntry = server.getApiServices().nameToIdCache().getByUuid(uuid);
								String name = configEntry.map(PlayerConfigEntry::name).orElse(uuid.toString());
								players.add(new PlayerInfo(uuid, name));
							}
						} catch (Exception e) {
							Uuid.LOGGER.debug("Failed to read player file {}", p, e);
						}
					});
			}
		} catch (Exception e) {
			Uuid.LOGGER.error("Failed to list offline players", e);
		}

		return players;
	}

	public static class PlayerInfo {
		public final UUID uuid;
		public final String name;

		public PlayerInfo(UUID uuid, String name) {
			this.uuid = uuid;
			this.name = name;
		}
	}
}
