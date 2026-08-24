package cn.academy.entity;

import cn.academy.client.render.util.SubArc;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EntityIntensifyEffect extends EntitySurroundArc {

    public EntityIntensifyEffect(Player player) {
        super(player);
        setArcType(ArcType.THIN);
        this.autoGenerate = false;
        this.life = 15;
    }

    @Override
    public void tick() {
        super.tick();
        if (getArcHandler() == null) return;

        switch (tickCount) {
            case 0 -> genAtHt(2.0);
            case 1 -> genAtHt(1.8);
            case 3 -> genAtHt(1.5);
            case 4 -> genAtHt(1.0);
            case 6 -> genAtHt(0.5);
            case 7 -> genAtHt(0.0);
            case 8 -> genAtHt(-0.1);
            default -> { }
        }
    }

    private void genAtHt(double ht) {
        int gen = RandUtils.rangei(3, 4);
        while (gen-- > 0) {
            double phi = RandUtils.ranged(0.5, 0.6);
            double theta = RandUtils.ranged(0, Math.PI * 2);
            SubArc arc = getArcHandler().generateAt(
                    new Vec3(phi * Math.sin(theta), ht, phi * Math.cos(theta)));
            arc.life = 3;
        }
    }
}
