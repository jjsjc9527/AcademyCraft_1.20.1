package cn.lambdalib2.datapart;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DataPartCapability {

    public static final Capability<IEntityData> DATA_PART_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation KEY = new ResourceLocation("academy", "entity_data");

    private static final DataPartCapability INSTANCE = new DataPartCapability();

    private DataPartCapability() {}

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(DataPartCapability::onRegisterCapabilities);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void init() {
        EntityData.scanAndRegister();
        EntityData.bake();
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IEntityData.class);
    }

    @SubscribeEvent
    public void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (!EntityData.isBaked()) return;
        Entity entity = event.getObject();
        if (!EntityData.needEntityDataFor(entity.getClass())) return;
        event.addCapability(KEY, new Provider(entity));
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EntityData.isBaked()) return;
        EntityData<LivingEntity> data = EntityData.get(event.getEntity());
        if (data != null) {
            data.tick();
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!EntityData.isBaked()) return;
        if (event.getEntity() instanceof Player player) {
            EntityData<Player> data = EntityData.get(player);
            if (data != null) {
                data.onOwnerDead();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!EntityData.isBaked()) return;
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            EntityData<Player> oldData = EntityData.get(original);
            EntityData<Player> newData = EntityData.get(event.getEntity());
            if (oldData != null && newData != null) {
                CompoundTag tag = new CompoundTag();
                oldData.writeNBT(tag);
                newData.readNBT(tag);
            }
        } finally {
            original.invalidateCaps();
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final Entity entity;
        private EntityData<Entity> instance;
        private final LazyOptional<IEntityData> optional = LazyOptional.of(this::getInstance);

        Provider(Entity entity) {
            this.entity = entity;
        }

        private EntityData<Entity> getInstance() {
            if (instance == null) {
                instance = new EntityData<>(entity);
            }
            return instance;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == DATA_PART_CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            getInstance().writeNBT(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            getInstance().readNBT(tag);
        }
    }
}
