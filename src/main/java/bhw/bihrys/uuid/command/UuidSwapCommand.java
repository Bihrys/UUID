package bhw.bihrys.uuid.command;

import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.gui.PlayerSelectionGui;
import bhw.bihrys.uuid.player.PlayerDataManager;
import bhw.bihrys.uuid.player.PlayerInfo;
import bhw.bihrys.uuid.util.PermissionUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public final class UuidSwapCommand {
    private UuidSwapCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("uuidswap")
                .requires(source -> PermissionUtil.has(source, UuidConfig.INSTANCE.playerSwapPermissionLevel))
                .executes(context -> openSelfSwapGui(context.getSource()))
                .then(CommandManager.literal("list")
                        .executes(context -> listPlayers(context.getSource())))
                .then(CommandManager.literal("apply")
                        .then(CommandManager.argument("sourceUuid", UuidArgumentType.uuid())
                                .then(CommandManager.argument("targetUuid", UuidArgumentType.uuid())
                                        .executes(context -> applySwap(
                                                context.getSource(),
                                                UuidArgumentType.getUuid(context, "sourceUuid"),
                                                UuidArgumentType.getUuid(context, "targetUuid")
                                        )))))
                .then(CommandManager.argument("targetUuid", UuidArgumentType.uuid())
                        .executes(context -> swapSelfToTarget(context.getSource(), UuidArgumentType.getUuid(context, "targetUuid")))));
    }

    private static int openSelfSwapGui(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<PlayerInfo> players = PlayerDataManager.getAllPlayers(source.getServer());
        PlayerSelectionGui.open(player, players, Text.literal("选择要复制的数据来源"), selected -> {
            PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                    source.getServer(),
                    player.getUuid(),
                    selected.uuid(),
                    source.getName()
            );
            player.sendMessage(formatResult(result), false);
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int listPlayers(ServerCommandSource source) {
        List<PlayerInfo> players = PlayerDataManager.getAllPlayers(source.getServer());
        source.sendFeedback(() -> Text.literal("=== UUID Swap 玩家列表 ===").formatted(Formatting.GOLD), false);
        for (PlayerInfo info : players) {
            source.sendFeedback(() -> Text.literal((info.online() ? "[在线] " : "[离线] ") + info.name() + "  " + info.uuid())
                    .formatted(info.online() ? Formatting.GREEN : Formatting.YELLOW), false);
        }
        source.sendFeedback(() -> Text.literal("玩家自用: /uuidswap <targetUuid>；控制台/OP指定: /uuidswap apply <sourceUuid> <targetUuid>").formatted(Formatting.GRAY), false);
        return players.size();
    }

    private static int swapSelfToTarget(ServerCommandSource source, UUID targetUuid) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                source.getServer(),
                player.getUuid(),
                targetUuid,
                source.getName()
        );
        sendResult(source, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int applySwap(ServerCommandSource source, UUID sourceUuid, UUID targetUuid) {
        PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                source.getServer(),
                sourceUuid,
                targetUuid,
                source.getName()
        );
        sendResult(source, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private static void sendResult(ServerCommandSource source, PlayerDataManager.SwapResult result) {
        if (result.success()) {
            source.sendFeedback(() -> Text.literal(result.message()).formatted(Formatting.GREEN), true);
        } else {
            source.sendError(Text.literal(result.message()).formatted(Formatting.RED));
        }
    }

    private static Text formatResult(PlayerDataManager.SwapResult result) {
        return Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED);
    }
}
