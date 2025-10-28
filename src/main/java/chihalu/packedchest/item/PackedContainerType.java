package chihalu.packedchest.item;

import net.minecraft.text.Text;

public enum PackedContainerType {
        CHEST("packed_chest"),
        LARGE_CHEST("packed_large_chest"),
        COPPER_CHEST("packed_copper_chest"),
        LARGE_COPPER_CHEST("packed_copper_large_chest"),
        EXPOSED_COPPER_CHEST("packed_exposed_copper_chest"),
        LARGE_EXPOSED_COPPER_CHEST("packed_exposed_copper_large_chest"),
        WEATHERED_COPPER_CHEST("packed_weathered_copper_chest"),
        LARGE_WEATHERED_COPPER_CHEST("packed_weathered_copper_large_chest"),
        OXIDIZED_COPPER_CHEST("packed_oxidized_copper_chest"),
        LARGE_OXIDIZED_COPPER_CHEST("packed_oxidized_copper_large_chest"),
        WAXED_COPPER_CHEST("packed_waxed_copper_chest"),
        LARGE_WAXED_COPPER_CHEST("packed_waxed_copper_large_chest"),
        WAXED_EXPOSED_COPPER_CHEST("packed_waxed_exposed_copper_chest"),
        LARGE_WAXED_EXPOSED_COPPER_CHEST("packed_waxed_exposed_copper_large_chest"),
        WAXED_WEATHERED_COPPER_CHEST("packed_waxed_weathered_copper_chest"),
        LARGE_WAXED_WEATHERED_COPPER_CHEST("packed_waxed_weathered_copper_large_chest"),
        WAXED_OXIDIZED_COPPER_CHEST("packed_waxed_oxidized_copper_chest"),
        LARGE_WAXED_OXIDIZED_COPPER_CHEST("packed_waxed_oxidized_copper_large_chest"),
        BARREL("packed_barrel");

        private final String itemId;

        PackedContainerType(String itemId) {
                this.itemId = itemId;
        }

        public String itemId() {
                return this.itemId;
        }

        public Text displayName() {
                return Text.translatable("item.packed-chest." + this.itemId);
        }
}
