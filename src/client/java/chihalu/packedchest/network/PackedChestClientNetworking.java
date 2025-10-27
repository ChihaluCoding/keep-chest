package chihalu.packedchest.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Client helper for sending preview updates to the server.
 */
public final class PackedChestClientNetworking {
        private PackedChestClientNetworking() {
        }

        public static void sendPreviewUpdate(int slotIndex, DefaultedList<ItemStack> stacks) {
                DefaultedList<ItemStack> payloadStacks = DefaultedList.ofSize(stacks.size(), ItemStack.EMPTY);
                for (int i = 0; i < stacks.size(); i++) {
                        payloadStacks.set(i, stacks.get(i).copy());
                }
                ClientPlayNetworking.send(new ApplyPreviewPayload(slotIndex, payloadStacks));
        }
}

