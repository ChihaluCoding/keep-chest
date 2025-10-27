package chihalu.packedchest;

import java.util.Optional;

import chihalu.packedchest.config.PackedChestClientConfig;
import chihalu.packedchest.item.PackedChestItem;
import chihalu.packedchest.item.PackedChestItems;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;

public class PackedChestClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                PackedChestClientConfig.get();
                // Client-specific setup is handled via dedicated mixins.
        }

        public static Optional<HeldPackedChest> findHeldPackedChest(ClientPlayerEntity player) {
                ItemStack main = player.getMainHandStack();
                if (PackedChestItems.isPackedContainer(main)) {
                        return Optional.of(new HeldPackedChest(main, Hand.MAIN_HAND));
                }

                ItemStack offHand = player.getOffHandStack();
                if (PackedChestItems.isPackedContainer(offHand)) {
                        return Optional.of(new HeldPackedChest(offHand, Hand.OFF_HAND));
                }

                return Optional.empty();
        }

        public record HeldPackedChest(ItemStack stack, Hand hand) {
        }

        public static Optional<PackedChestItem.PlacementPreview> findPlacementPreview(MinecraftClient client) {
                ClientPlayerEntity player = client.player;
                if (player == null || client.world == null) {
                        return Optional.empty();
                }

                HitResult hitResult = client.crosshairTarget;
                if (!(hitResult instanceof BlockHitResult blockHit)) {
                        return Optional.empty();
                }

                Optional<HeldPackedChest> held = findHeldPackedChest(player);
                if (held.isEmpty()) {
                        return Optional.empty();
                }

                HeldPackedChest handStack = held.get();
                ItemPlacementContext placementContext = new ItemPlacementContext(player, handStack.hand(),
                                handStack.stack(), blockHit);

                return PackedChestItem.getPlacementPreview(client.world, placementContext, handStack.stack());
        }

        public static Text directionText(Direction direction) {
                if (direction == null) {
                        return Text.translatable("hud.packed_chest.preview.unknown");
                }
                return Text.translatable("hud.packed_chest.direction." + direction.asString());
        }
}
