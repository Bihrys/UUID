package bhw.bihrys.uuid.gui;

import bhw.bihrys.uuid.player.PlayerInfo;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class PlayerSelectionGui {
    private static final int ROWS = 6;
    private static final int SIZE = 54;
    private static final int PLAYERS_PER_PAGE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private PlayerSelectionGui() {
    }

    public static void open(ServerPlayerEntity player, List<PlayerInfo> players, Text title, Consumer<PlayerInfo> onSelect) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new Handler(syncId, playerInventory, players, onSelect),
                title
        ));
    }

    private static final class Handler extends GenericContainerScreenHandler {
        private final SimpleInventory inventory;
        private final List<PlayerInfo> players;
        private final Consumer<PlayerInfo> onSelect;
        private int page;

        private Handler(int syncId, PlayerInventory playerInventory, List<PlayerInfo> players, Consumer<PlayerInfo> onSelect) {
            this(syncId, playerInventory, new SimpleInventory(SIZE), players, onSelect);
        }

        private Handler(int syncId, PlayerInventory playerInventory, SimpleInventory inventory, List<PlayerInfo> players, Consumer<PlayerInfo> onSelect) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, ROWS);
            this.inventory = inventory;
            this.players = List.copyOf(players);
            this.onSelect = onSelect;
            this.page = 0;
            fill();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player) {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            if (slotId < 0 || slotId >= SIZE) {
                return;
            }

            if (slotId == PREVIOUS_SLOT) {
                if (page > 0) {
                    page--;
                    fill();
                    sendContentUpdates();
                }
                return;
            }

            if (slotId == NEXT_SLOT) {
                if ((page + 1) * PLAYERS_PER_PAGE < players.size()) {
                    page++;
                    fill();
                    sendContentUpdates();
                }
                return;
            }

            int index = page * PLAYERS_PER_PAGE + slotId;
            if (slotId < PLAYERS_PER_PAGE && index >= 0 && index < players.size()) {
                PlayerInfo selected = players.get(index);
                serverPlayer.closeHandledScreen();
                onSelect.accept(selected);
            }
        }

        private void fill() {
            for (int i = 0; i < SIZE; i++) {
                inventory.setStack(i, ItemStack.EMPTY);
            }

            int start = page * PLAYERS_PER_PAGE;
            int end = Math.min(start + PLAYERS_PER_PAGE, players.size());
            for (int i = start; i < end; i++) {
                inventory.setStack(i - start, playerHead(players.get(i)));
            }

            inventory.setStack(PREVIOUS_SLOT, page > 0 ? button(new ItemStack(Items.ARROW), "上一页", Formatting.YELLOW) : filler());
            inventory.setStack(INFO_SLOT, infoItem());
            inventory.setStack(NEXT_SLOT, end < players.size() ? button(new ItemStack(Items.ARROW), "下一页", Formatting.YELLOW) : filler());
        }

        private ItemStack playerHead(PlayerInfo info) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponentTypes.PROFILE, new ProfileComponent(Optional.empty(), Optional.of(info.uuid()), new PropertyMap()));
            stack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(info.name()).formatted(info.online() ? Formatting.GREEN : Formatting.GOLD));

            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("UUID: " + info.uuid()).formatted(Formatting.GRAY));
            lore.add(Text.literal(info.online() ? "状态: 在线" : "状态: 离线/历史存档").formatted(Formatting.DARK_GRAY));
            lore.add(Text.literal("点击选择该玩家数据").formatted(Formatting.AQUA));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
            return stack;
        }

        private ItemStack infoItem() {
            ItemStack stack = new ItemStack(Items.BOOK);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("UUID Swap 玩家选择").formatted(Formatting.AQUA));
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("玩家数: " + players.size()).formatted(Formatting.GRAY),
                    Text.literal("页码: " + (page + 1) + "/" + Math.max(1, (int) Math.ceil(players.size() / (double) PLAYERS_PER_PAGE))).formatted(Formatting.GRAY),
                    Text.literal("头像=玩家皮肤；Lore 显示 UUID").formatted(Formatting.DARK_GRAY)
            )));
            return stack;
        }

        private ItemStack filler() {
            ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            return stack;
        }

        private ItemStack button(ItemStack stack, String name, Formatting formatting) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(formatting));
            return stack;
        }
    }
}
