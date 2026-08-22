package bhw.bihrys.uuid.command;

import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.player.PlayerDataManager;
import bhw.bihrys.uuid.util.PermissionUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class UuidSwapCommand {
  private UuidSwapCommand() {}
  public static void register(CommandDispatcher<CommandSourceStack> d) {
    d.register(Commands.literal("uuidswap").requires(s -> PermissionUtil.has(s, UuidConfig.INSTANCE.playerSwapPermissionLevel))
      .executes(c -> { c.getSource().sendSuccess(() -> Component.literal("用法: /uuidswap <目标UUID> 或 /uuidswap apply <源UUID> <目标UUID>"), false); return Command.SINGLE_SUCCESS; })
      .then(Commands.literal("list").executes(c -> { c.getSource().getServer().getPlayerList().getPlayers().forEach(p -> c.getSource().sendSuccess(() -> Component.literal(p.getName().getString()+" "+p.getUUID()), false)); return Command.SINGLE_SUCCESS; }))
      .then(Commands.literal("apply").then(Commands.argument("sourceUuid", UuidArgument.uuid()).then(Commands.argument("targetUuid", UuidArgument.uuid()).executes(c -> apply(c.getSource(), UuidArgument.getUuid(c,"sourceUuid"), UuidArgument.getUuid(c,"targetUuid"))))) )
      .then(Commands.argument("targetUuid", UuidArgument.uuid()).executes(c -> { ServerPlayer p=c.getSource().getPlayerOrException(); return apply(c.getSource(), p.getUUID(), UuidArgument.getUuid(c,"targetUuid")); })));
  }
  private static int apply(CommandSourceStack s, UUID source, UUID target) {
    var r=PlayerDataManager.requestSwap(s.getServer(),source,target,s.getTextName());
    if(r.success()) s.sendSuccess(() -> Component.literal(r.message()).withStyle(ChatFormatting.GREEN), true); else s.sendFailure(Component.literal(r.message()));
    return r.success()?1:0;
  }
}
