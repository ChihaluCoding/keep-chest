package chihalu.keepchest.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(WorldRenderer.class)
interface WorldRendererInvoker {
        @Invoker("drawBlockOutline")
        void keepChest$drawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double cameraX,
                        double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int color);
}
