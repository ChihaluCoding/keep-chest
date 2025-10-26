package chihalu.keepchest.mixin.client;

import java.util.ArrayList;
import java.util.List;
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
                List<OutlineRenderState> previewStates = keepChest$createPreviewStates(outlineRenderState, player,
                                client, placementPreview);
                if (previewStates.isEmpty()) {
                        return;
                }

                keepChest$renderingPreview = true;
                try {
                        WorldRendererInvoker renderer = (WorldRendererInvoker) (Object) this;
                        for (OutlineRenderState previewState : previewStates) {
                                renderer.keepChest$drawBlockOutline(matrices, vertexConsumer, cameraX, cameraY, cameraZ,
                                                previewState, KEEP_CHEST_PREVIEW_COLOR);
                        }
                } finally {
                        keepChest$renderingPreview = false;
                }
        }

        @Unique
        private List<OutlineRenderState> keepChest$createPreviewStates(OutlineRenderState outlineRenderState,
                        ClientPlayerEntity player, MinecraftClient client, PackedChestItem.PlacementPreview preview) {
                BlockPos primaryPos = preview.primaryPos();
                BlockState primaryState = preview.primaryState();
                if (primaryPos == null || primaryState == null) {
                        return List.of();
                }

                boolean translucent = outlineRenderState.isTranslucent();
                boolean highContrast = outlineRenderState.highContrast();

                VoxelShape primaryShape = keepChest$getOutlineShape(client, player, primaryPos, primaryState);
                List<OutlineRenderState> previewStates = new ArrayList<>();
                previewStates.add(new OutlineRenderState(primaryPos, translucent, highContrast, primaryShape));

                if (preview.isDouble()) {
                        BlockPos secondaryPos = preview.secondaryPos();
                        BlockState secondaryState = preview.secondaryState();
                        if (secondaryPos != null && secondaryState != null) {
                                VoxelShape secondaryShape = keepChest$getOutlineShape(client, player, secondaryPos,
                                                secondaryState);
                                previewStates.add(new OutlineRenderState(secondaryPos, translucent, highContrast,
                                                secondaryShape));
                        }
                }

                return previewStates;
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
