package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.config.Property;
import cn.academy.event.BlockDestroyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbilityPipeline {

    private AbilityPipeline() {}

    private static Property propAttackPlayer;
    private static Property propDestroyBlocks;
    private static Property propWorldsDestroyingBlocks;
    private static Property propUseMouseWheel;
    private static Property propTpWheelSensitivity;

    public static void init() {
        cn.academy.config.Configuration conf = AcademyCraft.config;

        propAttackPlayer = conf.get("generic", "attackPlayer", true,
                "Whether the skills are effective on players.");
        propDestroyBlocks = conf.get("generic", "destroyBlocks", true,
                "Whether the skills will destroy blocks in the world.");
        propWorldsDestroyingBlocks = conf.get("generic", "worldsWhitelistedDestroyingBlocks", new String[]{},
                "The worlds which whitelisted destroying blocks. Dimension IDs like "
                        + "\"minecraft:the_nether\" (or just \"the_nether\") are supported.");
        propUseMouseWheel = conf.get("generic", "useMouseWheel", false,
                "Whether teleporter can use mouse wheel to control the destination.");
        propTpWheelSensitivity = conf.get("generic", "tpWheelSensitivity", 25,
                "Teleporter mouse wheel sensitivity percent: 0 = 1 block per notch (old speed), "
                        + "100 = one notch sweeps the full current max distance.");

        MinecraftForge.EVENT_BUS.register(new AbilityPipeline());
    }

    static boolean canBreakBlock(Level world) {
        if (propDestroyBlocks.getBoolean()) {
            return true;
        }
        ResourceLocation id = world.dimension().location();
        for (String s : propWorldsDestroyingBlocks.getStringList()) {
            if (s.equals(id.toString()) || s.equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }

    static boolean isAllWorldDisableBreakBlock() {
        return !propDestroyBlocks.getBoolean() && propWorldsDestroyingBlocks.getIntList().length == 0;
    }

    public static boolean canAttackPlayer() {
        return propAttackPlayer.getBoolean();
    }

    public static boolean canTarget(Player attacker, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof Player p)) {
            return true;
        }
        return canAttackPlayer() && !p.isSpectator() && attacker.canHarmPlayer(p);
    }

    public static boolean canUseMouseWheel() {
        return propUseMouseWheel.getBoolean();
    }

    public static float tpWheelSensitivity() {
        return Math.max(0, Math.min(100, propTpWheelSensitivity.getInt())) / 100f;
    }

    static boolean canBreakBlock(Level world, Player player, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, player, new BlockPos(x, y, z)));
    }

    static boolean canBreakBlock(Level world, Player player, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, player, pos));
    }

    static boolean canBreakBlock(Level world, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, new BlockPos(x, y, z)));
    }

    static boolean canBreakBlock(Level world, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, pos));
    }

    static boolean canBreakBlock(Player player, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(player, new BlockPos(x, y, z)));
    }

    static boolean canBreakBlock(Player player, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(player, pos));
    }

    @SubscribeEvent
    public void onBlockDestroy(BlockDestroyEvent event) {
        if (!canBreakBlock(event.world)) {
            event.setCanceled(true);
        }
    }
}
