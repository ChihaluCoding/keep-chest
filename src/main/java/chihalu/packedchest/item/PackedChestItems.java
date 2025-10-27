package chihalu.packedchest.item;

import java.util.EnumMap;
import java.util.Map;

import chihalu.packedchest.PackedChest;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class PackedChestItems {
        private static final Map<PackedContainerType, RegistryKey<Item>> PACKED_CONTAINER_KEYS = new EnumMap<>(
                        PackedContainerType.class);
        private static final Map<PackedContainerType, PackedChestItem> PACKED_CONTAINERS = new EnumMap<>(
                        PackedContainerType.class);

        static {
                for (PackedContainerType type : PackedContainerType.values()) {
                        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PackedChest.MOD_ID, type.itemId()));
                        PACKED_CONTAINER_KEYS.put(type, key);
                        Item.Settings settings = new Item.Settings().registryKey(key).maxCount(1);
                        PACKED_CONTAINERS.put(type, new PackedChestItem(type, settings));
                }
        }

        private PackedChestItems() {
        }

        public static void register() {
                PACKED_CONTAINERS.forEach((type, item) -> Registry.register(Registries.ITEM,
                                PACKED_CONTAINER_KEYS.get(type), item));

                ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                                .register(entries -> PACKED_CONTAINERS.values().forEach(entries::add));
        }

        public static ItemStack createPackedContainerStack(PackedContainerType type) {
                return new ItemStack(PACKED_CONTAINERS.get(type));
        }

        public static boolean isPackedContainer(ItemStack stack) {
                return PACKED_CONTAINERS.values().stream().anyMatch(stack::isOf);
        }

        public static PackedContainerType fromItem(Item item) {
                if (item instanceof PackedChestItem packed) {
                        return packed.getContainerType();
                }

                return null;
        }
}
