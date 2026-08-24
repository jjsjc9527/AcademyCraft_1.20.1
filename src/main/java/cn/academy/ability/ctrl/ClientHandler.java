package cn.academy.ability.ctrl;

import cn.academy.AcademyCraft;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.client.auxgui.PresetEditUI;
import cn.academy.client.auxgui.CPBar;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.datapart.PresetData;
import cn.academy.event.ConfigModifyEvent;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.event.ability.PresetSwitchEvent;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.academy.util.ACKeyManager;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientHandler {

    private ClientHandler() {}

    public static final String
            KEY_SWITCH_PRESET = "switch_preset",
            KEY_EDIT_PRESET = "edit_preset",
            KEY_ACTIVATE_ABILITY = "ability_activation";

    private static final int[] keyIDsInit = new int[]{
            KeyManager.MOUSE_LEFT,
            KeyManager.MOUSE_RIGHT,
            GLFW.GLFW_KEY_R,
            GLFW.GLFW_KEY_F
    };

    private static final int[] keyIDs = new int[keyIDsInit.length];

    public static void init() {
        updateAbilityKeys();
        for (int i = 0; i < keyIDsInit.length; ++i) {
            SettingsUI.addProperty(PropertyElements.KEY, "keys", "ability_" + i, keyIDsInit[i], false);
        }

        ACKeyManager.instance.addKeyHandler(KEY_EDIT_PRESET, "", GLFW.GLFW_KEY_N, keyEditPreset);
        ACKeyManager.instance.addKeyHandler(KEY_ACTIVATE_ABILITY, "", GLFW.GLFW_KEY_V, keyActivate);
        ACKeyManager.instance.addKeyHandler(KEY_SWITCH_PRESET, "", GLFW.GLFW_KEY_C, keySwitchPreset);

        MinecraftForge.EVENT_BUS.register(new ConfigHandler());
    }

    private static void updateAbilityKeys() {
        cn.academy.config.Configuration cfg = AcademyCraft.config;
        for (int i = 0; i < getKeyCount(); ++i) {
            keyIDs[i] = cfg.getInt("ability_" + i, "keys", keyIDsInit[i], "Ability control key #" + i);
        }

        MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
    }

    public static int getKeyMapping(int id) {
        return keyIDs[id];
    }

    public static int getKeyCount() {
        return keyIDsInit.length;
    }

    private static boolean academy$notReady(Player player) {
        return player == null || !cn.lambdalib2.datapart.EntityData.isReady(player);
    }

    public static final KeyHandler keyActivate = new KeyHandler() {

        double lastKeyDown;

        boolean armed;

        @Override
        public void onKeyUp() {

            boolean paired = armed;
            armed = false;
            double delta = GameTimer.getTime() - lastKeyDown;

            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (cn.academy.util.ACDiag.ON)
                org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/Keys").warn(
                        "[key-v] onKeyUp | delta={} | pairedKeyDown={} | willToggleActivation={} | currentScreen={}",
                        String.format("%.4f", delta), paired, paired && delta < 0.300,
                        mc.screen == null ? "none" : mc.screen.getClass().getName());
            } catch (Throwable ignored) {

            }
            if (paired && delta < 0.300) {
                Player player = getPlayer();

                if (academy$notReady(player)) {
                    CPBar.instance.stopDisplayNumbers();
                    return;
                }

                if (cn.academy.client.gui.RemoteControlScreen.tryOpenFromKey(player)) {
                    CPBar.instance.stopDisplayNumbers();
                    return;
                }

                AbilityData aData = AbilityData.get(player);

                if (aData.hasCategory()) {

                    CPData cpData = CPData.get(player);
                    if (!cpData.isActivated()
                            && cn.academy.ability.vanilla.mentalout.passiveskill.WideCast
                                    .unlocked(player)) {
                        return;
                    }
                    ClientRuntime.instance().getActivateHandler().onKeyDown(player);
                }
            }

            CPBar.instance.stopDisplayNumbers();
        }

        @Override
        public void onKeyDown() {
            lastKeyDown = GameTimer.getTime();
            armed = true;
            CPBar.instance.startDisplayNumbers();
        }

    };

    public static final KeyHandler keyEditPreset = new KeyHandler() {
        @Override
        public void onKeyDown() {
            Player player = getPlayer();
            if (academy$notReady(player)) {
                return;
            }
            if (AbilityData.get(player).hasCategory()) {
                Minecraft.getInstance().setScreen(new PresetEditUI());
            }
        }
    };

    public static final KeyHandler keySwitchPreset = new KeyHandler() {
        @Override
        public void onKeyDown() {
            Player player = getPlayer();
            if (academy$notReady(player)) {
                return;
            }
            PresetData data = PresetData.get(player);
            CPData cpData = CPData.get(player);

            if (cpData.isActivated()) {

                int next = (data.getCurrentID() + 1) % PresetData.MAX_PRESETS;
                data.switchFromClient(next);
                MinecraftForge.EVENT_BUS.post(new PresetSwitchEvent(data.getEntity()));
            }
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static class ConfigHandler {

        @SubscribeEvent
        public void onConfigModify(ConfigModifyEvent evt) {
            updateAbilityKeys();
        }
    }

}
