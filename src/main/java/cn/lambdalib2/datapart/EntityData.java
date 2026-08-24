package cn.lambdalib2.datapart;

import cn.lambdalib2.util.Debug;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class EntityData<Ent extends Entity> implements IEntityData {

    private static final String ID = "LL_EntityData";

    private static final List<RegData> regList = new ArrayList<>();
    private static final List<RegData> bothSideList = new ArrayList<>();
    private static final Map<Class<? extends Entity>, Boolean> neededEntityMap = new HashMap<>();

    private static boolean _baked;

    @SuppressWarnings("unchecked")
    static <T extends Entity> void register(
            Class<? extends DataPart<T>> type,
            EnumSet<LogicalSide> sides,
            Predicate<Class<? extends T>> pred) {
        Debug.assert2(!_baked, "Can't register DataPart type after EntityData is used");

        RegData add = new RegData();
        add.type = (Class) type;
        add.sides = EnumSet.copyOf(sides);
        add.pred = (Predicate) pred;

        regList.add(add);
        if (add.sides.contains(LogicalSide.CLIENT) && add.sides.contains(LogicalSide.SERVER)) {
            bothSideList.add(add);
        }
    }

    @SuppressWarnings("unchecked")
    public static void scanAndRegister() {
        Type annoType = Type.getType(RegDataPart.class);
        for (ModFileScanData scan : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData a : scan.getAnnotations()) {
                if (!annoType.equals(a.annotationType())) continue;
                String className = a.clazz().getClassName();
                try {
                    Class<?> clazz = Class.forName(className, false, EntityData.class.getClassLoader());
                    if (!DataPart.class.isAssignableFrom(clazz)) {
                        Debug.warn("@RegDataPart on non-DataPart class: " + className);
                        continue;
                    }
                    RegDataPart anno = clazz.getAnnotation(RegDataPart.class);
                    Class<? extends Entity> regType = anno.value();
                    register(
                            (Class<? extends DataPart<Entity>>) clazz.asSubclass(DataPart.class),
                            EnumSet.copyOf(Arrays.asList(anno.side())),
                            (Predicate<Class<? extends Entity>>) regType::isAssignableFrom);
                } catch (Throwable ex) {
                    Debug.error("Failed to register DataPart " + className, ex);
                }
            }
        }
    }

    public static boolean needEntityDataFor(Class<? extends Entity> type) {
        Debug.assert2(_baked);
        if (neededEntityMap.containsKey(type)) {
            return neededEntityMap.get(type);
        }
        boolean need = false;
        for (RegData data : bothSideList) {
            if (data.pred.test(type)) {
                need = true;
                break;
            }
        }
        neededEntityMap.put(type, need);
        return need;
    }

    static void bake() {
        bothSideList.sort(Comparator.comparing(lhs -> lhs.type.getName()));
        Preconditions.checkState(bothSideList.size() < Byte.MAX_VALUE);
        IntStream.range(0, bothSideList.size()).forEach(i -> bothSideList.get(i).networkID = (byte) i);

        Debug.log("EntityData baked, participants: " +
                bothSideList.stream().map(x -> x.type.getCanonicalName()).collect(Collectors.toList()));
        _baked = true;
    }

    static boolean isBaked() {
        return _baked;
    }

    public static byte networkIdOf(Class<? extends DataPart<?>> type) {
        for (RegData d : bothSideList) {
            if (d.type == type) return d.networkID;
        }
        throw new IllegalStateException(type + " is not a DataPart registered on both sides and has no network id");
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends DataPart<?>> typeOfNetworkId(byte id) {
        return (Class<? extends DataPart<?>>) bothSideList.get(id).type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityData<T> get(T entity) {
        Objects.requireNonNull(entity);
        Debug.assert2(_baked);
        if (!needEntityDataFor(entity.getClass()))
            return null;

        IEntityData ret = entity.getCapability(DataPartCapability.DATA_PART_CAPABILITY).orElse(null);
        if (!(ret instanceof EntityData)) {
            return null;
        }
        ((EntityData<T>) ret).checkInit();
        return (EntityData<T>) ret;
    }

    public static boolean isReady(Entity entity) {

        return entity != null && _baked && get(entity) != null;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static boolean isLocalPlayerReady() {
        return isReady(net.minecraft.client.Minecraft.getInstance().player);
    }

    private final ImmutableMap<Class<?>, DataPart<?>> constructed;

    private Ent entity;

    private boolean _init = false;

    @SuppressWarnings("unchecked")
    public EntityData(Ent entity) {
        Debug.assertNotNull(entity);
        this.entity = entity;

        Map<Class<?>, DataPart<?>> map = new HashMap<>();
        for (RegData data : regList) {
            if (data.isApplicable(entity)) {
                try {
                    DataPart<?> instance = data.type.getDeclaredConstructor().newInstance();
                    ((DataPart<Ent>) instance).entityData = this;
                    map.put(data.type, instance);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException("Failed to construct DataPart " + data.type, ex);
                }
            }
        }
        constructed = ImmutableMap.copyOf(map);
    }

    private void checkInit() {
        if (_init)
            return;
        _init = true;
        for (DataPart<?> dp : constructed.values()) {
            dp.wake();
        }
    }

    public boolean isInitialized() {
        return entity != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends DataPart<?>> T getPart(Class<T> type) {
        return Debug.assertNotNull(
                (T) constructed.get(type),
                () -> "No DataPart of type " + type + " in " + this);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> DataPart<T> getPartNonCreate(Class<? extends DataPart<T>> type) {
        return (DataPart<T>) constructed.getOrDefault(type, null);
    }

    public Ent getEntity() {
        return entity;
    }

    void setEntity(Ent entity) {
        this.entity = entity;
    }

    public void syncAllToClient() {
        for (DataPart<?> dp : constructed.values()) {
            try {
                dp.sync();
            } catch (Throwable ignored) {

            }
        }
    }

    @Override
    public void writeNBT(CompoundTag tag_) {
        Debug.assert2(isInitialized());
        CompoundTag tag = new CompoundTag();
        constructed.values().forEach(part -> {
            if (part.needNBTStorage) {
                CompoundTag partTag = new CompoundTag();
                part.toNBT(partTag);
                tag.put(_partNBTID(part), partTag);
            }
        });
        tag_.put(ID, tag);
    }

    @Override
    public void readNBT(CompoundTag tag_) {
        CompoundTag tag = tag_.getCompound(ID);
        for (DataPart<?> dp : constructed.values()) {
            if (dp.needNBTStorage) {
                String id = _partNBTID(dp);
                if (tag.contains(id)) {
                    dp.fromNBT(tag.getCompound(id));
                }
            }
        }
    }

    private String _partNBTID(DataPart<?> part) {
        return part.getClass().getCanonicalName();
    }

    void tick() {
        checkInit();
        for (DataPart<?> part : constructed.values()) {
            part.callTick();
        }
    }

    void onOwnerDead() {
        constructed.values().forEach(DataPart::onPlayerDead);
    }

    static final class RegData {
        Class<? extends DataPart<?>> type;
        EnumSet<LogicalSide> sides;
        Predicate<Class<? extends Entity>> pred;
        byte networkID;

        boolean isApplicable(Entity ent) {
            LogicalSide runtimeSide = ent.level().isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER;
            return sides.contains(runtimeSide) && pred.test(ent.getClass());
        }
    }
}
