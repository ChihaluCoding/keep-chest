package chihalu.keepchest.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.keepchest.KeepChestClient;
import chihalu.keepchest.item.PackedChestItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
        @Unique
        private static final int KEEP_CHEST_PREVIEW_COLOR = 0x7850FF78;

        @Unique
        private boolean keepChest$renderingPreview;

        @Inject(method = "drawBlockOutline", at = @At("TAIL"))
        private void keepChest$drawPackedChestPreview(MatrixStack matrices, VertexConsumer vertexConsumer,
                        double cameraX, double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int color,
                        CallbackInfo ci) {
                if (keepChest$renderingPreview) {
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                Optional<PackedChestItem.PlacementPreview> preview = KeepChestClient.findPlacementPreview(client);
                if (preview.isEmpty()) {
                        return;
                }

                PackedChestItem.PlacementPreview placementPreview = preview.get();
                ClientPlayerEntity player = client.player;
                if (player == null) {
                        return;
                }
                OutlineRenderState previewState = keepChest$createPreviewState(outlineRenderState, player, client,
                                placementPreview);
                if (previewState == null) {
                        return;
                }

                keepChest$renderingPreview = true;
                try {
                        ((WorldRendererInvoker) (Object) this).keepChest$drawBlockOutline(matrices, vertexConsumer,
                                        cameraX, cameraY, cameraZ, previewState, KEEP_CHEST_PREVIEW_COLOR);
                } finally {
                        keepChest$renderingPreview = false;
                }
        }

        @Unique
        private OutlineRenderState keepChest$createPreviewState(OutlineRenderState outlineRenderState,
                        ClientPlayerEntity player, MinecraftClient client, PackedChestItem.PlacementPreview preview) {
                BlockPos primaryPos = preview.primaryPos();
                BlockState primaryState = preview.primaryState();
                if (primaryPos == null || primaryState == null) {
                        return null;
                }

                VoxelShape primaryShape = keepChest$getOutlineShape(client, player, primaryPos, primaryState);
                Box primaryWorldBox = primaryShape.getBoundingBox().offset(primaryPos);
                Box combinedWorldBox = primaryWorldBox;
                BlockPos basePos = primaryPos;

                if (preview.isDouble()) {
                        BlockPos secondaryPos = preview.secondaryPos();
                        BlockState secondaryState = preview.secondaryState();
                        if (secondaryPos != null && secondaryState != null) {
                                VoxelShape secondaryShape = keepChest$getOutlineShape(client, player, secondaryPos,
                                                secondaryState);
                                Box secondaryWorldBox = secondaryShape.getBoundingBox().offset(secondaryPos);
                                combinedWorldBox = combinedWorldBox.union(secondaryWorldBox);

                                int baseX = Math.min(primaryPos.getX(), secondaryPos.getX());
                                int baseY = Math.min(primaryPos.getY(), secondaryPos.getY());
                                int baseZ = Math.min(primaryPos.getZ(), secondaryPos.getZ());
                                basePos = new BlockPos(baseX, baseY, baseZ);
                        }
                }

                double relativeMinX = combinedWorldBox.minX - basePos.getX();
                double relativeMinY = combinedWorldBox.minY - basePos.getY();
                double relativeMinZ = combinedWorldBox.minZ - basePos.getZ();
                double relativeMaxX = combinedWorldBox.maxX - basePos.getX();
                double relativeMaxY = combinedWorldBox.maxY - basePos.getY();
                double relativeMaxZ = combinedWorldBox.maxZ - basePos.getZ();
                VoxelShape combinedShape = VoxelShapes.cuboid(relativeMinX, relativeMinY, relativeMinZ, relativeMaxX,
                                relativeMaxY, relativeMaxZ);

                return new OutlineRenderState(basePos, true, outlineRenderState.highContrast(), combinedShape);
        }

        @Unique
        private VoxelShape keepChest$getOutlineShape(MinecraftClient client, ClientPlayerEntity player, BlockPos pos,
                        BlockState state) {
                VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.of(player));
                if (shape.isEmpty()) {
                        return VoxelShapes.fullCube();
                }
                return shape;
        }
}
