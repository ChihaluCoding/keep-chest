package chihalu.keepchest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

import chihalu.keepchest.item.KeepChestItems;
import chihalu.keepchest.item.PackedChestItem;

/**
 * Places a chest containing a player's inventory when they die.
 */
public final class DeathChestManager {
        private static final int MAX_VERTICAL_SEARCH = 4;

        private DeathChestManager() {
        }

        public static boolean handleDeathDrops(ServerPlayerEntity player) {
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                if (world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                        return false;
                }

                CollectedDrops drops = collectInventory(player, world);
                if (drops.chestItems().isEmpty()) {
                        restoreInventory(player, drops.originalInventory());
                        return false;
                }

                BlockPos candidate = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
                Optional<BlockPos> placement = findPlacement(world, candidate);
                if (placement.isEmpty()) {
                        // No safe location for the chest, allow vanilla handling.
                        restoreInventory(player, drops.originalInventory());
                        return false;
                }

                BlockPos chestPos = placement.get();
                if (!placeChest(world, chestPos, player)) {
                        restoreInventory(player, drops.originalInventory());
                        return false;
                }

                ChestBlockEntity chest = getChest(world, chestPos);
                if (chest == null) {
                        restoreInventory(player, drops.originalInventory());
                        world.breakBlock(chestPos, false);
                        return false;
                }

                fillChest(chest, drops.chestItems(), world, chestPos, player);
                chest.markDirty();
                world.updateListeners(chestPos, chest.getCachedState(), chest.getCachedState(), Block.NOTIFY_ALL);
                dropAdditionalItems(world, chestPos, drops.regularDrops(), player.getUuid());
                dropPackedChestItems(world, chestPos, drops.packedChestCount(), player.getUuid());
                player.sendMessage(Text.translatable("text.keep-chest.death_chest_placed", chestPos.getX(), chestPos.getY(), chestPos.getZ()), false);
                return true;
        }

        private static CollectedDrops collectInventory(ServerPlayerEntity player, ServerWorld world) {
                PlayerInventory inventory = player.getInventory();
                List<ItemStack> original = new ArrayList<>(inventory.size());
                List<ItemStack> chestItems = new ArrayList<>();
                List<ItemStack> regularDrops = new ArrayList<>();
                int packedChestCount = 0;
                for (int slot = 0; slot < inventory.size(); slot++) {
                        ItemStack stack = inventory.getStack(slot);
                        if (!stack.isEmpty()) {
                                original.add(stack.copy());
                                if (stack.isOf(KeepChestItems.PACKED_CHEST)) {
                                        packedChestCount++;
                                        chestItems.addAll(PackedChestItem.getStoredItems(world.getRegistryManager(), stack));
                                } else {
                                        regularDrops.add(stack.copy());
                                }
                                inventory.setStack(slot, ItemStack.EMPTY);
                        }
                }

                inventory.markDirty();
                return new CollectedDrops(original, chestItems, regularDrops, packedChestCount);
        }

        private static void restoreInventory(ServerPlayerEntity player, List<ItemStack> drops) {
                PlayerInventory inventory = player.getInventory();
                int slot = 0;
                for (ItemStack stack : drops) {
                        while (slot < inventory.size() && !inventory.getStack(slot).isEmpty()) {
                                slot++;
                        }

                        if (slot >= inventory.size()) {
                                break;
                        }

                        inventory.setStack(slot, stack);
                        slot++;
                }

                inventory.markDirty();
        }

        private static Optional<BlockPos> findPlacement(ServerWorld world, BlockPos origin) {
                BlockPos.Mutable mutable = origin.mutableCopy();
                for (int offset = 0; offset <= MAX_VERTICAL_SEARCH; offset++) {
                        if (canReplace(world, mutable) && hasSupport(world, mutable)) {
                                return Optional.of(mutable.toImmutable());
                        }

                        mutable.move(Direction.UP);
                }

                return Optional.empty();
        }

        private static boolean canReplace(ServerWorld world, BlockPos pos) {
                BlockState state = world.getBlockState(pos);
                return state.isAir() || state.getCollisionShape(world, pos).isEmpty();
        }

        private static boolean hasSupport(ServerWorld world, BlockPos pos) {
                BlockPos below = pos.down();
                if (below.getY() < world.getBottomY()) {
                        return true;
                }

                BlockState support = world.getBlockState(below);
                return support.isSideSolidFullSquare(world, below, Direction.UP);
        }

        private static boolean placeChest(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
                BlockState state = Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, player.getHorizontalFacing().getOpposite());
                return world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }

        private static ChestBlockEntity getChest(ServerWorld world, BlockPos pos) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                return blockEntity instanceof ChestBlockEntity chest ? chest : null;
        }

        private static void fillChest(ChestBlockEntity chest, List<ItemStack> storedItems, ServerWorld world, BlockPos chestPos, ServerPlayerEntity owner) {
                for (ItemStack stack : storedItems) {
                        ItemStack remaining = stack.copy();
                        for (int slot = 0; slot < chest.size() && !remaining.isEmpty(); slot++) {
                                if (chest.getStack(slot).isEmpty()) {
                                        chest.setStack(slot, remaining.copy());
                                        remaining.setCount(0);
                                }
                        }

                        if (!remaining.isEmpty()) {
                                dropRemainder(world, chestPos, remaining, owner.getUuid());
                        }
                }
        }

        private static void dropRemainder(ServerWorld world, BlockPos chestPos, ItemStack stack, UUID owner) {
                Vec3d spawnPos = Vec3d.ofCenter(chestPos).add(0.0, 0.3, 0.0);
                ItemStack toDrop = stack.copy();
                if (toDrop.isEmpty()) {
                        return;
                }
                ItemEntity entity = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, toDrop);
                entity.setToDefaultPickupDelay();
                entity.setOwner(owner);
                world.spawnEntity(entity);
        }

        private static void dropAdditionalItems(ServerWorld world, BlockPos chestPos, List<ItemStack> stacks, UUID owner) {
                for (ItemStack stack : stacks) {
                        if (!stack.isEmpty()) {
                                dropRemainder(world, chestPos, stack, owner);
                        }
                }
        }

        private static void dropPackedChestItems(ServerWorld world, BlockPos chestPos, int count, UUID owner) {
                for (int i = 0; i < count; i++) {
                        dropRemainder(world, chestPos, new ItemStack(KeepChestItems.PACKED_CHEST), owner);
                }
        }

        private record CollectedDrops(List<ItemStack> originalInventory, List<ItemStack> chestItems, List<ItemStack> regularDrops,
                        int packedChestCount) {
        }
}
