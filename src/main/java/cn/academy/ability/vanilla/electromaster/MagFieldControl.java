package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.Skill;

public class MagFieldControl extends Skill {

    public static final MagFieldControl INSTANCE = new MagFieldControl();

    public MagFieldControl() {
        super("mag_gravity", 4);
        this.canControl = false;
    }
}
