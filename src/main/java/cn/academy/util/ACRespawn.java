package cn.academy.util;

import java.util.function.Supplier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

public final class ACRespawn {

    private ACRespawn() {}

    private static boolean rebuilding;

    public static boolean isRebuilding() {
        return rebuilding;
    }

    public static <T> T inRebuild(Supplier<T> body) {
        boolean prev = rebuilding;
        rebuilding = true;
        try {
            return body.get();
        } finally {
            rebuilding = prev;
        }
    }

    public static void inRebuild(Runnable body) {
        inRebuild(() -> {
            body.run();
            return null;
        });
    }

    private static final java.util.Map<java.util.UUID, Long> PENDING_UNTIL = new java.util.HashMap<>();

    public static void markPending(java.util.UUID id, long deadlineGameTime) {
        PENDING_UNTIL.put(id, deadlineGameTime);
    }

    public static void clearPending(java.util.UUID id) {
        PENDING_UNTIL.remove(id);
    }

    public static boolean isPendingRebuild(java.util.UUID id, long now) {
        Long until = PENDING_UNTIL.get(id);
        if (until == null) {
            return false;
        }
        if (now > until) {
            PENDING_UNTIL.remove(id);
            return false;
        }
        return true;
    }

    public static ServerPlayer rebuildInPlace(ServerPlayer old, float health) {
        MinecraftServer server = old.getServer();
        if (server == null || !(old.level() instanceof ServerLevel level)) {
            return null;
        }

        final float yRot = old.getYRot();
        final float xRot = old.getXRot();

        double sx = old.getX();
        double sy = old.getY();
        double sz = old.getZ();
        if (sy < level.getMinBuildHeight() + 1) {

            double[] safe = latestSafeSpot(old.getUUID());
            if (safe != null) {

                LOG.info("[ac-respawn] in the void, using the nearest safe spot: ({}, {}, {}) -> ({}, {}, {})",
                        sx, sy, sz, safe[0], safe[1], safe[2]);
                return rebuildAt(old, health, server, level, safe[0], safe[1], safe[2], yRot, xRot);
            }

            int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.util.Mth.floor(sx), net.minecraft.util.Mth.floor(sz));
            if (gy > level.getMinBuildHeight()) {
                LOG.info("[ac-respawn] in the void, raising Y to ground: ({}, {}, {}) -> y={}", sx, sy, sz, gy);
                sy = gy;
            } else {

                net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();
                int spawnTop = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        spawn.getX(), spawn.getZ());
                LOG.info("[ac-respawn] in the void and column ({}, {}) has no ground, falling back to spawn {} at ground y={}",
                        sx, sz, spawn, spawnTop);
                sx = spawn.getX() + 0.5;
                sy = Math.max(spawnTop, spawn.getY());
                sz = spawn.getZ() + 0.5;
            }
        }
        return rebuildAt(old, health, server, level, sx, sy, sz, yRot, xRot);
    }

    private static final java.util.Map<java.util.UUID, double[][]> SAFE = new java.util.HashMap<>();

    public static void pushSafeSpot(java.util.UUID id, double x, double y, double z) {
        double[][] slots = SAFE.computeIfAbsent(id, k -> new double[2][]);
        slots[1] = slots[0];
        slots[0] = new double[] {x, y, z};

    }

    public static double[] latestSafeSpot(java.util.UUID id) {
        double[][] slots = SAFE.get(id);
        if (slots == null) {
            return null;
        }
        return slots[0] != null ? slots[0] : slots[1];
    }

    public static void snapshotInventory(ServerPlayer sp) {
        try {
            net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
            sp.getInventory().save(tag);
            INV.put(sp.getUUID(), new InvSnapshot(tag, sp.level().getGameTime(),
                    sp.getX(), sp.getY(), sp.getZ()));

            LOG.info("[ac-respawn] inventory snapshot: {} slots", tag.size());
        } catch (Throwable t) {
            LOG.warn("[ac-respawn] inventory snapshot failed: {}", t.toString());
        }
    }

    private record InvSnapshot(net.minecraft.nbt.ListTag inv, long gameTime,
                               double x, double y, double z) {}

    private static final java.util.Map<java.util.UUID, InvSnapshot> INV = new java.util.HashMap<>();

    private static void restoreInventory(ServerPlayer fresh, ServerLevel level) {
        InvSnapshot snap = INV.remove(fresh.getUUID());
        if (snap == null) {
            return;
        }
        long age = Math.max(0L, level.getGameTime() - snap.gameTime()) + 5L;
        int cleared = 0;
        try {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    snap.x() - 16, snap.y() - 128, snap.z() - 16,
                    snap.x() + 16, snap.y() + 16, snap.z() + 16);
            for (net.minecraft.world.entity.item.ItemEntity it
                    : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
                if (it.tickCount <= age) {
                    it.discard();
                    cleared++;
                }
            }
        } catch (Throwable t) {
            LOG.warn("[ac-respawn] failed to clear dropped items (inventory will still be restored): {}", t.toString());
        }
        try {
            fresh.getInventory().load(snap.inv());

            fresh.inventoryMenu.broadcastFullState();
            LOG.info("[ac-respawn] inventory restored and fully synced, also cleared {} items dropped this time", cleared);
        } catch (Throwable t) {
            LOG.warn("[ac-respawn] inventory restore failed: {}", t.toString());
        }
    }

    private static ServerPlayer rebuildAt(ServerPlayer old, float health, MinecraftServer server,
                                          ServerLevel level, double x, double y, double z,
                                          float yRot, float xRot) {
        return inRebuild(() -> {
            PlayerList list = server.getPlayerList();
            cn.academy.mixin.PlayerListAccessor acc = (cn.academy.mixin.PlayerListAccessor) list;

            acc.academy$players().remove(old);
            level.removePlayerImmediately(old, Entity.RemovalReason.DISCARDED);

            ServerPlayer fresh = new ServerPlayer(server, level, old.getGameProfile());
            fresh.connection = old.connection;
            fresh.connection.player = fresh;
            fresh.restoreFrom(old, true);
            fresh.setHealth(health);
            fresh.setId(old.getId());
            fresh.setMainArm(old.getMainArm());
            for (String tag : old.getTags()) {
                fresh.addTag(tag);
            }
            fresh.moveTo(x, y, z, yRot, xRot);

            level.addRespawnedPlayer(fresh);
            acc.academy$players().add(fresh);
            acc.academy$playersByUUID().put(fresh.getUUID(), fresh);
            fresh.initInventoryMenu();

            fresh.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(
                    fresh.level().dimensionTypeId(),
                    fresh.level().dimension(),
                    net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(level.getSeed()),
                    fresh.gameMode.getGameModeForPlayer(),
                    fresh.gameMode.getPreviousGameModeForPlayer(),
                    fresh.level().isDebug(),
                    level.isFlat(),
                    (byte) 1,
                    fresh.getLastDeathLocation(),
                    fresh.getPortalCooldown()));
            fresh.connection.teleport(x, y, z, yRot, xRot);
            fresh.connection.send(new net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket(
                    level.getSharedSpawnPos(), level.getSharedSpawnAngle()));
            fresh.connection.send(new net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket(
                    level.getLevelData().getDifficulty(), level.getLevelData().isDifficultyLocked()));
            fresh.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                    fresh.experienceProgress, fresh.totalExperience, fresh.experienceLevel));
            list.sendLevelInfo(fresh, level);
            list.sendPlayerPermissionLevel(fresh);
            cn.academy.network.DeathScreenReleaseMessage.send(fresh);

            restoreInventory(fresh, level);

            try {
                cn.lambdalib2.datapart.EntityData<net.minecraft.world.entity.player.Player> ed =
                        cn.lambdalib2.datapart.EntityData.get(fresh);
                if (ed != null) {
                    ed.syncAllToClient();
                    LOG.info("[ac-respawn] all DataParts resent to the client");
                }
            } catch (Throwable t) {
                LOG.warn("[ac-respawn] DataPart resend failed: {}", t.toString());
            }

            int bound = cn.academy.ability.context.ContextManager.instance.rebindAll(old, fresh);
            boolean ready = cn.lambdalib2.datapart.EntityData.isReady(fresh);
            Object cp;
            try {
                cp = cn.academy.datapart.CPData.get(fresh);
            } catch (Throwable t) {
                cp = "threw while reading: " + t;
            }
            LOG.info("[ac-respawn] context rebind={} | new shell EntityData ready={} | CPData={}",
                    bound, ready, cp == null ? "null" : "yes");

            net.minecraft.core.Direction g = cn.academy.gravity.ACGravity.getGravityDirection(fresh);
            if (g != net.minecraft.core.Direction.DOWN) {
                cn.academy.gravity.ACGravity.initGravityDirection(fresh, g);
                cn.academy.network.GravitySyncMessage.sync(fresh, g, false, true);
            }
            return fresh;
        });
    }

    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("ACRespawn");
}
