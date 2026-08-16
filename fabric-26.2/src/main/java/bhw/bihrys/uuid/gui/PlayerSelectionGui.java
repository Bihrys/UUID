package bhw.bihrys.uuid.gui;

import bhw.bihrys.uuid.player.PlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
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

    public static void open(ServerPlayer player, List<PlayerInfo> players, Component title, Consumer<PlayerInfo> onSelect) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, ignored) -> new Handler(syncId, playerInventory, players, onSelect),
                title
        ));
    }

    private static final class Handler extends ChestMenu {
        private final SimpleContainer inventory;
        private final List<PlayerInfo> players;
        private final Consumer<PlayerInfo> onSelect;
        private int page;

        private Handler(int syncId, Inventory playerInventory, List<PlayerInfo> players, Consumer<PlayerInfo> onSelect) {
            this(syncId, playerInventory, new SimpleContainer(SIZE), players, onSelect);
        }

        private Handler(int syncId, Inventory playerInventory, SimpleContainer inventory, List<PlayerInfo> players, Consumer<PlayerInfo> onSelect) {
            super(MenuType.GENERIC_9x6, syncId, playerInventory, inventory, ROWS);
            this.inventory = inventory;
            this.players = List.copyOf(players);
            this.onSelect = onSelect;
            this.page = 0;
            fill();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput actionType, Player player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (slotId < 0 || slotId >= SIZE) {
                return;
            }

            if (slotId == PREVIOUS_SLOT) {
                if (page > 0) {
                    page--;
                    fill();
                    broadcastChanges();
                }
                return;
            }

            if (slotId == NEXT_SLOT) {
                if ((page + 1) * PLAYERS_PER_PAGE < players.size()) {
                    page++;
                    fill();
                    broadcastChanges();
                }
                return;
            }

            int index = page * PLAYERS_PER_PAGE + slotId;
            if (slotId < PLAYERS_PER_PAGE && index >= 0 && index < players.size()) {
                PlayerInfo selected = players.get(index);
                serverPlayer.closeContainer();
                onSelect.accept(selected);
            }
        }

        private void fill() {
            for (int i = 0; i < SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }

            int start = page * PLAYERS_PER_PAGE;
            int end = Math.min(start + PLAYERS_PER_PAGE, players.size());
            for (int i = start; i < end; i++) {
                inventory.setItem(i - start, playerHead(players.get(i)));
            }

            inventory.setItem(PREVIOUS_SLOT, page > 0 ? button(new ItemStack(Items.ARROW), "上一页", ChatFormatting.YELLOW) : filler());
            inventory.setItem(INFO_SLOT, infoItem());
            inventory.setItem(NEXT_SLOT, end < players.size() ? button(new ItemStack(Items.ARROW), "下一页", ChatFormatting.YELLOW) : filler());
        }

        private ItemStack playerHead(PlayerInfo info) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(info.uuid()));
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal(info.name()).withStyle(info.online() ? ChatFormatting.GREEN : ChatFormatting.GOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("UUID: " + info.uuid()).withStyle(ChatFormatting.GRAY));
            lore.add(Component.literal(info.online() ? "状态: 在线" : "状态: 离线/历史存档").withStyle(ChatFormatting.DARK_GRAY));
            lore.add(Component.literal("点击选择该玩家数据").withStyle(ChatFormatting.AQUA));
            stack.set(DataComponents.LORE, new ItemLore(lore));
            return stack;
        }

        private ItemStack infoItem() {
            ItemStack stack = new ItemStack(Items.BOOK);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("UUID Swap 玩家选择").withStyle(ChatFormatting.AQUA));
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("玩家数: " + players.size()).withStyle(ChatFormatting.GRAY),
                    Component.literal("页码: " + (page + 1) + "/" + Math.max(1, (int) Math.ceil(players.size() / (double) PLAYERS_PER_PAGE))).withStyle(ChatFormatting.GRAY),
                    Component.literal("头像=玩家皮肤；Lore 显示 UUID").withStyle(ChatFormatting.DARK_GRAY)
            )));
            return stack;
        }

        private ItemStack filler() {
            ItemStack stack = new ItemStack(Items.STAINED_GLASS_PANE.gray());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            return stack;
        }

        private ItemStack button(ItemStack stack, String name, ChatFormatting formatting) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(formatting));
            return stack;
        }
    }
}
