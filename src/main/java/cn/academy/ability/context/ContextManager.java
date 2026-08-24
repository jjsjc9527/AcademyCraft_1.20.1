package cn.academy.ability.context;

import cn.academy.ability.context.Context.Status;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.academy.event.ability.OverloadEvent;
import cn.lambdalib2.s11n.network.NetS11nAdaptor;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;

import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings({"rawtypes", "unchecked"})
public enum ContextManager {
    instance;

    private static final double
            T_KA_TOL = 1.5,
            T_KA = 0.5;

    private static final String
        M_BEGIN_LINK = "l",
        M_ESTABLISH_LINK = "ld",
        M_MAKEALIVE = "m",
        M_TERM_ATLOCAL = "tl",
        M_TERM_ATSERVER = "ts",
        M_KEEPALIVE = "ka";

    public static void bootstrap() {
        NetworkS11n.registerType(Context.class);
        NetworkS11n.registerType(ClientContext.class);
        NetworkS11n.registerType(LocalManager.class);
        NetworkS11n.registerType(ServerManager.class);
        NetworkS11n.registerType(ClientManager.class);

        NetworkS11n.addDirect(Context.class, CONTEXT_ADAPTOR);

        MinecraftForge.EVENT_BUS.register(ServerManager.instance);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(LocalManager.instance);
            MinecraftForge.EVENT_BUS.register(ClientManager.instance);
        });
    }

    public void activate(Context ctx) {
        Preconditions.checkState(ctx.status == Status.CONSTRUCTED, "Can't activate one context multiple times");
        Preconditions.checkState(ctx.isLocal(), "Can only activate context at local.");

        LocalManager.instance.activate(ctx);
    }

    public void terminate(Context ctx) {
        if (!ctx.isRemote()) {
            ServerManager.instance.terminate(ctx);
        } else if (ctx.isLocal()) {
            LocalManager.instance.terminate(ctx);
        } else throw wrongSide();
    }

    public int rebindAll(Player old, Player fresh) {
        if (SideUtils.isClient()) {
            throw wrongSide();
        }
        return ServerManager.instance.rebindAll(old, fresh);
    }

    public <T> Optional<T> find(Class<T> type) {
        if (SideUtils.isClient()) {
            Optional<T> test1 = findLocal(type);
            if (test1.isPresent()) return test1;
            return findIn(ClientManager.instance.alive.stream().map(d -> d.ctx), type);
        } else {
            return findIn(ServerManager.instance.alive.stream().map(d -> d.ctx), type);
        }
    }

    public <T> Optional<T> find(Class<T> type, net.minecraft.world.entity.player.Player player) {
        Stream<Context> stream = SideUtils.isClient()
                ? Stream.concat(LocalManager.instance.alive.stream().map(d -> d.ctx),
                                ClientManager.instance.alive.stream().map(d -> d.ctx))
                : ServerManager.instance.alive.stream().map(d -> d.ctx);
        return findIn(stream.filter(c -> c.player == player), type);
    }

    public <T> Optional<T> findLocal(Class<T> type) {
        return findIn(LocalManager.instance.alive.stream().map(d -> d.ctx), type);
    }

    private <T> Optional<T> findIn(Stream<Context> stream, Class<T> type) {
        return (Optional) stream.filter(type::isInstance).findAny();
    }

    void mToSelf(Context ctx, String channel, Object... args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToSelf(ctx, channel, args);
        } else if (ctx.isLocal()) {
            LocalManager.instance.mToSelf(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToServer(Context ctx, String channel, Object... args) {
        if (!checkStatus(ctx)) return;
        if (ctx.isLocal()) {
            LocalManager.instance.mToServer(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToLocal(Context ctx, String channel, Object... args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToLocal(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToClient(Context ctx, String channel, Object... args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToClient(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToExceptLocal(Context ctx, String channel, Object... args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToExceptLocal(ctx, channel, args);
        } else throw wrongSide();
    }

    private boolean checkStatus(Context ctx) {
        return ctx.getStatus() != Status.TERMINATED;
    }

    private static IllegalStateException wrongSide() {
        return new IllegalStateException("Wrong context side!");
    }

    private static IllegalStateException notFound() {
        return new IllegalStateException("Illegal state: alive context not found in data!");
    }

    private static Object writeContextType(Class<? extends Context> type) {
        return type.getName();
    }

    private static Class<? extends Context> readContextType(Object in) {
        try {
            return (Class) Class.forName((String) in);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final NetS11nAdaptor<Context> CONTEXT_ADAPTOR = new NetS11nAdaptor<>() {
        @Override
        public void write(FriendlyByteBuf buf, Context obj) {
            Preconditions.checkState(obj.serverID != -1);
            buf.writeInt(obj.serverID);
        }

        @Override
        public Context read(FriendlyByteBuf buf) {
            int serverID = buf.readInt();
            if (!SideUtils.isClient()) {
                ServerManager.ContextData data = ServerManager.instance.findOrNull(serverID);

                if (data != null && !ServerManager.ownedBySender(data)) {
                    throw new ContextException("Context " + serverID + " does not belong to the sender");
                }
                if (data != null) return data.ctx;
                else throw new ContextException("Can't find server context");
            } else {
                LocalManager.ContextData data0 = LocalManager.instance.findOrNull(serverID);
                if (data0 != null) return data0.ctx;
                ClientManager.ContextData data1 = ClientManager.instance.findOrNull(serverID);
                if (data1 != null) return data1.ctx;
                throw new ContextException("Can't find client context");
            }
        }
    };

    private static final org.apache.logging.log4j.Logger CTX_LOG =
            org.apache.logging.log4j.LogManager.getLogger("ACContext");

    private static int rebindFailTicks;

    public enum LocalManager {
        instance;

        Map<Integer, ContextData> suspended = new HashMap<>();
        List<ContextData> alive = new LinkedList<>();

        int nextClientID;

        void activate(Context ctx) {
            ContextData data = new ContextData();
            data.ctx = ctx;

            suspended.put(nextClientID, data);
            NetworkMessage.sendToServer(ServerManager.instance, M_BEGIN_LINK,
                    writeContextType(ctx.getClass()), player(), nextClientID);

            nextClientID += 1;
        }

        void terminate(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                data.disposed = true;
                return;
            }
            for (ContextData data : suspended.values()) if (data.ctx == ctx) {
                data.disposed = true;
                return;
            }
            throw new IllegalStateException("Not found");
        }

        void mToSelf(Context ctx, String channel, Object[] args) {
            if (ctx.status == Status.CONSTRUCTED || ctx.status == Status.ALIVE) {
                NetworkMessage.sendToSelf(ctx, channel, args);
            }
        }

        void mToServer(Context ctx, String channel, Object[] args) {
            if (ctx.status == Status.ALIVE) {
                for (ContextData data : alive) {
                    if (data.ctx == ctx) {
                        NetworkMessage.sendToServer(ctx, channel, args);
                        return;
                    }
                }
                throw notFound();
            } else if (ctx.status == Status.CONSTRUCTED) {
                for (ContextData data : suspended.values()) {
                    if (data.ctx == ctx) {
                        Call call = new Call();
                        call.msg = channel;
                        call.args = args;
                        data.calls.add(call);
                        return;
                    }
                }
                throw notFound();
            }
        }

        private Player player() {
            return SideUtils.getThePlayer();
        }

        @Listener(channel = M_ESTABLISH_LINK, side = LogicalSide.CLIENT)
        private void hEstablishLink(int clientID, int serverID) {
            ContextData data = suspended.remove(clientID);
            if (data != null) {
                data.ctx.status = Status.ALIVE;
                data.serverID = serverID;
                data.ctx.serverID = serverID;

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);
                for (Call call : data.calls) {
                    mToServer(data.ctx, call.msg, call.args);
                }
                data.calls = null;
            }
        }

        @Listener(channel = M_TERM_ATSERVER, side = LogicalSide.CLIENT)
        private void hTerminate(int serverID) {
            Optional.ofNullable(findOrNull(serverID)).ifPresent(x -> x.disposed = true);
        }

        @Listener(channel = M_KEEPALIVE, side = LogicalSide.CLIENT)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null) {
                data.lastKeepAlive = time();
            }
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        private class ContextData {
            Context ctx;
            int serverID;

            double lastKeepAlive = time();
            double lastSentKeepAlive = time() - 0.5;
            boolean disposed;

            List<Call> calls = new ArrayList<>();
        }

        private class Call {
            String msg;
            Object[] args;
        }

        private double time() {
            return GameTimer.getTime();
        }

        @SubscribeEvent
        public void __onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END && SideUtils.isPlayerInGame()) {
                double time = time();

                boolean ready = cn.lambdalib2.datapart.EntityData.isLocalPlayerReady();
                boolean paused = SideUtils.isGamePaused();

                net.minecraft.world.entity.player.Player nowLocal = SideUtils.getThePlayer();
                if (nowLocal != null) {
                    for (ContextData data : alive) {
                        if (data.disposed || data.ctx.player == nowLocal) {
                            continue;
                        }

                        if (data.ctx.rebind(nowLocal)) {
                            CTX_LOG.info("[ctx-local] rebind ok: {} (alive={} suspended={})",
                                    data.ctx.getClass().getSimpleName(), alive.size(), suspended.size());
                        } else if ((rebindFailTicks++ % 40) == 0) {
                            CTX_LOG.warn("[ctx-local] rebind failed: {} | local data ready={} | alive={} suspended={}",
                                    data.ctx.getClass().getSimpleName(),
                                    cn.lambdalib2.datapart.EntityData.isReady(nowLocal),
                                    alive.size(), suspended.size());
                        }
                    }
                }

                for (ContextData data : alive) {

                    if (paused) {
                        data.lastKeepAlive = time;
                    }
                    if (time - data.lastKeepAlive > T_KA_TOL) {
                        data.disposed = true;
                    } else {
                        if (time - data.lastSentKeepAlive > T_KA) {
                            NetworkMessage.sendToServer(ServerManager.instance, M_KEEPALIVE, data.serverID);
                            data.lastSentKeepAlive = time;
                        }
                        if (ready) {
                            NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                        }
                    }
                }

                Iterator<ContextData> itr = alive.iterator();
                while (itr.hasNext()) {
                    ContextData data = itr.next();
                    if (data.disposed) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
                        NetworkMessage.sendToServer(ServerManager.instance, M_TERM_ATLOCAL, data.serverID);
                        itr.remove();
                    }
                }
            }
        }

        @SubscribeEvent
        public void __onDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
            for (ContextData data : alive) {
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
            }
            alive.clear();
            suspended.clear();
        }
    }

    public enum ServerManager {
        instance;

        List<ContextData> alive = new LinkedList<>();

        int nextServerID;

        void terminate(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                data.disposed = true;
            }
        }

        int rebindAll(Player old, Player fresh) {
            int n = 0;

            int total = 0, disposed = 0, other = 0, failed = 0;
            for (ContextData data : alive) {
                total++;
                if (data.disposed) {
                    disposed++;
                    continue;
                }
                if (data.ctx.player != old) {
                    other++;
                    continue;
                }
                if (data.ctx.rebind(fresh)) {
                    n++;
                } else {
                    failed++;
                }
            }
            if (n == 0) {
                org.apache.logging.log4j.LogManager.getLogger("ACRespawn").warn(
                        "[ac-respawn] rebindAll bound nothing: alive contexts={} disposed={} owned by others={} rebind failed={}",
                        total, disposed, other, failed);
            }
            return n;
        }

        void mToSelf(Context ctx, String channel, Object[] args) {
            NetworkMessage.sendToSelf(ctx, channel, args);
        }

        void mToLocal(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendTo(data.ctx.player, ctx, channel, args);
        }

        void mToClient(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendTo(data.ctx.player, ctx, channel, args);
            NetworkMessage.sendToPlayers(data.targets, ctx, channel, args);
        }

        void mToExceptLocal(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendToPlayers(data.targets, ctx, channel, args);
        }

        private static ServerPlayer[] nearbyWatchers(ServerPlayer self, double range) {
            if (!(self.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
                return new ServerPlayer[0];
            }
            double r2 = range * range;
            java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
            for (ServerPlayer p : sl.players()) {
                if (p != self && p.position().distanceToSqr(self.position()) <= r2) {
                    out.add(p);
                }
            }
            return out.toArray(new ServerPlayer[0]);
        }

        private void refreshTargets(ContextData data) {
            if (!(data.ctx.player instanceof ServerPlayer self)
                    || !(self.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
                return;
            }

            double enter = data.ctx.getRange();
            double leave = enter * LEAVE_SLACK;
            Set<ServerPlayer> before = new HashSet<>(Arrays.asList(data.targets));
            List<ServerPlayer> now = new ArrayList<>();
            for (ServerPlayer p : sl.players()) {
                if (p == self) {
                    continue;
                }
                double r = before.contains(p) ? leave : enter;
                if (p.position().distanceToSqr(self.position()) <= r * r) {
                    now.add(p);
                }
            }
            Set<ServerPlayer> after = new HashSet<>(now);
            if (before.equals(after)) {
                return;
            }

            for (ServerPlayer p : now) {
                if (!before.contains(p)) {

                    NetworkMessage.sendTo(p, ClientManager.instance, M_MAKEALIVE,
                            writeContextType(data.ctx.getClass()), self, data.serverID);

                    data.ctx.onWatcherJoined(p);
                }
            }
            for (ServerPlayer p : data.targets) {
                if (!after.contains(p) && sl.players().contains(p)) {
                    NetworkMessage.sendTo(p, ClientManager.instance, M_TERM_ATSERVER, data.serverID);
                }
            }
            data.targets = now.toArray(new ServerPlayer[0]);
        }

        private ContextData find(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                return data;
            }
            throw new IllegalStateException("ContextData not present");
        }

        @Listener(channel = M_BEGIN_LINK, side = LogicalSide.SERVER)
        private void hBeginLink(Object typein, ServerPlayer player, int clientID) {
            try {
                Class<? extends Context> type = readContextType(typein);
                Context ctx = type.getConstructor(Player.class).newInstance(player);
                ContextData data = new ContextData();
                data.ctx = ctx;

                data.targets = nearbyWatchers(player, ctx.getRange());

                data.ctx.status = Status.ALIVE;
                data.serverID = nextServerID;
                data.ctx.serverID = nextServerID;

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);

                NetworkMessage.sendTo(player, LocalManager.instance, M_ESTABLISH_LINK, clientID, nextServerID);
                NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_MAKEALIVE,
                        writeContextType(ctx.getClass()), player, nextServerID);
                nextServerID += 1;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        static boolean ownedBySender(ContextData data) {
            ServerPlayer sender = cn.lambdalib2.s11n.network.NetS11nContext.getSender();
            return sender == null || data.ctx.player == sender;
        }

        @Listener(channel = M_TERM_ATLOCAL, side = LogicalSide.SERVER)
        private void hTerminate(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null && ownedBySender(data)) {
                data.disposed = true;
            }
        }

        @Listener(channel = M_KEEPALIVE, side = LogicalSide.SERVER)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null && ownedBySender(data)) {
                data.lastKeepAlive = time();
            }
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        private static final int T_RETARGET = 10;

        private static final double LEAVE_SLACK = 1.2;
        private int retargetTick;

        @SubscribeEvent
        public void __onServerTick(ServerTickEvent evt) {
            if (evt.phase == Phase.END) {
                double time = time();

                if (++retargetTick >= T_RETARGET) {
                    retargetTick = 0;
                    for (ContextData data : alive) {
                        if (!data.disposed) {
                            refreshTargets(data);
                        }
                    }
                }

                for (ContextData data : alive) {
                    if (data.disposed || time - data.lastKeepAlive > T_KA_TOL) {
                        data.disposed = true;
                    } else {
                        if (time - data.lastSentKeepAlive > T_KA) {
                            NetworkMessage.sendTo(data.ctx.player, LocalManager.instance, M_KEEPALIVE, data.serverID);
                            NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_KEEPALIVE, data.serverID);
                            data.lastSentKeepAlive = time;
                        }

                        if (cn.lambdalib2.datapart.EntityData.isReady(data.ctx.player)) {
                            NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                        }
                    }
                }

                Iterator<ContextData> itr = alive.iterator();
                while (itr.hasNext()) {
                    ContextData data = itr.next();

                    if (data.disposed || (data.ctx.player.isRemoved()
                            && !cn.academy.util.ACRespawn.isPendingRebuild(
                                    data.ctx.player.getUUID(),
                                    data.ctx.player.level().getGameTime()))) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);

                        NetworkMessage.sendTo(data.ctx.player, LocalManager.instance, M_TERM_ATSERVER, data.serverID);
                        NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_TERM_ATSERVER, data.serverID);

                        itr.remove();
                    }
                }
            }
        }

        @SubscribeEvent
        public void __onOverload(OverloadEvent evt) {
            disposePlayer(evt.player);
        }

        @SubscribeEvent
        public void __onCategoryChange(CategoryChangeEvent evt) {
            if (!evt.player.level().isClientSide()) {
                disposePlayer(evt.player);
            }
        }

        private void disposePlayer(Player p) {
            for (ContextData d : alive) if (d.ctx.player.equals(p)) {
                d.disposed = true;
            }
        }

        private class ContextData {
            Context ctx;
            ServerPlayer[] targets;
            int serverID;
            boolean disposed = false;

            double lastKeepAlive = time();
            double lastSentKeepAlive = time() - 0.5;
        }

        private double time() {
            return GameTimer.getTime();
        }
    }

    public enum ClientManager {
        instance;

        List<ContextData> alive = new LinkedList<>();

        @Listener(channel = M_MAKEALIVE, side = LogicalSide.CLIENT)
        private void hMakeAlive(Object typein, Player player, int serverID) {

            if (findOrNull(serverID) != null) {
                return;
            }
            try {
                Class<? extends Context> type = readContextType(typein);
                Context ctx = type.getConstructor(Player.class).newInstance(player);
                ContextData data = new ContextData();

                data.ctx = ctx;
                data.serverID = serverID;
                data.ctx.serverID = serverID;
                data.ctx.status = Status.ALIVE;
                data.lastKeepAlive = time();

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Listener(channel = M_KEEPALIVE, side = LogicalSide.CLIENT)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null) {
                data.lastKeepAlive = time();
            }
        }

        @Listener(channel = M_TERM_ATSERVER, side = LogicalSide.CLIENT)
        private void hTerminate(int serverID) {
            Optional.ofNullable(findOrNull(serverID)).ifPresent(x -> x.disposed = true);
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        private class ContextData {
            Context ctx;
            int serverID;
            double lastKeepAlive = time();
            boolean disposed = false;
        }

        @SubscribeEvent
        public void __onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END && SideUtils.isPlayerInGame()) {
                double time = time();

                boolean ready = cn.lambdalib2.datapart.EntityData.isLocalPlayerReady();

                boolean paused = SideUtils.isGamePaused();

                for (ContextData data : alive) {
                    if (paused) {
                        data.lastKeepAlive = time;
                    }
                    if (data.disposed || time - data.lastKeepAlive > T_KA_TOL) {
                        data.disposed = true;
                    } else if (ready) {
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                    }
                }

                Iterator<ContextData> iter = alive.iterator();
                while (iter.hasNext()) {
                    ContextData data = iter.next();
                    if (data.disposed) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
                        iter.remove();
                    }
                }
            }
        }

        @SubscribeEvent
        public void __onDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
            alive.clear();
        }

        private double time() {
            return GameTimer.getTime();
        }
    }
}
