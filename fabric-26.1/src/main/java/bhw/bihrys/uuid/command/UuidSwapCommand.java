package bhw.bihrys.uuid.command;

import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.gui.PlayerSelectionGui;
import bhw.bihrys.uuid.player.PlayerDataManager;
import bhw.bihrys.uuid.player.PlayerInfo;
import bhw.bihrys.uuid.util.PermissionUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class UuidSwapCommand {
    private UuidSwapCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("uuidswap")
                .requires(source -> PermissionUtil.has(source, UuidConfig.INSTANCE.playerSwapPermissionLevel))
                .executes(context -> openSelfSwapGui(context.getSource()))
                .then(Commands.literal("list")
                        .executes(context -> listPlayers(context.getSource())))
                .then(Commands.literal("apply")
                        .then(Commands.argument("sourceUuid", UuidArgument.uuid())
                                .then(Commands.argument("targetUuid", UuidArgument.uuid())
                                        .executes(context -> applySwap(
                                                context.getSource(),
                                                UuidArgument.getUuid(context, "sourceUuid"),
                                                UuidArgument.getUuid(context, "targetUuid")
                                        )))))
                .then(Commands.argument("targetUuid", UuidArgument.uuid())
                        .executes(context -> swapSelfToTarget(context.getSource(), UuidArgument.getUuid(context, "targetUuid")))));
    }

    private static int openSelfSwapGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<PlayerInfo> players = PlayerDataManager.getAllPlayers(source.getServer());
        PlayerSelectionGui.open(player, players, Component.literal("选择要复制的数据来源"), selected -> {
            PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                    source.getServer(),
                    player.getUUID(),
                    selected.uuid(),
                    source.getTextName()
            );
            player.sendSystemMessage(formatResult(result), false);
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int listPlayers(CommandSourceStack source) {
        List<PlayerInfo> players = PlayerDataManager.getAllPlayers(source.getServer());
        source.sendSuccess(() -> Component.literal("=== UUID Swap 玩家列表 ===").withStyle(ChatFormatting.GOLD), false);
        for (PlayerInfo info : players) {
            source.sendSuccess(() -> Component.literal((info.online() ? "[在线] " : "[离线] ") + info.name() + "  " + info.uuid())
                    .withStyle(info.online() ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        }
        source.sendSuccess(() -> Component.literal("玩家自用: /uuidswap <targetUuid>；控制台/OP指定: /uuidswap apply <sourceUuid> <targetUuid>").withStyle(ChatFormatting.GRAY), false);
        return players.size();
    }

    private static int swapSelfToTarget(CommandSourceStack source, UUID targetUuid) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                source.getServer(),
                player.getUUID(),
                targetUuid,
                source.getTextName()
        );
        sendResult(source, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int applySwap(CommandSourceStack source, UUID sourceUuid, UUID targetUuid) {
        PlayerDataManager.SwapResult result = PlayerDataManager.requestSwap(
                source.getServer(),
                sourceUuid,
                targetUuid,
                source.getTextName()
        );
        sendResult(source, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private static void sendResult(CommandSourceStack source, PlayerDataManager.SwapResult result) {
        if (result.success()) {
            source.sendSuccess(() -> Component.literal(result.message()).withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure(Component.literal(result.message()).withStyle(ChatFormatting.RED));
        }
    }

    private static Component formatResult(PlayerDataManager.SwapResult result) {
        return Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
