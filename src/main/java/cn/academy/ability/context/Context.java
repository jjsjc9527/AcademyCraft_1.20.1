package cn.academy.ability.context;

import cn.academy.AcademyCraft;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.IMessageDelegate;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Context<TSkill extends Skill> implements IMessageDelegate {

    public static final boolean DEBUG_MSG = false;

    public static final String
        MSG_TERMINATED = "i_term",
        MSG_MADEALIVE = "i_alive",
        MSG_TICK = "i_tick";

    public static final String MSG_KEYDOWN = "keydown";
    public static final String MSG_KEYTICK = "keytick";
    public static final String MSG_KEYUP = "keyup";
    public static final String MSG_KEYABORT = "keyabort";

    public enum Status { CONSTRUCTED, ALIVE, TERMINATED }

    private final ContextManager mgr = ContextManager.instance;

    List<ClientContext> clientContexts;

    public Player player;
    public final TSkill skill;

    public AbilityContext ctx;

    Status status = Status.CONSTRUCTED;

    int serverID = -1;

    public Context(Player _player, TSkill _skill) {
        player = _player;
        skill = _skill;

        ctx = AbilityContext.of(_player, _skill);

        if (isRemote()) {
            constructClientContexts();
        }
    }

    public boolean rebind(Player fresh) {
        if (fresh == null || fresh == player) {
            return false;
        }
        AbilityContext freshCtx = AbilityContext.ofIfReady(fresh, skill);
        if (freshCtx == null) {
            return false;
        }
        player = fresh;
        ctx = freshCtx;

        if (clientContexts != null) {
            for (ClientContext cc : clientContexts) {
                cc.rebind(fresh);
            }
        }
        onRebound();
        return true;
    }

    protected void onRebound() {}

    private void constructClientContexts() {
        clientContexts = new ArrayList<>();
        for (Function<Context, ClientContext> supplier : ClientContext.clientTypes.get(getClass())) {
            clientContexts.add(supplier.apply(this));
        }
    }

    Player getPlayer() {
        return player;
    }

    public Status getStatus() {
        return status;
    }

    public void terminate() {
        ContextManager.instance.terminate(this);
    }

    public final boolean isRemote() {
        return player.level().isClientSide();
    }

    public final boolean isLocal() {
        if (isRemote()) {
            return player.equals(SideUtils.getThePlayer());
        } else {
            return false;
        }
    }

    public double getRange() {
        return 50.0;
    }

    public void sendToServer(String channel, Object... args) {
        messageDebug("ToServer: " + channel);
        mgr.mToServer(this, channel, args);
    }

    public void sendToClient(String channel, Object... args) {
        messageDebug("ToClient: " + channel);
        mgr.mToClient(this, channel, args);
    }

    public void sendToLocal(String channel, Object... args) {
        messageDebug("ToLocal: " + channel);
        mgr.mToLocal(this, channel, args);
    }

    public void sendToExceptLocal(String channel, Object... args) {
        messageDebug("ToExceptLocal: " + channel);
        mgr.mToExceptLocal(this, channel, args);
    }

    public void sendToSelf(String channel, Object... args) {
        messageDebug("ToSelf: " + channel);
        mgr.mToSelf(this, channel, args);
    }

    public void onWatcherJoined(net.minecraft.server.level.ServerPlayer watcher) {}

    public void sendToWatcher(net.minecraft.server.level.ServerPlayer watcher,
                              String channel, Object... args) {
        messageDebug("ToWatcher: " + channel);
        NetworkMessage.sendTo(watcher, this, channel, args);
    }

    private void messageDebug(String s) {
        if (AcademyCraft.DEBUG_MODE && DEBUG_MSG) {
            Debug.log("[Context]" + (isRemote() ? "[C] " : "[S] ") + getClass().getSimpleName() + ": " + s);
        }
    }

    protected Level world() {
        return player.level();
    }

    protected void debug(Object message) {
        AcademyCraft.LOGGER.info("[CTX]" + message);
    }

    @Override
    public final void onMessage(String channel, Object... args) {
        messageDebug("Recv: " + channel);
        if (isRemote() && clientContexts != null) {
            for (ClientContext cctx : clientContexts) {
                NetworkMessage.sendToSelf(cctx, channel, args);
            }
        }
    }
}
