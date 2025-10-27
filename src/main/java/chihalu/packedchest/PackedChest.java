package chihalu.packedchest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chihalu.packedchest.item.PackedChestItem;
import chihalu.packedchest.item.PackedChestItems;
import chihalu.packedchest.network.PackedChestNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PackedChest implements ModInitializer {
        public static final String MOD_ID = "packed-chest";

        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                PackedChestItems.register();
                registerPackedChestHandler();
                PackedChestNetworking.init();
                LOGGER.info("Packed Chest is active. Containers can now be packed and restored.");
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

                        if (!(state.getBlock() instanceof AbstractChestBlock) && !(state.getBlock() instanceof BarrelBlock)) {
                                return true;
                        }

                        if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof BarrelBlockEntity)) {
                                return true;
                        }

                        Optional<PackedChestItem.PackResult> result = PackedChestItem.pack(serverWorld, pos, state,
                                        (BlockEntity) blockEntity);
                        if (result.isEmpty()) {
                                serverPlayer.sendMessage(Text.translatable("message.packed-chest.pack_failed"), true);
                                return true;
                        }

                        PackedChestItem.PackResult packResult = result.get();
                        ItemStack stack = packResult.stack();
                        Block.dropStack(serverWorld, pos, stack);

                        removeChestBlocks(serverWorld, pos, state, packResult.secondaryPos());

                        serverPlayer.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
                        BlockSoundGroup soundGroup = state.getSoundGroup();
                        world.playSound(null, pos, soundGroup.getBreakSound(), SoundCategory.BLOCKS,
                                        soundGroup.getVolume(), soundGroup.getPitch());
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
