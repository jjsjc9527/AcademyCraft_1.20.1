package cn.academy;

import cn.academy.entity.EntityArc;
import cn.academy.entity.EntityRippleMark;
import cn.academy.entity.EntitySurroundArc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACEntities {

    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AcademyCraft.MODID);

    public static final RegistryObject<EntityType<EntityArc>> ARC =
            REGISTER.register("ac_arc", () -> EntityType.Builder.<EntityArc>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("ac_arc"));

    public static final RegistryObject<EntityType<EntitySurroundArc>> SURROUND_ARC =
            REGISTER.register("ac_surround_arc", () -> EntityType.Builder.<EntitySurroundArc>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("ac_surround_arc"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityWave>> WAVE =
            REGISTER.register("ac_wave", () -> EntityType.Builder
                    .<cn.academy.entity.EntityWave>createNothing(MobCategory.MISC)
                    .sized(3.0F, 3.0F).noSave().noSummon().build("ac_wave"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityParabola>> PARABOLA =
            REGISTER.register("ac_parabola", () -> EntityType.Builder
                    .<cn.academy.entity.EntityParabola>createNothing(MobCategory.MISC)
                    .sized(1.0F, 1.0F).noSave().noSummon().build("ac_parabola"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityStormWing>> STORM_WING =
            REGISTER.register("ac_storm_wing", () -> EntityType.Builder
                    .<cn.academy.entity.EntityStormWing>createNothing(MobCategory.MISC)
                    .sized(3.0F, 3.0F).noSave().noSummon().build("ac_storm_wing"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityDualWing>> DUAL_WING =
            REGISTER.register("ac_dual_wing", () -> EntityType.Builder
                    .<cn.academy.entity.EntityDualWing>createNothing(MobCategory.MISC)
                    .sized(14.0F, 14.0F).noSave().noSummon().build("ac_dual_wing"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityGustTornado>> GUST_TORNADO =
            REGISTER.register("ac_gust_tornado", () -> EntityType.Builder
                    .<cn.academy.entity.EntityGustTornado>createNothing(MobCategory.MISC)
                    .sized(6.0F, 6.0F).noSave().noSummon().build("ac_gust_tornado"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityPlasmaBody>> PLASMA_BODY =
            REGISTER.register("ac_plasma_body", () -> EntityType.Builder
                    .<cn.academy.entity.EntityPlasmaBody>createNothing(MobCategory.MISC)
                    .sized(10.0F, 10.0F).noSave().noSummon().build("ac_plasma_body"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityPlasmaTornado>> PLASMA_TORNADO =
            REGISTER.register("ac_plasma_tornado", () -> EntityType.Builder
                    .<cn.academy.entity.EntityPlasmaTornado>createNothing(MobCategory.MISC)
                    .sized(16.0F, 12.0F).noSave().noSummon().build("ac_plasma_tornado"));

    public static final RegistryObject<EntityType<EntityRippleMark>> RIPPLE_MARK =
            REGISTER.register("ac_ripple_mark", () -> EntityType.Builder.<EntityRippleMark>createNothing(MobCategory.MISC)
                    .sized(2.0F, 2.0F).noSave().noSummon().build("ac_ripple_mark"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMarker>> MARKER =
            REGISTER.register("ac_marker", () -> EntityType.Builder.<cn.academy.entity.EntityMarker>createNothing(MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().build("ac_marker"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityTPMarking>> TP_MARKING =
            REGISTER.register("ac_tp_marking", () -> EntityType.Builder.<cn.academy.entity.EntityTPMarking>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("ac_tp_marking"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityThunderStrike>> THUNDER_STRIKE =
            REGISTER.register("thunder_strike", () -> EntityType.Builder.<cn.academy.entity.EntityThunderStrike>createNothing(MobCategory.MISC)
                    .sized(4.0F, 4.0F).noSave().noSummon().build("thunder_strike"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityRailgunFX>> RAILGUN_FX =
            REGISTER.register("railgun_fx", () -> EntityType.Builder.<cn.academy.entity.EntityRailgunFX>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("railgun_fx"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMdRaySmall>> MD_RAY_SMALL =
            REGISTER.register("md_ray_small", () -> EntityType.Builder.<cn.academy.entity.EntityMdRaySmall>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("md_ray_small"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMdRayBarrage>> MD_RAY_BARRAGE =
            REGISTER.register("md_ray_barrage", () -> EntityType.Builder.<cn.academy.entity.EntityMdRayBarrage>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("md_ray_barrage"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMDRay>> MD_RAY =
            REGISTER.register("md_ray", () -> EntityType.Builder.<cn.academy.entity.EntityMDRay>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("md_ray"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMdBall>> MD_BALL =
            REGISTER.register("md_ball", () -> EntityType.Builder.<cn.academy.entity.EntityMdBall>createNothing(MobCategory.MISC)
                    .sized(2.0F, 2.0F).noSave().noSummon().build("md_ball"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityMdShield>> MD_SHIELD =
            REGISTER.register("md_shield", () -> EntityType.Builder.<cn.academy.entity.EntityMdShield>createNothing(MobCategory.MISC)
                    .sized(2.0F, 2.0F).noSave().noSummon().build("md_shield"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityDiamondShield>> DIAMOND_SHIELD =
            REGISTER.register("diamond_shield", () -> EntityType.Builder.<cn.academy.entity.EntityDiamondShield>createNothing(MobCategory.MISC)
                    .sized(3.0F, 3.0F).noSave().noSummon().build("diamond_shield"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityRailgunHand>> RAILGUN_HAND =
            REGISTER.register("railgun_hand", () -> EntityType.Builder.<cn.academy.entity.EntityRailgunHand>createNothing(MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().noSummon().build("railgun_hand"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntitySilbarn>> SILBARN =
            REGISTER.register("silbarn", () -> EntityType.Builder
                    .<cn.academy.entity.EntitySilbarn>of(
                            cn.academy.entity.EntitySilbarn::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).noSave().noSummon().build("silbarn"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityCoinThrowing>> COIN_THROWING =
            REGISTER.register("coin_throwing", () -> EntityType.Builder
                    .<cn.academy.entity.EntityCoinThrowing>of(
                            cn.academy.entity.EntityCoinThrowing::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().noSummon().build("coin_throwing"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityShiftBlock>> SHIFT_BLOCK =
            REGISTER.register("shift_block", () -> EntityType.Builder
                    .<cn.academy.entity.EntityShiftBlock>of(
                            cn.academy.entity.EntityShiftBlock::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).noSave().noSummon().build("shift_block"));

    public static final RegistryObject<EntityType<cn.academy.entity.EntityShiftNeedle>> SHIFT_NEEDLE =
            REGISTER.register("shift_needle", () -> EntityType.Builder
                    .<cn.academy.entity.EntityShiftNeedle>of(
                            cn.academy.entity.EntityShiftNeedle::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().build("shift_needle"));

    private ACEntities() {}
}
