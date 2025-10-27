package chihalu.packedchest.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.packedchest.PackedChestClient;
import chihalu.packedchest.item.PackedChestItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
        @Unique
        private static final int PACKED_CHEST_PREVIEW_COLOR = 0x7850FF78;

        @Unique
        private boolean packedChest$renderingPreview;

        @Unique
        private OutlineRenderData packedChest$previewData;

        @Inject(method = "drawBlockOutline", at = @At("TAIL"))
        private void packedChest$drawPackedChestPreview(MatrixStack matrices, VertexConsumer vertexConsumer,
                        double cameraX, double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int color,
                        CallbackInfo ci) {
                packedChest$previewData = null;
                if (packedChest$renderingPreview) {
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                Optional<PackedChestItem.PlacementPreview> preview = PackedChestClient.findPlacementPreview(client);
                if (preview.isEmpty()) {
                        return;
                }

                PackedChestItem.PlacementPreview placementPreview = preview.get();
                ClientPlayerEntity player = client.player;
                if (player == null) {
                        return;
                }
                OutlineRenderState previewState = packedChest$createPreviewState(outlineRenderState, player, client,
                                placementPreview);
                if (previewState == null) {
                        return;
                }

                packedChest$renderingPreview = true;
                try {
                        ((WorldRendererInvoker) (Object) this).packedChest$drawBlockOutline(matrices, vertexConsumer,
                                        cameraX, cameraY, cameraZ, previewState, PACKED_CHEST_PREVIEW_COLOR);
                } finally {
                        packedChest$renderingPreview = false;
                }

                packedChest$renderFilledPreview(matrices, cameraX, cameraY, cameraZ);
        }

        @Unique
        private OutlineRenderState packedChest$createPreviewState(OutlineRenderState outlineRenderState,
                        ClientPlayerEntity player, MinecraftClient client, PackedChestItem.PlacementPreview preview) {
                BlockPos primaryPos = preview.primaryPos();
                BlockState primaryState = preview.primaryState();
                if (primaryPos == null || primaryState == null) {
                        return null;
                }

                VoxelShape primaryShape = packedChest$getOutlineShape(client, player, primaryPos, primaryState);
                Box primaryWorldBox = primaryShape.getBoundingBox().offset(primaryPos);
                Box combinedWorldBox = primaryWorldBox;
                BlockPos basePos = primaryPos;

                if (preview.isDouble()) {
                        BlockPos secondaryPos = preview.secondaryPos();
                        BlockState secondaryState = preview.secondaryState();
                        if (secondaryPos != null && secondaryState != null) {
                                VoxelShape secondaryShape = packedChest$getOutlineShape(client, player, secondaryPos,
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

                packedChest$previewData = new OutlineRenderData(combinedWorldBox, packedChest$getFrontDirection(primaryState));

                return new OutlineRenderState(basePos, true, outlineRenderState.highContrast(), combinedShape);
        }

        @Unique
        private VoxelShape packedChest$getOutlineShape(MinecraftClient client, ClientPlayerEntity player, BlockPos pos,
                        BlockState state) {
                VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.of(player));
                if (shape.isEmpty()) {
                        return VoxelShapes.fullCube();
                }
                return shape;
        }

        @Unique
        private Direction packedChest$getFrontDirection(BlockState state) {
                if (state == null) {
                        return Direction.NORTH;
                }

                if (state.contains(ChestBlock.FACING)) {
                        return state.get(ChestBlock.FACING);
                }

                if (state.contains(Properties.HORIZONTAL_FACING)) {
                        return state.get(Properties.HORIZONTAL_FACING);
                }

                if (state.contains(Properties.FACING)) {
                        return state.get(Properties.FACING);
                }
                return Direction.NORTH;
        }

        @Unique
        private void packedChest$renderFilledPreview(MatrixStack matrices, double cameraX, double cameraY, double cameraZ) {
                if (packedChest$previewData == null) {
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
                Box worldBox = packedChest$previewData.box().expand(0.002);

                float minX = (float) (worldBox.minX - cameraX);
                float minY = (float) (worldBox.minY - cameraY);
                float minZ = (float) (worldBox.minZ - cameraZ);
                float maxX = (float) (worldBox.maxX - cameraX);
                float maxY = (float) (worldBox.maxY - cameraY);
                float maxZ = (float) (worldBox.maxZ - cameraZ);

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                VertexConsumer consumer = consumers.getBuffer(RenderLayer.getDebugFilledBox());

                Direction front = packedChest$previewData.frontDirection();

                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.DOWN, front);
                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.UP, front);
                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.NORTH, front);
                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.SOUTH, front);
                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.WEST, front);
                packedChest$drawFace(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, Direction.EAST, front);

                consumers.draw(RenderLayer.getDebugFilledBox());
        }

        @Unique
        private void packedChest$drawFace(VertexConsumer consumer, Matrix4f matrix, float minX, float minY, float minZ,
                        float maxX, float maxY, float maxZ, Direction face, Direction front) {
                boolean isFront = face == front;
                float alpha = isFront ? 0.5f : 0.35f;
                float red = 0.0f;
                float green = isFront ? 1.0f : 0.9f;
                float blue = 0.0f;

                switch (face) {
                case DOWN -> packedChest$emitQuad(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, minX, minY, maxZ,
                                maxX, minY, maxZ, red, green, blue, alpha);
                case UP -> packedChest$emitQuad(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, minZ,
                                maxX, maxY, maxZ, red, green, blue, alpha);
                case NORTH -> packedChest$emitQuad(consumer, matrix, maxX, maxY, minZ, maxX, minY, minZ, minX, maxY, minZ,
                                minX, minY, minZ, red, green, blue, alpha);
                case SOUTH -> packedChest$emitQuad(consumer, matrix, minX, maxY, maxZ, minX, minY, maxZ, maxX, maxY, maxZ,
                                maxX, minY, maxZ, red, green, blue, alpha);
                case WEST -> packedChest$emitQuad(consumer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, minZ,
                                minX, maxY, maxZ, red, green, blue, alpha);
                case EAST -> packedChest$emitQuad(consumer, matrix, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, maxZ,
                                maxX, maxY, minZ, red, green, blue, alpha);
                }
        }

        @Unique
        private void packedChest$emitQuad(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2,
                        float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g,
                        float b, float a) {
                consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
                consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
                consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
                consumer.vertex(matrix, x4, y4, z4).color(r, g, b, a);
        }

        @Unique
        private record OutlineRenderData(Box box, Direction frontDirection) {
        }
}
