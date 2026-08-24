package cn.academy.api;

import cn.academy.client.render.ACEffect;
import cn.academy.client.render.EffectDrawCtx;
import cn.academy.client.render.EffectDrawers;
import cn.academy.client.render.entity.ACEffectEntities;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ACEffectAPI {

    private ACEffectAPI() {}

    public static void register(Class<? extends Entity> cls, EffectDrawers.Drawer drawer) {
        EffectDrawers.register(cls, drawer);
    }

    public static void spawn(Entity effect) {
        ACEffectEntities.spawn(effect);
    }
}
