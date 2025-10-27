package chihalu.packedchest.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

/**
 * Minimal configuration screen exposed via ModMenu with a toggle for preview mutability.
 */
public final class PackedChestConfigScreen extends Screen {
        private final Screen parent;
        private CyclingButtonWidget<Boolean> previewMovementToggle;

        public PackedChestConfigScreen(Screen parent) {
                super(Text.translatable("screen.packed_chest.config.title"));
                this.parent = parent;
        }

        @Override
        protected void init() {
                int centerX = this.width / 2;
                int y = this.height / 4;

                PackedChestClientConfig config = PackedChestClientConfig.get();
                this.previewMovementToggle = CyclingButtonWidget.onOffBuilder()
                                .initially(config.allowPreviewItemMovement())
                                .build(centerX - 100, y, 200, 20,
                                                Text.translatable("option.packed_chest.preview_movement"),
                                                (button, value) -> config.setAllowPreviewItemMovement(value));
                this.addDrawableChild(this.previewMovementToggle);

                this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                                .position(centerX - 100, y + 24)
                                .size(200, 20)
                                .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                this.renderDarkening(context);
                super.render(context, mouseX, mouseY, delta);
                context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        }

        @Override
        public void close() {
                this.client.setScreen(this.parent);
        }
}
