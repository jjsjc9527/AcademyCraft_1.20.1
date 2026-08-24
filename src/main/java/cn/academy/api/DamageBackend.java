package cn.academy.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface DamageBackend {

    String id();

    default boolean handles(Player attacker, LivingEntity target) {
        return false;
    }

    @Nullable
    default Boolean strike(Player attacker, LivingEntity target, DamageSource normal, float damage) {
        return null;
    }
}
