package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.ResizeDisplayPacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import static me.chrr.camerapture.block.DisplayBlockEntity.MAX_DIM;
import static me.chrr.camerapture.block.DisplayBlockEntity.MAX_OFFSET;

public class DisplayScreen extends HandledScreen<DisplayScreenHandler> {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/container/display.png");

    private final BlockPos pos;

    private final CounterWidget sizeX = new CounterWidget(1f, MAX_DIM, 1f, false);
    private final CounterWidget sizeY = new CounterWidget(1f, MAX_DIM, 1f, false);

    private final CounterWidget offsetX = new CounterWidget(-MAX_OFFSET, MAX_OFFSET, 0.5f, true);
    private final CounterWidget offsetY = new CounterWidget(-MAX_OFFSET, MAX_OFFSET, 0.5f, true);

    public DisplayScreen(DisplayScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.empty());

        this.backgroundHeight = 176;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        this.pos = handler.pos;

        this.offsetX.value = handler.initialOffsetX;
        this.offsetY.value = handler.initialOffsetY;
        this.sizeX.value = handler.initialWidth;
        this.sizeY.value = handler.initialHeight;
    }

    @Override
    protected void init() {
        super.init();

        int x = width / 2 - backgroundWidth / 2;
        int y = height / 2 - backgroundHeight / 2;

        sizeX.init(x + 29, y + 40);
        sizeY.init(x + 29, y + 52);

        offsetX.init(x + 102, y + 40);
        offsetY.init(x + 102, y + 52);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        Text status = Text.translatable("text.camerapture.display.insert_image")
                .formatted(Formatting.RED);

        ClientPictureStore.Picture picture = null;
        ItemStack stack = handler.getSlot(0).getStack();
        if (stack.isOf(Camerapture.PICTURE)) {
            picture = ClientPictureStore.getInstance().getServerPicture(PictureItem.getUuid(stack));
        }

        if (picture != null) {
            status = switch (picture.getStatus()) {
                case FETCHING -> Text.translatable("text.camerapture.display.fetching")
                        .formatted(Formatting.WHITE);
                case SUCCESS -> Text.translatable("text.camerapture.display.picture_ok")
                        .formatted(Formatting.GREEN);
                case ERROR -> Text.translatable("text.camerapture.display.fetching_failed")
                        .formatted(Formatting.RED);
            };
        }

        context.drawText(textRenderer, status, 35, 12, 0x00ff00, false);

        context.drawText(textRenderer, Text.translatable("text.camerapture.display.size"), 29, 29, 0x404040, false);
        context.drawText(textRenderer, "X:", 15, 40, 0x404040, false);
        context.drawText(textRenderer, "Y:", 15, 52, 0x404040, false);

        context.drawText(textRenderer, Text.translatable("text.camerapture.display.offset"), 102, 29, 0x404040, false);
        context.drawText(textRenderer, "X:", 88, 40, 0x404040, false);
        context.drawText(textRenderer, "Y:", 88, 52, 0x404040, false);

        sizeX.render(context, 29, 40);
        sizeY.render(context, 29, 52);

        offsetX.render(context, 102, 40);
        offsetY.render(context, 102, 52);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void updateSize() {
        ClientPlayNetworking.send(new ResizeDisplayPacket(pos, offsetX.value, offsetY.value, sizeX.value, sizeY.value));
    }

    private class CounterWidget {
        private final float min;
        private final float max;
        private final float step;
        private final boolean showDecimals;

        public float value = 1f;

        private CounterWidget(float min, float max, float step, boolean showDecimals) {
            this.min = min;
            this.max = max;
            this.step = step;
            this.showDecimals = showDecimals;
        }

        public void init(int x, int y) {
            addDrawableChild(ButtonWidget.builder(Text.of("-"), (button) -> {
                float value = this.value - this.step;
                this.value = Math.min(Math.max(value, this.min), this.max);
                updateSize();
            }).dimensions(x - 1, y - 1, 9, 9).build());

            addDrawableChild(ButtonWidget.builder(Text.of("+"), (button) -> {
                float value = this.value + this.step;
                this.value = Math.min(Math.max(value, this.min), this.max);
                updateSize();
            }).dimensions(x + 36 - 1, y - 1, 9, 9).build());
        }

        public void render(DrawContext context, int x, int y) {
            String text = this.showDecimals ? String.valueOf(this.value) : String.valueOf((int) this.value);

            int width = textRenderer.getWidth(text);
            context.drawText(textRenderer, text, x + 22 - width / 2, y, 0xffffff, false);
        }
    }
}
