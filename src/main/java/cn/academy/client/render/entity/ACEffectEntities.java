package cn.academy.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.atomic.AtomicInteger;

@OnlyIn(Dist.CLIENT)
public final class ACEffectEntities {

    private ACEffectEntities() {}

    private static final AtomicInteger CLIENT_ID = new AtomicInteger(-1);

    public static void spawn(Entity e) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int id = CLIENT_ID.getAndDecrement();
        e.setId(id);
        level.putNonPlayerEntity(id, e);
    }
}
