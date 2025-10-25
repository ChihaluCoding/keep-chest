package chihalu.keepchest.item;

import chihalu.keepchest.KeepChest;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class KeepChestItems {
        public static final PackedChestItem PACKED_CHEST = new PackedChestItem(new Item.Settings().maxCount(1));

        private KeepChestItems() {
        }

        public static void register() {
                Registry.register(Registries.ITEM, Identifier.of(KeepChest.MOD_ID, "packed_chest"), PACKED_CHEST);

                ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(PACKED_CHEST));
        }
}
