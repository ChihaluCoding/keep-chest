package chihalu.packedchest.network;

import java.util.List;
import java.util.Objects;

import chihalu.packedchest.PackedChest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

/**
 * Custom payload for applying preview inventory mutations back to the held packed chest item.
 */
public record ApplyPreviewPayload(int slotIndex, DefaultedList<ItemStack> stacks) implements CustomPayload {
        public static final Id<ApplyPreviewPayload> ID = new Id<>(
                        Identifier.of(PackedChest.MOD_ID, "apply_preview"));

        public static final PacketCodec<RegistryByteBuf, ApplyPreviewPayload> CODEC = PacketCodec
                        .tuple(PacketCodecs.VAR_INT, ApplyPreviewPayload::slotIndex,
                                        ItemStack.OPTIONAL_LIST_PACKET_CODEC, ApplyPreviewPayload::stacks,
                                        ApplyPreviewPayload::of);

        public ApplyPreviewPayload {
                Objects.requireNonNull(stacks, "stacks");
        }

        private static ApplyPreviewPayload of(int slotIndex, List<ItemStack> stacks) {
                DefaultedList<ItemStack> copied = DefaultedList.ofSize(stacks.size(), ItemStack.EMPTY);
                for (int i = 0; i < stacks.size(); i++) {
                        copied.set(i, stacks.get(i));
                }
                return new ApplyPreviewPayload(slotIndex, copied);
        }

        @Override
        public Id<ApplyPreviewPayload> getId() {
                return ID;
        }
}
