package cn.lambdalib2.input;

import cn.lambdalib2.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class InputGate {

    private static final InputGate INSTANCE = new InputGate();

    private static final Set<Integer> stale = new HashSet<>();

    private static final Set<Integer> heldThrough = new HashSet<>();

    private static boolean inGame = false;

    private InputGate() {}

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static boolean isStale(int keyID) {
        return inGame ? stale.contains(keyID) : !heldThrough.contains(keyID);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        if (Minecraft.getInstance().getWindow() == null) return;

        boolean now = ClientUtils.isPlayerInGame();

        if (!now) {
            if (inGame) {

                heldThrough.clear();
                collectDown(heldThrough);
            } else {

                heldThrough.removeIf(k -> !KeyManager.getKeyDown(k));
            }
        } else if (!inGame) {

            Set<Integer> justPressed = new HashSet<>();
            collectDown(justPressed);
            justPressed.removeAll(heldThrough);
            stale.addAll(justPressed);
            heldThrough.clear();
        }

        inGame = now;

        if (!stale.isEmpty()) {
            stale.removeIf(k -> !KeyManager.getKeyDown(k));
        }
    }

    private static void collectDown(Set<Integer> out) {
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; ++k) {
            if (KeyManager.getKeyDown(k)) out.add(k);
        }
        for (int b = 0; b <= GLFW.GLFW_MOUSE_BUTTON_LAST; ++b) {
            if (KeyManager.getKeyDown(b - 100)) out.add(b - 100);
        }
    }
}
