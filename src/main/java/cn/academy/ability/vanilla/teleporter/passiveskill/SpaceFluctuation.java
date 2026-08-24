package cn.academy.ability.vanilla.teleporter.passiveskill;

import cn.academy.ability.Skill;

public class SpaceFluctuation extends Skill {

    public static final SpaceFluctuation INSTANCE = new SpaceFluctuation();

    public SpaceFluctuation() {
        super("space_fluct", 4);
        canControl = false;
    }
}
