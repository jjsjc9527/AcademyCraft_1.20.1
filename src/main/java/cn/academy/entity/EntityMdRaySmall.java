package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMdRaySmall extends EntityRayBase {

    private static final long WIDTH_BLEND = 500;

    private static final double PARTICLE_RANGE = 10;

    public EntityMdRaySmall(Level level) {
        super(ACEntities.MD_RAY_SMALL.get(), level);
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 14;
        this.length = 15.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        Vec3 dir = lookVec();
        Vec3 at = position().add(dir.scale(RandUtils.ranged(0, PARTICLE_RANGE)));
        level().addParticle(cn.academy.ACParticles.MD.get(),
                at.x, at.y, at.z,
                RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015));
    }

    private Vec3 lookVec() {
        float f = getXRot() * Mth.DEG_TO_RAD;
        float f1 = -getYRot() * Mth.DEG_TO_RAD;
        float cosF = Mth.cos(f);
        return new Vec3(Mth.sin(f1) * cosF, -Mth.sin(f), Mth.cos(f1) * cosF);
    }

    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        long lifeMS = getLifeMS();
        if (dt > lifeMS - WIDTH_BLEND) {
            double timeFactor = MathUtils.clampd(0, 1, (double) (dt - (lifeMS - WIDTH_BLEND)) / WIDTH_BLEND);
            return 1 - timeFactor;
        }
        return 1.0;
    }
}
