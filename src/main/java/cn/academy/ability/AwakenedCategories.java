package cn.academy.ability;

import cn.academy.datapart.AbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AwakenedCategories extends SavedData {

    private static final String FILE = "academy_awakened";
    private static final String TAG_OWNER = "owner";

    private final Map<UUID, String> owner = new HashMap<>();

    public static AwakenedCategories get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(AwakenedCategories::load, AwakenedCategories::new, FILE);
    }

    @Nullable
    public static AwakenedCategories of(@Nullable Player player) {
        if (player == null) return null;
        MinecraftServer server = player.getServer();
        return server == null ? null : get(server);
    }

    public static AwakenedCategories load(CompoundTag tag) {
        AwakenedCategories data = new AwakenedCategories();
        CompoundTag map = tag.getCompound(TAG_OWNER);
        for (String key : map.getAllKeys()) {
            try {
                data.owner.put(UUID.fromString(key), map.getString(key));
            } catch (IllegalArgumentException ignored) {

            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag map = new CompoundTag();
        owner.forEach((uuid, cat) -> map.putString(uuid.toString(), cat));
        tag.put(TAG_OWNER, map);
        return tag;
    }

    public void set(UUID player, @Nullable String categoryName) {
        String old = categoryName == null ? owner.remove(player) : owner.put(player, categoryName);
        if (!Objects.equals(old, categoryName)) {
            setDirty();
        }
    }

    public Set<String> takenExcept(UUID self) {
        Set<String> taken = new HashSet<>();
        owner.forEach((uuid, cat) -> {
            if (!uuid.equals(self)) {
                taken.add(cat);
            }
        });
        return taken;
    }

    @Mod.EventBusSubscriber(modid = "academy")
    public static final class Backfill {
        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
            if (!(e.getEntity() instanceof ServerPlayer sp)) return;
            AbilityData data = AbilityData.get(sp);
            if (data.hasCategory()) {
                get(sp.server).set(sp.getUUID(), data.getCategory().getName());
            }
        }
    }
}
