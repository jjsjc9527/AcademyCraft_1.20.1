package cn.lambdalib2.auxgui;

import cn.lambdalib2.util.GameTimer;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AuxGuiHandler {

    private static final AuxGuiHandler instance = new AuxGuiHandler();

    private AuxGuiHandler() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(instance);
    }

    private static boolean iterating;
    private static final List<AuxGui> auxGuiList = new LinkedList<>();
    private static final List<AuxGui> toAddList = new ArrayList<>();

    public static void register(AuxGui gui) {
        if (!iterating)
            doAdd(gui);
        else
            toAddList.add(gui);
    }

    public static List<AuxGui> active() {
        return ImmutableList.copyOf(auxGuiList);
    }

    public static boolean hasForegroundGui() {
        return auxGuiList.stream().anyMatch(gui -> !gui.disposed && gui.foreground);
    }

    private static void doAdd(AuxGui gui) {
        auxGuiList.add(gui);
        MinecraftForge.EVENT_BUS.post(new OpenAuxGuiEvent(gui));
        gui.onEnable();
    }

    private static void startIterating() {
        iterating = true;
    }

    private static void endIterating() {
        iterating = false;
    }

    @SubscribeEvent
    public void drawHudEvent(RenderGuiEvent.Post event) {
        doRender(event.getGuiGraphics());
    }

    private void doRender(GuiGraphics gg) {
        if (auxGuiList.isEmpty()) return;
        if (!cn.lambdalib2.datapart.EntityData.isLocalPlayerReady()) return;

        Minecraft mc = Minecraft.getInstance();
        float w = (float) mc.getWindow().getGuiScaledWidth();
        float h = (float) mc.getWindow().getGuiScaledHeight();

        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        startIterating();
        for (AuxGui gui : auxGuiList) {
            if (!gui.disposed) {
                if (!gui.lastFrameActive)
                    gui.lastActivateTime = GameTimer.getTime();
                gui.draw(gg, w, h);
                gui.lastFrameActive = true;
            }
        }
        endIterating();

        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @SubscribeEvent
    public void clientTick(ClientTickEvent event) {
        if (!Minecraft.getInstance().isPaused()) {
            for (AuxGui gui : toAddList)
                doAdd(gui);
            toAddList.clear();

            boolean ready = cn.lambdalib2.datapart.EntityData.isLocalPlayerReady();

            Iterator<AuxGui> iter = auxGuiList.iterator();
            startIterating();
            while (iter.hasNext()) {
                AuxGui gui = iter.next();

                if (gui.disposed) {
                    gui.onDisposed();
                    gui.lastFrameActive = false;
                    iter.remove();
                } else if (gui.requireTicking && ready) {
                    if (!gui.lastFrameActive)
                        gui.lastActivateTime = GameTimer.getTime();
                    gui.onTick();
                    gui.lastFrameActive = true;
                }
            }
            endIterating();
        }
    }

    @SubscribeEvent
    public void disconnected(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        startIterating();
        Iterator<AuxGui> iter = auxGuiList.iterator();
        while (iter.hasNext()) {
            AuxGui gui = iter.next();
            if (!gui.consistent) {
                gui.onDisposed();
                iter.remove();
            }
        }
        endIterating();
    }

}
