package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.vanilla.mentalout.ControlState;
import cn.academy.ability.vanilla.mentalout.MentalCharm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import cn.academy.util.ACDefense;

public final class CognitionRewrite {

    private CognitionRewrite() {}

    private static final String KEY = "mo_cog";

    private static final String CASTER = "c";
    private static final String SUBJECT = "s";
    private static final String HOSTILE = "h";

    private static final int MAX_RECORDS = 8;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CogEvents());
    }

    private static ListTag list(Entity e) {
        return e.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
    }

    public static boolean isActive(Entity e) {
        return !list(e).isEmpty();
    }

    public enum Result {

        NONE,

        APPLIED,

        CLEARED
    }

    public static Result set(Player caster, LivingEntity holder, LivingEntity subject, boolean hostile) {
        if (holder == subject || holder == null || subject == null) {
            return Result.NONE;
        }
        ListTag l = list(holder);
        String cs = caster.getUUID().toString(), sb = subject.getUUID().toString();
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            if (cs.equals(t.getString(CASTER)) && sb.equals(t.getString(SUBJECT))) {
                if (t.getBoolean(HOSTILE) == hostile) {
                    l.remove(i);
                    writeBack(holder, l);
                    releaseTargetIfAny(holder);
                    return Result.CLEARED;
                }
                t.putBoolean(HOSTILE, hostile);
                writeBack(holder, l);
                return Result.APPLIED;
            }
        }
        CompoundTag t = new CompoundTag();
        t.putString(CASTER, cs);
        t.putString(SUBJECT, sb);
        t.putBoolean(HOSTILE, hostile);
        l.add(t);
        while (l.size() > MAX_RECORDS) {
            l.remove(0);
        }
        writeBack(holder, l);

        if (holder instanceof Mob mob) {
            drive(mob);
        }
        return Result.APPLIED;
    }

    public static boolean has(Player caster, LivingEntity holder, LivingEntity subject, boolean hostile) {
        ListTag l = list(holder);
        String cs = caster.getUUID().toString(), sb = subject.getUUID().toString();
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            if (cs.equals(t.getString(CASTER)) && sb.equals(t.getString(SUBJECT))
                    && t.getBoolean(HOSTILE) == hostile) {
                return true;
            }
        }
        return false;
    }

    private static void writeBack(LivingEntity holder, ListTag l) {
        if (l.isEmpty()) {
            holder.getPersistentData().remove(KEY);
        } else {
            holder.getPersistentData().put(KEY, l);
        }
    }

    private static void releaseTargetIfAny(LivingEntity holder) {
        if (holder instanceof Mob mob && mob.getTarget() != null) {
            mob.setTarget(null);
        }
    }

    public static boolean clearFrom(Player caster, LivingEntity holder) {
        ListTag l = list(holder);
        if (l.isEmpty()) {
            return false;
        }
        String cs = caster.getUUID().toString();
        boolean any = false;
        for (int i = l.size() - 1; i >= 0; --i) {
            if (cs.equals(l.getCompound(i).getString(CASTER))) {
                l.remove(i);
                any = true;
            }
        }
        if (!any) {
            return false;
        }
        if (l.isEmpty()) {
            holder.getPersistentData().remove(KEY);
        } else {
            holder.getPersistentData().put(KEY, l);
        }

        if (holder instanceof Mob mob && mob.getTarget() != null) {
            mob.setTarget(null);
        }
        return true;
    }

    @Nullable
    private static LivingEntity resolve(Entity holder, String uuid) {
        if (!(holder.level() instanceof ServerLevel sl)) {
            return null;
        }
        try {
            Entity e = sl.getEntity(UUID.fromString(uuid));
            return e instanceof LivingEntity le && le.isAlive() ? le : null;
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    public static boolean isForcedFoe(Entity holder, Entity subject) {
        ListTag l = list(holder);
        if (l.isEmpty()) {
            return false;
        }
        String sb = subject.getUUID().toString();
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            if (t.getBoolean(HOSTILE) && sb.equals(t.getString(SUBJECT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllyOf(Entity holder, Entity subject) {
        if (!hasAllyRecord(holder, subject) && !sameTeam(holder, subject)) {
            return false;
        }
        return notSuppressed(holder, subject);
    }

    public static boolean isDirectAllyOf(Entity holder, Entity subject) {
        if (!hasAllyRecord(holder, subject)) {
            return false;
        }
        return notSuppressed(holder, subject);
    }

    private static boolean notSuppressed(Entity holder, Entity subject) {
        if (!(holder instanceof Mob m)) {
            return true;
        }
        if (ControlState.commandedTarget(m) == subject) {
            return false;
        }
        return !(MentalCharm.isActive(m) && MentalCharm.isHostileFlip(m)
                && MentalCharm.getOwner(m) == subject);
    }

    private static boolean sameTeam(Entity holder, Entity subject) {
        ListTag l = list(holder);
        if (l.isEmpty()) {
            return false;
        }
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            String cs = t.getString(CASTER);

            if (!t.getBoolean(HOSTILE) && cs.equals(t.getString(SUBJECT))
                    && followsSamePlayer(subject, cs)) {
                return true;
            }
        }
        return false;
    }

    private static boolean followsSamePlayer(Entity e, String playerId) {
        ListTag l = list(e);
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            if (!t.getBoolean(HOSTILE) && playerId.equals(t.getString(CASTER))
                    && playerId.equals(t.getString(SUBJECT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllyRecord(Entity holder, Entity subject) {
        ListTag l = list(holder);
        String sb = subject.getUUID().toString();
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            if (sb.equals(t.getString(SUBJECT)) && !t.getBoolean(HOSTILE)) {
                return true;
            }
        }
        return false;
    }

    private static void drive(Mob mob) {
        ListTag l = list(mob);
        if (l.isEmpty()) {
            return;
        }

        boolean yieldAggro = ControlState.isControlled(mob) || MentalCharm.isActive(mob);

        LivingEntity foe = null;
        for (int i = 0; i < l.size(); ++i) {
            CompoundTag t = l.getCompound(i);
            LivingEntity s = resolve(mob, t.getString(SUBJECT));
            if (s == null) {
                continue;
            }
            if (t.getBoolean(HOSTILE)) {
                if (yieldAggro) {
                    continue;
                }

                if (mob.getTarget() == s) {
                    foe = s;
                    break;
                }
                if (foe == null) {
                    foe = s;
                }
            } else if (isAllyOf(mob, s)) {
                MentalCharm.deaggroFrom(mob, s);
            }
        }
        if (foe != null) {
            MentalCharm.aggroOnto(mob, foe);
            return;
        }

        LivingEntity cur = mob.getTarget();
        if (cur != null && isAllyOf(mob, cur)) {
            MentalCharm.deaggroFrom(mob, cur);
        }
    }

    public static class CogEvents {

        @SubscribeEvent
        public void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
            ListTag old = list(event.getOriginal());
            if (!old.isEmpty()) {
                event.getEntity().getPersistentData().put(KEY, old.copy());
            }
        }

        @SubscribeEvent
        public void onCogChangeTarget(LivingChangeTargetEvent event) {
            LivingEntity ent = event.getEntity();
            if (ent.level().isClientSide || event.getNewTarget() == null || !isActive(ent)) {
                return;
            }
            if (isAllyOf(ent, event.getNewTarget())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onCogAttack(LivingAttackEvent event) {
            Entity src = event.getSource().getEntity();
            if (!(src instanceof LivingEntity attacker) || attacker.level().isClientSide
                    || !isActive(attacker)) {
                return;
            }
            if (isAllyOf(attacker, event.getEntity())) {
                ACDefense.block(event);
            }
        }

        @SubscribeEvent
        public void onCogDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
            if (!event.getEntity().level().isClientSide) {
                forgetAlly(event.getEntity());
            }
        }

        @SubscribeEvent
        public void onCogLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide
                    || !(event.getEntity() instanceof LivingEntity e)) {
                return;
            }
            net.minecraft.world.entity.Entity.RemovalReason r = e.getRemovalReason();
            if (r != null && r.shouldDestroy()) {
                forgetAlly(e);
            }
        }

        private void forgetAlly(LivingEntity e) {
            ListTag l = list(e);
            for (int i = 0; i < l.size(); ++i) {
                CompoundTag t = l.getCompound(i);
                String cs = t.getString(CASTER);

                boolean hostile = t.getBoolean(HOSTILE);
                if (!hostile && !cs.equals(t.getString(SUBJECT))) {
                    continue;
                }
                try {
                    Player owner = e.level().getPlayerByUUID(java.util.UUID.fromString(cs));
                    cn.academy.datapart.RemoteData rd =
                            owner == null ? null : cn.academy.datapart.RemoteData.get(owner);
                    if (rd == null) {
                        continue;
                    }
                    boolean changed = hostile ? rd.removeEnraged(e.getUUID())
                                              : rd.removeAlly(e.getUUID());
                    if (changed) {
                        rd.sync();
                    }
                } catch (IllegalArgumentException bad) {

                }
            }
        }

        @SubscribeEvent
        public void onCogTick(LivingEvent.LivingTickEvent event) {
            LivingEntity ent = event.getEntity();
            if (ent.level().isClientSide || !(ent instanceof Mob mob)) {
                return;
            }
            drive(mob);
        }
    }
}
