package cn.academy.tutorial;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.lambdalib2.util.Colors;

import static cn.academy.tutorial.Conditions.itemObtained;
import static cn.academy.tutorial.ViewGroups.*;

public class TutorialInit {

    public static void init() {
        defnTut("welcome");

        defnTut("ores")
                .addCondition(itemObtained(ACBlocks.CONSTRAINT_METAL.get()))
                .addCondition(itemObtained(ACBlocks.IMAGSIL_ORE.get()))
                .addCondition(itemObtained(ACBlocks.CRYSTAL_ORE.get()))
                .addCondition(itemObtained(ACBlocks.RESO_ORE.get()))
                .addPreview(drawsBlock(ACBlocks.CONSTRAINT_METAL.get()))
                .addPreview(drawsBlock(ACBlocks.IMAGSIL_ORE.get()))
                .addPreview(drawsBlock(ACBlocks.CRYSTAL_ORE.get()))
                .addPreview(drawsBlock(ACBlocks.RESO_ORE.get()))
                .addPreview(displayIcon("item/phase_liquid_mat", 0, 0, 1, Colors.white()))
                .addPreview(recipes(ACItems.CONSTRAINT_PLATE.get()))
                .addPreview(recipes(ACItems.IMAG_SILICON_INGOT.get()))
                .addPreview(recipes(ACItems.WAFER.get()))
                .addPreview(recipes(ACItems.IMAG_SILICON_PIECE.get()));

        defnTut("phase_generator")
                .addCondition(itemObtained(ACBlocks.PHASE_GEN.get()))
                .addPreview(recipes(ACBlocks.PHASE_GEN.get()));

        defnTut("solar_generator");

        defnTut("wind_generator")
                .addCondition(itemObtained(ACBlocks.WINDGEN_BASE.get()))
                .addCondition(itemObtained(ACItems.WINDGEN_FAN.get()))
                .addCondition(itemObtained(ACBlocks.WINDGEN_MAIN.get()))
                .addCondition(itemObtained(ACBlocks.WINDGEN_PILLAR.get()))
                .addPreview(recipes(ACBlocks.WINDGEN_BASE.get()))
                .addPreview(recipes(ACBlocks.WINDGEN_PILLAR.get()))
                .addPreview(recipes(ACBlocks.WINDGEN_MAIN.get()))
                .addPreview(recipes(ACItems.WINDGEN_FAN.get()));

        defnTut("metal_former")
                .addCondition(itemObtained(ACBlocks.METAL_FORMER.get()))
                .addPreview(recipes(ACBlocks.METAL_FORMER.get()));

        defnTut("imag_fusor")
                .addCondition(itemObtained(ACBlocks.IMAG_FUSOR.get()))
                .addPreview(recipes(ACBlocks.IMAG_FUSOR.get()));

        defnTut("terminal")
                .addCondition(itemObtained(ACItems.TERMINAL_INSTALLER.get()))
                .addPreview(recipes(ACItems.TERMINAL_INSTALLER.get()));

        defnTut("ability_developer")
                .addCondition(itemObtained(ACBlocks.DEV_NORMAL.get()))
                .addCondition(itemObtained(ACBlocks.DEV_ADVANCED.get()))
                .addCondition(itemObtained(ACItems.DEVELOPER_PORTABLE.get()))
                .addPreview(recipes(ACBlocks.DEV_NORMAL.get()))
                .addPreview(recipes(ACBlocks.DEV_ADVANCED.get()))
                .addPreview(recipes(ACItems.DEVELOPER_PORTABLE.get()));

        defnTut("ability_basis");
        defnTut("misc");
        defnTut("develop_ability");
        defnTut("wireless_network");
    }

    public static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }
}
