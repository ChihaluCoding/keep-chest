package chihalu.keepchest.item;

import chihalu.keepchest.KeepChest;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class KeepChestItems {
	public static final RegistryKey<Item> PACKED_CHEST_KEY = RegistryKey.of(RegistryKeys.ITEM,
			Identifier.of(KeepChest.MOD_ID, "packed_chest"));
	public static final PackedChestItem PACKED_CHEST = new PackedChestItem(
			new Item.Settings().registryKey(PACKED_CHEST_KEY).maxCount(1));

	private KeepChestItems() {
	}

	public static void register() {
		Registry.register(Registries.ITEM, PACKED_CHEST_KEY, PACKED_CHEST);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(PACKED_CHEST));
	}
}
