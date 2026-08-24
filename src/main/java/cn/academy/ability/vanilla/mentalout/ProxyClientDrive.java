package cn.academy.ability.vanilla.mentalout;

import cn.lambdalib2.s11n.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ProxyClientDrive {

    private static final int DRIVE_TIMEOUT = 40;

    private ProxyClientDrive() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ProxyClientDrive());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        if (beingDriven()) {

            drainClicks(mc);
            return;
        }
        if (!ProxyState.isProxyOwner(p)) {
            return;
        }
        sendInput(mc, p);
        sendAttacks(mc, p);
    }

    public static void passThroughPerspective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null
                || (!ProxyState.isProxyOwner(mc.player) && !beingDriven())) {
            return;
        }
        for (; mc.options.keyTogglePerspective.consumeClick(); mc.levelRenderer.needsUpdate()) {
            net.minecraft.client.CameraType was = mc.options.getCameraType();
            mc.options.setCameraType(was.cycle());
            if (was.isFirstPerson() != mc.options.getCameraType().isFirstPerson()) {
                mc.gameRenderer.checkEntityPostEffect(
                        mc.options.getCameraType().isFirstPerson() ? mc.getCameraEntity() : null);
            }
        }
    }

    private static void drainClicks(Minecraft mc) {
        int n = 0;
        while (mc.options.keyAttack.consumeClick() && n < 64) {
            ++n;
        }
        n = 0;
        while (mc.options.keyUse.consumeClick() && n < 64) {
            ++n;
        }
    }

    private static void sendInput(Minecraft mc, LocalPlayer p) {
        float fwd = p.input.forwardImpulse;
        float strafe = p.input.leftImpulse;
        float yaw = p.getYRot();
        float pitch = p.getXRot();

        int flags = (mc.options.keyJump.isDown() ? ProxyState.F_JUMP : 0)
                | (mc.options.keyShift.isDown() ? ProxyState.F_SNEAK : 0)
                | (p.isSprinting() ? ProxyState.F_SPRINT : 0);

        NetworkMessage.sendToServer(NetworkMessage.staticCaller(ProxyState.class),
                ProxyState.MSG_INPUT, p, fwd, strafe, yaw, pitch, flags);
    }

    private static void sendAttacks(Minecraft mc, LocalPlayer p) {
        int n = 0;
        while (mc.options.keyAttack.consumeClick() && n < 4) {
            ++n;
        }
        if (n == 0) {
            return;
        }
        for (int i = 0; i < n; ++i) {

            NetworkMessage.sendToServer(NetworkMessage.staticCaller(ProxyState.class),
                    ProxyState.MSG_ATTACK, p);
        }
    }

    public static boolean beingDriven() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && ProxyState.drivenIs(p)
                && net.minecraft.Util.getMillis() - ProxyState.drivenAt() < DRIVE_TIMEOUT * 50L;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInput(MovementInputUpdateEvent event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || event.getEntity() != p || !beingDriven()) {
            return;
        }
        int flags = ProxyState.drivenFlags();
        event.getInput().forwardImpulse = ProxyState.drivenForward();
        event.getInput().leftImpulse = ProxyState.drivenStrafe();
        event.getInput().jumping = (flags & ProxyState.F_JUMP) != 0;
        event.getInput().shiftKeyDown = (flags & ProxyState.F_SNEAK) != 0;
        p.setSprinting((flags & ProxyState.F_SPRINT) != 0);

        applyView(p, ProxyState.drivenYaw(), ProxyState.drivenPitch());
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        if (beingDriven()) {
            applyView(p, ProxyState.drivenYaw(), ProxyState.drivenPitch());
            return;
        }
        predictOwnerView(p);
    }

    private static void predictOwnerView(LocalPlayer p) {
        ProxyState.Link link = ProxyState.linkOf(p);
        if (link == null) {
            return;
        }
        if (cn.academy.client.render.AllyCamFeed.resolve(link.target)
                instanceof net.minecraft.world.entity.LivingEntity le) {
            applyView(le, p.getYRot(), p.getXRot());
        }
    }

    private static void applyView(net.minecraft.world.entity.LivingEntity e,
                                  float yaw, float pitch) {

        if (!(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
            e.setYRot(yaw);
            e.yRotO = yaw;
            e.yBodyRot = yaw;
            e.yBodyRotO = yaw;
        }
        e.yHeadRot = yaw;
        e.yHeadRotO = yaw;
        e.setXRot(pitch);
        e.xRotO = pitch;
    }
}
