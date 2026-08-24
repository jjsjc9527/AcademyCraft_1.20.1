package cn.academy.ability.vanilla.meltdowner.passiveskill;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;

public class RadiationIntensify extends Skill {

    public static final RadiationIntensify INSTANCE = new RadiationIntensify();

    public static final String MSG_MARK = "rad_mark";

    @Listener(channel = MSG_MARK, side = LogicalSide.CLIENT)
    private void c_mark(Entity target, Integer ticks) {
        if (target != null) {
            cn.academy.ability.vanilla.meltdowner.skill.MDDamageHelper.setMarkTick(target, ticks);
        }
    }

    private RadiationIntensify() {
        super("rad_intensify", 1);
        canControl = false;
        expCustomized = true;
    }

    @Override
    public float getSkillExp(AbilityData data) {
        CPData cp = CPData.get(data.getEntity());
        return MathUtils.clampf(0, 1, cp.getMaxCP() / cp.getInitCP(5));
    }

    public static float getRate(AbilityData data) {
        return MathUtils.lerpf(1.4f, 1.8f, data.getSkillExp(INSTANCE));
    }
}
