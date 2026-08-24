package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.vanilla.mentalout.advanced.MentalMastery;
import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;
import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.datapart.RemoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class WideCastExecutor {

    private static final String PENDING = "mo_rc_pending";

    private static final String PENDING_AT = "mo_rc_pending_at";

    private static final String PENDING_PID = "mo_rc_pending_pid";

    private static final int PENDING_TIMEOUT = 400;

    private WideCastExecutor() {}

    private static boolean needsSecondPhase(RemoteData.Program prog, ServerPlayer player) {
        int usable = MentalMastery.usableSlots(player);
        for (int i = 0; i < usable; ++i) {
            Skill s = prog.getSkill(i);
            if (s instanceof WideCastable wc && wc.wideSwitchesToAim(prog.getCommand(i))) {
                return true;
            }
        }
        return false;
    }

    public static void fire(ServerPlayer player, ItemStack stack) {

        if (!cn.lambdalib2.datapart.EntityData.isReady(player)) {
            return;
        }
        AbilityData aData = AbilityData.get(player);
        if (!aData.hasCategory()) {
            return;
        }
        RemoteData rd = RemoteData.get(player);
        if (rd == null) {
            return;
        }
        RemoteData.Program prog = RemoteData.Book.of(stack).getCurrent();
        if (prog.isEmpty(MentalMastery.usableSlots(player))) {
            return;
        }

        float wcExp = aData.getSkillExp(WideCast.INSTANCE);
        int range = prog.effectiveRange(wcExp);
        int count = prog.effectiveCount(wcExp);
        float aimRange = AbilityConfig.stat("wide_cast", "aim_range", wcExp);

        LivingEntity aimEntity = ForcedControl.traceVictim(player, aimRange);
        BlockPos aimBlock = ForcedControl.traceBlock(player, aimRange);

        boolean usesAim = usesAim(prog, player);

        LivingEntity excluded = usesAim ? aimEntity : null;

        CompoundTag pdata = player.getPersistentData();
        long now = player.level().getGameTime();
        int pid = RemoteData.Book.of(stack).getCurrentID();
        boolean twoPhase = needsSecondPhase(prog, player);

        if (pdata.contains(PENDING)
                && (now - pdata.getLong(PENDING_AT) > PENDING_TIMEOUT
                    || pdata.getInt(PENDING_PID) != pid)) {
            pdata.remove(PENDING);
        }

        List<LivingEntity> crowd;
        if (twoPhase && !pdata.contains(PENDING)) {

            List<LivingEntity> picked = pickCrowd(player, prog, aimEntity, range, count);
            if (picked.isEmpty()) {
                return;
            }
            int[] ids = new int[picked.size()];
            for (int i = 0; i < ids.length; ++i) {
                ids[i] = picked.get(i).getId();

                WideCastFx.at(picked.get(i), net.minecraft.core.particles.ParticleTypes.END_ROD, 12, 0.02);
            }
            pdata.putIntArray(PENDING, ids);
            pdata.putLong(PENDING_AT, now);
            pdata.putInt(PENDING_PID, pid);
            return;
        }
        if (twoPhase) {

            int[] ids = pdata.getIntArray(PENDING);

            if (aimEntity == null && aimBlock == null) {
                return;
            }
            pdata.remove(PENDING);
            crowd = new ArrayList<>(ids.length);
            for (int id : ids) {

                if (player.level().getEntity(id) instanceof LivingEntity le && le.isAlive()) {
                    crowd.add(le);
                }
            }
            if (crowd.isEmpty()) {
                return;
            }
        } else if (count == 1 && !prog.isSyncMind() && !usesAim) {

            crowd = aimEntity == null ? Collections.emptyList()
                                      : Collections.singletonList(aimEntity);
        } else {
            crowd = gather(player, range, count, excluded);
        }

        CPData cpData = CPData.get(player);
        float ratio = AbilityConfig.stat("wide_cast", "cost_ratio", wcExp);
        float expBonus = AbilityConfig.stat("wide_cast", "exp_bonus", wcExp);

        boolean aimScope = false;
        int firedSlots = 0;

        java.util.Set<Skill> firedUnique = null;

        List<LivingEntity> allyCrowd = null;

        int usable = MentalMastery.usableSlots(player);
        for (int i = 0; i < usable; ++i) {
            Skill skill = prog.getSkill(i);
            if (skill == null) {
                continue;
            }
            if (!(skill instanceof WideCastable wc)) {

                break;
            }

            if (skill instanceof WideCastable wcU && wcU.wideUniquePerProgram()) {
                if (firedUnique == null) {
                    firedUnique = new java.util.HashSet<>();
                }
                if (!firedUnique.add(skill)) {
                    continue;
                }
            }
            if (!aData.isSkillLearned(skill)) {

                break;
            }

            float exp = aData.getSkillExp(skill);

            WideCastable.Call call = new WideCastable.Call(
                    player, exp, prog.getCommand(i), aimEntity, aimBlock, range, count);

            boolean aimOnly = wc.wideAimOnly();
            boolean syncScope = !aimOnly && !aimScope && prog.isSyncMind();
            if (syncScope && allyCrowd == null) {
                allyCrowd = gatherAllies(player, aData, range, count);
            }

            List<LivingEntity> scope;
            if (aimOnly || aimScope) {
                if (aimEntity != null) {
                    scope = Collections.singletonList(aimEntity);
                } else if (aimOnly && !aimScope && wc.wideAimFallbackToCrowd()) {

                    scope = syncScope ? allyCrowd : crowd;
                } else {
                    scope = Collections.emptyList();
                }
            } else {
                scope = syncScope ? allyCrowd : crowd;
            }

            boolean dropAllies = !syncScope && !aimOnly && !aimScope && !wc.wideAffectsAlliesWhenOff();
            List<LivingEntity> targets = new ArrayList<>();
            for (LivingEntity t : scope) {
                if (!t.isAlive() || !wc.wideAccepts(call, t)) {
                    continue;
                }
                if (dropAllies && CognitionRewrite.isAllyOf(t, player)) {
                    continue;
                }
                targets.add(t);
            }
            if (targets.isEmpty()) {
                break;
            }

            float cp = AbilityConfig.cp(skill.getName(), exp) * targets.size() * ratio;
            float ol = AbilityConfig.overload(skill.getName(), exp) * targets.size() * ratio;
            if (!cpData.perform(ol, cp)) {
                break;
            }

            int hit = 0;
            for (LivingEntity t : targets) {

                if (!wc.wideIsRelease(call) && MentalImmune.blocked(player, skill, t)) {
                    continue;
                }
                if (wc.wideApply(call, t)) {
                    hit++;
                }
            }
            firedSlots++;
            if (hit > 0) {

                aData.addSkillExp(skill, wc.wideExp() * expBonus);
            }
            if (wc.wideSwitchesToAim(prog.getCommand(i))) {
                aimScope = true;
            }
        }

        if (firedSlots == 0) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(),
                Math.max(0, (int) AbilityConfig.stat("wide_cast", "cooldown", wcExp)));
    }

    private static List<LivingEntity> pickCrowd(ServerPlayer player, RemoteData.Program prog,
                                                LivingEntity aimEntity, int range, int count) {
        if (count == 1 && !prog.isSyncMind()) {
            return aimEntity == null ? Collections.emptyList() : Collections.singletonList(aimEntity);
        }
        return gather(player, range, count, null);
    }

    private static boolean usesAim(RemoteData.Program prog, ServerPlayer player) {

        int usable = MentalMastery.usableSlots(player);
        for (int i = 0; i < usable; ++i) {
            Skill s = prog.getSkill(i);

            if (s instanceof WideCastable wc && wc.wideSwitchesToAim(prog.getCommand(i))) {
                return true;
            }
        }
        return false;
    }

    private static List<LivingEntity> gatherAllies(ServerPlayer player, AbilityData aData,
                                                   int range, int count) {
        double r2 = (double) range * range;

        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= r2
                        && CognitionRewrite.isDirectAllyOf(e, player)
                        && AbilityPipeline.canTarget(player, e));

        RemoteData rd = RemoteData.get(player);
        if (rd != null) {
            boolean dirty = false;
            for (LivingEntity e : list) {
                dirty |= rd.addAlly(e.getUUID(), e.getType());
            }
            if (dirty) {
                rd.sync();
            }
        }
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return list.size() <= count ? list : new ArrayList<>(list.subList(0, count));
    }

    private static List<LivingEntity> gather(ServerPlayer player, int range, int count,
                                             LivingEntity excluded) {
        double r2 = (double) range * range;
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> e != player && e != excluded && e.isAlive()
                        && e.distanceToSqr(player) <= r2
                        && AbilityPipeline.canTarget(player, e));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return list.size() <= count ? list : new ArrayList<>(list.subList(0, count));
    }

}
