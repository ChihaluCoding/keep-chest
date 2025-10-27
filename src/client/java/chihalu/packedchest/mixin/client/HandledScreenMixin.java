package chihalu.packedchest.mixin.client;

import java.util.Optional;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chihalu.packedchest.client.gui.PackedChestPreviewScreen;
import chihalu.packedchest.client.gui.PackedChestPreviewScreenHandler;
import chihalu.packedchest.item.PackedChestItem;
import chihalu.packedchest.item.PackedChestItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

@Mixin(HandledScreen.class)
abstract class HandledScreenMixin {
        @Shadow
        protected Slot focusedSlot;

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void packedChest$openPackedPreview(Click click, boolean doubled,
                        CallbackInfoReturnable<Boolean> cir) {
                if (doubled || click.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.getWindow() == null) {
                        return;
                }

                boolean shiftDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                if (!shiftDown) {
                        return;
                }

                Slot slot = this.focusedSlot;
                if (slot == null || !slot.hasStack()) {
                        return;
                }

                ItemStack stack = slot.getStack();
                if (!PackedChestItems.isPackedContainer(stack)) {
                        return;
                }

                if (client.world == null) {
                        return;
                }

                Optional<PackedChestItem.InventoryPreview> preview = PackedChestItem
                                .getInventoryPreview(client.world.getRegistryManager(), stack);
                if (preview.isEmpty()) {
                        return;
                }

                if (client.player == null) {
                        return;
                }

                PackedChestItem.InventoryPreview inventoryPreview = preview.get();
                PackedChestPreviewScreenHandler handler = new PackedChestPreviewScreenHandler(
                                client.player.getInventory(), inventoryPreview);
                int playerSlot = slot.inventory instanceof PlayerInventory ? slot.getIndex() : -1;
                Screen parent = client.currentScreen;
                PackedChestPreviewScreen previewScreen = new PackedChestPreviewScreen(parent,
                                client.player.getInventory(), handler, inventoryPreview.title(), stack, playerSlot);
                client.setScreen(previewScreen);
                cir.setReturnValue(true);
                cir.cancel();
        }

        @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
        private void packedChest$renderPackedPreview(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
                // Hover previews are disabled; Shift inspections use the dedicated preview screen.
        }
}
