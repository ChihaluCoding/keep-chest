package chihalu.packedchest.client.gui;

import chihalu.packedchest.config.PackedChestClientConfig;
import chihalu.packedchest.item.PackedChestItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;

/**
 * Client-only screen handler that exposes packed chest contents as an immutable inventory.
 */
public class PackedChestPreviewScreenHandler extends GenericContainerScreenHandler {
        private final int previewSlotCount;
        private boolean previewDirty;

        private PackedChestPreviewScreenHandler(PlayerInventory playerInventory, PackedChestItem.InventoryPreview preview,
                        PreviewInventory inventory) {
                super(selectType(preview.rows()), 0, playerInventory, inventory, preview.rows());
                this.previewSlotCount = preview.slots().size();
                inventory.setDirtyListener(() -> this.previewDirty = true);
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

        public void simulateClientClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
                if (!PackedChestClientConfig.get().allowPreviewItemMovement()) {
                        return;
                }
                if (slotIndex == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
                        if (actionType == SlotActionType.THROW || actionType == SlotActionType.PICKUP) {
                                setCursorStack(ItemStack.EMPTY);
                                sendContentUpdates();
                        }
                        return;
                }
                if (!isPreviewSlot(slotIndex)) {
                        return;
                }
                Slot slot = this.slots.get(slotIndex);
                switch (actionType) {
                        case PICKUP -> handlePickup(slot, button);
                        case PICKUP_ALL -> handlePickupAll(slot);
                        default -> {
                        }
                }
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
                return ItemStack.EMPTY;
        }

        public boolean isPreviewDirty() {
                return this.previewDirty;
        }

        public void resetPreviewDirty() {
                this.previewDirty = false;
        }

        public DefaultedList<ItemStack> copyPreviewStacks() {
                DefaultedList<ItemStack> snapshot = DefaultedList.ofSize(this.previewSlotCount, ItemStack.EMPTY);
                for (int i = 0; i < this.previewSlotCount; i++) {
                        ItemStack stack = this.slots.get(i).getStack();
                        snapshot.set(i, stack.copy());
                }
                return snapshot;
        }

        private void handlePickup(Slot slot, int button) {
                ItemStack cursor = this.getCursorStack();
                ItemStack slotStack = slot.getStack();

                if (button == 0) {
                        // Left click
                        if (cursor.isEmpty()) {
                                if (!slotStack.isEmpty()) {
                                        setCursorStack(slotStack.copy());
                                        slot.setStack(ItemStack.EMPTY);
                                }
                        } else if (slotStack.isEmpty()) {
                                slot.setStack(cursor.copy());
                                cursor.setCount(0);
                                setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                        } else if (canMerge(slotStack, cursor)) {
                                int space = slot.getMaxItemCount(cursor) - slotStack.getCount();
                                if (space > 0) {
                                        int transfer = Math.min(cursor.getCount(), space);
                                        slotStack.increment(transfer);
                                        cursor.decrement(transfer);
                                        slot.setStack(slotStack);
                                        setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                                }
                        } else {
                                ItemStack originalSlot = slotStack.copy();
                                slot.setStack(cursor.copy());
                                setCursorStack(originalSlot);
                        }
                } else if (button == 1) {
                        // Right click
                        if (cursor.isEmpty()) {
                                if (!slotStack.isEmpty()) {
                                        int take = MathHelper.ceil(slotStack.getCount() / 2.0F);
                                        ItemStack taken = slotStack.copy();
                                        taken.setCount(take);
                                        setCursorStack(taken);
                                        slotStack.decrement(take);
                                        slot.setStack(slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
                                }
                        } else if (slotStack.isEmpty()) {
                                ItemStack placed = cursor.copy();
                                placed.setCount(1);
                                slot.setStack(placed);
                                cursor.decrement(1);
                                setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                        } else if (canMerge(slotStack, cursor)
                                        && slotStack.getCount() < slot.getMaxItemCount(cursor)) {
                                slotStack.increment(1);
                                slot.setStack(slotStack);
                                cursor.decrement(1);
                                setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                        }
                }

                slot.markDirty();
                sendContentUpdates();
        }

        private void handlePickupAll(Slot origin) {
                ItemStack cursor = this.getCursorStack();
                if (cursor.isEmpty()) {
                        return;
                }

                int maxTotal = Math.min(origin.getMaxItemCount(cursor), cursor.getMaxCount());
                int remaining = maxTotal - cursor.getCount();
                if (remaining <= 0) {
                        return;
                }

                for (int i = 0; i < this.previewSlotCount && remaining > 0; i++) {
                        Slot slot = this.slots.get(i);
                        if (slot == origin) {
                                continue;
                        }

                        ItemStack stack = slot.getStack();
                        if (stack.isEmpty() || !canMerge(stack, cursor)) {
                                continue;
                        }

                        int move = Math.min(stack.getCount(), remaining);
                        if (move <= 0) {
                                continue;
                        }

                        cursor.increment(move);
                        stack.decrement(move);
                        slot.setStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
                        slot.markDirty();
                        remaining -= move;
                }

                setCursorStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                origin.markDirty();
                sendContentUpdates();
        }

        private static boolean canMerge(ItemStack first, ItemStack second) {
                return !first.isEmpty() && !second.isEmpty()
                                && ItemStack.areItemsAndComponentsEqual(first, second);
        }

        private static final class PreviewInventory extends SimpleInventory {
                private Runnable dirtyListener = () -> {};

                PreviewInventory(DefaultedList<ItemStack> stacks) {
                        super(stacks.size());
                        for (int i = 0; i < stacks.size(); i++) {
                                super.setStack(i, stacks.get(i).copy());
                        }
                }

                void setDirtyListener(Runnable listener) {
                        this.dirtyListener = listener == null ? () -> {} : listener;
                }

                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                        return true;
                }

                @Override
                public void markDirty() {
                        if (PackedChestClientConfig.get().allowPreviewItemMovement()) {
                                this.dirtyListener.run();
                                super.markDirty();
                        }
                }

                @Override
                public ItemStack removeStack(int slot, int amount) {
                        if (!PackedChestClientConfig.get().allowPreviewItemMovement()) {
                                return ItemStack.EMPTY;
                        }
                        return super.removeStack(slot, amount);
                }

                @Override
                public ItemStack removeStack(int slot) {
                        if (!PackedChestClientConfig.get().allowPreviewItemMovement()) {
                                return ItemStack.EMPTY;
                        }
                        return super.removeStack(slot);
                }

                @Override
                public void setStack(int slot, ItemStack stack) {
                        if (!PackedChestClientConfig.get().allowPreviewItemMovement()) {
                                return;
                        }
                        super.setStack(slot, stack);
                }
        }
}
