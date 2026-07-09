package bhw.bihrys.uuid.command;

import bhw.bihrys.uuid.Uuid;
import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.player.PlayerDataManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class UuidSwapCommand {

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				CommandManager.literal("uuidswap")
					.requires(UuidSwapCommand::hasPermission)
					.executes(ctx -> listPlayers(ctx.getSource()))
					.then(
						CommandManager.argument("targetUuid", UuidArgumentType.uuid())
							.executes(ctx -> swapToPlayer(ctx.getSource(), UuidArgumentType.getUuid(ctx, "targetUuid")))
					)
			);
		});
	}

	private static boolean hasPermission(ServerCommandSource source) {
		int requiredLevel = UuidConfig.REQUIRED_PERMISSION_LEVEL;

		// Check if entity is a player with op status
		if (source.getEntity() instanceof ServerPlayerEntity player) {
			var permLevel = source.getServer().getPermissionLevel(player.getGameProfile());
			return permLevel >= requiredLevel;
		}

		// Console always has permission
		return source.getEntity() == null;
	}

	private static int listPlayers(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();

		// Get all players
		var allPlayers = PlayerDataManager.getAllPlayers(source.getServer());

		// Send feedback
		source.sendFeedback(() -> Text.literal("§6=== Available Players ==="), false);
		for (var info : allPlayers) {
			source.sendFeedback(() -> Text.literal(String.format("  §e%s §7(%s)", info.name, info.uuid)), false);
		}

		source.sendFeedback(() -> Text.literal("§6Use: /uuidswap <uuid>"), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int swapToPlayer(ServerCommandSource source, UUID targetUuid) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayerOrThrow();

		// Prevent swapping to same UUID
		if (player.getUuid().equals(targetUuid)) {
			source.sendError(Text.literal("§cYou cannot swap to your own UUID!"));
			return 0;
		}

		// Perform the swap
		PlayerDataManager.swapPlayerData(player, targetUuid, source.getServer());
		source.sendFeedback(() -> Text.literal("§aPlayer data swap completed!"), true);

		return Command.SINGLE_SUCCESS;
	}
}
