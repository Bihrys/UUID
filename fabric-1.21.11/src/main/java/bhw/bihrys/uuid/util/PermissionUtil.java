package bhw.bihrys.uuid.util;

import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.CommandManager;
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
        return check(requiredLevel).allows(source.getPermissions());
    }

    public static boolean has(ServerPlayerEntity player, int requiredLevel) {
        PermissionPredicate permissions = player.getPermissions();
        return check(requiredLevel).allows(permissions);
    }

    private static PermissionCheck check(int level) {
        return switch (Math.max(0, Math.min(4, level))) {
            case 0 -> CommandManager.ALWAYS_PASS_CHECK;
            case 1 -> CommandManager.MODERATORS_CHECK;
            case 2 -> CommandManager.GAMEMASTERS_CHECK;
            case 3 -> CommandManager.ADMINS_CHECK;
            default -> CommandManager.OWNERS_CHECK;
        };
    }
}
