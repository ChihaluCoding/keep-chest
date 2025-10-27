package chihalu.packedchest.network;

import chihalu.packedchest.item.PackedChestItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles client/server synchronisation for preview inventory mutations.
 */
public final class PackedChestNetworking {
        private PackedChestNetworking() {
        }

        public static void init() {
                PayloadTypeRegistry.playC2S().register(ApplyPreviewPayload.ID, ApplyPreviewPayload.CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ApplyPreviewPayload.ID,
                                (payload, context) -> context.server()
                                                .execute(() -> applyPreviewOnServer(context.player(), payload,
                                                                context.server().getRegistryManager())));
        }

        private static void applyPreviewOnServer(ServerPlayerEntity player, ApplyPreviewPayload payload,
                        RegistryWrapper.WrapperLookup registryLookup) {
                int slotIndex = payload.slotIndex();
                if (slotIndex < 0) {
                        return;
                }

                PlayerInventory inventory = player.getInventory();
                if (slotIndex >= inventory.size()) {
                        return;
                }

                ItemStack stack = inventory.getStack(slotIndex);
                if (!(stack.getItem() instanceof PackedChestItem)) {
                        return;
                }

                if (PackedChestItem.updateStoredInventory(registryLookup, stack, payload.stacks())) {
                        inventory.markDirty();
                        player.currentScreenHandler.sendContentUpdates();
                }
        }

}
