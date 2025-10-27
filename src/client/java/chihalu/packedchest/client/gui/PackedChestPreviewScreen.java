package chihalu.packedchest.client.gui;

import org.jetbrains.annotations.Nullable;

import chihalu.packedchest.config.PackedChestClientConfig;
import chihalu.packedchest.item.PackedChestItem;
import chihalu.packedchest.network.PackedChestClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

/**
 * Generic container screen configured for a read-only snapshot of a packed chest.
 */
public class PackedChestPreviewScreen extends GenericContainerScreen {
        private final Screen parent;
        private final PackedChestPreviewScreenHandler previewHandler;
        private final ItemStack previewStack;
        private final int playerSlot;

        public PackedChestPreviewScreen(@Nullable Screen parent, PlayerInventory playerInventory,
                        PackedChestPreviewScreenHandler handler, Text title, ItemStack previewStack, int playerSlot) {
                super(handler, playerInventory, title);
                this.parent = parent;
                this.previewHandler = handler;
                this.previewStack = previewStack;
                this.playerSlot = playerSlot;
                handler.setCursorStack(ItemStack.EMPTY);
        }

        @Override
        protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
                if (!PackedChestClientConfig.get().allowPreviewItemMovement()) {
                        return;
                }
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) {
                        return;
                }

                int actualSlotId = slot != null ? slot.id : slotId;
                if (slot != null && !this.previewHandler.isPreviewSlot(actualSlotId)) {
                        return;
                }

                this.previewHandler.simulateClientClick(actualSlotId, button, actionType, client.player);
        }

        @Override
        protected boolean handleHotbarKeyPressed(KeyInput input) {
                return false;
        }

        @Override
        public void close() {
                if (PackedChestClientConfig.get().allowPreviewItemMovement()) {
                        applyPreviewChanges();
                }
                MinecraftClient client = MinecraftClient.getInstance();
                if (this.parent != null) {
                        client.setScreen(this.parent);
                } else {
                        client.setScreen(null);
                }
        }

        private void applyPreviewChanges() {
                if (!this.previewHandler.isPreviewDirty()) {
                        return;
                }

                if (this.playerSlot < 0 || !(this.previewStack.getItem() instanceof PackedChestItem)) {
                        this.previewHandler.resetPreviewDirty();
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null || client.player == null) {
                        this.previewHandler.resetPreviewDirty();
                        return;
                }

                DefaultedList<ItemStack> snapshot = this.previewHandler.copyPreviewStacks();
                if (PackedChestItem.updateStoredInventory(client.world.getRegistryManager(), this.previewStack, snapshot)) {
                        PackedChestClientNetworking.sendPreviewUpdate(this.playerSlot, snapshot);
                }
                this.previewHandler.resetPreviewDirty();
        }
}
