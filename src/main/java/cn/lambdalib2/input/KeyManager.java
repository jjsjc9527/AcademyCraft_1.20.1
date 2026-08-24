package cn.lambdalib2.input;

import cn.lambdalib2.auxgui.AuxGuiHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@OnlyIn(Dist.CLIENT)
public class KeyManager {

    public static final KeyManager dynamic = new KeyManager();

    public static final int
            MOUSE_LEFT = -100, MOUSE_MIDDLE = -98, MOUSE_RIGHT = -99,
            MWHEELDOWN = -50, MWHEELUP = -49;

    public static String getKeyName(int keyID) {
        try {
            if (keyID > 0) {
                return InputConstants.Type.KEYSYM.getOrCreate(keyID).getDisplayName().getString();
            }
            int btn = keyID + 100;
            if (btn < 0 || btn > GLFW.GLFW_MOUSE_BUTTON_LAST) return "undefined";
            return InputConstants.Type.MOUSE.getOrCreate(btn).getDisplayName().getString();
        } catch (Exception e) {
            return "undefined";
        }
    }

    public static boolean getKeyDown(int keyID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();

        if (keyID > 0) {
            return InputConstants.isKeyDown(window, keyID);
        }
        int btn = keyID + 100;
        if (btn < 0 || btn > GLFW.GLFW_MOUSE_BUTTON_LAST) return false;
        return GLFW.glfwGetMouseButton(window, btn) == GLFW.GLFW_PRESS;
    }

    private static boolean isPlayerInGame() {
        return cn.lambdalib2.util.ClientUtils.isPlayerInGame();
    }

    private boolean active = true;

    private int _anonymousHandlerCount = 0;

    private final Map<String, KeyHandlerState> _bindingMap = new HashMap<>();

    public KeyManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getKeyID(KeyHandler handler) {
        KeyHandlerState kb = getKeyBinding(handler);
        return kb == null ? -1 : kb.keyID;
    }

    public void addKeyHandler(int keyID, KeyHandler handler) {
        addKeyHandler(keyID, false, handler);
    }

    public void addKeyHandler(int keyID, boolean global, KeyHandler handler) {
        String name = "_anonymous_" + _anonymousHandlerCount;
        addKeyHandler(name, "", keyID, global, handler);
        ++_anonymousHandlerCount;
    }

    public void addKeyHandler(String name, int defKeyID, KeyHandler handler) {
        addKeyHandler(name, "", defKeyID, false, handler);
    }

    public void addKeyHandler(String name, String keyDesc, int defKeyID, KeyHandler handler) {
        addKeyHandler(name, keyDesc, defKeyID, false, handler);
    }

    public void addKeyHandler(String name, String keyDesc, int defKeyID, boolean global, KeyHandler handler) {
        if (_bindingMap.containsKey(name))
            throw new RuntimeException("Duplicate key: " + name + " of object " + handler);

        int keyID = loadKeyID(name, defKeyID);
        _bindingMap.put(name, new KeyHandlerState(handler, keyID, global));
    }

    public void removeKeyHandler(String name) {
        KeyHandlerState kb = _bindingMap.get(name);
        if (kb != null)
            kb.dead = true;
    }

    public void resetBindingKey(String name, int newKey) {
        KeyHandlerState kb = _bindingMap.get(name);
        if (kb != null) {
            saveKeyID(name, newKey);

            kb.keyID = newKey;
            if (kb.keyDown)
                kb.handler.onKeyAbort();

            kb.keyDown = false;
        }
    }

    protected int loadKeyID(String name, int defKeyID) {
        return defKeyID;
    }

    protected void saveKeyID(String name, int keyID) {}

    private void tick() {
        Iterator<Entry<String, KeyHandlerState>> iter = _bindingMap.entrySet().iterator();
        boolean shouldAbort = !isPlayerInGame();

        while (iter.hasNext()) {
            Entry<String, KeyHandlerState> entry = iter.next();
            KeyHandlerState kb = entry.getValue();
            if (kb.dead) {
                iter.remove();
            } else {
                boolean down = getKeyDown(kb.keyID);

                if (down && !kb.isGlobal && InputGate.isStale(kb.keyID)) {
                    down = false;
                }

                boolean abort = shouldAbort && !kb.isGlobal;

                if (kb.keyDown && abort) {
                    kb.keyDown = false;
                    kb.keyAborted = true;
                    kb.handler.onKeyAbort();
                } else if (!kb.keyDown && down && !abort && !kb.keyAborted) {
                    kb.keyDown = true;
                    kb.handler.onKeyDown();
                } else if (kb.keyDown && !down && !abort) {
                    kb.keyDown = false;
                    kb.handler.onKeyUp();
                } else if (kb.keyDown && down && !abort) {
                    kb.handler.onKeyTick();
                }

                if (!down) {
                    kb.keyAborted = false;
                }

                kb.keyDown = down;
            }
        }
    }

    private KeyHandlerState getKeyBinding(KeyHandler handler) {
        for (KeyHandlerState kb : _bindingMap.values()) {
            if (kb.handler == handler)
                return kb;
        }
        return null;
    }

    @SubscribeEvent
    public void _onEvent(ClientTickEvent event) {
        if (event.phase == Phase.START && active) {
            tick();
        }
    }

    private static class KeyHandlerState {
        final KeyHandler handler;
        final boolean isGlobal;

        int keyID;

        boolean keyDown;
        boolean keyAborted;

        boolean dead;

        private KeyHandlerState(KeyHandler h, int k, boolean g) {
            handler = h;
            keyID = k;
            isGlobal = g;
        }
    }

}
