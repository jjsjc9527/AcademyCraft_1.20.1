package cn.academy.client.render;

import cn.academy.ability.vanilla.mentalout.ProxyState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AllyCastView {

    private static UUID target;

    private static Component targetName = Component.empty();

    private static Entity prevCam;
    private static boolean prevRenderHand;

    private static boolean swapped;

    private AllyCastView() {}

    public static boolean isCasting() {
        return target != null;
    }

    public static boolean isCastMainView() {
        return target != null && !AllyCamFeed.isRendering();
    }

    public static UUID targetId() {
        return target;
    }

    public static boolean begin(UUID id, Component name) {
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.level == null || mc.player == null) {
            return false;
        }

        if (!cn.academy.ability.vanilla.mentalout.advanced.FreeManip.isLearned(mc.player)) {
            return false;
        }
        Entity e = AllyCamFeed.resolve(id);
        if (e == null || !e.isAlive() || e == mc.player) {
            return false;
        }
        target = id;
        targetName = name == null ? e.getName() : name;

        ProxyState.put(mc.player, e.getUUID(), mc.level.getGameTime());

        cn.lambdalib2.s11n.network.NetworkMessage.sendToServer(
                cn.lambdalib2.s11n.network.NetworkMessage.staticCaller(ProxyState.class),
                ProxyState.MSG_BEGIN, mc.player, e.getId());

        AllyCastEyelid.onCastBegin();
        return true;
    }

    public static void end(Component reason) {
        if (target == null) {
            return;
        }
        target = null;
        targetName = Component.empty();

        AllyCastEyelid.onCastEnd();

        Minecraft mcRef = Minecraft.getInstance();
        if (mcRef.player != null && ProxyState.isProxyOwner(mcRef.player)) {
            ProxyState.drop(mcRef.player);

            cn.lambdalib2.s11n.network.NetworkMessage.sendToServer(
                    cn.lambdalib2.s11n.network.NetworkMessage.staticCaller(ProxyState.class),
                    ProxyState.MSG_END, mcRef.player);
        }

        Minecraft mc = Minecraft.getInstance();
        if (reason != null && mc.gui != null) {
            mc.gui.setOverlayMessage(reason, false);
        }
    }

    public static void enterLevel() {

        if (swapped || target == null || AllyCamFeed.isRendering()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity e = AllyCamFeed.resolve(target);
        if (e == null || !e.isAlive() || e == mc.player) {
            return;
        }
        prevCam = mc.getCameraEntity();

        prevRenderHand = ((cn.academy.mixin.client.GameRendererAccessor) mc.gameRenderer)
                .academy$isRenderHand();
        swapped = true;
        academy$setCamera(mc, e);
        mc.gameRenderer.setRenderHand(false);
    }

    private static void academy$setCamera(Minecraft mc, Entity e) {
        mc.cameraEntity = e;
    }

    public static void leaveLevel() {
        if (!swapped) {
            return;
        }
        swapped = false;
        Minecraft mc = Minecraft.getInstance();
        academy$setCamera(mc, prevCam);
        mc.gameRenderer.setRenderHand(prevRenderHand);
        prevCam = null;

        if (mc.player != null) {
            Vec3 eye = mc.player.getEyePosition();
            mc.hitResult = BlockHitResult.miss(eye, Direction.UP, BlockPos.containing(eye));
            mc.crosshairPickEntity = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        String deny = ProxyState.takeDeny();
        if (deny != null && target != null) {
            Minecraft mcRef = Minecraft.getInstance();
            if (mcRef.player != null) {
                ProxyState.drop(mcRef.player);
            }

            if ("range_lost".equals(deny) || "reflected".equals(deny)) {
                AllyCastEyelid.armNoise();
            }
            end(Component.translatable(denyKey(deny)));
            return;
        }
        if (target == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            end(null);
            return;
        }
        if (mc.player.isDeadOrDying()) {
            end(Component.translatable("gui.academy.remote_control.cast_end_self"));
            return;
        }
        Entity e = AllyCamFeed.resolve(target);
        if (e == null || !e.isAlive() || e == mc.player) {
            end(Component.translatable("gui.academy.remote_control.cast_lost"));
        }
    }

    private static String denyKey(String why) {
        switch (why) {
            case "not_ally":
                return "gui.academy.remote_control.cast_deny_ally";
            case "self_down":
                return "gui.academy.remote_control.cast_end_self";

            case "no_skill":
                return "gui.academy.remote_control.cast_deny_skill";

            case "reflected":
                return "gui.academy.remote_control.cast_deny_reflected";

            case "out_of_range":
                return "gui.academy.remote_control.cast_deny_range";

            case "range_lost":
                return "gui.academy.remote_control.cast_lost_range";
            default:
                return "gui.academy.remote_control.cast_lost";
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (target == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        Component msg = Component.translatable("gui.academy.remote_control.cast_hint", targetName);
        int w = event.getGuiGraphics().guiWidth();
        int x = w / 2;
        int tw = mc.font.width(msg);
        event.getGuiGraphics().fill(x - tw / 2 - 4, 4, x + tw / 2 + 4, 16, 0x90000000);
        event.getGuiGraphics().drawCenteredString(mc.font, msg, x, 6, 0xFFFFCD46);
    }
}
