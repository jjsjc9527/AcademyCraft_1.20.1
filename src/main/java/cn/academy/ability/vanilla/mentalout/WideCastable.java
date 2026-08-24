package cn.academy.ability.vanilla.mentalout;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface WideCastable {

    boolean wideApply(Call call, LivingEntity target);

    default boolean wideAccepts(Call call, LivingEntity target) {
        return true;
    }

    default boolean wideNeedsCommand() {
        return wideOptionCount() > 0;
    }

    default int wideOptionCount() {
        return 0;
    }

    default net.minecraft.network.chat.Component wideOptionName(int id) {
        return net.minecraft.network.chat.Component.empty();
    }

    default net.minecraft.resources.ResourceLocation wideOptionIcon(int id) {
        return null;
    }

    default boolean wideAimOnly() {
        return false;
    }

    default boolean wideUniquePerProgram() {
        return false;
    }

    default boolean wideAimFallbackToCrowd() {
        return false;
    }

    default boolean wideAffectsAlliesWhenOff() {
        return false;
    }

    default boolean wideSwitchesToAim(int commandId) {
        return false;
    }

    float wideExp();

    boolean releaseFrom(net.minecraft.world.entity.player.Player caster, LivingEntity target);

    default boolean wideIsRelease(Call call) {
        return false;
    }

    static int releaseAll(net.minecraft.world.entity.player.Player caster, LivingEntity target) {
        cn.academy.datapart.AbilityData data = cn.academy.datapart.AbilityData.get(caster);
        if (data == null || !data.hasCategory() || target == null) {
            return 0;
        }
        int n = 0;
        for (cn.academy.ability.Skill s : data.getCategory().getSkillList()) {
            if (s instanceof WideCastable wc && wc.releaseFrom(caster, target)) {
                n++;
            }
        }
        return n;
    }

    final class Call {

        public final ServerPlayer caster;

        public final float exp;

        public final int commandId;

        public final LivingEntity aimEntity;

        public final BlockPos aimBlock;

        public final int crowdRange;

        public final int crowdCount;

        public Call(ServerPlayer caster, float exp, int commandId,
                    LivingEntity aimEntity, BlockPos aimBlock, int crowdRange, int crowdCount) {
            this.caster = caster;
            this.exp = exp;
            this.commandId = commandId;
            this.aimEntity = aimEntity;
            this.aimBlock = aimBlock;
            this.crowdRange = crowdRange;
            this.crowdCount = crowdCount;
        }
    }
}
