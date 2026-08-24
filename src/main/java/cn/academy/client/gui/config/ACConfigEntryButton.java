package cn.academy.client.gui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ACConfigEntryButton {

    private static final int SIZE = 20;

    private static final int GAP = 4;

    private static final ResourceLocation ICON =
            new ResourceLocation("academy", "textures/gui/logo_bq.png");

    private static final int TEX_W = 32, TEX_H = 33;

    private static final int ICON_SIZE = 16;

    private ACConfigEntryButton() {}

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.register(new ACConfigEntryButton());
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        final String anchorKey;
        final int fallbackX, fallbackY;
        if (screen instanceof TitleScreen) {
            anchorKey = "fml.menu.mods";

            fallbackX = screen.width / 2 - 100;
            fallbackY = screen.height / 4 + 48 + 48;
        } else if (screen instanceof PauseScreen) {
            anchorKey = "menu.options";

            fallbackX = screen.width / 2 - 102;
            fallbackY = screen.height / 4 + 80;
        } else {
            return;
        }

        AbstractWidget anchor = findByLangKey(event, anchorKey);
        int ax, ay;
        if (anchor != null) {
            ax = anchor.getX();
            ay = anchor.getY();
        } else {

            if (!hasAnyWidget(event)) {
                return;
            }
            ax = fallbackX;
            ay = fallbackY;
        }

        int x = ax - GAP - SIZE;
        int y = ay;
        for (int guard = 0; guard < 6 && occupied(event, x, y); guard++) {
            y += 24;
        }

        LogoButton button = new LogoButton(x, y,
                b -> Minecraft.getInstance().setScreen(new ACConfigHomeScreen(screen)));
        button.setTooltip(Tooltip.create(Component.translatable("gui.academy.config.title")));
        event.addListener(button);
    }

    private static AbstractWidget findByLangKey(ScreenEvent.Init event, String langKey) {
        for (GuiEventListener l : event.getListenersList()) {
            if (l instanceof AbstractWidget w
                    && w.getMessage() != null
                    && w.getMessage().getContents() instanceof TranslatableContents tc
                    && langKey.equals(tc.getKey())) {
                return w;
            }
        }
        return null;
    }

    private static boolean hasAnyWidget(ScreenEvent.Init event) {
        for (GuiEventListener l : event.getListenersList()) {
            if (l instanceof AbstractWidget) {
                return true;
            }
        }
        return false;
    }

    private static final class LogoButton extends Button {
        LogoButton(int x, int y, OnPress onPress) {
            super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partial) {
            super.renderWidget(gg, mouseX, mouseY, partial);
            gg.blit(ICON,
                    getX() + (this.width - ICON_SIZE) / 2,
                    getY() + (this.height - ICON_SIZE) / 2,
                    ICON_SIZE, ICON_SIZE,
                    0F, 0F, TEX_W, TEX_H, TEX_W, TEX_H);
        }
    }

    private static boolean occupied(ScreenEvent.Init event, int x, int y) {
        for (GuiEventListener l : event.getListenersList()) {
            if (l instanceof AbstractWidget w
                    && Math.abs(w.getX() - x) < SIZE && Math.abs(w.getY() - y) < SIZE) {
                return true;
            }
        }
        return false;
    }
}
