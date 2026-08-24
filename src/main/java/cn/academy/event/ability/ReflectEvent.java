package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class ReflectEvent extends AbilityEvent {

    public final Skill skill;
    public final Entity target;

    public Vec3 incomingFrom = null;

    public Vec3 incomingDir = null;

    public Vec3 hitPos = null;

    public double hitDist = 0;

    public int arriveDelay = 0;

    public float damage = 0;

    public float difficulty = 1.0f;

    public boolean deflectable = true;

    public double beamLength = 0;

    public Vec3 reflectDir = null;

    public boolean returnToCaster = false;

    public boolean bend = false;

    public ReflectEvent(Player player, Skill _skill, Entity _target) {
        super(player);
        skill = _skill;
        target = _target;
    }

    public boolean isRay() {
        return incomingDir != null && hitPos != null;
    }
}
