package chihalu.keepchest.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.keepchest.KeepChestClient;
import chihalu.keepchest.item.PackedChestItem;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
        @Unique
        private boolean keepChest$renderingSecondaryOutline;

        @Inject(method = "drawBlockOutline", at = @At("TAIL"))
        private void keepChest$drawPackedChestPreview(MatrixStack matrices, VertexConsumer vertexConsumer,
                        double cameraX, double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int color,
                        CallbackInfo ci) {
                if (keepChest$renderingSecondaryOutline) {
                        return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;
                if (player == null || client.world == null) {
                        return;
                }

                HitResult hitResult = client.crosshairTarget;
                if (!(hitResult instanceof BlockHitResult blockHit)) {
                        return;
                }

                Optional<KeepChestClient.HeldPackedChest> held = KeepChestClient.findHeldPackedChest(player);
                if (held.isEmpty()) {
                        return;
                }

                KeepChestClient.HeldPackedChest handStack = held.get();
                ItemPlacementContext placementContext = new ItemPlacementContext(player, handStack.hand(),
                                handStack.stack(), blockHit);

                Optional<PackedChestItem.PlacementPreview> preview = PackedChestItem
                                .getPlacementPreview(client.world, placementContext, handStack.stack());
                if (preview.isEmpty()) {
                        return;
                }

                PackedChestItem.PlacementPreview placementPreview = preview.get();
                if (!placementPreview.isDouble() || placementPreview.secondaryPos() == null
                                || placementPreview.secondaryState() == null) {
                        return;
                }

                VoxelShape shape = placementPreview.secondaryState().getOutlineShape(client.world,
                                placementPreview.secondaryPos(), ShapeContext.of(player));
                if (shape.isEmpty()) {
                        shape = VoxelShapes.fullCube();
                }

                OutlineRenderState secondaryState = new OutlineRenderState(placementPreview.secondaryPos(),
                                outlineRenderState.isTranslucent(), outlineRenderState.highContrast(), shape);

                keepChest$renderingSecondaryOutline = true;
                try {
                        ((WorldRendererInvoker) (Object) this).keepChest$drawBlockOutline(matrices, vertexConsumer,
                                        cameraX, cameraY, cameraZ, secondaryState, color);
                } finally {
                        keepChest$renderingSecondaryOutline = false;
                }
        }
}
