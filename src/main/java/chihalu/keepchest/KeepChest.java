package chihalu.keepchest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chihalu.keepchest.item.KeepChestItems;
import chihalu.keepchest.item.PackedChestItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class KeepChest implements ModInitializer {
        public static final String MOD_ID = "keep-chest";

        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                KeepChestItems.register();
                registerPackedChestHandler();
                LOGGER.info("Keep Chest is active. Chests can now be packed and restored.");
        }

        private void registerPackedChestHandler() {
                PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
                        if (!(world instanceof ServerWorld serverWorld)) {
                                return true;
                        }

                        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                                return true;
                        }

                        if (!player.isSneaking()) {
                                return true;
                        }

                        if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof BarrelBlock)) {
                                return true;
                        }

                        if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof BarrelBlockEntity)) {
                                return true;
                        }

                        Optional<PackedChestItem.PackResult> result = PackedChestItem.pack(serverWorld, pos, state,
                                        (BlockEntity) blockEntity);
                        if (result.isEmpty()) {
                                serverPlayer.sendMessage(Text.translatable("message.keep-chest.pack_failed"), true);
                                return true;
                        }

                        PackedChestItem.PackResult packResult = result.get();
                        ItemStack stack = packResult.stack();
                        serverPlayer.giveItemStack(stack);

                        removeChestBlocks(serverWorld, pos, state, packResult.secondaryPos());

                        serverPlayer.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
                        serverPlayer.sendMessage(
                                        Text.translatable("message.keep-chest.packed", state.getBlock().getName()),
                                        true);
                        SoundEvent sound = state.getBlock() instanceof BarrelBlock ? SoundEvents.BLOCK_BARREL_CLOSE
                                        : SoundEvents.BLOCK_CHEST_CLOSE;
                        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.75f, 0.9f);
                        return false;
                });
        }

        private static void removeChestBlocks(ServerWorld world, BlockPos primaryPos, BlockState state, BlockPos secondaryPos) {
                world.removeBlockEntity(primaryPos);
                world.removeBlock(primaryPos, false);
                state.getBlock().onBroken(world, primaryPos, state);

                if (secondaryPos != null) {
                        BlockState secondaryState = world.getBlockState(secondaryPos);
                        world.removeBlockEntity(secondaryPos);
                        world.removeBlock(secondaryPos, false);
                        secondaryState.getBlock().onBroken(world, secondaryPos, secondaryState);
                }
        }
}
