package bhw.bihrys.uuid.player;

import bhw.bihrys.uuid.Uuid;
import bhw.bihrys.uuid.config.UuidConfig;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    private static final Map<UUID, PendingSwap> PENDING_ONLINE_SWAPS = new ConcurrentHashMap<>();
    private static final List<PendingSwap> READY_TO_APPLY = new ArrayList<>();

    private PlayerDataManager() {
    }

    public static void registerEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            if (player == null) {
                return;
            }

            PendingSwap pending = PENDING_ONLINE_SWAPS.remove(player.getUuid());
            if (pending != null) {
                synchronized (READY_TO_APPLY) {
                    READY_TO_APPLY.add(pending);
                }
                Uuid.LOGGER.info("Queued UUID data replacement after disconnect: source={}, target={}", pending.sourceUuid(), pending.targetUuid());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<PendingSwap> swaps;
            synchronized (READY_TO_APPLY) {
                if (READY_TO_APPLY.isEmpty()) {
                    return;
                }
                swaps = new ArrayList<>(READY_TO_APPLY);
                READY_TO_APPLY.clear();
            }

            for (PendingSwap swap : swaps) {
                try {
                    applySwap(server, swap.sourceUuid(), swap.targetUuid(), swap.initiatorName());
                    Uuid.LOGGER.info("Applied delayed UUID data replacement: source={}, target={}", swap.sourceUuid(), swap.targetUuid());
                } catch (Exception e) {
                    Uuid.LOGGER.error("Failed to apply delayed UUID data replacement: source={}, target={}", swap.sourceUuid(), swap.targetUuid(), e);
                }
            }
        });
    }

    /**
     * Replace source player's saved data with target player's saved data.
     * If source is online, the player is disconnected first so Minecraft finishes its final save before files are overwritten.
     */
    public static SwapResult requestSwap(MinecraftServer server, UUID sourceUuid, UUID targetUuid, String initiatorName) {
        if (sourceUuid.equals(targetUuid)) {
            return SwapResult.fail("不能把玩家数据替换成自己。源 UUID 和目标 UUID 相同。");
        }
        if (!hasAnyData(server, targetUuid)) {
            return SwapResult.fail("找不到目标玩家的数据文件：" + targetUuid);
        }

        ServerPlayerEntity onlineSource = server.getPlayerManager().getPlayer(sourceUuid);
        if (onlineSource != null) {
            PENDING_ONLINE_SWAPS.put(sourceUuid, new PendingSwap(sourceUuid, targetUuid, initiatorName));
            onlineSource.networkHandler.disconnect(Text.literal("UUID Swap: 你的玩家数据将在断开连接后替换。请重新进入世界。"));
            return SwapResult.ok("源玩家在线，已断开连接；数据会在断开后的下一 tick 自动替换。替换后让该玩家重新进入世界。");
        }

        try {
            applySwap(server, sourceUuid, targetUuid, initiatorName);
            return SwapResult.ok("玩家数据替换完成。source=" + sourceUuid + ", target=" + targetUuid);
        } catch (Exception e) {
            Uuid.LOGGER.error("Failed to replace player data: source={}, target={}", sourceUuid, targetUuid, e);
            return SwapResult.fail("玩家数据替换失败：" + e.getMessage());
        }
    }

    private static void applySwap(MinecraftServer server, UUID sourceUuid, UUID targetUuid, String initiatorName) throws IOException {
        if (UuidConfig.INSTANCE.backupBeforeOverwrite) {
            PlayerDataFiles.backup(
                    getWorldRoot(server).resolve("uuid_backups"),
                    sourceUuid,
                    targetUuid,
                    initiatorName,
                    getPlayerDataFile(server, sourceUuid),
                    getStatsFile(server, sourceUuid),
                    getAdvancementsFile(server, sourceUuid)
            );
        }

        PlayerDataFiles.replace(
                getPlayerDataFile(server, targetUuid),
                getPlayerDataFile(server, sourceUuid),
                getStatsFile(server, targetUuid),
                getStatsFile(server, sourceUuid),
                getAdvancementsFile(server, targetUuid),
                getAdvancementsFile(server, sourceUuid)
        );

        if (UuidConfig.INSTANCE.transferPetsAutomatically) {
            int transferred = transferLoadedPets(server, targetUuid, sourceUuid);
            Uuid.LOGGER.info("Transferred {} loaded tameable entities from {} to {}", transferred, targetUuid, sourceUuid);
        }
    }

    public static boolean hasAnyData(MinecraftServer server, UUID uuid) {
        return PlayerDataFiles.hasAnyData(
                getPlayerDataFile(server, uuid),
                getStatsFile(server, uuid),
                getAdvancementsFile(server, uuid)
        );
    }

    public static List<PlayerInfo> getAllPlayers(MinecraftServer server) {
        Map<UUID, PlayerInfo> result = new LinkedHashMap<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            result.put(player.getUuid(), new PlayerInfo(player.getUuid(), player.getName().getString(), true));
        }

        addPlayersFromDirectory(server, result, server.getSavePath(WorldSavePath.PLAYERDATA), ".dat");
        addPlayersFromDirectory(server, result, server.getSavePath(WorldSavePath.STATS), ".json");
        addPlayersFromDirectory(server, result, server.getSavePath(WorldSavePath.ADVANCEMENTS), ".json");

        return result.values().stream()
                .sorted(Comparator.comparing(PlayerInfo::online).reversed().thenComparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void addPlayersFromDirectory(MinecraftServer server, Map<UUID, PlayerInfo> result, Path directory, String extension) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(extension)).forEach(path -> {
                String fileName = path.getFileName().toString();
                String uuidText = fileName.substring(0, fileName.length() - extension.length());
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    result.putIfAbsent(uuid, new PlayerInfo(uuid, resolveName(server, uuid), server.getPlayerManager().getPlayer(uuid) != null));
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID-named player file.
                }
            });
        } catch (IOException e) {
            Uuid.LOGGER.warn("Failed to scan player directory {}", directory, e);
        }
    }

    private static String resolveName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }
        Optional<GameProfile> profile = server.getUserCache().getByUuid(uuid);
        return profile.map(GameProfile::getName).orElse(uuid.toString());
    }

    public static int transferLoadedPets(MinecraftServer server, UUID oldOwner, UUID newOwner) {
        int count = 0;
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof TameableEntity tameable) || !tameable.isTamed()) {
                    continue;
                }
                UUID owner = tameable.getOwnerUuid();
                if (oldOwner.equals(owner)) {
                    tameable.setOwnerUuid(newOwner);
                    count++;
                }
            }
        }
        return count;
    }

    private static Path getWorldRoot(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.PLAYERDATA).getParent();
    }

    private static Path getPlayerDataFile(MinecraftServer server, UUID uuid) {
        return server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuid + ".dat");
    }

    private static Path getStatsFile(MinecraftServer server, UUID uuid) {
        return server.getSavePath(WorldSavePath.STATS).resolve(uuid + ".json");
    }

    private static Path getAdvancementsFile(MinecraftServer server, UUID uuid) {
        return server.getSavePath(WorldSavePath.ADVANCEMENTS).resolve(uuid + ".json");
    }

    private record PendingSwap(UUID sourceUuid, UUID targetUuid, String initiatorName) {
    }

    public record SwapResult(boolean success, String message) {
        public static SwapResult ok(String message) {
            return new SwapResult(true, message);
        }

        public static SwapResult fail(String message) {
            return new SwapResult(false, message);
        }
    }
}
