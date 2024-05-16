package me.chrr.camerapture.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

// Because of differences between 1.20.1 and 1.20.4, background
// rendering changed in weird ways. I don't want to repeat these
// for multiple screens, so here we are.
public abstract class InGameScreen extends Screen {
    protected InGameScreen(Text title) {
        super(title);
    }

    /*? if >=1.20.4 { */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
        this.renderScreen(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
    /*? } else { *//*
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.renderScreen(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
    *//*? } */

    public abstract void renderScreen(DrawContext context, int mouseX, int mouseY, float delta);
}
