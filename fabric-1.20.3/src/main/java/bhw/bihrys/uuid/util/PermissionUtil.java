package bhw.bihrys.uuid.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(ServerCommandSource source, int requiredLevel) {
        // Dedicated-server console is allowed to use management commands.
        if (source.getEntity() == null) {
            return true;
        }
        return source.hasPermissionLevel(clamp(requiredLevel));
    }

    public static boolean has(ServerPlayerEntity player, int requiredLevel) {
        return player.hasPermissionLevel(clamp(requiredLevel));
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(4, level));
    }
}
