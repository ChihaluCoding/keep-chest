package chihalu.keepchest.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.keepchest.KeepChestClient;
import chihalu.keepchest.item.PackedChestItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
        @Inject(method = "render", at = @At("TAIL"))
        private void keepChest$renderPlacementOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
                MinecraftClient client = MinecraftClient.getInstance();
                Optional<PackedChestItem.PlacementPreview> preview = KeepChestClient.findPlacementPreview(client);
                if (preview.isEmpty()) {
                        return;
                }

                PackedChestItem.PlacementPreview placementPreview = preview.get();
                BlockPos primaryPos = placementPreview.primaryPos();
                BlockState primaryState = placementPreview.primaryState();
                if (primaryPos == null || primaryState == null) {
                        return;
                }

                Direction facing = keepChest$getPreviewFacing(primaryState);
                List<Text> lines = new ArrayList<>();
                lines.add(Text.translatable("hud.keep_chest.preview.facing", KeepChestClient.directionText(facing)));

                if (placementPreview.isDouble() && primaryState.getBlock() instanceof ChestBlock) {
                        BlockPos secondaryPos = placementPreview.secondaryPos();
                        Direction extension = null;
                        if (secondaryPos != null) {
                                int dx = secondaryPos.getX() - primaryPos.getX();
                                int dy = secondaryPos.getY() - primaryPos.getY();
                                int dz = secondaryPos.getZ() - primaryPos.getZ();
                                if (dx > 0) {
                                        extension = Direction.EAST;
                                } else if (dx < 0) {
                                        extension = Direction.WEST;
                                } else if (dz > 0) {
                                        extension = Direction.SOUTH;
                                } else if (dz < 0) {
                                        extension = Direction.NORTH;
                                } else if (dy > 0) {
                                        extension = Direction.UP;
                                } else if (dy < 0) {
                                        extension = Direction.DOWN;
                                }
                        }

                        Text extensionText = extension != null ? KeepChestClient.directionText(extension)
                                        : Text.translatable("hud.keep_chest.preview.unknown");
                        lines.add(Text.translatable("hud.keep_chest.preview.secondary", extensionText));

                        if (primaryState.contains(ChestBlock.CHEST_TYPE)) {
                                ChestType chestType = primaryState.get(ChestBlock.CHEST_TYPE);
                                if (chestType != ChestType.SINGLE) {
                                        lines.add(Text.translatable("hud.keep_chest.preview.half."
                                                        + chestType.asString()));
                                }
                        }
                }

                if (lines.isEmpty()) {
                        return;
                }

                TextRenderer textRenderer = client.textRenderer;
                int centerX = context.getScaledWindowWidth() / 2;
                int startY = context.getScaledWindowHeight() / 2 + 10;
                int lineHeight = textRenderer.fontHeight + 2;

                for (int i = 0; i < lines.size(); i++) {
                        Text line = lines.get(i);
                        int width = textRenderer.getWidth(line);
                        int x = centerX - width / 2;
                        int y = startY + i * lineHeight;
                        context.drawTextWithShadow(textRenderer, line, x, y, 0xFFFFFF);
                }
        }

        private Direction keepChest$getPreviewFacing(BlockState state) {
                if (state == null) {
                        return null;
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

                return null;
        }
}
