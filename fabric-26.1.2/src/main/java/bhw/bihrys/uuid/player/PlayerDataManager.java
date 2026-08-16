package bhw.bihrys.uuid.player;

import bhw.bihrys.uuid.Uuid;
import bhw.bihrys.uuid.config.UuidConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

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
            ServerPlayer player = handler.player;
            if (player == null) {
                return;
            }

            PendingSwap pending = PENDING_ONLINE_SWAPS.remove(player.getUUID());
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

        ServerPlayer onlineSource = server.getPlayerList().getPlayer(sourceUuid);
        if (onlineSource != null) {
            PENDING_ONLINE_SWAPS.put(sourceUuid, new PendingSwap(sourceUuid, targetUuid, initiatorName));
            onlineSource.connection.disconnect(Component.literal("UUID Swap: 你的玩家数据将在断开连接后替换。请重新进入世界。"));
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
                    server.getWorldPath(LevelResource.ROOT).resolve("uuid_backups"),
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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            result.put(player.getUUID(), new PlayerInfo(player.getUUID(), player.getName().getString(), true));
        }

        addPlayersFromDirectory(server, result, server.getWorldPath(LevelResource.PLAYER_DATA_DIR), ".dat");
        addPlayersFromDirectory(server, result, server.getWorldPath(LevelResource.PLAYER_STATS_DIR), ".json");
        addPlayersFromDirectory(server, result, server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR), ".json");

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
            result.putIfAbsent(uuid, new PlayerInfo(uuid, resolveName(server, uuid), server.getPlayerList().getPlayer(uuid) != null));
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID-named player file.
                }
            });
        } catch (IOException e) {
            Uuid.LOGGER.warn("Failed to scan player directory {}", directory, e);
        }
    }

    private static String resolveName(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }
        Optional<NameAndId> entry = server.services().nameToIdCache().get(uuid);
        return entry.map(NameAndId::name).orElse(uuid.toString());
    }

    public static int transferLoadedPets(MinecraftServer server, UUID oldOwner, UUID newOwner) {
        int count = 0;
        EntityReference<LivingEntity> newOwnerReference = EntityReference.of(newOwner);

        for (ServerLevel world : server.getAllLevels()) {
            for (Entity entity : world.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tameable) || !tameable.isTame()) {
                    continue;
                }
                EntityReference<LivingEntity> owner = tameable.getOwnerReference();
                if (owner != null && oldOwner.equals(owner.getUUID())) {
                    tameable.setOwnerReference(newOwnerReference);
                    count++;
                }
            }
        }
        return count;
    }

    private static Path getPlayerDataFile(MinecraftServer server, UUID uuid) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + ".dat");
    }

    private static Path getStatsFile(MinecraftServer server, UUID uuid) {
        return server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json");
    }

    private static Path getAdvancementsFile(MinecraftServer server, UUID uuid) {
        return server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
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
