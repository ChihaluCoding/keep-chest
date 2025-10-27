package chihalu.keepchest.client.gui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
                        PackedChestPreviewScreenHandler handler, Text title) {
                super(handler, playerInventory, title);
                this.parent = parent;
                handler.setCursorStack(ItemStack.EMPTY);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                this.renderBackground(context, mouseX, mouseY, delta);
                super.render(context, mouseX, mouseY, delta);
                this.drawMouseoverTooltip(context, mouseX, mouseY);
        }

        @Override
        protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
                // Disable all slot interactions; this view is read-only.
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
