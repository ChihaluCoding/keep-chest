package chihalu.keepchest.item;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Item that stores the contents of a chest and can recreate it later.
 */
public class PackedChestItem extends Item {
        private static final String KEEP_CHEST_DATA_KEY = "KeepChestData";
        private static final String PRIMARY_STATE_KEY = "PrimaryState";
        private static final String SECONDARY_STATE_KEY = "SecondaryState";
        private static final String SECONDARY_BLOCK_ENTITY_KEY = "SecondaryBlockEntity";
        private static final String SECONDARY_BLOCK_ENTITY_TYPE_KEY = "SecondaryBlockEntityType";
        private static final String DOUBLE_KEY = "Double";

        public PackedChestItem(Settings settings) {
                super(settings);
        }

        public static Optional<PackResult> pack(ServerWorld world, BlockPos pos, BlockState state, ChestBlockEntity chest) {
                ItemStack stack = new ItemStack(KeepChestItems.PACKED_CHEST);

                TypedEntityData<BlockEntityType<?>> primaryData = TypedEntityData.create(
                                chest.getType(), chest.createNbt(world.getRegistryManager()));
                stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, primaryData);

                NbtCompound keepChestData = new NbtCompound();
                keepChestData.put(PRIMARY_STATE_KEY, NbtHelper.fromBlockState(state));

                BlockPos secondaryPos = null;
                if (state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                        secondaryPos = findConnectedChestPos(pos, state);
                        if (secondaryPos == null) {
                                return Optional.empty();
                        }

                        BlockState secondaryState = world.getBlockState(secondaryPos);
                        BlockEntity secondaryEntity = world.getBlockEntity(secondaryPos);
                        if (!(secondaryState.getBlock() instanceof ChestBlock)
                                        || !(secondaryEntity instanceof ChestBlockEntity secondaryChest)) {
                                return Optional.empty();
                        }

                        if (secondaryState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                                return Optional.empty();
                        }

                        keepChestData.put(SECONDARY_STATE_KEY, NbtHelper.fromBlockState(secondaryState));
                        keepChestData.put(SECONDARY_BLOCK_ENTITY_KEY, secondaryChest.createNbt(world.getRegistryManager()));

                        Identifier secondaryTypeId = Registries.BLOCK_ENTITY_TYPE.getId(secondaryChest.getType());
                        if (secondaryTypeId != null) {
                                keepChestData.putString(SECONDARY_BLOCK_ENTITY_TYPE_KEY, secondaryTypeId.toString());
                        }
                }

                keepChestData.putBoolean(DOUBLE_KEY, secondaryPos != null);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(keepChestData));

                return Optional.of(new PackResult(stack, secondaryPos));
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
                World world = context.getWorld();
                if (world.isClient()) {
                        return ActionResult.SUCCESS;
                }

                ItemStack stack = context.getStack();
                TypedEntityData<BlockEntityType<?>> primaryData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
                if (primaryData == null) {
                        return ActionResult.PASS;
                }

                NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                NbtCompound keepChestData = customData.copyNbt();
                if (keepChestData.isEmpty() || !keepChestData.contains(PRIMARY_STATE_KEY)) {
                        return ActionResult.PASS;
                }

                RegistryEntryLookup<Block> blockLookup = world.getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
                BlockState primaryState = NbtHelper.toBlockState(blockLookup, keepChestData.getCompoundOrEmpty(PRIMARY_STATE_KEY));

                ItemPlacementContext placementContext = new ItemPlacementContext(context);
                BlockPos primaryPos = placementContext.getBlockPos();

                if (!canPlace(world, primaryPos, placementContext, primaryState)) {
                        notifyPlacementFailure(context.getPlayer());
                        return ActionResult.FAIL;
                }

                boolean isDouble = keepChestData.getBoolean(DOUBLE_KEY, false);
                BlockPos secondaryPos = null;
                BlockState secondaryState = null;
                TypedEntityData<BlockEntityType<?>> secondaryData = null;

                if (isDouble) {
                        if (!keepChestData.contains(SECONDARY_STATE_KEY) || !keepChestData.contains(SECONDARY_BLOCK_ENTITY_KEY)) {
                                notifyPlacementFailure(context.getPlayer());
                                return ActionResult.FAIL;
                        }

                        secondaryPos = findConnectedChestPos(primaryPos, primaryState);
                        if (secondaryPos == null) {
                                notifyPlacementFailure(context.getPlayer());
                                return ActionResult.FAIL;
                        }

                        secondaryState = NbtHelper.toBlockState(blockLookup, keepChestData.getCompoundOrEmpty(SECONDARY_STATE_KEY));
                        if (!canPlace(world, secondaryPos, placementContext, secondaryState)) {
                                notifyPlacementFailure(context.getPlayer());
                                return ActionResult.FAIL;
                        }

                        NbtCompound secondaryEntityNbt = keepChestData.getCompoundOrEmpty(SECONDARY_BLOCK_ENTITY_KEY);
                        BlockEntityType<?> secondaryType = resolveSecondaryType(keepChestData, secondaryState);
                        secondaryData = TypedEntityData.create(secondaryType, secondaryEntityNbt);
                }

                if (!placeChest(world, primaryPos, primaryState, primaryData)) {
                        notifyPlacementFailure(context.getPlayer());
                        return ActionResult.FAIL;
                }

                if (isDouble && secondaryPos != null && secondaryState != null && secondaryData != null) {
                        placeChest(world, secondaryPos, secondaryState, secondaryData);
                }

                PlayerEntity player = context.getPlayer();
                if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.incrementStat(Stats.USED.getOrCreateStat(this));
                        serverPlayer.sendMessage(
                                        Text.translatable("message.keep-chest.unpacked", primaryPos.getX(), primaryPos.getY(),
                                                        primaryPos.getZ()),
                                        true);
                }

                stack.decrement(1);
                world.playSound(null, primaryPos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.8f, 0.95f);
                return ActionResult.SUCCESS;
        }

        private static void notifyPlacementFailure(@Nullable PlayerEntity player) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.sendMessage(Text.translatable("message.keep-chest.unpack_failed"), true);
                }
        }

        private static boolean placeChest(World world, BlockPos pos, BlockState state,
                        TypedEntityData<BlockEntityType<?>> entityData) {
                if (!world.setBlockState(pos, state, Block.NOTIFY_ALL)) {
                        return false;
                }

                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (!(blockEntity instanceof ChestBlockEntity chestBlockEntity)) {
                        return false;
                }

                if (!entityData.applyToBlockEntity(chestBlockEntity, world.getRegistryManager())) {
                        return false;
                }

                chestBlockEntity.markDirty();
                world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
                return true;
        }

        private static boolean canPlace(World world, BlockPos pos, ItemPlacementContext context, BlockState state) {
                BlockState existing = world.getBlockState(pos);
                if (!existing.canReplace(context) && !existing.isAir()) {
                        return false;
                }

                if (!state.canPlaceAt(world, pos)) {
                        return false;
                }

                PlayerEntity player = context.getPlayer();
                return player == null || player.canPlaceOn(pos, context.getSide(), context.getStack());
        }

        @Nullable
        private static BlockPos findConnectedChestPos(BlockPos pos, BlockState state) {
                if (!(state.getBlock() instanceof ChestBlock)) {
                        return null;
                }

                if (state.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                        return null;
                }

                Direction facing = state.get(ChestBlock.FACING);
                Direction offset = state.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT
                                ? facing.rotateYCounterclockwise()
                                : facing.rotateYClockwise();
                return pos.offset(offset);
        }

        public record PackResult(ItemStack stack, @Nullable BlockPos secondaryPos) {
        }

        private static BlockEntityType<?> resolveSecondaryType(NbtCompound keepChestData, BlockState secondaryState) {
                if (keepChestData.contains(SECONDARY_BLOCK_ENTITY_TYPE_KEY)) {
                        Optional<String> typeId = keepChestData.getString(SECONDARY_BLOCK_ENTITY_TYPE_KEY);
                        if (typeId.isPresent()) {
                                Identifier identifier = Identifier.tryParse(typeId.get());
                                if (identifier != null) {
                                        BlockEntityType<?> resolved = Registries.BLOCK_ENTITY_TYPE.get(identifier);
                                        if (resolved != null) {
                                                return resolved;
                                        }
                                }
                        }
                }

                return secondaryState.getBlock() instanceof TrappedChestBlock ? BlockEntityType.TRAPPED_CHEST
                                : BlockEntityType.CHEST;
        }
}
