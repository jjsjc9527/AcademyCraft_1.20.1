package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMDRay extends EntityRayBase {

    private static final double PARTICLE_RANGE = 10;

    private static final double PARTICLE_CHANCE = 0.8;

    public net.minecraft.core.Direction gravAtFire = net.minecraft.core.Direction.DOWN;
    public float localYawAtFire = 0;

    public EntityMDRay(Level level) {
        super(ACEntities.MD_RAY.get(), level);
        this.blendInTime = 200;
        this.blendOutTime = 700;
        this.life = 50;
        this.length = 30;
        noCulling = true;
    }

    public void aimFromPlayer(Player player, double len) {
        rememberCaster(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        setPos(eye.x, eye.y, eye.z);
        double dxz = Math.sqrt(look.x * look.x + look.z * look.z);
        setYRot((float) (-Math.atan2(look.x, look.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(look.y, dxz) * 180 / Math.PI));
        this.length = len;
        xOld = getX();
        yOld = getY();
        zOld = getZ();
    }

    public void bendAlong(Player player, java.util.List<Vec3> path) {
        rememberCaster(player);
        setPath(path);
    }

    private void rememberCaster(Player player) {
        setSpawner(player);
        this.gravAtFire = cn.academy.gravity.ACGravity.getGravityDirection(player);
        this.localYawAtFire = player.getYRot();
    }

    public void aimAt(Vec3 from, Vec3 dir, double len) {
        Vec3 d = dir.normalize();
        setPos(from.x, from.y, from.z);
        double dxz = Math.sqrt(d.x * d.x + d.z * d.z);
        setYRot((float) (-Math.atan2(d.x, d.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(d.y, dxz) * 180 / Math.PI));
        this.length = len;
        xOld = getX();
        yOld = getY();
        zOld = getZ();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        if (RandUtils.nextDouble() >= PARTICLE_CHANCE) {
            return;
        }
        Vec3 dir = lookVec();
        Vec3 at = position().add(dir.scale(RandUtils.ranged(0, PARTICLE_RANGE)));
        level().addParticle(cn.academy.ACParticles.MD.get(),
                at.x, at.y, at.z,
                RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03));
    }

    private Vec3 lookVec() {
        float f = getXRot() * Mth.DEG_TO_RAD;
        float f1 = -getYRot() * Mth.DEG_TO_RAD;
        float cosF = Mth.cos(f);
        return new Vec3(Mth.sin(f1) * cosF, -Mth.sin(f), Mth.cos(f1) * cosF);
    }
}
