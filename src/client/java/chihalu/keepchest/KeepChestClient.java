package chihalu.keepchest;

import java.util.Optional;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import chihalu.keepchest.item.KeepChestItems;

public class KeepChestClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                // Client-specific setup is handled via dedicated mixins.
        }

        public static Optional<HeldPackedChest> findHeldPackedChest(ClientPlayerEntity player) {
                ItemStack main = player.getMainHandStack();
                if (main.isOf(KeepChestItems.PACKED_CHEST)) {
                        return Optional.of(new HeldPackedChest(main, Hand.MAIN_HAND));
                }

                ItemStack offHand = player.getOffHandStack();
                if (offHand.isOf(KeepChestItems.PACKED_CHEST)) {
                        return Optional.of(new HeldPackedChest(offHand, Hand.OFF_HAND));
                }

                return Optional.empty();
        }

        public record HeldPackedChest(ItemStack stack, Hand hand) {
        }
}
