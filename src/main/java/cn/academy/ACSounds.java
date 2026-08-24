package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACSounds {

    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AcademyCraft.MODID);

    public static final RegistryObject<SoundEvent> MACHINE_WORK = register("machine.machine_work");

    public static final RegistryObject<SoundEvent> IMAG_FUSOR_WORK = register("machine.imag_fusor_work");

    public static final RegistryObject<SoundEvent> TERMINAL_SELECT = register("terminal.select");

    public static final RegistryObject<SoundEvent> EM_ARC_WEAK = register("em.arc_weak");

    public static final RegistryObject<SoundEvent> EM_ARC_STRONG = register("em.arc_strong");

    public static final RegistryObject<SoundEvent> EM_CHARGE_LOOP = register("em.charge_loop");
    public static final RegistryObject<SoundEvent> EM_INTENSIFY_LOOP = register("em.intensify_loop");

    public static final RegistryObject<SoundEvent> EM_INTENSIFY_ACTIVATE = register("em.intensify_activate");

    public static final RegistryObject<SoundEvent> EM_MOVE_LOOP = register("em.move_loop");

    public static final RegistryObject<SoundEvent> TP_TP = register("tp.tp");

    public static final RegistryObject<SoundEvent> TP_MOVE_PLAYER = register("tp.move_player");

    public static final RegistryObject<SoundEvent> TP_SHIFT = register("tp.tp_shift");

    public static final RegistryObject<SoundEvent> TP_FLASHING = register("tp.tp_flashing");

    public static final RegistryObject<SoundEvent> TP_MOVE_BLOCK = register("tp.move_block");

    public static final RegistryObject<SoundEvent> TP_MOVE_BLOCK_SPEED = register("tp.move_block_speed");

    public static final RegistryObject<SoundEvent> EM_RAILGUN = register("em.railgun");

    public static final RegistryObject<SoundEvent> EM_THUNDER_CLAP = register("em.thunder_clap");

    public static final RegistryObject<SoundEvent> EM_IRONSAND_ATTACK = register("em.ironsand_attack");

    public static final RegistryObject<SoundEvent> EM_IRONSAND_CYCLE = register("em.ironsand_cycle");

    public static final RegistryObject<SoundEvent> MD_RAY_SMALL = register("md.ray_small");

    public static final RegistryObject<SoundEvent> MD_MEL_LASER = register("md.mel_laser");

    public static final RegistryObject<SoundEvent> MD_SHIELD_STARTUP = register("md.shield_startup");

    public static final RegistryObject<SoundEvent> MD_SHIELD_LOOP = register("md.shield_loop");

    public static final RegistryObject<SoundEvent> MD_MELTDOWNER = register("md.meltdowner");

    public static final RegistryObject<SoundEvent> MD_CHARGE = register("md.md_charge");

    public static final RegistryObject<SoundEvent> SILBARN_HEAVY = register("entity.silbarn_heavy");
    public static final RegistryObject<SoundEvent> SILBARN_LIGHT = register("entity.silbarn_light");

    public static final RegistryObject<SoundEvent> VM_DIRECTED_SHOCK = register("vecmanip.directed_shock");

    public static final RegistryObject<SoundEvent> VM_GROUNDSHOCK = register("vecmanip.groundshock");

    public static final RegistryObject<SoundEvent> VM_DIRECTED_BLAST = register("vecmanip.directed_blast");

    public static final RegistryObject<SoundEvent> VM_VEC_ACCEL = register("vecmanip.vec_accel");

    public static final RegistryObject<SoundEvent> VM_VEC_DEVIATION = register("vecmanip.vec_deviation");

    public static final RegistryObject<SoundEvent> VM_VEC_REFLECTION = register("vecmanip.vec_reflection");

    public static final RegistryObject<SoundEvent> VM_STORM_WING = register("vecmanip.storm_wing");

    public static final RegistryObject<SoundEvent> VM_WING_FLAP = register("vecmanip.wing_flap");

    public static final RegistryObject<SoundEvent> VM_CRUSH_LOOP = register("vecmanip.crush_loop");

    public static final RegistryObject<SoundEvent> VM_VEC_EXPLOSION = register("vecmanip.vec_explosion");

    public static final RegistryObject<SoundEvent> VM_PLASMA_CANNON = register("vecmanip.plasma_cannon");

    public static final RegistryObject<SoundEvent> VM_PLASMA_CANNON_T = register("vecmanip.plasma_cannon_t");

    public static final RegistryObject<SoundEvent> ENTITY_FLIPCOIN = register("entity.flipcoin");

    public static final RegistryObject<SoundEvent> MO_CONTROL = register("mo.controlsound");

    public static final RegistryObject<SoundEvent> V_EGG_THROW = register("vanilla.egg_throw");

    public static final RegistryObject<SoundEvent> V_BELL_RESONATE = register("vanilla.bell_resonate");

    public static final RegistryObject<SoundEvent> V_PLAYER_BREATH = register("vanilla.player_breath");

    public static final RegistryObject<SoundEvent> V_AMETHYST_CHIME = register("vanilla.amethyst_chime");

    public static final RegistryObject<SoundEvent> V_ITEM_BREAK = register("vanilla.item_break");

    public static final RegistryObject<SoundEvent> V_GENERIC_EXPLODE = register("vanilla.generic_explode");

    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(AcademyCraft.MODID, name)));
    }

    private ACSounds() {}
}
