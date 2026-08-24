package cn.academy.ability.context;

import cn.academy.ability.Skill;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class KeyDelegate {

    private Integer identifier = null;

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    protected final Minecraft getMC() {
        return Minecraft.getInstance();
    }

    protected final Player getPlayer() {
        return getMC().player;
    }

    public abstract ResourceLocation getIcon();

    public abstract int createID();

    public abstract Skill getSkill();

    public final Integer getIdentifier() {
        if (identifier == null) {
            identifier = createID();
        }
        return identifier;
    }

    public DelegateState getState() {
        return DelegateState.IDLE;
    }

}
