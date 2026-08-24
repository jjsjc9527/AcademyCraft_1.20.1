package cn.academy.ability;

import cn.academy.ACConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.datapart.CooldownData;
import cn.academy.event.ability.CalcEvent;
import cn.academy.event.ability.ReflectEvent;
import cn.academy.util.ACPierce;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.function.Predicate;

public class AbilityContext {

    public static AbilityContext of(Player player, Skill skill) {
        return new AbilityContext(player, skill);
    }

    @javax.annotation.Nullable
    public static AbilityContext ofIfReady(Player player, Skill skill) {
        return cn.lambdalib2.datapart.EntityData.isReady(player)
                ? new AbilityContext(player, skill) : null;
    }

    public final Player player;
    public final Skill skill;

    public final AbilityData aData;
    public final CPData cpData;
    public final CooldownData cdData;

    private AbilityContext(Player p, Skill s) {
        player = p;
        skill = s;
        aData = AbilityData.get(player);
        cpData = CPData.get(player);
        cdData = CooldownData.of(player);
    }

    public static float calcSkillDamage(Player owner, Skill skill, Entity target, float damage) {
        if (owner == null) {
            return damage;
        }
        return CalcEvent.calc(new CalcEvent.SkillAttack(owner, skill, target, damage));
    }

    public void attack(Entity target, float damage) {
        attackInternal(target, damage, ACPierce.SKILL_PIERCE);
    }

    public void attackPierce(Entity target, float damage, ResourceKey<DamageType> pierceKey) {
        attackInternal(target, damage, pierceKey);
    }

    public void attackNoPierce(Entity target, float damage) {
        attackInternal(target, damage, null);
    }

    private void attackInternal(Entity target, float damage, ResourceKey<DamageType> pierceKey) {

        damage = calcSkillDamage(player, skill, target, damage);

        if (damage > 0 && canTarget(target) && canAttack(target)) {
            float finalDamage = getFinalDamage(damage);
            if (pierceKey != null && target instanceof LivingEntity living) {
                ACPierce.hurtOrPierce(living, skillDamageSource(), pierceKey, finalDamage);
            } else {
                target.hurt(skillDamageSource(), finalDamage);
            }
        }
    }

    public void attackIgnoreArmor(Entity target, float damage) {

        attack(target, damage);
    }

    public void attackReflect(Entity target, float damage, java.util.function.Consumer<ReflectEvent> reflectCallback) {
        attackReflect(target, damage, e -> {}, reflectCallback);
    }

    public void attackReflect(Entity target, float damage,
                              java.util.function.Consumer<ReflectEvent> prefill,
                              java.util.function.Consumer<ReflectEvent> reflectCallback) {
        ReflectEvent event = new ReflectEvent(player, skill, target);

        event.damage = damage;
        prefill.accept(event);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            reflectCallback.accept(event);
        } else {
            attack(target, damage);
        }
    }

    public boolean tryReflect(Entity target, java.util.function.Consumer<ReflectEvent> prefill) {
        return tryReflect(target, prefill, ev -> {});
    }

    public boolean tryReflect(Entity target,
                              java.util.function.Consumer<ReflectEvent> prefill,
                              java.util.function.Consumer<ReflectEvent> onReflected) {
        ReflectEvent event = new ReflectEvent(player, skill, target);
        prefill.accept(event);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            onReflected.accept(event);
            return true;
        }
        return false;
    }

    public boolean canAttack(Entity entity) {
        return canBreakBlock(entity.level()) || (!(entity instanceof Painting) && !(entity instanceof ItemFrame));
    }

    public boolean canTarget(Entity entity) {
        return AbilityPipeline.canTarget(player, entity);
    }

    public void attackRange(double x, double y, double z, double range,
                            float damage, Predicate<Entity> entitySelector) {
        attackRange(x, y, z, range, damage, entitySelector, this::attack);
    }

    public void attackRange(double x, double y, double z, double range,
                            float damage, Predicate<Entity> entitySelector,
                            java.util.function.BiConsumer<Entity, Float> perTarget) {
        AABB box = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);

        List<Entity> list = player.level().getEntitiesOfClass(Entity.class, box,
                entitySelector.and(this::canTarget));
        for (Entity ent : list) {
            double dist = MathUtils.distance(x, y, z, ent.getX(), ent.getY(), ent.getZ());
            float factor = 1 - MathUtils.clampf(0, 1, (float) (dist / range));
            float appliedDamage = MathUtils.lerpf(0, damage, factor);
            perTarget.accept(ent, appliedDamage);
        }
    }

    public boolean canConsumeCP(float cp) {
        return cpData.canPerform(cp);
    }

    public boolean consume(float overload, float cp) {
        return cpData.perform(getFinalConsO(overload), getFinalConsCP(cp));
    }

    public void consumeWithForce(float overload, float cp) {
        cpData.performWithForce(overload, cp);
    }

    public float getSkillExp() {
        return aData.getSkillExp(skill);
    }

    public void addSkillExp(float amt) {
        aData.addSkillExp(skill, getFinalExpIncr(amt));
    }

    public void setCooldown(int ticks) {
        cdData.set(skill, ticks);
    }

    public void setCooldownSub(int subID, int ticks) {
        cdData.setSub(skill, subID, ticks);
    }

    private float g_getDamageScale() {
        return (float) ACConfig.getDouble("ac.ability.calc_global.damage_scale", 1.0);
    }

    public boolean canBreakBlock(Level world, int x, int y, int z) {
        return skill.shouldDestroyBlocks() && AbilityPipeline.canBreakBlock(world, player, x, y, z);
    }

    public boolean canBreakBlock(Level world, BlockPos pos) {
        return skill.shouldDestroyBlocks() && AbilityPipeline.canBreakBlock(world, player, pos);
    }

    public boolean canBreakBlock(Level world) {
        return skill.shouldDestroyBlocks() && AbilityPipeline.canBreakBlock(world);
    }

    public boolean isEntirelyDisableBreakBlock() {
        return AbilityPipeline.isAllWorldDisableBreakBlock();
    }

    private float getFinalDamage(float damage) {
        return g_getDamageScale() * skill.getDamageScale() * damage;
    }

    public static float finalSkillDamage(Player owner, Skill skill, Entity target, float damage) {
        float withExtra = calcSkillDamage(owner, skill, target, damage);
        float global = (float) ACConfig.getDouble("ac.ability.calc_global.damage_scale", 1.0);
        float local = skill == null ? 1.0f : skill.getDamageScale();
        return global * local * withExtra;
    }

    private float getFinalExpIncr(float expincr) {
        return skill.getExpIncrSpeed() * expincr;
    }

    private float getFinalConsCP(float cp) {
        return skill.getCPConsumeSpeed() * cp;
    }

    private float getFinalConsO(float overload) {
        return skill.getOverloadConsumeSpeed() * overload;
    }

    private DamageSource skillDamageSource() {
        return player.damageSources().playerAttack(player);
    }
}
