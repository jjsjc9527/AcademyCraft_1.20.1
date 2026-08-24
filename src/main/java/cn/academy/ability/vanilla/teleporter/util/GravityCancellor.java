package cn.academy.ability.vanilla.teleporter.util;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class GravityCancellor {

    private static final double ANTI_GRAVITY = 0.072;

    private final Player player;
    private final int maxTick;
    private int tick = 0;
    private boolean dead = false;

    public GravityCancellor(Player player, int ticks) {
        this.player = player;
        this.maxTick = ticks;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead() {
        if (!dead) {
            dead = true;
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || dead) {
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        if (++tick > maxTick || !player.isAlive()) {
            setDead();
            return;
        }
        if (!player.getAbilities().flying && !player.onGround()) {
            Direction g = ACGravity.getGravityDirection(player);
            Vec3 up = RotationUtil.vecPlayerToWorld(new Vec3(0, 1, 0), g);
            player.setDeltaMovement(player.getDeltaMovement().add(up.scale(ANTI_GRAVITY)));
        }
    }
}
