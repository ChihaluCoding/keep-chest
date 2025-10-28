package chihalu.packedchest.client.gui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Generic container screen configured for a read-only snapshot of a packed chest.
 */
public class PackedChestPreviewScreen extends GenericContainerScreen {
        private final Screen parent;

        public PackedChestPreviewScreen(@Nullable Screen parent, PlayerInventory playerInventory,
                        PackedChestPreviewScreenHandler handler, Text title, ItemStack previewStack, int playerSlot) {
                super(handler, playerInventory, title);
                this.parent = parent;
                handler.setCursorStack(ItemStack.EMPTY);
        }

        @Override
        protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
                // Preview is read-only; ignore item interaction.
        }

        @Override
        protected boolean handleHotbarKeyPressed(KeyInput input) {
                return false;
        }

        @Override
        public void close() {
                MinecraftClient client = MinecraftClient.getInstance();
                if (this.parent != null) {
                        client.setScreen(this.parent);
                } else {
                        client.setScreen(null);
                }
        }
}
