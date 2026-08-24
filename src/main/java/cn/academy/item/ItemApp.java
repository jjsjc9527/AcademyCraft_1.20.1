package cn.academy.item;

import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemApp extends Item {

    private static final Map<String, ItemApp> items = new HashMap<>();

    public static ItemApp getItemForApp(App app) {
        return items.get(app.getName());
    }

    private final String _appName;

    private App _app;

    public ItemApp(Properties p, String name) {
        super(p);
        _appName = name;
        items.put(_appName, this);
    }

    private App getApp() {
        if (_app == null) {
            _app = AppRegistry.getByName(_appName);
            if (_app == null) {
                throw new IllegalStateException("App not found: " + _appName
                        + " -- an ItemApp was registered without a matching App");
            }
        }
        return _app;
    }

    @Override
    public String getDescriptionId() {
        return "item.academy.apps";
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        App app = getApp();
        if (!world.isClientSide) {
            TerminalData terminalData = TerminalData.get(player);
            if (!terminalData.isTerminalInstalled()) {
                player.sendSystemMessage(Component.translatable("terminal.academy.notinstalled"));
            } else if (terminalData.isInstalled(app)) {
                player.sendSystemMessage(Component.translatable("terminal.academy.app_alrdy_installed",
                        Component.translatable(app.getDisplayKey())));
            } else {
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                terminalData.installApp(app);
                player.sendSystemMessage(Component.translatable("terminal.academy.app_installed",
                        Component.translatable(app.getDisplayKey())));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable(getApp().getDisplayKey()));
    }

}
