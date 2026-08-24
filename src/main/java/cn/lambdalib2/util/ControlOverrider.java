package cn.lambdalib2.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ControlOverrider {

    private ControlOverrider() {}

    private static final Map<String, int[]> overrideGroups = new HashMap<>();

    private static final Set<Integer> overridden = new HashSet<>();

    private static boolean completeOverriding;

    private static final ControlOverrider EVENTS = new ControlOverrider();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(EVENTS);
    }

    public static void override(String name, int... keys) {
        overrideGroups.put(name, keys.clone());
        rebuild();
    }

    public static void endOverride(String name) {
        if (overrideGroups.remove(name) != null) {
            rebuild();
        }
    }

    public static void startCompleteOverride() {
        completeOverriding = true;
    }

    public static void endCompleteOverride() {
        if (!completeOverriding) {
            throw new RuntimeException("ControlOverrider error: Try to stop complete override while not overriding at all");
        }
        completeOverriding = false;
    }

    public static boolean isOverriding(int keyID) {
        return completeOverriding || overridden.contains(keyID);
    }

    private static void rebuild() {
        overridden.clear();
        for (int[] keys : overrideGroups.values()) {
            for (int k : keys) overridden.add(k);
        }

        suppress();
    }

    private static InputConstants.Key toMcKey(int keyID) {
        if (keyID > 0) {
            return InputConstants.Type.KEYSYM.getOrCreate(keyID);
        }
        return InputConstants.Type.MOUSE.getOrCreate(keyID + 100);
    }

    public static void suppressAllNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        for (KeyMapping m : mc.options.keyMappings) {
            m.setDown(false);

            while (m.consumeClick()) {}
        }
    }

    private static void suppress() {
        if (!completeOverriding && overridden.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        Set<InputConstants.Key> targets = new HashSet<>();
        if (!completeOverriding) {
            for (int keyID : overridden) targets.add(toMcKey(keyID));
        }

        for (KeyMapping m : mc.options.keyMappings) {
            if (completeOverriding || targets.contains(m.getKey())) {
                m.setDown(false);

                while (m.consumeClick()) {}
            }
        }
    }

    @SubscribeEvent
    public void onMouseButtonPre(InputEvent.MouseButton.Pre event) {

        if (Minecraft.getInstance().screen != null) {
            return;
        }

        if (isOverriding(event.getButton() - 100)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            suppress();
        }
    }

}
