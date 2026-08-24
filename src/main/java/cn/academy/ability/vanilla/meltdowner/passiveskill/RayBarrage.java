package cn.academy.ability.vanilla.meltdowner.passiveskill;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.entity.EntityMdRayBarrage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class RayBarrage extends Skill {

    public static final RayBarrage INSTANCE = new RayBarrage();

    private RayBarrage() {
        super("ray_barrage", 4);
        canControl = false;
    }

    public static boolean isLearned(Player player) {
        return AbilityData.get(player).isSkillLearned(INSTANCE);
    }

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent evt) {
        if (evt.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            cn.academy.ability.vanilla.meltdowner.skill.MdBarrage.serverTick();
        }
    }

    public static final String MSG_BURST = "barrage_burst";

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_BURST, side = LogicalSide.CLIENT)
    private void c_burst(Player owner, Float x, Float y, Float z,
                         Float dx, Float dy, Float dz,
                         Float len, Integer seed, Integer count,
                         Integer flickerInterval, Integer flickerTicks) {
        EntityMdRayBarrage barrage = new EntityMdRayBarrage(owner.level());

        barrage.setSpawner(owner);
        barrage.burst(new Vec3(x, y, z), new Vec3(dx, dy, dz), len, seed, count,
                flickerInterval, flickerTicks);
        cn.academy.client.render.entity.ACEffectEntities.spawn(barrage);

    }

    public static final String MSG_DEFLECTED = "barrage_deflected";

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_DEFLECTED, side = LogicalSide.CLIENT)
    private void c_deflected(Player owner, byte[] raw, Integer life) {
        java.util.List<Vec3> path = cn.academy.util.RayReflect.decodePath(raw);
        if (path == null) {
            return;
        }
        cn.academy.entity.EntityMdRaySmall ray =
                new cn.academy.entity.EntityMdRaySmall(owner.level());
        ray.viewOptimize = false;
        ray.setPath(path);
        if (life > 0) {
            ray.life = life;
        }
        cn.academy.client.render.entity.ACEffectEntities.spawn(ray);
    }
}
