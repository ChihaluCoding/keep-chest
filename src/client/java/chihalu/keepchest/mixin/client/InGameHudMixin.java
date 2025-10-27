package chihalu.keepchest.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
        @Inject(method = "render", at = @At("TAIL"))
        private void keepChest$renderPlacementOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
                // Placement direction overlay disabled by request.
        }
}
