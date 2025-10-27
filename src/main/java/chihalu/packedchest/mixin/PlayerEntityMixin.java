package chihalu.packedchest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.packedchest.DeathChestManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
        @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
        private void packedChest$dropInventory(CallbackInfo info) {
                if (!((Object) this instanceof ServerPlayerEntity serverPlayer)) {
                        return;
                }

                if (DeathChestManager.handleDeathDrops(serverPlayer)) {
                        info.cancel();
                }
        }
}
