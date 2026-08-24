package cn.academy.util;

import cn.academy.AcademyCraft;
import cn.academy.event.ConfigModifyEvent;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ACKeyManager extends KeyManager {

    public static final ACKeyManager instance = new ACKeyManager();

    private ACKeyManager() {}

    @SubscribeEvent
    public void onConfigModified(ConfigModifyEvent event) {
        if (event.property.isIntValue())
            resetBindingKey(event.property.getName(), event.property.getInt());
    }

    @Override
    public void addKeyHandler(String name, String keyDesc, int defKeyID, boolean global, KeyHandler handler) {
        super.addKeyHandler(name, keyDesc, defKeyID, global, handler);
        SettingsUI.addProperty(PropertyElements.KEY, "keys", name, defKeyID, false);
    }

    @Override
    protected int loadKeyID(String name, int defKeyID) {
        return AcademyCraft.config.get("keys", name, defKeyID).getInt();
    }

    @Override
    protected void saveKeyID(String name, int keyID) {
        AcademyCraft.config.get("keys", name, keyID).set(keyID);
    }
}
