package bhw.bihrys.uuid.pet;

import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.gui.PlayerSelectionGui;
import bhw.bihrys.uuid.player.PlayerDataManager;
import bhw.bihrys.uuid.util.PermissionUtil;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;

public final class PetOwnerSwapHandler {
    private PetOwnerSwapHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!UuidConfig.INSTANCE.enablePetOwnerSwap) {
                return ActionResult.PASS;
            }
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!serverPlayer.isSneaking() || !serverPlayer.getStackInHand(hand).isOf(Items.REDSTONE)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof TameableEntity tameable) || !tameable.isTamed()) {
                return ActionResult.PASS;
            }
            if (!PermissionUtil.has(serverPlayer, UuidConfig.INSTANCE.petOwnerSwapPermissionLevel)) {
                serverPlayer.sendMessage(Text.literal("你没有权限替换宠物主人。").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }

            var candidates = new ArrayList<>(PlayerDataManager.getAllPlayers(serverWorld.getServer()));
            candidates.sort((left, right) -> {
                if (left.uuid().equals(serverPlayer.getUuid())) {
                    return -1;
                }
                if (right.uuid().equals(serverPlayer.getUuid())) {
                    return 1;
                }
                return 0;
            });

            PlayerSelectionGui.open(
                    serverPlayer,
                    candidates,
                    Text.literal("选择宠物新主人"),
                    selected -> {
                        tameable.setOwnerUuid(selected.uuid());
                        serverPlayer.sendMessage(Text.literal("已将该宠物主人替换为 " + selected.name() + " (" + selected.uuid() + ")")
                                .formatted(Formatting.GREEN), false);
                    }
            );
            return ActionResult.SUCCESS;
        });
    }
}
