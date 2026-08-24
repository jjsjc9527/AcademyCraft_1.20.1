package cn.lambdalib2.s11n.network;

import cn.academy.network.ACNetwork;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.SideUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkMessage {

    private NetworkMessage() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Listener {
        String channel();

        LogicalSide[] side();
    }

    public interface IMessageDelegate {
        void onMessage(String channel, Object... args);
    }

    public static ClassDelegate staticCaller(Class<?> type) {
        return new ClassDelegate(type);
    }

    public static final class ClassDelegate {
        final Class<?> type;

        ClassDelegate(Class<?> type) {
            this.type = type;
        }
    }

    public static void sendToServer(Object target, String channel, Object... params) {
        ACNetwork.sendRpcToServer(build(target, channel, params));
    }

    public static void sendTo(Player player, Object target, String channel, Object... params) {
        if (player instanceof ServerPlayer sp) {
            ACNetwork.sendRpcToPlayer(build(target, channel, params), sp);
        } else {
            Debug.warn("sendTo requires a ServerPlayer, got " + player);
        }
    }

    public static void sendToTracking(Entity entity, Object target, String channel, Object... params) {
        ACNetwork.sendRpcToTracking(build(target, channel, params), entity);
    }

    public static void sendToSelf(Object target, String channel, Object... params) {
        processDynamic(target, channel, SideUtils.getRuntimeSide(), params);
    }

    public static void sendToPlayers(ServerPlayer[] players, Object target, String channel, Object... params) {
        for (ServerPlayer p : players) {
            ACNetwork.sendRpcToPlayer(build(target, channel, params), p);
        }
    }

    private static final byte MODE_SCHEMA = 0, MODE_DYNAMIC = 1;

    private static boolean isSchemaTarget(Object target) {
        return target instanceof ClassDelegate || target instanceof DataPart<?>;
    }

    private static FriendlyByteBuf build(Object target, String channel, Object[] params) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        if (isSchemaTarget(target)) {
            buf.writeByte(MODE_SCHEMA);
            writeTarget(buf, target);
            buf.writeUtf(channel);

            Class<?> handlerClass = handlerClassOf(target);
            Class<?>[] schema = schemaOf(handlerClass, channel, params);
            for (int i = 0; i < schema.length; i++) {
                NetworkS11n.serializeWithHint(buf, params[i], castType(schema[i]));
            }
        } else {
            buf.writeByte(MODE_DYNAMIC);
            NetworkS11n.serialize(buf, target, false);
            buf.writeUtf(channel);
            buf.writeByte(params.length);
            for (Object p : params) {
                NetworkS11n.serialize(buf, p, true);
            }
        }
        return buf;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? super T> castType(Class<?> c) {
        return (Class<? super T>) c;
    }

    private static final byte TAG_STATIC = 0, TAG_DATAPART = 1;

    private static final String[] STATIC_TARGET_PREFIXES = {"cn.academy.", "cn.lambdalib2."};

    private static boolean isAllowedStaticTarget(String className) {
        for (String prefix : STATIC_TARGET_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void writeTarget(FriendlyByteBuf buf, Object target) {
        if (target instanceof ClassDelegate cd) {
            buf.writeByte(TAG_STATIC);
            buf.writeUtf(cd.type.getName());
        } else if (target instanceof DataPart<?> part) {
            buf.writeByte(TAG_DATAPART);
            buf.writeVarInt(part.getEntity().getId());
            buf.writeByte(EntityData.networkIdOf((Class<? extends DataPart<?>>) part.getClass()));
        } else {
            throw new RuntimeException("unsupported network target type: " + target);
        }
    }

    private static ResolvedTarget readTarget(FriendlyByteBuf buf) {
        byte tag = buf.readByte();
        switch (tag) {
            case TAG_STATIC: {
                String className = buf.readUtf();

                if (!isAllowedStaticTarget(className)) {
                    Debug.warn("rejected out-of-scope RPC: static target class is not whitelisted " + className);
                    throw new NetworkS11n.ContextException("static target class is not whitelisted");
                }
                try {
                    Class<?> type = Class.forName(className, false, NetworkMessage.class.getClassLoader());
                    return new ResolvedTarget(type, null, true);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("static target class not found " + className, e);
                }
            }
            case TAG_DATAPART: {
                int entityId = buf.readVarInt();
                byte netId = buf.readByte();
                Class<? extends DataPart<?>> partType = EntityData.typeOfNetworkId(netId);
                Object instance = null;
                Level level = NetS11nContext.getLevel();

                ServerPlayer sender = NetS11nContext.getSender();
                String cross = null;
                if (sender != null && entityId != sender.getId()) {
                    cross = sender.getGameProfile().getName()
                            + " tried to operate on entity " + entityId + " part " + partType.getSimpleName();
                }
                if (level != null) {
                    Entity e = level.getEntity(entityId);
                    if (e != null) {
                        EntityData<?> data = EntityData.get(e);
                        if (data != null) {
                            instance = data.getPart((Class) partType);
                        }
                    }
                }
                return new ResolvedTarget(partType, instance, false, cross);
            }
            default:
                throw new RuntimeException("unknown target tag " + tag);
        }
    }

    private static Class<?> handlerClassOf(Object target) {
        if (target instanceof ClassDelegate cd) return cd.type;
        if (target instanceof DataPart<?> part) return part.getClass();
        throw new RuntimeException("unsupported network target type: " + target);
    }

    private static final class ResolvedTarget {
        final Class<?> handlerClass;
        final Object instance;
        final boolean isStatic;

        final String crossEntity;

        ResolvedTarget(Class<?> handlerClass, Object instance, boolean isStatic) {
            this(handlerClass, instance, isStatic, null);
        }

        ResolvedTarget(Class<?> handlerClass, Object instance, boolean isStatic, String crossEntity) {
            this.handlerClass = handlerClass;
            this.instance = instance;
            this.isStatic = isStatic;
            this.crossEntity = crossEntity;
        }
    }

    public static void dispatch(FriendlyByteBuf buf, LogicalSide side, Level level, ServerPlayer sender) {
        NetS11nContext.setLevel(level);
        NetS11nContext.setSender(sender);
        try {
            byte mode = buf.readByte();
            if (mode == MODE_SCHEMA) {
                dispatchSchema(buf, side);
            } else {
                dispatchDynamic(buf, side);
            }
        } catch (NetworkS11n.ContextException ce) {

        } catch (Exception ex) {
            Debug.error("RPC dispatch failed", ex);
        } finally {
            NetS11nContext.clear();
        }
    }

    private static void dispatchSchema(FriendlyByteBuf buf, LogicalSide side) {
        ResolvedTarget target = readTarget(buf);
        String channel = buf.readUtf();

        if (target.crossEntity != null && !DataPart.isReadOnlyQuery(channel)) {
            Debug.warn("rejected out-of-scope RPC: " + target.crossEntity + "(channel=" + channel + ")");
            return;
        }

        List<Method> methods = resolve(target.handlerClass, channel);
        if (methods.isEmpty()) {
            return;
        }
        Class<?>[] schema = methods.get(0).getParameterTypes();
        Object[] params = new Object[schema.length];
        for (int i = 0; i < schema.length; i++) {
            params[i] = NetworkS11n.deserializeWithHint(buf, schema[i]);
        }

        params = sanitizeParams(schema, params);

        for (Method m : methods) {
            Listener l = m.getAnnotation(Listener.class);
            if (!sideMatches(l.side(), side)) continue;
            Object inst = Modifier.isStatic(m.getModifiers()) ? null : target.instance;
            if (!Modifier.isStatic(m.getModifiers()) && inst == null) {
                continue;
            }
            try {
                m.invoke(inst, params);
            } catch (Exception ex) {
                Debug.error("RPC dispatch invocation failed: " + m, ex);
            }
        }
    }

    private static void dispatchDynamic(FriendlyByteBuf buf, LogicalSide side) {
        Object instance = NetworkS11n.deserialize(buf);
        String channel = buf.readUtf();
        int n = buf.readByte() & 0xFF;
        Object[] params = new Object[n];
        for (int i = 0; i < n; i++) {
            params[i] = NetworkS11n.deserialize(buf);
        }
        if (instance == null) return;
        processDynamic(instance, channel, side, params);
    }

    private static void processDynamic(Object instance, String channel, LogicalSide side, Object[] params) {
        if (instance instanceof IMessageDelegate) {
            ((IMessageDelegate) instance).onMessage(channel, params);
        }
        List<Method> methods = resolve(instance.getClass(), channel);
        for (Method m : methods) {
            Listener l = m.getAnnotation(Listener.class);
            if (!sideMatches(l.side(), side)) continue;
            Object inst = Modifier.isStatic(m.getModifiers()) ? null : instance;
            try {

                m.invoke(inst, sanitizeParams(m.getParameterTypes(), adaptParams(m, params)));
            } catch (Exception ex) {
                Debug.error("RPC dynamic dispatch invocation failed: " + m, ex);
            }
        }
    }

    private static Object[] sanitizeParams(Class<?>[] schema, Object[] params) {
        ServerPlayer sender = NetS11nContext.getSender();
        if (sender == null) {
            return params;
        }
        Object[] out = params;
        for (int i = 0; i < schema.length && i < out.length; i++) {
            if (Player.class.isAssignableFrom(schema[i]) && out[i] != sender) {
                if (out == params) {
                    out = params.clone();
                }
                out[i] = sender;
            }
        }
        return out;
    }

    private static Object[] adaptParams(Method m, Object[] params) {
        int pc = m.getParameterCount();
        return pc == params.length ? params : Arrays.copyOf(params, pc);
    }

    private static final Map<String, List<Method>> methodCache = new ConcurrentHashMap<>();

    private static List<Method> resolve(Class<?> handlerClass, String channel) {
        String key = handlerClass.getName() + "#" + channel;
        return methodCache.computeIfAbsent(key, k -> {
            List<Method> list = new ArrayList<>();
            for (Class<?> c = handlerClass; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    Listener l = m.getAnnotation(Listener.class);
                    if (l != null && l.channel().equals(channel)) {
                        m.setAccessible(true);
                        list.add(m);
                    }
                }
            }
            return list;
        });
    }

    private static Class<?>[] schemaOf(Class<?> handlerClass, String channel, Object[] params) {
        List<Method> methods = resolve(handlerClass, channel);
        if (!methods.isEmpty()) {
            return methods.get(0).getParameterTypes();
        }

        Class<?>[] rt = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            rt[i] = params[i].getClass();
        }
        return rt;
    }

    private static boolean sideMatches(LogicalSide[] sides, LogicalSide side) {
        for (LogicalSide s : sides) {
            if (s == side) return true;
        }
        return false;
    }
}
