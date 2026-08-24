package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ACSounds;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.vanilla.teleporter.LocTeleportData;
import cn.academy.ability.vanilla.teleporter.Location;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class LocationTeleport extends Skill {

    public static final LocationTeleport INSTANCE = new LocationTeleport();

    public LocationTeleport() {
        super("location_teleport", 3);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public ResourceLocation getIcon() {
                return LocationTeleport.this.getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Skill getSkill() {
                return LocationTeleport.INSTANCE;
            }

            @Override
            public void onKeyDown() {

                cn.academy.client.gui.LocTeleUI.open();
            }
        });
    }

    public static boolean canCrossDimension(Player player) {
        return AbilityData.get(player).getSkillExp(INSTANCE) > 0.8f;
    }

    public static boolean isCrossDim(Player player, Location dest) {
        return !player.level().dimension().location().toString().equals(dest.dim);
    }

    public static float[] getConsumption(Player player, Location dest) {
        float exp = AbilityData.get(player).getSkillExp(INSTANCE);
        float distance = (float) Math.sqrt(player.distanceToSqr(dest.x, dest.y, dest.z));
        float dimPenalty = isCrossDim(player, dest) ? 2 : 1;
        float cp = AbilityConfig.cp("location_teleport", exp) * dimPenalty
                * Math.max(8.0f, (float) Math.sqrt(Math.min(800, distance)));
        return new float[]{AbilityConfig.overload("location_teleport", exp), cp};
    }

    public static String getPerformStat(Player player, Location dest) {
        if (isCrossDim(player, dest) && !canCrossDimension(player)) {
            return "gui.academy.loctele.err_exp";
        }
        AbilityContext ctx = AbilityContext.of(player, INSTANCE);
        if (!ctx.canConsumeCP(getConsumption(player, dest)[1])) {
            return "gui.academy.loctele.err_cp";
        }
        return null;
    }

    public static void perform(ServerPlayer player, Location dest) {

        ServerLevel target = player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dest.dim)));
        if (target == null) {
            player.sendSystemMessage(Component.literal("目标维度不存在: " + dest.dim));
            return;
        }
        if (getPerformStat(player, dest) != null) {
            return;
        }

        AbilityContext ctx = AbilityContext.of(player, INSTANCE);
        float exp = ctx.getSkillExp();
        float[] oc = getConsumption(player, dest);
        ctx.consumeWithForce(oc[0], oc[1]);

        List<Entity> targets = new ArrayList<>();
        targets.add(player);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(5),
                e -> e != player && e.isAlive()
                        && e.getBbWidth() * e.getBbWidth() * e.getBbHeight() < 80f
                        && e.distanceToSqr(player) <= 25)) {
            targets.add(e);
        }

        double dist = Math.sqrt(player.distanceToSqr(dest.x, dest.y, dest.z));
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        for (Entity e : targets) {
            double dx = e.getX() - px, dy = e.getY() - py, dz = e.getZ() - pz;
            if (e.isPassenger()) e.stopRiding();
            e.teleportTo(target, dest.x + dx, dest.y + dy, dest.z + dz,
                    Set.of(), e.getYRot(), e.getXRot());
            e.fallDistance = 0;
        }

        ctx.addSkillExp(dist >= 200 ? 0.03f : 0.015f);
        ctx.setCooldown((int) AbilityConfig.cooldown("location_teleport", exp));

        target.playSound(player, dest.x, dest.y, dest.z,
                ACSounds.TP_MOVE_PLAYER.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
    }

    public static final class Net {
        public static final Net INSTANCE = new Net();

        public static final String MSG_ADD = "add";
        public static final String MSG_REMOVE = "remove";
        public static final String MSG_PERFORM = "perform";
        public static final String MSG_REFRESH = "refresh";

        private Net() {}

        public static void init() {
            NetworkS11n.addDirectInstance(INSTANCE);
        }

        @Listener(channel = MSG_ADD, side = LogicalSide.SERVER)
        private void hAdd(Player player, String name) {
            LocTeleportData data = LocTeleportData.of(player);
            data.add(name, player.level().dimension().location().toString(),
                    (float) player.getX(), (float) player.getY(), (float) player.getZ());
            NetworkMessage.sendTo(player, INSTANCE, MSG_REFRESH);
        }

        @Listener(channel = MSG_REMOVE, side = LogicalSide.SERVER)
        private void hRemove(Player player, int id) {
            LocTeleportData.of(player).remove(id);
            NetworkMessage.sendTo(player, INSTANCE, MSG_REFRESH);
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void hPerform(Player player, int id) {
            Location dest = LocTeleportData.of(player).get(id);
            if (dest != null && player instanceof ServerPlayer sp) {
                perform(sp, dest);
            }
        }

        @Listener(channel = MSG_REFRESH, side = LogicalSide.CLIENT)
        private void hRefresh() {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> cn.academy.client.gui.LocTeleUI::refreshCurrent);
        }
    }
}
