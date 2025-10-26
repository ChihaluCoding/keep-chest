package chihalu.keepchest.item;

import net.minecraft.text.Text;

public enum PackedContainerType {
        CHEST("packed_chest"),
        LARGE_CHEST("packed_large_chest"),
        COPPER_CHEST("packed_copper_chest"),
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
