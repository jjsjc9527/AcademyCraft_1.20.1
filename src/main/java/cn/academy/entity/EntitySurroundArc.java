package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.academy.client.render.util.CubePointFactory;
import cn.academy.client.render.util.IPointFactory;
import cn.academy.client.render.util.SubArcHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntitySurroundArc extends Entity implements cn.academy.client.render.ACEffect {

    static {
        ArcFactory factory = new ArcFactory();
        factory.widthShrink = 0.9;
        factory.maxOffset = 0.8;
        factory.passes = 3;
        factory.width = 0.2;
        factory.branchFactor = 0.7;

        ArcType.THIN.templates = factory.generateList(10, 1.5, 2);

        factory.width = 0.3;
        ArcType.NORMAL.templates = factory.generateList(10, 3, 4);

        factory.passes = 3;
        factory.width = 0.35;
        factory.maxOffset = 1.2;
        factory.branchFactor = 0.45;
        ArcType.BOLD.templates = factory.generateList(10, 3.5, 4.5);
    }

    public enum ArcType {
        THIN(4), NORMAL(6), BOLD(5);

        public Arc[] templates;
        public int count;

        ArcType(int count) {
            this.count = count;
        }
    }

    private ArcType arcType = ArcType.BOLD;
    private final PosObject pos;

    public boolean draw = true;

    public int life = 100;

    public final double spawnTime = cn.lambdalib2.util.GameTimer.getPausableTime();

    @Override
    public boolean effectExpired(double now) {
        return life >= 0 && (now - spawnTime) * 20.0 > life + 5;
    }

    private int discardCountdown = -1;

    private SubArcHandler arcHandler;

    protected boolean autoGenerate = true;

    private final IPointFactory pointFactory;

    public EntitySurroundArc(Entity follow) {
        this(follow, 1.3);
    }

    public EntitySurroundArc(Entity follow, double sizeMultiplyer) {
        super(ACEntities.SURROUND_ARC.get(), follow.level());
        pos = new EntityPos(follow);
        setPos(follow.getX(), follow.getY(), follow.getZ());
        pointFactory = new CubePointFactory(
                follow.getBbWidth() * sizeMultiplyer,
                follow.getBbHeight() * sizeMultiplyer,
                follow.getBbWidth() * sizeMultiplyer).setCentered(true);
    }

    public EntitySurroundArc(Level level, double x, double y, double z, double wl, double h) {
        super(ACEntities.SURROUND_ARC.get(), level);
        setPos(x, y, z);
        pos = new ConstPos(x, y, z);
        pointFactory = new CubePointFactory(wl, h, wl).setCentered(true);
    }

    public void updatePos(double x, double y, double z) {
        pos.x = x;
        pos.y = y;
        pos.z = z;
    }

    @Nullable
    public Entity getFollowEntity() {
        return pos instanceof EntityPos ep ? ep.entity : null;
    }

    public EntitySurroundArc setArcType(ArcType type) {
        arcType = type;
        return this;
    }

    public EntitySurroundArc setLife(int life) {
        this.life = life;
        return this;
    }

    public void discardAfter(int ticks) {
        discardCountdown = ticks;
    }

    public SubArcHandler getArcHandler() {
        return arcHandler;
    }

    private void firstUpdate() {
        arcHandler = new SubArcHandler(arcType.templates);
        arcHandler.frameRate = 0.6;
        arcHandler.switchRate = 0.7;

        if (autoGenerate) doGenerate();
    }

    private void doGenerate() {
        for (int i = 0; i < arcType.count; ++i) {
            arcHandler.generateAt(pointFactory.next());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (arcHandler == null) {
            firstUpdate();
        }

        if (autoGenerate && arcHandler.isEmpty()) {
            doGenerate();
        }

        arcHandler.tick();

        pos.tick();
        setPos(pos.x, pos.y, pos.z);
        setYRot(pos.yaw);
        setXRot(pos.pitch);

        if (discardCountdown > 0 && --discardCountdown == 0) {
            discard();
        }

        if (tickCount >= life) {
            discard();
        }
    }

    private abstract static class PosObject {
        double x, y, z;
        float yaw, pitch;

        void tick() {}
    }

    private static class EntityPos extends PosObject {

        final Entity entity;
        final boolean isPlayer;

        EntityPos(Entity e) {
            entity = e;
            isPlayer = e instanceof Player && e.equals(Minecraft.getInstance().player);
        }

        @Override
        void tick() {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            yaw = entity instanceof LivingEntity ? ((LivingEntity) entity).getYHeadRot() : entity.getYRot();
            pitch = entity.getXRot();
        }
    }

    private static class ConstPos extends PosObject {

        ConstPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
