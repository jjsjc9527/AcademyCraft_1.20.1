package cn.academy.client.render.util;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class CubePointFactory implements IPointFactory {

    private double w, h, l;
    private static final Random RNG = new Random();
    private boolean centered;

    public CubePointFactory(double w, double h, double l) {
        setSize(w, h, l);
    }

    public CubePointFactory setCentered(boolean b) {
        centered = b;
        return this;
    }

    public void setSize(double w, double h, double l) {
        this.w = w;
        this.h = h;
        this.l = l;
    }

    private int randFace() {
        return RNG.nextInt(6);
    }

    @Override
    public Vec3 next() {
        int face = randFace();
        double a, b;
        double xOffset = 0, zOffset = 0;
        if (centered) {
            xOffset = -w * 0.5;
            zOffset = -l * 0.5;
        }
        switch (face) {
            case 0:
            case 1:
                a = RNG.nextDouble() * w;
                b = RNG.nextDouble() * l;
                return new Vec3(a + xOffset, face == 0 ? 0 : h, b + zOffset);
            case 2:
            case 3:
                a = RNG.nextDouble() * h;
                b = RNG.nextDouble() * w;
                return new Vec3(b + xOffset, a, (face == 2 ? 0 : l) + zOffset);
            case 4:
            case 5:
                a = RNG.nextDouble() * h;
                b = RNG.nextDouble() * l;
                return new Vec3((face == 4 ? 0 : w) + xOffset, a, b + zOffset);
            default:
                throw new IllegalStateException("unreachable: randFace() only returns 0..5");
        }
    }
}
