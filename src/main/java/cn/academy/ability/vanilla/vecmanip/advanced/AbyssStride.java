package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.condition.DevConditionModPresent;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;

public class AbyssStride extends Skill {

    public static final String BACKEND_MODID = "vec_wing";

    public static final DevConditionModPresent BACKEND = new DevConditionModPresent(BACKEND_MODID);

    public static final AbyssStride INSTANCE = new AbyssStride();

    private AbyssStride() {

        super("abyss_stride", LearningHelper.ADVANCED_TREE_LEVEL);

        this.tab = SkillTab.ADVANCED;

        this.canControl = false;
    }

    @Override
    protected void initSkill() {

        addDevCondition(BACKEND);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && BACKEND.present();
    }

    public static boolean isLearned(Player player) {
        if (player == null || !EntityData.isReady(player)) {
            return false;
        }
        AbilityData data = AbilityData.get(player);
        return data != null && data.isSkillLearned(INSTANCE);
    }

    public static float cpMultiplier() {
        return cn.academy.api.AbyssBackends.cpMultiplier();
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = cn.academy.AcademyCraft.MODID)
    public static final class CPBoost {

        private static int cooldown;

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            if (--cooldown > 0) {
                return;
            }
            cooldown = 20;
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            for (Player p : server.getPlayerList().getPlayers()) {
                try {
                    if (!EntityData.isReady(p)) {
                        continue;
                    }
                    CPData cp = CPData.get(p);
                    if (cp != null) {
                        cp.setMaxCpMultiplier(isLearned(p) ? cpMultiplier() : 1.0f);
                    }
                } catch (Throwable ignored) {

                }
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    public static Player takeoverBy(net.minecraft.world.damagesource.DamageSource normal,
                                    net.minecraft.world.entity.LivingEntity target) {
        if (!cn.academy.api.DamageBackends.isExternal()
                && !cn.academy.api.AbyssBackends.isExternal()) {
            return null;
        }
        if (normal == null || !(normal.getEntity() instanceof Player p)) {
            return null;
        }
        if (!isLearned(p)) {
            return null;
        }
        return cn.academy.api.AbyssBackends.takeoverDamage(p, target);
    }
}
