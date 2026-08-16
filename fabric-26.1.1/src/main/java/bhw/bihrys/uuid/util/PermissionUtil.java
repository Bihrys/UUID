package bhw.bihrys.uuid.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(CommandSourceStack source, int requiredLevel) {
        // Dedicated-server console is allowed to use management commands.
        if (source.getEntity() == null) {
            return true;
        }
        return check(requiredLevel).check(source.permissions());
    }

    public static boolean has(ServerPlayer player, int requiredLevel) {
        PermissionSet permissions = player.permissions();
        return check(requiredLevel).check(permissions);
    }

    private static PermissionCheck check(int level) {
        return switch (Math.max(0, Math.min(4, level))) {
            case 0 -> Commands.LEVEL_ALL;
            case 1 -> Commands.LEVEL_MODERATORS;
            case 2 -> Commands.LEVEL_GAMEMASTERS;
            case 3 -> Commands.LEVEL_ADMINS;
            default -> Commands.LEVEL_OWNERS;
        };
    }
}
