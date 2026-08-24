package cn.lambdalib2.s11n.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NetworkS11n {

    private static final Map<Class<?>, NetS11nAdaptor<?>> adaptors = new HashMap<>();

    private static final BiMap<Integer, Class<?>> serTypes = HashBiMap.create();
    private static final byte DYN_MAGIC = 0x47;
    private static final int IDX_NULL = -1, IDX_ARRAY = -2;

    private NetworkS11n() {}

    public static <T> void register(Class<T> type, NetS11nAdaptor<T> adaptor) {
        adaptors.put(type, adaptor);
        registerType(type);
    }

    private static <T> void registerBoth(Class<T> boxed, Class<?> primitive, NetS11nAdaptor<T> adaptor) {
        adaptors.put(boxed, adaptor);
        adaptors.put(primitive, adaptor);
        registerType(boxed);
        registerType(primitive);
    }

    public static void registerType(Class<?> type) {
        int hash = type.getName().hashCode();
        if (!serTypes.containsKey(hash)) {
            serTypes.put(hash, type);
        }
    }

    public static <T> void addDirect(Class<T> type, NetS11nAdaptor<T> adaptor) {
        register(type, adaptor);
    }

    @SuppressWarnings("unchecked")
    public static <T> void addDirectInstance(T instance) {
        Class<T> cls = (Class<T>) instance.getClass();
        addDirect(cls, new NetS11nAdaptor<T>() {
            public void write(FriendlyByteBuf buf, T obj) {}
            public T read(FriendlyByteBuf buf) { return instance; }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> NetS11nAdaptor<T> find(Class<?> type) {
        NetS11nAdaptor<?> a = adaptors.get(type);
        if (a != null) return (NetS11nAdaptor<T>) a;
        for (Class<?> c = type.getSuperclass(); c != null; c = c.getSuperclass()) {
            a = adaptors.get(c);
            if (a != null) {
                adaptors.put(type, a);
                return (NetS11nAdaptor<T>) a;
            }
        }
        for (Class<?> itf : type.getInterfaces()) {
            a = adaptors.get(itf);
            if (a != null) {
                adaptors.put(type, a);
                return (NetS11nAdaptor<T>) a;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void serializeWithHint(FriendlyByteBuf buf, T obj, Class<? super T> type) {
        if (type.isEnum()) {
            buf.writeByte(((Enum<?>) obj).ordinal());
            return;
        }
        NetS11nAdaptor adaptor = find(type);
        if (adaptor == null) {
            throw new RuntimeException("no serializer adapter for type " + type.getName() + " is registered");
        }
        adaptor.write(buf, obj);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserializeWithHint(FriendlyByteBuf buf, Class<?> type) {
        if (type.isEnum()) {
            return (T) type.getEnumConstants()[buf.readByte()];
        }
        NetS11nAdaptor<T> adaptor = find(type);
        if (adaptor == null) {
            throw new RuntimeException("no serializer adapter for type " + type.getName() + " is registered");
        }
        return adaptor.read(buf);
    }

    public static void serialize(FriendlyByteBuf buf, Object obj, boolean nullable) {
        if (obj == null) {
            if (nullable) {
                buf.writeByte(DYN_MAGIC);
                buf.writeInt(IDX_NULL);
            } else {
                throw new NullPointerException("null is not serializable here (nullable=false)");
            }
        } else {
            Class<?> type = obj.getClass();
            writeTypeIndex(buf, type);
            serializeDynamicPayload(buf, obj, type);
        }
    }

    public static Object deserialize(FriendlyByteBuf buf) {
        Class<?> type = readTypeIndex(buf);
        if (type == null) return null;
        return deserializeDynamicPayload(buf, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void serializeDynamicPayload(FriendlyByteBuf buf, Object obj, Class<?> type) {
        NetS11nAdaptor adaptor = find(type);
        if (adaptor != null) {
            adaptor.write(buf, obj);
        } else if (type.isEnum()) {
            buf.writeByte(((Enum<?>) obj).ordinal());
        } else if (type.isArray()) {
            int len = Array.getLength(obj);
            buf.writeShort(len);
            for (int i = 0; i < len; i++) {
                serialize(buf, Array.get(obj, i), true);
            }
        } else {
            throw new RuntimeException("cannot serialize dynamically (no adapter and not an enum or array): " + type.getName());
        }
    }

    private static Object deserializeDynamicPayload(FriendlyByteBuf buf, Class<?> type) {
        NetS11nAdaptor<?> adaptor = find(type);
        if (adaptor != null) {
            return adaptor.read(buf);
        } else if (type.isEnum()) {
            return type.getEnumConstants()[buf.readByte()];
        } else if (type.isArray()) {
            int size = buf.readShort();
            Class<?> comp = type.getComponentType();
            Object ret = Array.newInstance(comp, size);
            for (int i = 0; i < size; i++) {
                Array.set(ret, i, deserialize(buf));
            }
            return ret;
        } else {
            throw new RuntimeException("cannot deserialize dynamically (no adapter and not an enum or array): " + type.getName());
        }
    }

    private static int typeIndex(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Integer h = serTypes.inverse().get(c);
            if (h != null) return h;
        }
        return -1;
    }

    private static void writeTypeIndex(FriendlyByteBuf buf, Class<?> type) {
        buf.writeByte(DYN_MAGIC);
        if (type.isArray()) {
            buf.writeInt(IDX_ARRAY);
            writeTypeIndex(buf, type.getComponentType());
        } else {
            int idx = typeIndex(type);
            if (idx == -1) {
                throw new RuntimeException("type has no registered network serialization index: " + type.getName());
            }
            buf.writeInt(idx);
        }
    }

    private static Class<?> readTypeIndex(FriendlyByteBuf buf) {
        byte magic = buf.readByte();
        if (magic != DYN_MAGIC) {
            throw new RuntimeException("dynamic serialization MAGIC check failed: " + magic);
        }
        int idx = buf.readInt();
        if (idx == IDX_NULL) return null;
        if (idx == IDX_ARRAY) return arrayClassOf(readTypeIndex(buf));
        Class<?> ret = serTypes.get(idx);
        if (ret == null) {
            throw new RuntimeException("no class registered at type index " + idx);
        }
        return ret;
    }

    private static Class<?> arrayClassOf(Class<?> component) {
        return Array.newInstance(component, 0).getClass();
    }

    public static class ContextException extends RuntimeException {
        public ContextException(String msg) {
            super(msg);
        }
    }

    static {
        registerBoth(Integer.class, int.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Integer v) { buf.writeInt(v); }
            public Integer read(FriendlyByteBuf buf) { return buf.readInt(); }
        });
        registerBoth(Long.class, long.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Long v) { buf.writeLong(v); }
            public Long read(FriendlyByteBuf buf) { return buf.readLong(); }
        });
        registerBoth(Float.class, float.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Float v) { buf.writeFloat(v); }
            public Float read(FriendlyByteBuf buf) { return buf.readFloat(); }
        });
        registerBoth(Double.class, double.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Double v) { buf.writeDouble(v); }
            public Double read(FriendlyByteBuf buf) { return buf.readDouble(); }
        });
        registerBoth(Boolean.class, boolean.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Boolean v) { buf.writeBoolean(v); }
            public Boolean read(FriendlyByteBuf buf) { return buf.readBoolean(); }
        });
        registerBoth(Byte.class, byte.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Byte v) { buf.writeByte(v); }
            public Byte read(FriendlyByteBuf buf) { return buf.readByte(); }
        });
        registerBoth(Short.class, short.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Short v) { buf.writeShort(v); }
            public Short read(FriendlyByteBuf buf) { return buf.readShort(); }
        });
        registerBoth(Character.class, char.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Character v) { buf.writeChar(v); }
            public Character read(FriendlyByteBuf buf) { return buf.readChar(); }
        });

        register(String.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, String v) { buf.writeUtf(v); }
            public String read(FriendlyByteBuf buf) { return buf.readUtf(); }
        });
        register(UUID.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, UUID v) { buf.writeUUID(v); }
            public UUID read(FriendlyByteBuf buf) { return buf.readUUID(); }
        });
        register(ResourceLocation.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, ResourceLocation v) { buf.writeUtf(v.toString()); }
            public ResourceLocation read(FriendlyByteBuf buf) { return new ResourceLocation(buf.readUtf()); }
        });
        register(BlockPos.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, BlockPos v) { buf.writeBlockPos(v); }
            public BlockPos read(FriendlyByteBuf buf) { return buf.readBlockPos(); }
        });
        register(Vec3.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Vec3 v) {
                buf.writeDouble(v.x); buf.writeDouble(v.y); buf.writeDouble(v.z);
            }
            public Vec3 read(FriendlyByteBuf buf) {
                return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
        });
        register(ItemStack.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, ItemStack v) { buf.writeItem(v); }
            public ItemStack read(FriendlyByteBuf buf) { return buf.readItem(); }
        });
        register(CompoundTag.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, CompoundTag v) { buf.writeNbt(v); }
            public CompoundTag read(FriendlyByteBuf buf) { return buf.readNbt(); }
        });
        register(byte[].class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, byte[] v) { buf.writeByteArray(v); }
            public byte[] read(FriendlyByteBuf buf) { return buf.readByteArray(); }
        });

        register(Entity.class, new NetS11nAdaptor<>() {
            public void write(FriendlyByteBuf buf, Entity v) { buf.writeVarInt(v.getId()); }
            public Entity read(FriendlyByteBuf buf) {
                int id = buf.readVarInt();
                Level level = NetS11nContext.getLevel();
                return level == null ? null : level.getEntity(id);
            }
        });
    }

    public static boolean selfTest(java.util.function.BiConsumer<String, Boolean> report) {
        boolean all = true;
        all &= roundTrip(report, "int", 42, int.class);
        all &= roundTrip(report, "float", 3.5f, float.class);
        all &= roundTrip(report, "boolean", true, boolean.class);
        all &= roundTrip(report, "long", 123456789L, long.class);
        all &= roundTrip(report, "String", "héllo中文", String.class);
        all &= roundTrip(report, "enum(Direction)", Direction.NORTH, Direction.class);
        all &= roundTrip(report, "BlockPos", new BlockPos(1, 2, 3), BlockPos.class);
        all &= roundTrip(report, "Vec3", new Vec3(1.5, 2.5, 3.5), Vec3.class);
        all &= roundTrip(report, "ResourceLocation", new ResourceLocation("academy:test"), ResourceLocation.class);
        all &= roundTrip(report, "UUID", UUID.fromString("12345678-1234-1234-1234-1234567890ab"), UUID.class);
        all &= roundTrip(report, "ItemStack(EMPTY)", ItemStack.EMPTY, ItemStack.class,
                (a, b) -> a.isEmpty() == b.isEmpty());

        registerType(Direction.class);
        all &= roundTripDynamic(report, "dyn:int", 42);
        all &= roundTripDynamic(report, "dyn:String", "dynamic中文");
        all &= roundTripDynamic(report, "dyn:enum(Direction)", Direction.EAST);
        all &= roundTripDynamicNull(report, "dyn:null");
        return all;
    }

    private static <T> boolean roundTrip(java.util.function.BiConsumer<String, Boolean> report,
                                         String name, T value, Class<?> type) {
        return roundTrip(report, name, value, type, java.util.Objects::equals);
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean roundTrip(java.util.function.BiConsumer<String, Boolean> report,
                                         String name, T value, Class<?> type,
                                         java.util.function.BiPredicate<T, T> eq) {
        boolean ok;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            serializeWithHint(buf, value, (Class<? super T>) type);
            T back = deserializeWithHint(buf, type);
            ok = eq.test(value, back);
        } catch (Throwable ex) {
            ok = false;
        }
        report.accept(name, ok);
        return ok;
    }

    private static boolean roundTripDynamic(java.util.function.BiConsumer<String, Boolean> report,
                                            String name, Object value) {
        boolean ok;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            serialize(buf, value, true);
            Object back = deserialize(buf);
            ok = java.util.Objects.equals(value, back);
        } catch (Throwable ex) {
            ok = false;
        }
        report.accept(name, ok);
        return ok;
    }

    private static boolean roundTripDynamicNull(java.util.function.BiConsumer<String, Boolean> report, String name) {
        boolean ok;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            serialize(buf, null, true);
            Object back = deserialize(buf);
            ok = back == null;
        } catch (Throwable ex) {
            ok = false;
        }
        report.accept(name, ok);
        return ok;
    }
}
