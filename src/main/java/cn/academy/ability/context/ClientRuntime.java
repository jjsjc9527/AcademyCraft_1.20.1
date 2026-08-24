package cn.academy.ability.context;

import cn.academy.AcademyCraft;
import cn.academy.ability.Controllable;
import cn.academy.ability.context.Context.Status;
import cn.academy.ability.ctrl.ClientHandler;
import cn.academy.client.auxgui.TerminalUI;
import cn.academy.datapart.CPData;
import cn.academy.datapart.CooldownData;
import cn.academy.datapart.PresetData;
import cn.academy.datapart.PresetData.Preset;
import cn.academy.event.ability.AbilityActivateEvent;
import cn.academy.event.ability.AbilityDeactivateEvent;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.event.ability.PresetSwitchEvent;
import cn.academy.event.ability.PresetUpdateEvent;
import cn.academy.util.ACKeyManager;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.input.InputGate;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.util.ClientUtils;
import cn.lambdalib2.util.ControlOverrider;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
@RegDataPart(value = Player.class, side = LogicalSide.CLIENT)
public class ClientRuntime extends DataPart<Player> {

    public static final String DEFAULT_GROUP = "def";
    private static final String OVERRIDE_GROUP = "AC_ClientRuntime";

    public static ClientRuntime instance() {
        Player player = Minecraft.getInstance().player;
        Preconditions.checkNotNull(player);
        return EntityData.get(player).getPart(ClientRuntime.class);
    }

    public static boolean available() {
        Player player = Minecraft.getInstance().player;
        return player != null && EntityData.isReady(player) && EntityData.get(player) != null;
    }

    private final Map<Integer, DelegateNode> delegates = new TreeMap<>();
    private final Multimap<String, DelegateNode> delegateGroups = ArrayListMultimap.create();
    private final Map<Integer, KeyState> keyStates = new HashMap<>();

    private final LinkedList<IActivateHandler> activateHandlers = new LinkedList<>();

    private boolean ctrlDirty = false;
    private boolean requireFlush = false;

    {
        setTick(true);
    }

    private boolean isLocal() {
        return getEntity() == Minecraft.getInstance().player;
    }

    @Override
    public void tick() {
        if (!isLocal()) {
            return;
        }
        final CPData cpData = CPData.get(getEntity());
        final CooldownData cdData = CooldownData.of(getEntity());

        for (DelegateNode node : delegates.values()) {
            final KeyState state = getKeyState(node.keyID);

            final boolean keyDown = KeyManager.getKeyDown(node.keyID) && !InputGate.isStale(node.keyID);

            boolean shouldAbort =
                    !ClientUtils.isPlayerInGame() ||
                            cdData.isInCooldown(node.delegate.getSkill(), node.delegate.getIdentifier()) ||
                            !cpData.canUseAbility() ||
                            AuxGuiHandler.active().stream().anyMatch(a -> a instanceof TerminalUI) ||
                            cn.academy.ability.vanilla.mentalout.Helpless.isHelpless(getEntity());
            final KeyDelegate delegate = node.delegate;

            if (keyDown && state.state && !shouldAbort) {
                delegate.onKeyTick();
            }
            if (keyDown && !state.state && !state.realState && !shouldAbort) {
                delegate.onKeyDown();
                state.state = true;
            }
            if (!keyDown && state.state && !shouldAbort) {
                delegate.onKeyUp();
                state.state = false;
            }
            if (state.state && shouldAbort) {
                delegate.onKeyAbort();
                state.state = false;
            }

            state.realState = keyDown;
        }

        {
            Iterator<Entry<Integer, KeyState>> iter = keyStates.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Integer, KeyState> ent = iter.next();
                if (!ent.getValue().realState && !delegates.containsKey(ent.getKey())) {
                    iter.remove();
                }
            }
        }

        if (ctrlDirty) {
            rebuildOverrides();
        }

        if (requireFlush) {
            requireFlush = false;
            updateDefaultGroup();
        }
    }

    @Override
    public void wake() {
        ctrlDirty = true;
        requireFlush = true;
    }

    public void addKey(int keyID, KeyDelegate delegate) {
        addKey(DEFAULT_GROUP, keyID, delegate);
    }

    public void addKey(String group, int keyID, KeyDelegate delegate) {

        Preconditions.checkState(!delegateGroups.containsKey(keyID));

        DelegateNode node = new DelegateNode(delegate, keyID);
        delegates.put(keyID, node);
        delegateGroups.put(group, node);

        ctrlDirty = true;
    }

    public Collection<KeyDelegate> getDelegates(String group) {
        return delegateGroups.get(group).stream()
                .map(node -> node.delegate)
                .collect(Collectors.toList());
    }

    public void clearKeys(String group) {
        Collection<DelegateNode> nodes = delegateGroups.get(group);

        abortDelegates();

        delegates.values().removeAll(nodes);
        delegateGroups.removeAll(group);

        ctrlDirty = true;

        rebuildOverrides();
    }

    public void clearAllKeys() {
        List<String> all = new ArrayList<>(delegateGroups.keySet());

        for (String s : all) {
            clearKeys(s);
        }

        defaultGroupSuppress = 0;

        rebuildOverrides();
    }

    private int defaultGroupSuppress = 0;

    public void pushSuppressDefaultGroup() {
        defaultGroupSuppress++;
        clearKeys(DEFAULT_GROUP);
    }

    public void popSuppressDefaultGroup() {
        if (defaultGroupSuppress > 0) {
            defaultGroupSuppress--;
        }
        if (defaultGroupSuppress == 0) {
            requireFlush = true;
        }
    }

    public boolean hasActiveDelegate() {
        return delegates.values().stream().anyMatch(node -> getKeyState(node.keyID).state);
    }

    public void abortDelegates() {
        keyStates.entrySet().stream()
                .filter(e -> e.getValue().state)
                .forEach(e -> {
                    KeyState state = e.getValue();
                    state.state = false;
                    if (delegates.containsKey(e.getKey())) {
                        delegates.get(e.getKey()).delegate.onKeyAbort();
                    }
                });
    }

    @Override
    public void onPlayerDead() {
        clearAllKeys();
        keyStates.clear();
    }

    private KeyState getKeyState(int keyID) {
        return keyStates.computeIfAbsent(keyID, k -> new KeyState());
    }

    public Multimap<String, DelegateNode> getDelegateRawData() {
        return delegateGroups;
    }

    public void addActivateHandler(IActivateHandler handler) {
        activateHandlers.addFirst(handler);
    }

    public void removeActiveHandler(IActivateHandler handler) {
        activateHandlers.remove(handler);
    }

    public IActivateHandler getActivateHandler() {
        Player player = Minecraft.getInstance().player;
        for (IActivateHandler h : activateHandlers) {
            if (h.handles(player))
                return h;
        }
        throw new RuntimeException();
    }

    {

        addActivateHandler(new IActivateHandler() {
            @Override
            public boolean handles(Player player) {
                return true;
            }

            @Override
            public void onKeyDown(Player player) {
                CPData cpData = CPData.get(player);

                cpData.setActivateState(!cpData.isActivated(),
                        cn.academy.datapart.AbilityToggleSource.PLAYER_KEY);
            }

            @Override
            public String getHint() {
                return null;
            }
        });

        addActivateHandler(new IActivateHandler() {
            @Override
            public boolean handles(Player player) {
                return ClientRuntime.instance().hasActiveDelegate();
            }

            @Override
            public void onKeyDown(Player player) {
                ClientRuntime.instance().abortDelegates();
            }

            @Override
            public String getHint() {
                return "endskill";
            }
        });
    }

    private void updateDefaultGroup() {
        if (!isLocal()) {
            return;
        }
        clearKeys(DEFAULT_GROUP);

        if (defaultGroupSuppress > 0) {
            return;
        }

        Preset preset = PresetData.get(getEntity()).getCurrentPreset();

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            if (preset.hasMapping(i)) {
                Controllable c = preset.getControllable(i);
                c.activate(this, ClientHandler.getKeyMapping(i));
            }
        }
    }

    private void rebuildOverrides() {
        if (!isLocal()) {
            return;
        }
        AcademyCraft.debug("RebuildOverrides");
        CPData cpData = CPData.get(getEntity());

        ctrlDirty = false;

        int[] set = cpData.isActivated()
                ? delegates.values().stream().mapToInt(n -> n.keyID).toArray()
                : new int[0];
        ControlOverrider.override(OVERRIDE_GROUP, set);
    }

    private static class KeyState {
        boolean state = false;
        boolean realState = false;
    }

    public class DelegateNode {
        public final KeyDelegate delegate;
        public final int keyID;

        DelegateNode(KeyDelegate _delegate, int _keyID) {
            delegate = _delegate;
            keyID = _keyID;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof DelegateNode) {
                return ((DelegateNode) other).delegate == delegate;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Events {

        @SubscribeEvent
        public void presetSwitch(PresetSwitchEvent evt) {

            if (!ClientRuntime.available()) return;
            ClientRuntime.instance().updateDefaultGroup();
        }

        @SubscribeEvent
        public void presetEdit(PresetUpdateEvent evt) {

            if (SideUtils.isClient()) {

                if (!ClientRuntime.available()) return;
                ClientRuntime.instance().updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void activateAbility(AbilityActivateEvent evt) {
            if (SideUtils.isClient()) {

                if (!ClientRuntime.available()) return;
                ClientRuntime.instance().updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void deactivateAbility(AbilityDeactivateEvent evt) {
            if (SideUtils.isClient()) {

                if (!ClientRuntime.available()) return;
                ClientRuntime.instance().clearAllKeys();
            }
        }

        @SubscribeEvent
        public void flushControl(FlushControlEvent evt) {
            if (ClientRuntime.available())
                ClientRuntime.instance().requireFlush = true;
        }

    }

    @OnlyIn(Dist.CLIENT)
    public static class ActivateHandlers {

        public static IActivateHandler terminatesContext(Context ctx) {
            return new IActivateHandler() {
                @Override
                public boolean handles(Player player) {
                    return ctx.getStatus() == Status.ALIVE;
                }

                @Override
                public void onKeyDown(Player player) {
                    ctx.terminate();
                }

                @Override
                public String getHint() {
                    return ENDSPECIAL;
                }
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface IActivateHandler {

        String ENDSPECIAL = "endspecial";

        boolean handles(Player player);

        void onKeyDown(Player player);

        String getHint();

        default Optional<String> getHintTranslated() {
            String kname = KeyManager.getKeyName(ACKeyManager.instance.getKeyID(ClientHandler.keyActivate));
            String hint = ClientRuntime.instance().getActivateHandler().getHint();
            return hint == null ? Optional.empty() : Optional.of("[" + kname + "]: " + I18n.get(
                    "activate_key.academy." + hint + ".desc"));
        }

    }
}
