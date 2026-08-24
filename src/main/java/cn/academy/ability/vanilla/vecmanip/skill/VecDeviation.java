package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.config.AbilityConfig;
import cn.academy.event.ability.ReflectEvent;
import cn.academy.util.ACPierce;
import cn.academy.util.AimTrace;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import cn.academy.util.ACDefense;

public class VecDeviation extends Skill {

    public static final VecDeviation INSTANCE = new VecDeviation();

    static final double RANGE = 4;

    private static final double MIN_SPEED = 0.1;

    public enum Mode {

        SCATTER,

        RETURN;

        public Mode next() {
            return this == SCATTER ? RETURN : SCATTER;
        }
    }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> ANTIREF =
            net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    new net.minecraft.resources.ResourceLocation("academy", "antiref"));

    public static boolean isAntiReflect(net.minecraft.world.damagesource.DamageSource src) {
        return src != null && src.is(ANTIREF);
    }

    public static Vec3 hitPointOf(ReflectEvent event, LivingEntity defender) {
        return event.isRay() ? event.hitPos : defender.getEyePosition(1.0f);
    }

    public static Vec3 incomingOf(ReflectEvent event) {
        return event.isRay() ? event.incomingDir : event.player.getLookAngle();
    }

    public static Vec3 rayReflectDir(ReflectEvent event, LivingEntity defender, Mode mode) {
        if (mode == Mode.RETURN) {
            Vec3 back = event.player.getEyePosition(1.0f).subtract(hitPointOf(event, defender));
            return back.lengthSqr() < 1.0e-6 ? defender.getLookAngle() : back.normalize();
        }
        return cn.academy.util.RayReflect.mirror(incomingOf(event), defender.getLookAngle());
    }

    public VecDeviation() {
        super("vec_deviation", 4);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(VecDeviation.class);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {

                Optional<DeviationContext> opt = ContextManager.instance.findLocal(DeviationContext.class);
                if (opt.isPresent()) {
                    opt.get().terminate();
                } else {
                    ContextManager.instance.activate(new DeviationContext(getPlayer()));
                }
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {

        if (isAntiReflect(event.getSource())) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || !VMEntityAffection.isMarked(direct)) {
            return;
        }
        UUID reflector = VMEntityAffection.getReflector(direct);
        if (reflector != null && reflector.equals(event.getEntity().getUUID())) {
            ACDefense.block(event);
        }
    }

    public static class DeviationContext extends Context<VecDeviation> {

        static final String MSG_REFLECT = "reflect";
        static final String MSG_MODE = "mode";
        static final String MSG_MELEE = "melee";

        static final String MSG_RAY = "ray";

        static final String MSG_SONIC = "sonic";

        private final float exp = ctx.getSkillExp();

        private Mode mode = Mode.SCATTER;

        private boolean modeLocked = false;

        private static final int HURT_COOLDOWN = 20;

        private long lastBlockTime = -HURT_COOLDOWN;

        private boolean reflecting = false;

        private static boolean REFLECT_CHAIN = false;

        private static final java.util.Map<java.util.UUID, Long> REFLECT_AT =
                new java.util.concurrent.ConcurrentHashMap<>();

        private static final long PAIR_CD = 5L;

        private float pendingExplosionKb = 0;

        private Entity pendingExplosionSource = null;

        private long lastNoCpWarn = Long.MIN_VALUE / 2;

        private final java.util.List<float[]> pendingFx = new java.util.ArrayList<>();

        private final java.util.List<PendingSonic> pendingSonic = new java.util.ArrayList<>();

        private Entity nonDeflectableSrc;
        private long nonDeflectableTick = Long.MIN_VALUE;

        public DeviationContext(Player player) {
            super(player, INSTANCE);
        }

        public Mode getMode() {
            return mode;
        }

        public void lockMode(Mode m) {
            mode = m;
            modeLocked = true;
        }

        public void unlockMode() {
            modeLocked = false;
        }

        public boolean isModeLocked() {
            return modeLocked;
        }

        @Listener(channel = MSG_MODE, side = LogicalSide.SERVER)
        private void s_setMode(int ordinal) {
            if (modeLocked) {
                return;
            }
            Mode[] values = Mode.values();
            if (ordinal >= 0 && ordinal < values.length) {
                mode = values[ordinal];
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            MinecraftForge.EVENT_BUS.register(this);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }

        @SubscribeEvent
        public void onReflect(ReflectEvent event) {
            if (mode == null || event.target != player) return;
            if (event.player == player) return;

            float diff = event.difficulty;
            if (!ctx.consume(diff * AbilityConfig.stat("vec_deviation", "ray_overload", exp),
                    diff * AbilityConfig.stat("vec_deviation", "ray_cp", exp))) {
                warnNoCp();

                if (!event.deflectable) {
                    nonDeflectableSrc = event.player;
                    nonDeflectableTick = player.level().getGameTime();
                }
                return;
            }

            Vec3 hit = hitPointOf(event, player);
            Vec3 incoming = incomingOf(event);

            if (!event.deflectable) {
                event.setCanceled(true);
                ctx.addSkillExp(0.004f);
                blockedFx(event, hit);
                return;
            }

            event.reflectDir = rayReflectDir(event, player, mode);

            event.returnToCaster = mode == Mode.RETURN;

            event.setCanceled(true);
            ctx.addSkillExp(0.004f);
            blockedFx(event, hit);
        }

        private void blockedFx(ReflectEvent event, Vec3 hit) {

            Vec3 incoming = event.incomingDir != null
                    ? event.incomingDir : player.getLookAngle().scale(-1);
            if (event.arriveDelay <= 0) {

                sendToClient(MSG_RAY, hit, incoming);
            } else {
                pendingFx.add(new float[]{
                        event.arriveDelay, (float) hit.x, (float) hit.y, (float) hit.z,
                        (float) incoming.x, (float) incoming.y, (float) incoming.z});
            }
        }

        @SubscribeEvent
        public void onLivingAttack(LivingAttackEvent event) {
            if (mode == null || reflecting || event.getEntity() != player) return;

            if (isAntiReflect(event.getSource())) return;

            long now = player.level().getGameTime();
            if (now - lastBlockTime < HURT_COOLDOWN) {
                ACDefense.block(event);
                return;
            }

            float dmg = event.getAmount();

            float kb = pendingExplosionKb > 0 ? pendingExplosionKb
                    : estimateKnockback(event.getSource());

            if (applicable(event.getEntity(), event.getSource(), event.getAmount())
                    && tryMeleeReflect(event.getSource(), dmg, ratio() * dmg)) {
                lastBlockTime = now;
                releaseKnockback(event.getSource(), kb);
                ACDefense.block(event);
                return;
            }

            if (tryFallbackBlock(event.getSource(), dmg, kb)) {
                lastBlockTime = now;
                ACDefense.block(event);
            }
        }

        private static final java.lang.reflect.Field EXPLOSION_RADIUS = findExplosionRadius();

        private static java.lang.reflect.Field findExplosionRadius() {
            try {
                return net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(
                        net.minecraft.world.level.Explosion.class, "f_46017_");
            } catch (Throwable t) {
                return null;
            }
        }

        @SubscribeEvent
        public void onExplosionDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
            if (mode == null || reflecting || EXPLOSION_RADIUS == null) return;
            if (event.getLevel() != player.level()) return;
            java.util.List<Entity> list = event.getAffectedEntities();
            if (!list.contains(player)) return;

            net.minecraft.world.level.Explosion explosion = event.getExplosion();

            if (isAntiReflect(explosion.getDamageSource())) return;
            float radius;
            try {
                radius = EXPLOSION_RADIUS.getFloat(explosion);
            } catch (Throwable t) {
                return;
            }
            float f2 = radius * 2.0f;
            if (f2 <= 0) return;

            Vec3 center = explosion.getPosition();
            double d12 = Math.sqrt(player.distanceToSqr(center)) / f2;
            if (d12 > 1.0) return;

            double d5 = player.getX() - center.x;
            double d7 = player.getEyeY() - center.y;
            double d9 = player.getZ() - center.z;
            double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
            if (d13 == 0.0) return;
            d5 /= d13;
            d7 /= d13;
            d9 /= d13;

            double seen = net.minecraft.world.level.Explosion.getSeenPercent(center, player);
            double d10 = (1.0 - d12) * seen;
            float dmg = (float) ((int) ((d10 * d10 + d10) / 2.0 * 7.0 * f2 + 1.0));
            double d11 = net.minecraft.world.item.enchantment.ProtectionEnchantment
                    .getExplosionKnockbackAfterDampener(player, d10);
            Vec3 kbVec = new Vec3(d5 * d11, d7 * d11, d9 * d11);

            list.remove(player);

            boolean tookDamage;
            pendingExplosionKb = (float) kbVec.length();

            pendingExplosionSource = explosion.getDirectSourceEntity();
            try {
                tookDamage = player.hurt(explosion.getDamageSource(), dmg);
            } finally {
                pendingExplosionKb = 0;
                pendingExplosionSource = null;
            }

            if (tookDamage) {

                player.setDeltaMovement(player.getDeltaMovement().add(kbVec));
                explosion.getHitPlayers().put(player, kbVec);
            }

        }

        private float estimateKnockback(net.minecraft.world.damagesource.DamageSource src) {
            Entity attacker = src.getEntity();
            if (attacker == null || attacker == player) {
                return 0;
            }
            float kb = 0.4f;
            if (attacker instanceof LivingEntity le) {
                int bonus = net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus(le);

                if (le instanceof Player p && p.isSprinting()) {
                    bonus++;
                }
                kb += bonus * 0.5f;
            }
            return kb;
        }

        private void releaseKnockback(net.minecraft.world.damagesource.DamageSource src, float kb) {
            if (kb <= 0) {
                return;
            }
            if (mode == Mode.RETURN && src.getEntity() instanceof LivingEntity attacker
                    && attacker != player && attacker.isAlive() && ctx.canTarget(attacker)) {
                attacker.knockback(kb,
                        player.getX() - attacker.getX(),
                        player.getZ() - attacker.getZ());
                return;
            }
            sonicRelease(0, kb);
        }

        private void scatterTo(float dmg) {
            LivingEntity victim = pickScatterTarget();
            if (victim != null) {
                hurtTransfer(victim, dmg);
            }
        }

        private void sonicRelease(float dmg) {
            sonicRelease(dmg, 0f);
        }

        private void sonicRelease(float dmg, float knockback) {
            Vec3 origin = player.getEyePosition(1.0f);

            LivingEntity aimed = mode == Mode.RETURN ? pickNearestTarget(sonicLockRange()) : null;
            if (aimed != null) {
                sonicLockOn(origin, aimed, dmg, knockback);
                return;
            }

            double range = AbilityConfig.stat("vec_deviation", "scatter_range", exp);
            Vec3 dir = randomDir();
            Vec3 end = origin.add(dir.scale(range));

            net.minecraft.world.phys.BlockHitResult block = player.level().clip(
                    new net.minecraft.world.level.ClipContext(origin, end,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (block.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                end = block.getLocation();
            }

            net.minecraft.world.phys.EntityHitResult hit = AimTrace.firstResult(
                    player.level(), player, origin, end,
                    e -> e.isAlive() && e instanceof LivingEntity && sonicCanTarget(e));
            if (hit != null && hit.getEntity() instanceof LivingEntity victim) {

                Vec3 at = hit.getLocation();
                end = at.distanceToSqr(origin) < 1.0e-6 ? victim.getEyePosition(1.0f) : at;
                if (dmg > 0) {
                    hurtTransfer(victim, dmg);
                }
                if (knockback > 0) {

                    victim.knockback(knockback,
                            origin.x - victim.getX(),
                            origin.z - victim.getZ());
                }
            }

            sendToClient(MSG_SONIC, origin, end);
        }

        private static final double SONIC_SPEED = 6.0;

        private static int sonicTravelTicks(double len) {
            return (int) Math.round(len / SONIC_SPEED);
        }

        private void sonicLockOn(Vec3 origin, LivingEntity target, float dmg, float knockback) {
            Vec3 at = target.getEyePosition(1.0f);
            sendToClient(MSG_SONIC, origin, at);
            if (dmg <= 0 && knockback <= 0) {
                return;
            }
            int delay = sonicTravelTicks(origin.distanceTo(at));
            if (delay <= 0) {
                sonicLand(target, dmg, knockback, origin);
            } else {
                pendingSonic.add(new PendingSonic(delay, target, dmg, knockback, origin));
            }
        }

        private void sonicLand(LivingEntity target, float dmg, float kb, Vec3 from) {
            if (target.isRemoved() || !target.isAlive() || target.level() != player.level()) {
                return;
            }
            if (dmg > 0) {
                hurtTransfer(target, dmg);
            }
            if (kb > 0) {

                target.knockback(kb, from.x - target.getX(), from.z - target.getZ());
            }
        }

        private static final class PendingSonic {
            int ticks;
            final LivingEntity target;
            final float dmg;
            final float kb;
            final Vec3 from;

            PendingSonic(int ticks, LivingEntity target, float dmg, float kb, Vec3 from) {
                this.ticks = ticks;
                this.target = target;
                this.dmg = dmg;
                this.kb = kb;
                this.from = from;
            }
        }

        private Vec3 randomDir() {
            var rnd = player.getRandom();
            double z = rnd.nextDouble() * 2 - 1;
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(Math.max(0, 1 - z * z));
            return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
        }

        private boolean sonicCanTarget(Entity e) {
            return e != pendingExplosionSource && ctx.canTarget(e);
        }

        private boolean sonicIsHostile(Entity e) {
            if (!sonicCanTarget(e)) {
                return false;
            }
            if (e instanceof net.minecraft.world.entity.player.Player
                    || e instanceof net.minecraft.world.entity.monster.Enemy) {
                return true;
            }
            return e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player;
        }

        private double sonicLockRange() {
            return AbilityConfig.stat("vec_deviation", "sonic_lock_range", exp);
        }

        private LivingEntity pickNearestTarget(double range) {
            Vec3 eye = player.getEyePosition(1.0f);
            LivingEntity best = null;
            double bestSq = Double.MAX_VALUE;
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(range),
                    e -> e != player && e.isAlive() && sonicIsHostile(e))) {
                double d = e.getEyePosition(1.0f).distanceToSqr(eye);
                if (d < bestSq) {
                    bestSq = d;
                    best = e;
                }
            }
            return best;
        }

        private LivingEntity pickScatterTarget() {
            double r = AbilityConfig.stat("vec_deviation", "scatter_range", exp);
            java.util.List<LivingEntity> pool = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(r),
                    e -> e != player && e.isAlive() && ctx.canTarget(e));
            return pool.isEmpty() ? null : pool.get(player.getRandom().nextInt(pool.size()));
        }

        private void hurtTransfer(LivingEntity victim, float dmg) {
            reflecting = true;
            try {

                ctx.attackPierce(victim, dmg, ACPierce.VEC_REFLECT_PIERCE);
            } finally {
                reflecting = false;
            }
            sendToClient(MSG_MELEE, victim);
        }

        private boolean tryFallbackBlock(net.minecraft.world.damagesource.DamageSource src, float dmg,
                                         float kb) {
            if (mode == null || reflecting) return false;

            if (dmg <= 0 || !Float.isFinite(dmg) || ACDefense.isInstakill(src)) return false;

            Entity from = src.getDirectEntity();
            if (from == null || from == player) {
                from = src.getEntity();
            }

            if (from == null || src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                if (!ctx.consume(dmg * AbilityConfig.stat("vec_deviation", "block_overload", exp),
                        dmg * AbilityConfig.stat("vec_deviation", "block_cp", exp))) {
                    warnNoCp();
                    return false;
                }
                ctx.addSkillExp(dmg * 0.0004f);
                sonicRelease(dmg, kb);
                return true;
            }

            if (from == player) return false;

            if (!ctx.consume(dmg * AbilityConfig.stat("vec_deviation", "block_overload", exp),
                    dmg * AbilityConfig.stat("vec_deviation", "block_cp", exp))) {
                warnNoCp();
                return false;
            }
            ctx.addSkillExp(dmg * 0.0004f);

            if (mode == Mode.SCATTER) {
                scatterTo(dmg);
            }

            releaseKnockback(src, kb);

            Vec3 me = player.getEyePosition(1.0f);
            Vec3 incoming = me.subtract(from.getEyePosition(1.0f));
            if (incoming.lengthSqr() < 1.0e-6) {
                incoming = player.getLookAngle().scale(-1);
            }
            sendToClient(MSG_RAY, me, incoming.normalize());
            return true;
        }

        private float ratio() {
            return AbilityConfig.stat("vec_deviation", "melee_reflect_ratio", exp);
        }

        private boolean applicable(Entity victim, net.minecraft.world.damagesource.DamageSource src, float amount) {
            if (mode != Mode.RETURN) return false;
            if (reflecting) return false;
            if (victim != player) return false;

            if (amount <= 0 || !Float.isFinite(amount) || ACDefense.isInstakill(src)) return false;
            Entity attacker = attackerOf(src);
            if (attacker == null) return false;

            return !(attacker == nonDeflectableSrc
                    && player.level().getGameTime() == nonDeflectableTick);
        }

        private void warnNoCp() {
            long now = player.level().getGameTime();
            if (now - lastNoCpWarn < 40) return;
            lastNoCpWarn = now;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.academy.vec_deviation.no_cp"), true);
        }

        private Entity attackerOf(net.minecraft.world.damagesource.DamageSource src) {

            if (src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                return null;
            }

            Entity direct = src.getDirectEntity();
            Entity attacker = src.getEntity();
            if (direct == null || direct != attacker) {
                return null;
            }
            return attacker == player ? null : attacker;
        }

        private boolean tryMeleeReflect(net.minecraft.world.damagesource.DamageSource src,
                                        float dmg, float reflect) {
            Entity attacker = attackerOf(src);
            if (attacker == null) return false;

            if (REFLECT_CHAIN) {
                return false;
            }

            long now = player.level().getGameTime();
            Long last = REFLECT_AT.get(player.getUUID());
            if (last != null) {
                long d = now - last;
                if (d >= 0 && d < PAIR_CD) {
                    return false;
                }
            }

            if (!ctx.consume(dmg * AbilityConfig.stat("vec_deviation", "melee_overload", exp),
                    dmg * AbilityConfig.stat("vec_deviation", "melee_cp", exp))) {
                warnNoCp();
                return false;
            }

            boolean untouchable = attacker instanceof net.minecraft.world.entity.player.Player ap
                    && (ap.getAbilities().invulnerable || ap.isSpectator());

            reflecting = true;
            REFLECT_CHAIN = true;
            try {
                if (!untouchable) {

                    ctx.attackPierce(attacker, reflect, ACPierce.VEC_REFLECT_PIERCE);
                }
            } finally {
                reflecting = false;
                REFLECT_CHAIN = false;
            }

            REFLECT_AT.put(player.getUUID(), now);

            ctx.addSkillExp(dmg * 0.0004f);
            sendToClient(MSG_MELEE, attacker);
            return true;
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {

            if (!pendingFx.isEmpty()) {
                for (java.util.Iterator<float[]> it = pendingFx.iterator(); it.hasNext(); ) {
                    float[] f = it.next();
                    if (--f[0] <= 0) {
                        it.remove();
                        sendToClient(MSG_RAY, new Vec3(f[1], f[2], f[3]), new Vec3(f[4], f[5], f[6]));
                    }
                }
            }

            if (!pendingSonic.isEmpty()) {
                for (java.util.Iterator<PendingSonic> it = pendingSonic.iterator(); it.hasNext(); ) {
                    PendingSonic s = it.next();
                    if (--s.ticks <= 0) {
                        it.remove();
                        sonicLand(s.target, s.dmg, s.kb, s.from);
                    }
                }
            }

            if (!ctx.cpData.canUseAbility()) {
                terminate();
                return;
            }

            Vec3 center = player.getBoundingBox().getCenter();
            AABB box = player.getBoundingBox().inflate(RANGE);

            double budget = AbilityConfig.stat("vec_deviation", "reflect_budget", exp);
            double spent = 0;
            for (Entity e : player.level().getEntities(player, box, VMEntityAffection::canAffect)) {
                if (VMEntityAffection.isMarked(e)) continue;

                if (VMEntityAffection.ownerOf(e) == player) continue;
                if (e.position().distanceToSqr(center) > RANGE * RANGE) continue;

                if (e.getDeltaMovement().lengthSqr() < MIN_SPEED * MIN_SPEED) continue;

                float diff = VMEntityAffection.difficulty(e);

                float billable = (float) Math.max(0, Math.min(diff, budget - spent));
                if (billable > 0) {

                    if (!ctx.consume(billable * AbilityConfig.overload("vec_deviation", exp),
                            billable * AbilityConfig.cp("vec_deviation", exp))) {
                        warnNoCp();
                        continue;
                    }
                    spent += billable;
                }

                Vec3 incoming = e.getDeltaMovement().normalize();
                reflect(e);
                ctx.addSkillExp(diff * 0.0008f);
                sendToClient(MSG_REFLECT, e, incoming);
            }
        }

        private void reflect(Entity target) {

            if (target instanceof cn.academy.entity.EntityShiftNeedle needle) {
                Player caster = needle.getOwnerPlayer();
                boolean chaseBack = mode == Mode.RETURN && caster != null && caster != player;
                needle.deflect(reflectDirFor(needle, caster), player, chaseBack ? caster : null);
                VMEntityAffection.mark(needle, player.getUUID());
                return;
            }
            if (target instanceof cn.academy.entity.EntityShiftBlock block) {

                Player shooter = block.getOwnerPlayer();
                boolean chaseBack = mode == Mode.RETURN && shooter != null && shooter != player;
                block.deflect(reflectDirFor(block, shooter), player, chaseBack ? shooter : null);
                VMEntityAffection.mark(block, player.getUUID());
                return;
            }
            reflectProjectile((Projectile) target);
        }

        private Vec3 reflectDirFor(Entity target, Entity shooter) {
            if (mode == Mode.RETURN && shooter != null && shooter != player) {
                return shooter.getEyePosition(1.0f).subtract(target.position()).normalize();
            }
            return mirrorReflect(target.getDeltaMovement(), player.getLookAngle());
        }

        private void reflectProjectile(Projectile proj) {
            Entity shooter = proj.getOwner();
            Vec3 dir = reflectDirFor(proj, shooter);

            double speed = AbilityConfig.stat("vec_deviation", "reflect_speed", exp);
            proj.setDeltaMovement(dir.scale(speed));
            proj.hurtMarked = true;

            if (proj instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile fireball) {
                fireball.xPower = dir.x * 0.1;
                fireball.yPower = dir.y * 0.1;
                fireball.zPower = dir.z * 0.1;
            }

            double horiz = dir.horizontalDistance();
            proj.setYRot((float) (Mth.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
            proj.setXRot((float) (Mth.atan2(dir.y, horiz) * (180.0 / Math.PI)));
            proj.yRotO = proj.getYRot();
            proj.xRotO = proj.getXRot();

            if (VMEntityAffection.shouldTransferOwner(proj)) {
                proj.setOwner(player);
            }
            VMEntityAffection.mark(proj, player.getUUID());
        }

        static Vec3 mirrorReflect(Vec3 incoming, Vec3 normal) {

            return cn.academy.util.RayReflect.mirror(incoming, normal);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(DeviationContext.class)
    public static class DeviationContextC extends ClientContext {

        private final DeviationContext par;
        private ClientRuntime.IActivateHandler activateHandler;

        private static final int MAX_FX_PER_TICK = 8;

        private static final double SONIC_STEP = 3.0;
        private static final double SONIC_NEAR_CLIP = 5.0;
        private static final int SONIC_MAX_PARTICLES = 32;

        private static final double SONIC_DELAY = 0.5;

        private static final double SONIC_FX_SCALE = 2.0;

        private long fxTick = Long.MIN_VALUE;
        private int fxCount = 0;

        public DeviationContextC(DeviationContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_madeAlive() {
            if (!isLocal()) return;

            par.sendToServer(DeviationContext.MSG_MODE, ModeKeyHandler.mode().ordinal());

            activateHandler = new ClientRuntime.IActivateHandler() {
                @Override
                public boolean handles(Player p) {
                    return par.getStatus() == Context.Status.ALIVE;
                }

                @Override
                public void onKeyDown(Player p) {
                    par.terminate();

                    cn.academy.datapart.CPData.get(p).setActivateState(false,
                            cn.academy.datapart.AbilityToggleSource.SKILL_KEY);
                }

                @Override
                public String getHint() {
                    return "deactivate";
                }
            };
            ClientRuntime.instance().addActivateHandler(activateHandler);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            if (!isLocal()) return;
            if (activateHandler != null) {

            if (!ClientRuntime.available()) return;
                ClientRuntime.instance().removeActiveHandler(activateHandler);
                activateHandler = null;
            }
        }

        @Listener(channel = DeviationContext.MSG_REFLECT, side = LogicalSide.CLIENT)
        private void c_reflect(Entity proj, Vec3 incoming) {
            if (proj == null) return;

            spawnBlockWave(proj.getEyePosition(1.0f), incoming, 0.6, 8);
        }

        @Listener(channel = DeviationContext.MSG_MELEE, side = LogicalSide.CLIENT)
        private void c_melee(Entity attacker) {
            if (attacker == null) return;
            Vec3 at = attacker.getEyePosition(1.0f);
            Vec3 incoming = player.getEyePosition(1.0f).subtract(at);
            spawnBlockWave(at, incoming, 1.1, 12);
        }

        @Listener(channel = DeviationContext.MSG_RAY, side = LogicalSide.CLIENT)
        private void c_rayReflected(Vec3 at, Vec3 incoming) {
            if (at == null) return;
            spawnBlockWave(at, incoming, 1.6, 20);
        }

        @Listener(channel = DeviationContext.MSG_SONIC, side = LogicalSide.CLIENT)
        private void c_sonic(Vec3 from, Vec3 to) {
            if (from == null || to == null) return;
            Vec3 seg = to.subtract(from);
            double len = seg.length();
            if (len < 1.0e-4) return;
            Vec3 dir = seg.scale(1.0 / len);

            double start = Math.min(SONIC_NEAR_CLIP, len);
            int n = (int) Math.min(SONIC_MAX_PARTICLES, Math.max(1, (len - start) / SONIC_STEP));
            for (int i = 0; i <= n; i++) {
                Vec3 p = from.add(dir.scale(start + (len - start) * i / n));

                player.level().addParticle(cn.academy.ACParticles.SONIC_WAVE.get(),
                        p.x, p.y, p.z, i * SONIC_DELAY,
                        cn.academy.client.render.misc.SonicWaveParticle.SIZE, 0);
            }

            player.level().playLocalSound(from.x, from.y, from.z, ACSounds.VM_VEC_REFLECTION.get(),
                    SoundSource.AMBIENT, 0.5f, 1.0f, false);
        }

        private void spawnBlockWave(Vec3 at, Vec3 incoming, double size, int sparks) {

            long now = player.level().getGameTime();
            if (now != fxTick) {
                fxTick = now;
                fxCount = 0;
            }
            fxCount++;
            if (fxCount > MAX_FX_PER_TICK) {
                return;
            }

            Vec3 n = incoming == null || incoming.lengthSqr() < 1.0e-6
                    ? player.getLookAngle().scale(-1) : incoming.normalize().scale(-1);

            player.level().addParticle(cn.academy.ACParticles.SONIC_WAVE.get(),
                    at.x, at.y, at.z, 0, size * SONIC_FX_SCALE, 0);

            Vec3 up = Math.abs(n.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 u = n.cross(up).normalize();
            Vec3 w = n.cross(u).normalize();
            double phase = player.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < sparks; i++) {
                double a = phase + i * Math.PI * 2 / sparks;
                Vec3 out = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
                Vec3 vel = out.scale(0.35).add(n.scale(0.12));
                player.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        at.x, at.y, at.z, vel.x, vel.y, vel.z);
            }

            if (fxCount == 1) {
                player.level().playLocalSound(at.x, at.y, at.z, ACSounds.VM_VEC_REFLECTION.get(),
                        SoundSource.AMBIENT, 0.5f, 1.0f, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class ModeKeyHandler {

        private static final String KEY_GROUP = "VM_DeviationMode";

        private static Mode clientMode = Mode.SCATTER;

        public static Mode mode() {
            return clientMode;
        }

        public static void init() {
            MinecraftForge.EVENT_BUS.register(new ModeKeyHandler());
        }

        @SubscribeEvent
        public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

            Player p = net.minecraft.client.Minecraft.getInstance().player;

            if (p == null) {
                return;
            }

            boolean want = ContextManager.instance.findLocal(DeviationContext.class)
                    .filter(c -> !c.isModeLocked()).isPresent();

            if (!ClientRuntime.available()) {
                return;
            }
            ClientRuntime rt = ClientRuntime.instance();

            boolean has = !rt.getDelegates(KEY_GROUP).isEmpty();
            if (want == has) {
                return;
            }
            if (want) {
                rt.addKey(KEY_GROUP, cn.lambdalib2.input.KeyManager.MOUSE_MIDDLE, new ModeDelegate());
            } else {
                rt.clearKeys(KEY_GROUP);
            }
        }

        private static final class ModeDelegate extends KeyDelegate {
            @Override
            public void onKeyDown() {
                clientMode = clientMode.next();

                ContextManager.instance.findLocal(DeviationContext.class)
                        .ifPresent(c -> c.sendToServer(DeviationContext.MSG_MODE, clientMode.ordinal()));

                Player p = net.minecraft.client.Minecraft.getInstance().player;
                if (p != null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            clientMode == Mode.RETURN
                                    ? "gui.academy.vec_deviation.mode_return"
                                    : "gui.academy.vec_deviation.mode_scatter"), true);
                }
            }

            @Override
            public ResourceLocation getIcon() {
                return clientMode == Mode.RETURN
                        ? cn.academy.Resources.getTexture("abilities/vecmanip/skills/vec_reflection")
                        : cn.academy.Resources.getTexture("abilities/vecmanip/skills/vec_deviation");
            }

            @Override
            public int createID() {
                return 1;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        }
    }
}
