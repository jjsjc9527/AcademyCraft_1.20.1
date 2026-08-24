package cn.academy.ability.vanilla.teleporter.passiveskill;

import cn.academy.ability.Skill;

public class DimFoldingTheorem extends Skill {

    public static final DimFoldingTheorem INSTANCE = new DimFoldingTheorem();

    public DimFoldingTheorem() {
        super("dim_folding_theorem", 1);
        canControl = false;
    }
}
