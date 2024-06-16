package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.net.EditPictureFramePacket;
import me.chrr.camerapture.net.ResizePictureFramePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * This UI is fully and completely a stolen idea from the
 * <a href="https://www.curseforge.com/minecraft/mc-mods/camera-mod">Forge camera mod</a>.
 * It's far from perfect, I don't know if I like it, but I
 * can't think of anything better right now.
 */
public class EditPictureFrameScreen extends InGameScreen {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_picture_frame.png");

    private static final int backgroundWidth = 158;
    private static final int backgroundHeight = 52;

    private final PictureFrameEntity entity;

    private int frameWidth;
    private int frameHeight;
    private boolean glowing;
    private boolean fixed;

    private ButtonWidget upButton;
    private ButtonWidget leftButton;
    private ButtonWidget rightButton;
    private ButtonWidget downButton;

    public EditPictureFrameScreen(PictureFrameEntity entity) {
        super(Text.translatable("item.camerapture.picture"));

        this.entity = entity;

        this.frameWidth = entity.getFrameWidth();
        this.frameHeight = entity.getFrameHeight();
        this.glowing = entity.isPictureGlowing();
        this.fixed = entity.isFixed();
    }

    @Override
    protected void init() {
        super.init();

        upButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button ->
                                resizeRemotePicture(PictureFrameEntity.ResizeDirection.UP, hasShiftDown()))
                        .dimensions(width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2 - 20 - 4, backgroundWidth, 20)
                        .build());

        leftButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button ->
                                resizeRemotePicture(PictureFrameEntity.ResizeDirection.LEFT, hasShiftDown()))
                        .dimensions(width / 2 - backgroundWidth / 2 - 20 - 4, height / 2 - backgroundHeight / 2, 20, backgroundHeight)
                        .build());

        rightButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button ->
                                resizeRemotePicture(PictureFrameEntity.ResizeDirection.RIGHT, hasShiftDown()))
                        .dimensions(width / 2 + backgroundWidth / 2 + 4, height / 2 - backgroundHeight / 2, 20, backgroundHeight)
                        .build());

        downButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button ->
                                resizeRemotePicture(PictureFrameEntity.ResizeDirection.DOWN, hasShiftDown()))
                        .dimensions(width / 2 - backgroundWidth / 2, height / 2 + backgroundHeight / 2 + 4, backgroundWidth, 20)
                        .build());

        addDrawableChild(new SmallCheckboxWidget(Text.translatable("text.camerapture.edit_picture_frame.glowing"), (glowing) -> {
            this.glowing = glowing;
            updateRemotePicture();
        }, width / 2 - backgroundWidth / 2 + 7, height / 2 - backgroundHeight / 2 + 34, false, this.glowing));

        addDrawableChild(new SmallCheckboxWidget(Text.translatable("text.camerapture.edit_picture_frame.fixed"), (fixed) -> {
            this.fixed = fixed;
            updateRemotePicture();
        }, width / 2 + backgroundWidth / 2 - 7 - 11, height / 2 - backgroundHeight / 2 + 34, true, this.fixed));

        updateButtons();
    }

    @Override
    public void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0, 0, backgroundWidth, backgroundHeight);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.camerapture.edit_picture_frame.size", frameWidth, frameHeight), width / 2, height / 2 - backgroundHeight / 2 + 7, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.camerapture.edit_picture_frame.shrink_hint"), width / 2, height / 2 - backgroundHeight / 2 + 7 + textRenderer.fontHeight + 2, 0xa0a0a0);
    }

    private void resizeRemotePicture(PictureFrameEntity.ResizeDirection direction, boolean shrink) {
        switch (direction) {
            case UP, DOWN -> addSize(0, shrink ? -1 : 1);
            case LEFT, RIGHT -> addSize(shrink ? -1 : 1, 0);
        }

        ClientPlayNetworking.send(new ResizePictureFramePacket(entity.getUuid(), direction, shrink));
    }

    private void updateRemotePicture() {
        ClientPlayNetworking.send(new EditPictureFramePacket(entity.getUuid(), glowing, fixed));
    }

    private void updateButtons() {
        if (hasShiftDown()) {
            upButton.setMessage(Text.of("↓"));
            leftButton.setMessage(Text.of("→"));
            rightButton.setMessage(Text.of("←"));
            downButton.setMessage(Text.of("↑"));

            upButton.active = frameHeight > 1;
            leftButton.active = frameWidth > 1;
            rightButton.active = frameWidth > 1;
            downButton.active = frameHeight > 1;
        } else {
            upButton.setMessage(Text.of("↑"));
            leftButton.setMessage(Text.of("←"));
            rightButton.setMessage(Text.of("→"));
            downButton.setMessage(Text.of("↓"));

            upButton.active = frameHeight < 16;
            leftButton.active = frameWidth < 16;
            rightButton.active = frameWidth < 16;
            downButton.active = frameHeight < 16;
        }
    }

    private void addSize(int x, int y) {
        frameWidth += x;
        frameHeight += y;
        updateButtons();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        updateButtons();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        updateButtons();
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        // We don't pause the game, so we can actually see the picture frame
        // update in the background while we're editing it.
        return false;
    }

    private static class SmallCheckboxWidget extends PressableWidget {
        private final boolean leftText;
        private boolean checked;

        private final Consumer<Boolean> onChange;

        public SmallCheckboxWidget(Text text, Consumer<Boolean> onChange, int x, int y, boolean leftText, boolean checked) {
            super(x, y, 11, 11, text);

            this.onChange = onChange;
            this.leftText = leftText;
            this.checked = checked;
        }

        @Override
        public void onPress() {
            this.checked = !this.checked;
            onChange.accept(checked);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getNarrationMessage());

            if (this.active) {
                if (this.isFocused()) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.focused"));
                } else {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.hovered"));
                }
            }
        }

        @Override
        //? if >=1.20.4 {
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            //?} else
        /*protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {*/
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            TextRenderer textRenderer = minecraftClient.textRenderer;

            int textX = getX() + (leftText ? -4 - textRenderer.getWidth(getMessage()) : 11 + 4);
            context.drawTextWithShadow(textRenderer, getMessage(), textX, getY() + 2, 0xe0e0e0);

            context.drawTexture(TEXTURE, getX(), getY(), this.isSelected() ? 11 : 0, 52, 11, 11);

            if (checked) {
                context.drawTexture(TEXTURE, getX(), getY(), 22, 52, 11, 11);
            }
        }
    }
}
