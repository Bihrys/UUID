package bhw.bihrys.uuid.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(CommandSourceStack source, int requiredLevel) {
        return source.getEntity() == null || source.hasPermission(clamp(requiredLevel));
    }

    public static boolean has(ServerPlayer player, int requiredLevel) {
        return player.hasPermissions(clamp(requiredLevel));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(4, value));
    }
}
