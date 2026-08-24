package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.Resources;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.mentalout.BrainPressureState;
import cn.academy.ability.vanilla.mentalout.WideCastFx;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.skill.Faint;
import cn.academy.config.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BrainPressure extends MentalAdvSkill implements WideCastable {

    public static final BrainPressure INSTANCE = new BrainPressure();

    private static final String[] OPTION_KEYS = {"increase", "decrease"};

    private BrainPressure() {
        super("brain_pressure", Faint.INSTANCE, true);
    }

    @Override
    public int wideOptionCount() {
        return OPTION_KEYS.length;
    }

    @Override
    public Component wideOptionName(int id) {
        return Component.translatable("gui.academy.brain_pressure.mode." + keyOf(id));
    }

    @Override
    public ResourceLocation wideOptionIcon(int id) {
        return Resources.getTexture("abilities/mentalout/pressure/" + keyOf(id));
    }

    private static String keyOf(int id) {
        return OPTION_KEYS[id < 0 || id >= OPTION_KEYS.length ? 0 : id];
    }

    @Override
    public boolean wideApply(Call call, LivingEntity target) {
        float exp = call.exp;
        boolean inc = call.commandId != BrainPressureState.MODE_DECREASE;

        float damage = inc ? AbilityConfig.stat("brain_pressure", "inc_damage", exp)
                           : AbilityConfig.stat("brain_pressure", "dec_damage", exp);
        int interval = (int) (inc ? AbilityConfig.stat("brain_pressure", "inc_interval", exp)
                                  : AbilityConfig.stat("brain_pressure", "dec_interval", exp));
        BrainPressureState.apply(target,
                (int) AbilityConfig.stat("brain_pressure", "duration", exp),
                interval,

                    AbilityContext.finalSkillDamage(call.caster, this, target, damage),
                inc ? BrainPressureState.MODE_INCREASE : BrainPressureState.MODE_DECREASE,
                call.caster);

        WideCastFx.atHead(target, ParticleTypes.BUBBLE_POP, 16, 0.03);
        return true;
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {
        boolean any = false;
        for (int m = 0; m < BrainPressureState.MODES; ++m) {
            if (BrainPressureState.isPressured(target, m)
                    && BrainPressureState.ownerOf(target, m) == caster) {
                BrainPressureState.clear(target, m);
                any = true;
            }
        }
        return any;
    }

    @Override
    public float wideExp() {
        return 0.004f;
    }
}
