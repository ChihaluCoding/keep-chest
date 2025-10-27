package chihalu.keepchest.item;

import net.minecraft.text.Text;

public enum PackedContainerType {
        CHEST("packed_chest"),
        LARGE_CHEST("packed_large_chest"),
        COPPER_CHEST("packed_copper_chest"),
        EXPOSED_COPPER_CHEST("packed_exposed_copper_chest"),
        WEATHERED_COPPER_CHEST("packed_weathered_copper_chest"),
        OXIDIZED_COPPER_CHEST("packed_oxidized_copper_chest"),
        WAXED_COPPER_CHEST("packed_waxed_copper_chest"),
        WAXED_EXPOSED_COPPER_CHEST("packed_waxed_exposed_copper_chest"),
        WAXED_WEATHERED_COPPER_CHEST("packed_waxed_weathered_copper_chest"),
        WAXED_OXIDIZED_COPPER_CHEST("packed_waxed_oxidized_copper_chest"),
        BARREL("packed_barrel");

        private final String itemId;

        PackedContainerType(String itemId) {
                this.itemId = itemId;
        }

        public String itemId() {
                return this.itemId;
        }

        public Text displayName() {
                return Text.translatable("item.keep-chest." + this.itemId);
        }
}
