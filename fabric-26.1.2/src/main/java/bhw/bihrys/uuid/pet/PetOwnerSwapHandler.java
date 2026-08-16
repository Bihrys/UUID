package bhw.bihrys.uuid.pet;

import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.gui.PlayerSelectionGui;
import bhw.bihrys.uuid.player.PlayerDataManager;
import bhw.bihrys.uuid.util.PermissionUtil;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Items;

import java.util.ArrayList;

public final class PetOwnerSwapHandler {
    private PetOwnerSwapHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (!UuidConfig.INSTANCE.enablePetOwnerSwap) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!serverPlayer.isShiftKeyDown() || !serverPlayer.getItemInHand(hand).is(Items.REDSTONE)) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof TamableAnimal tameable) || !tameable.isTame()) {
                return InteractionResult.PASS;
            }
            if (!PermissionUtil.has(serverPlayer, UuidConfig.INSTANCE.petOwnerSwapPermissionLevel)) {
                serverPlayer.sendSystemMessage(Component.literal("你没有权限替换宠物主人。").withStyle(ChatFormatting.RED), false);
                return InteractionResult.FAIL;
            }

            var candidates = new ArrayList<>(PlayerDataManager.getAllPlayers(serverWorld.getServer()));
            candidates.sort((left, right) -> {
                if (left.uuid().equals(serverPlayer.getUUID())) {
                    return -1;
                }
                if (right.uuid().equals(serverPlayer.getUUID())) {
                    return 1;
                }
                return 0;
            });

            PlayerSelectionGui.open(
                    serverPlayer,
                    candidates,
                    Component.literal("选择宠物新主人"),
                    selected -> {
                        EntityReference<LivingEntity> newOwner = EntityReference.of(selected.uuid());
                        tameable.setOwnerReference(newOwner);
                        serverPlayer.sendSystemMessage(Component.literal("已将该宠物主人替换为 " + selected.name() + " (" + selected.uuid() + ")")
                                .withStyle(ChatFormatting.GREEN), false);
                    }
            );
            return InteractionResult.SUCCESS_SERVER;
        });
    }
}
