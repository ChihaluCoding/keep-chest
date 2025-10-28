package chihalu.packedchest.client.gui;

import chihalu.packedchest.item.PackedChestItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.collection.DefaultedList;

/**
 * Client-only screen handler that exposes packed chest contents as an immutable inventory.
 */
public class PackedChestPreviewScreenHandler extends GenericContainerScreenHandler {
        private final int previewSlotCount;

        private PackedChestPreviewScreenHandler(PlayerInventory playerInventory, PackedChestItem.InventoryPreview preview,
                        PreviewInventory inventory) {
                super(selectType(preview.rows()), 0, playerInventory, inventory, preview.rows());
                this.previewSlotCount = preview.slots().size();
        }

        public PackedChestPreviewScreenHandler(PlayerInventory playerInventory, PackedChestItem.InventoryPreview preview) {
                this(playerInventory, preview, new PreviewInventory(preview.slots()));
        }

        private static ScreenHandlerType<? extends GenericContainerScreenHandler> selectType(int rows) {
                return switch (rows) {
                        case 1 -> ScreenHandlerType.GENERIC_9X1;
                        case 2 -> ScreenHandlerType.GENERIC_9X2;
                        case 3 -> ScreenHandlerType.GENERIC_9X3;
                        case 4 -> ScreenHandlerType.GENERIC_9X4;
                        case 5 -> ScreenHandlerType.GENERIC_9X5;
                        default -> ScreenHandlerType.GENERIC_9X6;
                };
        }

        @Override
        public boolean canUse(PlayerEntity player) {
                return true;
        }

        public boolean isPreviewSlot(int slotIndex) {
                return slotIndex >= 0 && slotIndex < this.previewSlotCount;
        }

        public void simulateClientClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType,
                        PlayerEntity player) {
                // Preview inventory is immutable; ignore interaction attempts.
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
                return ItemStack.EMPTY;
        }

        public DefaultedList<ItemStack> copyPreviewStacks() {
                DefaultedList<ItemStack> snapshot = DefaultedList.ofSize(this.previewSlotCount, ItemStack.EMPTY);
                for (int i = 0; i < this.previewSlotCount; i++) {
                        ItemStack stack = this.slots.get(i).getStack();
                        snapshot.set(i, stack.copy());
                }
                return snapshot;
        }

        private static final class PreviewInventory extends SimpleInventory {
                PreviewInventory(DefaultedList<ItemStack> stacks) {
                        super(stacks.size());
                        for (int i = 0; i < stacks.size(); i++) {
                                super.setStack(i, stacks.get(i).copy());
                        }
                }

                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                        return true;
                }

                @Override
                public void markDirty() {
                        // Immutable preview; ignore dirty markers.
                }

                @Override
                public ItemStack removeStack(int slot, int amount) {
                        return ItemStack.EMPTY;
                }

                @Override
                public ItemStack removeStack(int slot) {
                        return ItemStack.EMPTY;
                }

                @Override
                public void setStack(int slot, ItemStack stack) {
                        // Prevent mutation.
                }
        }
}
