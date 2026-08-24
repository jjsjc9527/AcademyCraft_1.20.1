package cn.academy.ability;

import cn.academy.config.AbilityConfig;

import java.util.Map;

public final class SkillExpCurve {

    private SkillExpCurve() {}

    public static final String STAT = "exp_rate";

    private static final float MIN_RATE = 0.01f;

    private static final Map<String, String> ALIAS = Map.of(
            "arc_gen", "electric_arc"
    );

    public static float rate(Skill skill, float curExp) {
        if (skill == null) {
            return 1.0f;
        }
        String cfg = ALIAS.getOrDefault(skill.getName(), skill.getName());

        float r = AbilityConfig.statOr(cfg, STAT, clamp01(curExp), 1.0f);
        return Math.max(MIN_RATE, r);
    }

    public static float apply(Skill skill, float curExp, float raw) {
        if (raw <= 0.0f) {
            return raw;
        }
        return raw * rate(skill, curExp);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }
}
