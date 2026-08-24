package cn.academy.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface AbyssBackend {

    String id();

    default float cpMultiplier() {
        return 1.0f;
    }

    @Nullable
    default Player takeoverDamage(Player attacker, LivingEntity target) {
        return DamageBackends.handles(attacker, target) ? attacker : null;
    }
}
