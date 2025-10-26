package chihalu.keepchest;

import java.util.UUID;

import chihalu.keepchest.item.KeepChestItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

/**
 * Handles special drops when a player carrying a packed chest dies.
 */
public final class DeathChestManager {
        private DeathChestManager() {
        }

        public static boolean handleDeathDrops(ServerPlayerEntity player) {
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                if (world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                        return false;
                }

                PlayerInventory inventory = player.getInventory();
                boolean droppedPackedChest = false;
                Vec3d dropPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                UUID owner = player.getUuid();

                for (int slot = 0; slot < inventory.size(); slot++) {
                        ItemStack stack = inventory.getStack(slot);
                        if (stack.isEmpty() || !KeepChestItems.isPackedContainer(stack)) {
                                continue;
                        }

                        dropPackedChest(world, dropPos, owner, stack);
                        inventory.setStack(slot, ItemStack.EMPTY);
                        droppedPackedChest = true;
                }

                if (droppedPackedChest) {
                        inventory.markDirty();
                }

                // Allow vanilla handling for remaining drops.
                return false;
        }

        private static void dropPackedChest(ServerWorld world, Vec3d pos, UUID owner, ItemStack stack) {
                ItemStack toDrop = stack.copy();
                if (toDrop.isEmpty()) {
                        return;
                }

                ItemEntity entity = new ItemEntity(world, pos.x, pos.y, pos.z, toDrop);
                entity.setToDefaultPickupDelay();
                entity.setOwner(owner);
                world.spawnEntity(entity);
        }
}
