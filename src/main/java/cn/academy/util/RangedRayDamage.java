package cn.academy.util;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.AbilityPipeline;
import cn.academy.event.BlockDestroyEvent;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RangedRayDamage {

    static final double DISK_STEP = 0.5;

    public final Player player;
    public final Level world;
    public Vec3 pos, dir;
    public final AbilityContext ctx;

    public double range;

    public float totalEnergy;
    public int maxIncrement = 50;
    public float dropProb = 0.05f;

    public Predicate<Entity> entitySelector = e -> true;

    public float startDamage = 10.0f;

    public boolean carveBlocks = true;

    public double scanDepth = -1;

    public double minAlong = Double.NEGATIVE_INFINITY;

    public float energyLeft;

    public Predicate<Entity> shouldHit = e -> true;

    public Predicate<Entity> blocksBeam = e -> false;

    public Consumer<Entity> onHit = e -> {};

    private Vec3 start, slope;

    public RangedRayDamage(AbilityContext ctx, double range, float energy) {
        this.ctx = ctx;
        this.pos = ctx.player.position();
        this.dir = ctx.player.getLookAngle();
        this.player = ctx.player;
        this.world = ctx.player.level();
        this.range = range;
        this.totalEnergy = energy;
    }

    private double depth() {
        return scanDepth > 0 ? Math.min(scanDepth, maxIncrement) : maxIncrement;
    }

    public void perform() {
        Set<BlockPos> processed = new HashSet<>();

        energyLeft = totalEnergy;
        start = pos;
        slope = dir;

        Vec3 up = Math.abs(slope.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        final Vec3 vp0 = slope.cross(up).normalize();
        final Vec3 vp1 = slope.cross(vp0).normalize();

        double maxDistance = Double.MAX_VALUE;

        {
            Vec3 v0 = start.add(vp0.scale(-range)).add(vp1.scale(-range));
            Vec3 v1 = start.add(vp0.scale(range)).add(vp1.scale(-range));
            Vec3 v2 = start.add(vp0.scale(range)).add(vp1.scale(range));
            Vec3 v3 = start.add(vp0.scale(-range)).add(vp1.scale(range));
            double scan = depth();
            Vec3 v4 = v0.add(slope.scale(scan));
            Vec3 v5 = v1.add(slope.scale(scan));
            Vec3 v6 = v2.add(slope.scale(scan));
            Vec3 v7 = v3.add(slope.scale(scan));
            AABB aabb = minimumBounds(v0, v1, v2, v3, v4, v5, v6, v7);

            Predicate<Entity> areaSelector = target -> {
                Vec3 dv = target.position().subtract(start);
                if (dv.dot(slope) < minAlong) {
                    return false;
                }
                return dv.cross(slope).length() < range * 1.2;
            };

            List<Entity> targets = new ArrayList<>(
                    world.getEntities(player, aabb, entitySelector.and(areaSelector)
                            .and(t -> AbilityPipeline.canTarget(player, t))));

            targets.sort((lhs, rhs) -> Double.compare(
                    player.distanceToSqr(lhs.position()), player.distanceToSqr(rhs.position())));

            for (Entity e : targets) {
                if (!shouldHit.test(e)) {

                    if (blocksBeam.test(e)) {
                        maxDistance = e.distanceToSqr(player.position());
                        break;
                    }
                    continue;
                }
                if (!attackEntity(e)) {
                    maxDistance = e.distanceToSqr(player.position());
                    break;
                }
                onHit.accept(e);
            }
        }

        if (carveBlocks && ctx.canBreakBlock(world)) {
            float energy = totalEnergy;
            int maxDepth = (int) Math.min(depth(), Math.sqrt(maxDistance));

            depth:
            for (int d = 0; d <= maxDepth && energy > 0; d++) {
                Vec3 center = start.add(slope.scale(d));
                boolean snd = d < 20;
                for (double s = -range; s <= range + 1e-9; s += DISK_STEP) {
                    for (double t = -range; t <= range + 1e-9; t += DISK_STEP) {
                        if (s * s + t * t > range * range) continue;

                        Vec3 p = center.add(vp0.scale(s)).add(vp1.scale(t));

                        BlockPos bp = new BlockPos(
                                (int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
                        if (!processed.add(bp)) continue;

                        float cost = tryDestroy(bp, snd, energy);
                        if (cost < 0) continue;
                        energy -= cost;
                        if (energy <= 0) break depth;

                        if (RandUtils.ranged(0, 1) < 0.05) {
                            Direction dd = Direction.values()[RandUtils.rangei(0, 6)];
                            float c2 = tryDestroy(bp.relative(dd), snd, energy);
                            if (c2 >= 0) energy -= c2;
                        }
                    }
                }
            }
            energyLeft = Math.max(0, energy);
        }
    }

    private static AABB minimumBounds(Vec3... pts) {
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE;
        double x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        for (Vec3 p : pts) {
            x0 = Math.min(x0, p.x); y0 = Math.min(y0, p.y); z0 = Math.min(z0, p.z);
            x1 = Math.max(x1, p.x); y1 = Math.max(y1, p.y); z1 = Math.max(z1, p.z);
        }
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    private float tryDestroy(BlockPos pos, boolean snd, float energy) {
        BlockState state = world.getBlockState(pos);
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0) return -1;
        if (energy < hardness) return -1;
        if (MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(player, pos))) return -1;

        if (!state.isAir()) {

            if (world instanceof net.minecraft.server.level.ServerLevel sl
                    && RandUtils.ranged(0, 1) < dropProb) {
                Block.dropResources(state, sl, pos, world.getBlockEntity(pos), player, player.getMainHandItem());
            }
            if (snd && RandUtils.ranged(0, 1) < 0.1) {
                SoundType st = state.getSoundType(world, pos, player);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        st.getBreakSound(), SoundSource.BLOCKS,
                        (st.getVolume() + 1.0F) / 2.0F, st.getPitch());
            }
        }
        world.removeBlock(pos, false);
        return hardness;
    }

    protected boolean attackEntity(Entity target) {
        Vec3 dv = target.position().subtract(start);

        float dist = (float) Math.min(maxIncrement, dv.cross(slope).length());
        float realDmg = this.startDamage * MathUtils.lerpf(1, 0.2f, dist / maxIncrement);
        return applyAttack(target, realDmg);
    }

    protected boolean applyAttack(Entity target, float damage) {
        ctx.attack(target, damage);
        return true;
    }

    public static class Reflectible extends RangedRayDamage {

        public final java.util.function.BiConsumer<Entity, cn.academy.event.ability.ReflectEvent> callback;

        public Vec3 beamOrigin;

        public double standoff = RayReflect.DEFAULT_STANDOFF;

        public long extendMs = RayReflect.DEFAULT_EXTEND_MS;

        public double beamLength = 0;

        public Reflectible(AbilityContext ctx, double range, float energy,
                           java.util.function.BiConsumer<Entity, cn.academy.event.ability.ReflectEvent> callback) {
            super(ctx, range, energy);
            this.callback = callback;
            this.beamOrigin = ctx.player.getEyePosition(1.0f);
        }

        @Override
        protected boolean applyAttack(Entity target, float damage) {
            boolean[] result = {true};
            ctx.attackReflect(target, damage, this::fillContext, event -> {
                callback.accept(target, event);
                result[0] = false;
            });
            return result[0];
        }

        private void fillContext(cn.academy.event.ability.ReflectEvent event) {
            RayReflect.fill(event, beamOrigin, dir, event.target, standoff, extendMs);
            event.beamLength = beamLength;
        }
    }
}
