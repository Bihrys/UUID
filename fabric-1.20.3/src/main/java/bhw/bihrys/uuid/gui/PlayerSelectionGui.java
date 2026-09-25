package bhw.bihrys.uuid.gui;

import bhw.bihrys.uuid.player.PlayerInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtHelper;

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

    public static void open(ServerPlayerEntity player, List<PlayerInfo> players, Text title, Consumer<PlayerInfo> onSelect) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> new Handler(syncId, playerInventory, players, onSelect),
                title
        ));
    }

    /**
     * Unknown offline players fall back to their 36-character UUID string as display name,
     * which is not a valid profile name. A null name keeps the head resolvable by UUID only.
     */
    private static GameProfile createProfile(PlayerInfo info) {
        String name = isValidPlayerName(info.name()) ? info.name() : null;
        return new GameProfile(info.uuid(), name);
    }

    private static boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c == '_' || (c < 128 && Character.isLetterOrDigit(c)))) {
                return false;
            }
        }
        return true;
    }

    private static void setLore(ItemStack stack, List<Text> lines) {
        NbtList lore = new NbtList();
        for (Text line : lines) {
            lore.add(NbtString.of(Text.Serialization.toJsonString(line)));
        }
        stack.getOrCreateSubNbt("display").put("Lore", lore);
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
            stack.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), createProfile(info)));
            stack.setCustomName(Text.literal(info.name()).formatted(info.online() ? Formatting.GREEN : Formatting.GOLD));
            setLore(stack, List.of(
                    Text.literal("UUID: " + info.uuid()).formatted(Formatting.GRAY),
                    Text.literal(info.online() ? "状态: 在线" : "状态: 离线/历史存档").formatted(Formatting.DARK_GRAY),
                    Text.literal("点击选择该玩家数据").formatted(Formatting.AQUA)
            ));
            return stack;
        }

        private ItemStack infoItem() {
            ItemStack stack = new ItemStack(Items.BOOK);
            stack.setCustomName(Text.literal("UUID Swap 玩家选择").formatted(Formatting.AQUA));
            setLore(stack, List.of(
                    Text.literal("玩家数: " + players.size()).formatted(Formatting.GRAY),
                    Text.literal("页码: " + (page + 1) + "/" + Math.max(1, (int) Math.ceil(players.size() / (double) PLAYERS_PER_PAGE))).formatted(Formatting.GRAY),
                    Text.literal("头像=玩家皮肤；Lore 显示 UUID").formatted(Formatting.DARK_GRAY)
            ));
            return stack;
        }

        private ItemStack filler() {
            ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            stack.setCustomName(Text.literal(" "));
            return stack;
        }

        private ItemStack button(ItemStack stack, String name, Formatting formatting) {
            stack.setCustomName(Text.literal(name).formatted(formatting));
            return stack;
        }
    }
}
