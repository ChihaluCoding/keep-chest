package chihalu.keepchest.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CopperChestBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.inventory.Inventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Item that stores the contents of a chest and can recreate it later.
 */
public class PackedChestItem extends BlockItem {
        private static final String PRIMARY_STATE_KEY = "PrimaryState";
        private static final String SECONDARY_STATE_KEY = "SecondaryState";
        private static final String SECONDARY_BLOCK_ENTITY_KEY = "SecondaryBlockEntity";
        private static final String SECONDARY_BLOCK_ENTITY_TYPE_KEY = "SecondaryBlockEntityType";
        private static final String DOUBLE_KEY = "Double";

        private final PackedContainerType type;

        public PackedChestItem(PackedContainerType type, Block block, Settings settings) {
                super(block, settings);
                this.type = type;
        }

        public PackedContainerType getContainerType() {
                return this.type;
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
                return true;
        }

        public static Optional<PackResult> pack(ServerWorld world, BlockPos pos, BlockState state,
                        BlockEntity blockEntity) {
                if (blockEntity == null) {
                        return Optional.empty();
                }

                TypedEntityData<BlockEntityType<?>> primaryData = TypedEntityData.create(blockEntity.getType(),
                                blockEntity.createNbt(world.getRegistryManager()));

                NbtCompound keepChestData = new NbtCompound();
                keepChestData.put(PRIMARY_STATE_KEY, NbtHelper.fromBlockState(state));

                BlockPos secondaryPos = null;
                if (state.getBlock() instanceof ChestBlock && state.contains(ChestBlock.CHEST_TYPE)
                                && blockEntity instanceof ChestBlockEntity) {
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

                                if (!secondaryState.contains(ChestBlock.CHEST_TYPE)
                                                || secondaryState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                                        return Optional.empty();
                                }

                                keepChestData.put(SECONDARY_STATE_KEY, NbtHelper.fromBlockState(secondaryState));
                                keepChestData.put(SECONDARY_BLOCK_ENTITY_KEY,
                                                secondaryChest.createNbt(world.getRegistryManager()));

                                Identifier secondaryTypeId = Registries.BLOCK_ENTITY_TYPE.getId(secondaryChest.getType());
                                if (secondaryTypeId != null) {
                                        keepChestData.putString(SECONDARY_BLOCK_ENTITY_TYPE_KEY, secondaryTypeId.toString());
                                }
                        }
                }

                boolean isDouble = secondaryPos != null;
                keepChestData.putBoolean(DOUBLE_KEY, isDouble);

                PackedContainerType containerType = determineContainerType(state, isDouble);
                ItemStack stack = KeepChestItems.createPackedContainerStack(containerType);
                stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, primaryData);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(keepChestData));

                return Optional.of(new PackResult(stack, containerType, secondaryPos));
        }

        private static PackedContainerType determineContainerType(BlockState state, boolean isDouble) {
                Block block = state.getBlock();

                if (block instanceof BarrelBlock) {
                        return PackedContainerType.BARREL;
                }

                if (block instanceof CopperChestBlock) {
                        return PackedContainerType.COPPER_CHEST;
                }

                return isDouble ? PackedContainerType.LARGE_CHEST : PackedContainerType.CHEST;
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

                ItemPlacementContext placementContext = new ItemPlacementContext(context);
                Optional<PlacementPlan> placement = preparePlacement(world, placementContext, primaryData, keepChestData);
                if (placement.isEmpty()) {
                        notifyPlacementFailure(context.getPlayer());
                        return ActionResult.FAIL;
                }

                PlacementPlan plan = placement.get();
                if (!placeContainer(world, plan.primaryPos(), plan.primaryState(), plan.primaryData())) {
                        notifyPlacementFailure(context.getPlayer());
                        return ActionResult.FAIL;
                }

                if (plan.isDouble()) {
                        placeContainer(world, plan.secondaryPos(), plan.secondaryState(), plan.secondaryData());
                }

                PlayerEntity player = context.getPlayer();
                if (player instanceof ServerPlayerEntity serverPlayer) {
                        PackedContainerType containerType = determineContainerType(plan.primaryState(), plan.isDouble());
                        serverPlayer.incrementStat(Stats.USED.getOrCreateStat(this));
                        serverPlayer.sendMessage(Text.translatable("message.keep-chest.unpacked",
                                        containerType.displayName(), plan.primaryPos().getX(), plan.primaryPos().getY(),
                                        plan.primaryPos().getZ()), true);
                }

                stack.decrement(1);
                world.playSound(null, plan.primaryPos(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.8f, 0.95f);
                return ActionResult.SUCCESS;
        }

        private static void notifyPlacementFailure(@Nullable PlayerEntity player) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.sendMessage(Text.translatable("message.keep-chest.unpack_failed"), true);
                }
        }

        private static Optional<PlacementPlan> preparePlacement(World world, ItemPlacementContext placementContext,
                        TypedEntityData<BlockEntityType<?>> primaryData, NbtCompound keepChestData) {
                RegistryEntryLookup<Block> blockLookup = world.getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
                BlockState primaryState = NbtHelper.toBlockState(blockLookup,
                                keepChestData.getCompoundOrEmpty(PRIMARY_STATE_KEY));

                Direction desiredFacing = determineFacing(placementContext, primaryState);
                primaryState = applyFacing(primaryState, desiredFacing);

                BlockPos requestedPos = placementContext.getBlockPos();
                BlockPos primaryPos = requestedPos;
                ItemPlacementContext primaryPlacementContext = placementContext;
                boolean defaultPlacementValid = canPlace(world, primaryPlacementContext, primaryPos, primaryState);

                boolean isDouble = keepChestData.getBoolean(DOUBLE_KEY).orElse(false);
                BlockPos secondaryPos = null;
                BlockState secondaryState = null;
                TypedEntityData<BlockEntityType<?>> secondaryData = null;

                if (isDouble) {
                        if (!keepChestData.contains(SECONDARY_STATE_KEY) || !keepChestData.contains(SECONDARY_BLOCK_ENTITY_KEY)) {
                                return Optional.empty();
                        }

                        BlockPos offsetVector = findConnectedChestPos(BlockPos.ORIGIN, primaryState);
                        boolean shouldShift = placementContext.getPlayer() != null && placementContext.getPlayer().isSneaking();
                        ItemPlacementContext alternatePlacementContext = null;
                        BlockPos alternatePrimaryPos = null;

                        if (offsetVector != null && !offsetVector.equals(BlockPos.ORIGIN)) {
                                alternatePrimaryPos = requestedPos.subtract(offsetVector);
                                alternatePlacementContext = offsetPlacementContext(placementContext, alternatePrimaryPos);
                        }

                        if (shouldShift) {
                                if (alternatePlacementContext == null) {
                                        return Optional.empty();
                                }

                                boolean alternateValid = canPlace(world, alternatePlacementContext, alternatePrimaryPos,
                                                primaryState);
                                if (!alternateValid) {
                                        return Optional.empty();
                                }

                                primaryPos = alternatePrimaryPos;
                                primaryPlacementContext = alternatePlacementContext;
                                defaultPlacementValid = alternateValid;
                        } else if (alternatePlacementContext != null) {
                                boolean alternateValid = canPlace(world, alternatePlacementContext, alternatePrimaryPos,
                                                primaryState);
                                if (!defaultPlacementValid && alternateValid) {
                                        primaryPos = alternatePrimaryPos;
                                        primaryPlacementContext = alternatePlacementContext;
                                        defaultPlacementValid = alternateValid;
                                }
                        }

                        if (!defaultPlacementValid) {
                                return Optional.empty();
                        }

                        secondaryPos = findConnectedChestPos(primaryPos, primaryState);
                        if (secondaryPos == null) {
                                return Optional.empty();
                        }

                        secondaryState = NbtHelper.toBlockState(blockLookup,
                                        keepChestData.getCompoundOrEmpty(SECONDARY_STATE_KEY));
                        secondaryState = applyFacing(secondaryState, desiredFacing);

                        ItemPlacementContext secondaryPlacementContext = offsetPlacementContext(primaryPlacementContext,
                                        secondaryPos);
                        if (!canPlace(world, secondaryPlacementContext, secondaryPos, secondaryState)) {
                                return Optional.empty();
                        }

                        NbtCompound secondaryEntityNbt = keepChestData.getCompoundOrEmpty(SECONDARY_BLOCK_ENTITY_KEY);
                        BlockEntityType<?> secondaryType = resolveSecondaryType(keepChestData, secondaryState);
                        secondaryData = TypedEntityData.create(secondaryType, secondaryEntityNbt);
                }

                ItemPlacementContext finalPrimaryContext = offsetPlacementContext(primaryPlacementContext, primaryPos);
                if (!canPlace(world, finalPrimaryContext, primaryPos, primaryState)) {
                        return Optional.empty();
                }

                return Optional.of(new PlacementPlan(primaryPos, primaryState, primaryData, secondaryPos, secondaryState,
                                secondaryData));
        }

        private static boolean placeContainer(World world, BlockPos pos, BlockState state,
                        TypedEntityData<BlockEntityType<?>> entityData) {
                if (!world.setBlockState(pos, state, Block.NOTIFY_ALL)) {
                        return false;
                }

                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity == null) {
                        return false;
                }

                if (!entityData.applyToBlockEntity(blockEntity, world.getRegistryManager())) {
                        return false;
                }

                blockEntity.markDirty();
                world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
                return true;
        }

        private static boolean canPlace(World world, ItemPlacementContext context, BlockPos pos, BlockState state) {
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

        private static ItemPlacementContext offsetPlacementContext(ItemPlacementContext context, BlockPos newBlockPos) {
                if (context.getBlockPos().equals(newBlockPos)) {
                        return context;
                }

                Vec3d originalHitPos = context.getHitPos();
                Vec3d offset = Vec3d.of(newBlockPos.subtract(context.getBlockPos()));
                Vec3d adjustedHitPos = originalHitPos.add(offset);
                BlockHitResult hitResult = new BlockHitResult(adjustedHitPos, context.getSide(), newBlockPos,
                                context.hitsInsideBlock());
                return new ItemPlacementContext(context.getWorld(), context.getPlayer(), context.getHand(), context.getStack(),
                                hitResult);
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
                                ? facing.rotateYClockwise()
                                : facing.rotateYCounterclockwise();
                return pos.offset(offset);
        }

        public record PackResult(ItemStack stack, PackedContainerType containerType, @Nullable BlockPos secondaryPos) {
        }

        public record PlacementPreview(BlockPos primaryPos, BlockState primaryState, @Nullable BlockPos secondaryPos,
                        @Nullable BlockState secondaryState) {
                public boolean isDouble() {
                        return secondaryPos != null && secondaryState != null;
                }
        }

        private record PlacementPlan(BlockPos primaryPos, BlockState primaryState,
                        TypedEntityData<BlockEntityType<?>> primaryData, @Nullable BlockPos secondaryPos,
                        @Nullable BlockState secondaryState, @Nullable TypedEntityData<BlockEntityType<?>> secondaryData) {
                private boolean isDouble() {
                        return secondaryPos != null && secondaryState != null && secondaryData != null;
                }
        }

        public static Optional<PlacementPreview> getPlacementPreview(World world, ItemPlacementContext placementContext,
                        ItemStack stack) {
                TypedEntityData<BlockEntityType<?>> primaryData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
                if (primaryData == null) {
                        return Optional.empty();
                }

                NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                NbtCompound keepChestData = customData.copyNbt();
                if (keepChestData.isEmpty() || !keepChestData.contains(PRIMARY_STATE_KEY)) {
                        return Optional.empty();
                }

                Optional<PlacementPlan> placementPlan = preparePlacement(world, placementContext, primaryData, keepChestData);
                return placementPlan.map(plan -> new PlacementPreview(plan.primaryPos(), plan.primaryState(),
                                plan.secondaryPos(), plan.secondaryState()));
        }

        public static List<ItemStack> getStoredItems(RegistryWrapper.WrapperLookup registryLookup, ItemStack stack) {
                List<ItemStack> contents = new ArrayList<>();

                TypedEntityData<BlockEntityType<?>> primaryData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
                if (primaryData == null) {
                        return contents;
                }

                NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                NbtCompound keepChestData = customData.copyNbt();
                BlockState primaryState = readStateFromData(keepChestData.getCompoundOrEmpty(PRIMARY_STATE_KEY));
                appendInventoryFromEntityData(contents, registryLookup, primaryData, primaryState);
                if (keepChestData.getBoolean(DOUBLE_KEY).orElse(false)
                                && keepChestData.contains(SECONDARY_BLOCK_ENTITY_KEY)) {
                        BlockState secondaryState = readStateFromData(keepChestData.getCompoundOrEmpty(SECONDARY_STATE_KEY));
                        BlockEntityType<?> secondaryType = resolveSecondaryType(keepChestData, secondaryState);
                        TypedEntityData<BlockEntityType<?>> secondaryData = TypedEntityData.create(secondaryType,
                                        keepChestData.getCompoundOrEmpty(SECONDARY_BLOCK_ENTITY_KEY));
                        appendInventoryFromEntityData(contents, registryLookup, secondaryData, secondaryState);
                }

                return contents;
        }

        public static List<ItemStack> getContainerDrops(ItemStack stack) {
                List<ItemStack> drops = new ArrayList<>();

                NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                NbtCompound keepChestData = customData.copyNbt();

                BlockState primaryState = readStateFromData(keepChestData.getCompoundOrEmpty(PRIMARY_STATE_KEY));
                addContainerDrop(drops, primaryState);

                if (keepChestData.getBoolean(DOUBLE_KEY).orElse(false)) {
                        BlockState secondaryState = readStateFromData(keepChestData.getCompoundOrEmpty(SECONDARY_STATE_KEY));
                        addContainerDrop(drops, secondaryState);
                }

                if (drops.isEmpty()) {
                        drops.add(new ItemStack(Blocks.CHEST));
                }

                return drops;
        }

        private static void addContainerDrop(List<ItemStack> drops, BlockState state) {
                Block block = state.getBlock();
                if (!(block instanceof ChestBlock) && !(block instanceof TrappedChestBlock)
                                && !(block instanceof BarrelBlock) && !(block instanceof CopperChestBlock)) {
                        block = Blocks.CHEST;
                }

                drops.add(new ItemStack(block));
        }

        private static void appendInventoryFromEntityData(List<ItemStack> contents, RegistryWrapper.WrapperLookup registryLookup,
                        TypedEntityData<BlockEntityType<?>> entityData, BlockState preferredState) {
                if (entityData == null) {
                        return;
                }

                Object rawType = entityData.getType();
                if (!(rawType instanceof BlockEntityType<?> blockEntityType)) {
                        return;
                }

                BlockState state = preferredState != null ? preferredState
                                : (blockEntityType == BlockEntityType.TRAPPED_CHEST ? Blocks.TRAPPED_CHEST.getDefaultState()
                                                : Blocks.CHEST.getDefaultState());

                BlockEntity blockEntity = blockEntityType.instantiate(BlockPos.ORIGIN, state);
                if (blockEntity == null || !(blockEntity instanceof Inventory inventory)) {
                        return;
                }

                if (!entityData.applyToBlockEntity(blockEntity, registryLookup)) {
                        return;
                }

                for (int slot = 0; slot < inventory.size(); slot++) {
                        ItemStack stack = inventory.getStack(slot);
                        if (!stack.isEmpty()) {
                                contents.add(stack.copy());
                        }
                }
        }

        private static BlockState readStateFromData(NbtCompound stateData) {
                if (stateData.isEmpty()) {
                        return Blocks.CHEST.getDefaultState();
                }

                String blockName = stateData.getString("Name").orElse("");
                if (!blockName.isEmpty()) {
                        Identifier identifier = Identifier.tryParse(blockName);
                        if (identifier != null) {
                                Block block = Registries.BLOCK.get(identifier);
                                if (block != Blocks.AIR) {
                                        return block.getDefaultState();
                                }
                        }
                }

                return Blocks.CHEST.getDefaultState();
        }

        private static Direction determineFacing(ItemUsageContext context, BlockState savedState) {
                PlayerEntity player = context.getPlayer();

                if (savedState.getBlock() instanceof ChestBlock || savedState.getBlock() instanceof CopperChestBlock) {
                        if (player != null) {
                                return player.getHorizontalFacing().getOpposite();
                        }

                        return savedState.contains(ChestBlock.FACING) ? savedState.get(ChestBlock.FACING) : Direction.NORTH;
                }

                if (savedState.contains(Properties.FACING)) {
                        return savedState.get(Properties.FACING);
                }

                if (savedState.contains(Properties.HORIZONTAL_FACING)) {
                        if (player != null) {
                                return player.getHorizontalFacing().getOpposite();
                        }

                        return savedState.get(Properties.HORIZONTAL_FACING);
                }

                return Direction.NORTH;
        }

        private static BlockState applyFacing(BlockState state, Direction facing) {
                if (facing == null) {
                        return state;
                }

                if (state.contains(ChestBlock.FACING)) {
                        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
                        return state.with(ChestBlock.FACING, horizontal);
                }

                if (state.contains(Properties.HORIZONTAL_FACING)) {
                        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
                        return state.with(Properties.HORIZONTAL_FACING, horizontal);
                }

                if (state.contains(Properties.FACING)) {
                        return state.with(Properties.FACING, facing);
                }

                return state;
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
