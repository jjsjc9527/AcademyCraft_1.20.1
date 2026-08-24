package cn.academy;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACParticles {

    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "academy");

    public static final RegistryObject<SimpleParticleType> TP =
            REGISTER.register("tp_particle", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FORMULA =
            REGISTER.register("formula", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SMOKE =
            REGISTER.register("smoke", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MD =
            REGISTER.register("md_particle", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SILBARN_FRAG =
            REGISTER.register("silbarn_frag", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FEATHER =
            REGISTER.register("feather", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> GOLD_FEATHER =
            REGISTER.register("golden_feather", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> PLATINUM_FEATHER =
            REGISTER.register("platinum_feather", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> IRON_SAND =
            REGISTER.register("iron_sand", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> IRON_SAND_FINE =
            REGISTER.register("iron_sand_fine", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> IRON_SAND_WHIP =
            REGISTER.register("iron_sand_whip", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SONIC_WAVE =
            REGISTER.register("sonic_wave", () -> new SimpleParticleType(true));

    private ACParticles() {}
}
