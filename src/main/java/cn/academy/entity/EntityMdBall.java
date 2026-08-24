package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMdBall extends net.minecraft.world.entity.Entity implements cn.academy.client.render.ACEffect {

    public static final int MAX_TEXTURES = 5;

    public static final double RANGE_FROM = 0.8, RANGE_TO = 1.3;
    public static final double SUB_Y_FROM = -1.2, SUB_Y_TO = 0.2;

    public static final double YAW_SPREAD = Math.PI * 0.45;

    private static final double MAX_ACCEL = 4;

    private static final float BURST_TIME = 0.4f, BLEND_TIME = 0.15f;

    private Player spawner;

    private Vec3 sub = Vec3.ZERO;

    public int life = 20;

    public int texID;
    public float alphaWiggle = 0.8f;
    private double accel;
    private double lastTime;
    public double offsetX, offsetY, offsetZ;

    public final double spawnTime = GameTimer.getPausableTime();

    public EntityMdBall(Level level) {
        super(ACEntities.MD_BALL.get(), level);
        noCulling = true;
    }

    public void init(Player spawner, Vec3 sub, int life) {
        this.spawner = spawner;
        this.sub = sub;
        this.life = life;
        updatePosition();
        xOld = getX();
        yOld = getY();
        zOld = getZ();
    }

    public Player getSpawner() {
        return spawner;
    }

    private void updatePosition() {
        if (spawner == null) return;

        Vec3 p = cn.academy.ability.vanilla.meltdowner.skill.ElectronBomb.ballPos(spawner, sub);
        setPos(p.x, p.y, p.z);
    }

    public double age() {
        return GameTimer.getPausableTime() - spawnTime;
    }

    public boolean expired() {
        return spawner == null || age() > life * 0.05;
    }

    @Override
    public void tick() {
        super.tick();

        updatePosition();
        if (expired()) {
            discard();
        }
    }

    public boolean updateRenderTick() {
        if (spawner == null) return false;

        double time = GameTimer.getPausableTime();

        if (lastTime != 0) {
            double dt = time - lastTime;
            if (random.nextInt(8) < 3) {
                accel = RandUtils.ranged(-MAX_ACCEL, MAX_ACCEL);
            }
            alphaWiggle += (float) (accel * dt);
            if (alphaWiggle > 1) alphaWiggle = 1;
            if (alphaWiggle < 0) alphaWiggle = 0;
        }
        lastTime = time;

        if (random.nextInt(8) < 2) {
            texID = random.nextInt(MAX_TEXTURES);
        }

        float phase = (float) (age() / 0.3f);
        offsetX = 0.03 * Mth.sin(phase);
        offsetZ = 0.03 * Mth.cos(phase);
        offsetY = 0.04 * Mth.cos((float) (phase * 1.4 + Math.PI / 3.5));

        updatePosition();
        return true;
    }

    public float getAlpha() {
        float lifeS = life * 0.05f;
        float dt = (float) age();

        if (dt > lifeS - BLEND_TIME) {
            return Math.max(0, MathUtils.lerpf(1, 0, (dt - (lifeS - BLEND_TIME)) / BLEND_TIME));
        }
        if (dt > lifeS - BURST_TIME) {
            return MathUtils.lerpf(0.6f, 1.0f, (dt - (lifeS - BURST_TIME)) / (BURST_TIME - BLEND_TIME));
        }
        if (dt < 0.3f) {
            return MathUtils.lerpf(0, 0.6f, dt / 0.3f);
        }
        return 0.6f;
    }

    public float getSize() {
        float lifeS = life * 0.05f;
        float dt = (float) age();

        if (dt > lifeS - 0.1f) {
            return Math.max(0, MathUtils.lerpf(1.5f, 0, (dt - (lifeS - 0.1f)) / 0.1f));
        }
        if (dt > lifeS - 0.3f) {
            return MathUtils.lerpf(1, 1.5f, (dt - (lifeS - 0.3f)) / 0.2f);
        }
        return 1;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return (now - spawnTime) * 20.0 > life + 5;
    }
}
