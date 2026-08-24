package cn.academy.ability.vanilla.electromaster;

import cn.academy.ACSounds;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.AbilityPipeline;
import cn.academy.config.AbilityConfig;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.datapart.CPData;
import cn.academy.datapart.PresetData;
import cn.academy.entity.EntityCoinThrowing;
import cn.academy.event.CoinThrowEvent;
import cn.academy.item.CoinItem;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.Set;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class Railgun extends Skill {

    public static final Railgun INSTANCE = new Railgun();

    private static final String
            MSG_CHARGE_EFFECT = "charge_eff",
            MSG_PERFORM = "perform",
            MSG_REFLECT = "reflect",
            MSG_COIN_PERFORM = "coin_perform",
            MSG_ITEM_PERFORM = "item_perform",

            MSG_PERFORM_PATH = "perform_path";

    private static final int REFLECT_DISTANCE = 15;

    private static boolean hitEntity = false;

    private static final Set<Item> ACCEPTED_ITEMS = Set.of(Items.IRON_INGOT, Items.IRON_BLOCK);

    private static final int DAMAGE_TICKS = 44;

    private final cn.academy.util.RayBeam beam =
            new cn.academy.util.RayBeam(this, DAMAGE_TICKS, RayReflect.DEFAULT_EXTEND_MS, this::reflectServer);

    public Railgun() {
        super("railgun", 4);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END) {
            beam.serverTick();
        }
    }

    public static boolean isAccepted(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ACCEPTED_ITEMS.contains(stack.getItem());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new Delegate());
    }

    @SubscribeEvent
    public void onThrowCoin(CoinThrowEvent evt) {
        Player player = evt.getEntity();

        if (!cn.lambdalib2.datapart.EntityData.isReady(player)) return;
        boolean spawn = CPData.get(player).canUseAbility()
                && PresetData.get(player).getCurrentPreset().hasControllable(this);
        if (!spawn) return;

        if (player.level().isClientSide) {
            informDelegate(evt.coin);
        } else {

            NetworkMessage.sendToTracking(player, this, MSG_CHARGE_EFFECT, player, evt.hand.ordinal());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void informDelegate(EntityCoinThrowing coin) {
        for (KeyDelegate dele : ClientRuntime.instance().getDelegates(ClientRuntime.DEFAULT_GROUP)) {
            if (dele instanceof Delegate rgdele) {
                rgdele.informThrowCoin(coin);
                return;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_CHARGE_EFFECT, side = LogicalSide.CLIENT)
    private void hSpawnClientEffect(Player target, Integer handOrdinal) {
        spawnClientEffect(target, InteractionHand.values()[handOrdinal]);
    }

    @OnlyIn(Dist.CLIENT)
    static void spawnClientEffect(Player target, InteractionHand hand) {
        cn.academy.client.render.entity.ACEffectEntities.spawn(
                new cn.academy.entity.EntityRailgunHand(target, hand));
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_PERFORM, side = LogicalSide.CLIENT)
    private void performClient(Player player, Double length, Integer handOrdinal) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                ACSounds.EM_RAILGUN.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

        cn.academy.client.render.entity.ACEffectEntities.spawn(
                new cn.academy.entity.EntityRailgunFX(player, length, InteractionHand.values()[handOrdinal]));
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_PERFORM_PATH, side = LogicalSide.CLIENT)
    private void performPathClient(Player player, Integer handOrdinal, byte[] raw) {
        java.util.List<Vec3> path = RayReflect.decodePath(raw);
        if (path == null) {
            return;
        }
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                ACSounds.EM_RAILGUN.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

        cn.academy.entity.EntityRailgunFX eff = new cn.academy.entity.EntityRailgunFX(
                player, 0, InteractionHand.values()[handOrdinal]);

        eff.bendAlong(path);
        cn.academy.client.render.entity.ACEffectEntities.spawn(eff);
    }

    private void reflectServer(Player player, Entity reflector, Vec3 reflectDir, Vec3 hitPoint) {

        AbilityContext ctx = AbilityContext.ofIfReady(player, this);
        if (ctx == null) {
            return;
        }

        Vec3 dir = reflectDir != null ? reflectDir.normalize() : reflector.getLookAngle();

        LivingEntity hit = traceLiving(reflector, dir, REFLECT_DISTANCE);
        if (hit != null) {
            ctx.attack(hit, 14);
            hitEntity = true;
        }

        NetworkMessage.sendToTracking(player, this, MSG_REFLECT, player, reflector.getId(),
                (float) dir.x, (float) dir.y, (float) dir.z,
                (float) hitPoint.x, (float) hitPoint.y, (float) hitPoint.z);
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_REFLECT, side = LogicalSide.CLIENT)
    private void hReflectClient(Player player, Integer reflectorId, Float dx, Float dy, Float dz,
                                Float hx, Float hy, Float hz) {
        Entity reflector = player.level().getEntity(reflectorId);
        if (reflector == null) {
            return;
        }

        cn.academy.entity.EntityRailgunFX eff =
                new cn.academy.entity.EntityRailgunFX(player, REFLECT_DISTANCE, InteractionHand.MAIN_HAND);

        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(dx, dy, dz);
        if (dir.lengthSqr() <= 1.0e-6) {
            dir = reflector instanceof LivingEntity le
                    ? net.minecraft.world.phys.Vec3.directionFromRotation(le.getXRot(), le.getYHeadRot())
                    : reflector.getLookAngle();
        }
        eff.aimAt(new net.minecraft.world.phys.Vec3(hx, hy, hz), dir, REFLECT_DISTANCE);

        cn.academy.client.render.entity.ACEffectEntities.spawn(eff);

        player.level().playLocalSound(hx, hy, hz, ACSounds.EM_RAILGUN.get(),
                SoundSource.AMBIENT, 0.5f, 1.0f, false);
    }

    private static LivingEntity traceLiving(Entity from, double dist) {
        return traceLiving(from, from.getLookAngle(), dist);
    }

    private static LivingEntity traceLiving(Entity from, net.minecraft.world.phys.Vec3 dir, double dist) {
        net.minecraft.world.level.Level level = from.level();
        net.minecraft.world.phys.Vec3 eye = from.getEyePosition();
        net.minecraft.world.phys.Vec3 end = eye.add(dir.scale(dist));

        net.minecraft.world.phys.BlockHitResult bhr = level.clip(new net.minecraft.world.level.ClipContext(
                eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, from));
        net.minecraft.world.phys.Vec3 blockEnd =
                bhr.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? bhr.getLocation() : end;

        double bestSq = eye.distanceToSqr(blockEnd);
        LivingEntity best = null;
        net.minecraft.world.phys.AABB box = from.getBoundingBox()
                .expandTowards(dir.scale(dist)).inflate(1);
        for (Entity e : level.getEntities(from, box,
                e -> e instanceof LivingEntity && e.isPickable() && e != from

                        && (!(from instanceof net.minecraft.world.entity.player.Player fp)
                                || AbilityPipeline.canTarget(fp, e)))) {
            var clip = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d <= bestSq) {
                    bestSq = d;
                    best = (LivingEntity) e;
                }
            }
        }
        return best;
    }

    private void performServer(Player player, InteractionHand hand) {
        AbilityContext ctx = AbilityContext.of(player, this);
        float exp = ctx.getSkillExp();

        float cp = AbilityConfig.cp("railgun", exp);
        float overload = AbilityConfig.overload("railgun", exp);
        if (ctx.consume(overload, cp)) {
            float dmg = AbilityConfig.stat("railgun", "damage", exp);
            float energy = lerpf(900, 2000, exp);

            cn.academy.util.RayBeam.Shape shape = fireBeam(player, dmg, energy, 45d, e -> true, null);

            ctx.addSkillExp(hitEntity ? 0.01f : 0.005f);
            ctx.setCooldown((int) AbilityConfig.cooldown("railgun", exp));

            if (shape.isStraight()) {
                NetworkMessage.sendToTracking(player, this, MSG_PERFORM, player, shape.length(), hand.ordinal());
            } else {

                NetworkMessage.sendToTracking(player, this, MSG_PERFORM_PATH,
                        player, hand.ordinal(), RayReflect.encodePath(shape.path()));
            }
        }
    }

    public cn.academy.util.RayBeam.Shape fireBeam(
            Player caster, float dmg, float energy, double maxLen,
            java.util.function.Predicate<Entity> selector,
            java.util.function.BiConsumer<Entity, cn.academy.event.ability.ReflectEvent> onReflected) {

        return beam.fire(caster, dmg, energy, maxLen, 2, selector, onReflected);
    }

    @Listener(channel = MSG_COIN_PERFORM, side = LogicalSide.SERVER)
    private void consumeCoinAtServer(Player player) {
        EntityCoinThrowing coin = CoinItem.getPlayerCoin(player);

        InteractionHand hand = coin != null ? coin.getHand() : InteractionHand.MAIN_HAND;
        if (coin != null) {

            coin.discard();
        }
        performServer(player, hand);
    }

    @Listener(channel = MSG_ITEM_PERFORM, side = LogicalSide.SERVER)
    private void consumeItemAtServer(Player player) {
        ItemStack equipped = player.getMainHandItem();
        if (isAccepted(equipped)) {
            if (!player.getAbilities().instabuild) {
                equipped.shrink(1);
                if (equipped.getCount() == 0) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }

            performServer(player, InteractionHand.MAIN_HAND);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class Delegate extends KeyDelegate {

        EntityCoinThrowing coin;
        int chargeTicks = -1;

        void informThrowCoin(EntityCoinThrowing c) {
            if (this.coin == null || !this.coin.isAlive()) {
                this.coin = c;
                onKeyAbort();
            }
        }

        @Override
        public void onKeyDown() {
            if (coin == null) {
                if (Railgun.isAccepted(getPlayer().getMainHandItem())) {

                    Railgun.spawnClientEffect(getPlayer(), InteractionHand.MAIN_HAND);
                    chargeTicks = 20;
                }
            } else {

                if (coin.getProgress() > AbilityConfig.coinHitThreshold()) {
                    NetworkMessage.sendToServer(INSTANCE, MSG_COIN_PERFORM, getPlayer());
                }
                coin = null;
            }
        }

        @Override
        public void onKeyTick() {
            if (chargeTicks != -1) {
                chargeTicks -= 1;
                if (chargeTicks == 0) {
                    NetworkMessage.sendToServer(INSTANCE, MSG_ITEM_PERFORM, getPlayer());
                }
            }
        }

        @Override
        public void onKeyUp() {
            chargeTicks = -1;
        }

        @Override
        public void onKeyAbort() {
            chargeTicks = -1;
        }

        @Override
        public DelegateState getState() {
            if (coin != null && coin.isAlive()) {
                return coin.getProgress() < AbilityConfig.coinChargeThreshold() ? DelegateState.CHARGE : DelegateState.ACTIVE;
            }
            return chargeTicks == -1 ? DelegateState.IDLE : DelegateState.CHARGE;
        }

        @Override
        public ResourceLocation getIcon() {
            return INSTANCE.getHintIcon();
        }

        @Override
        public int createID() {
            return 0;
        }

        @Override
        public Skill getSkill() {
            return INSTANCE;
        }
    }
}
