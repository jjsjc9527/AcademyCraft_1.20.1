package cn.academy.client.render.entity;

import cn.academy.ACItems;
import cn.academy.entity.EntityShiftNeedle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class NeedleStuckLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<>();

    private final ItemRenderer itemRenderer;

    private final List<Matrix4f> mats = new ArrayList<>();
    private final List<ModelPart.Cube> cubes = new ArrayList<>();

    private int[] staticOrder;

    private final RandomSource rnd = RandomSource.create();

    private static final ItemStack NEEDLE_STACK = new ItemStack(ACItems.NEEDLE.get());

    public NeedleStuckLayer(RenderLayerParent<T, M> parent, ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        List<EntityShiftNeedle> needles = StuckNeedles.get(entity.getId());
        if (needles.isEmpty()) {
            return;
        }

        net.minecraft.client.resources.model.BakedModel model =
                cn.academy.client.render.ACClientRenderers.needleStuck;

        List<ModelPart> roots = rootsOf(getParentModel());
        final int[] n = {0};
        cubes.clear();
        PoseStack probe = new PoseStack();
        for (ModelPart root : roots) {
            root.visit(probe, (pose, path, index, cube) -> {

                if (mats.size() <= n[0]) {
                    mats.add(new Matrix4f(pose.pose()));
                } else {
                    mats.get(n[0]).set(pose.pose());
                }
                n[0]++;
                cubes.add(cube);
            });
        }
        if (cubes.isEmpty()) {
            return;
        }

        int[] ord = staticOrder(roots, cubes.size());

        for (EntityShiftNeedle needle : needles) {

            rnd.setSeed(needle.getId());
            int rank = Mth.clamp((int) ((1.0F - needle.aimHeightFraction()) * cubes.size()),
                    0, cubes.size() - 1);
            int idx = ord[rank];
            ModelPart.Cube cube = cubes.get(idx);
            float u = rnd.nextFloat(), v = rnd.nextFloat(), w = rnd.nextFloat();

            ps.pushPose();
            ps.mulPoseMatrix(mats.get(idx));
            ps.translate(Mth.lerp(u, cube.minX, cube.maxX) / 16.0F,
                    Mth.lerp(v, cube.minY, cube.maxY) / 16.0F,
                    Mth.lerp(w, cube.minZ, cube.maxZ) / 16.0F);

            float dx = u * 2.0F - 1.0F, dy = v * 2.0F - 1.0F, dz = w * 2.0F - 1.0F;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-4F) {
                dx = 0; dy = 1; dz = 0; len = 1;
            }
            dx /= len; dy /= len; dz /= len;
            float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(dy, -1.0F, 1.0F)));
            float yaw = (float) Math.toDegrees(Math.atan2(-dz, dx));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));

            ps.mulPose(Axis.ZP.rotationDegrees(model != null ? pitch : pitch - 45.0F));
            ps.scale(0.75F, 0.75F, 0.75F);
            if (model != null) {

                itemRenderer.render(NEEDLE_STACK, ItemDisplayContext.NONE, false, ps, buffers,
                        light, OverlayTexture.NO_OVERLAY, model);
            } else {
                itemRenderer.renderStatic(NEEDLE_STACK, ItemDisplayContext.NONE, light,
                        OverlayTexture.NO_OVERLAY, ps, buffers, entity.level(), 0);
            }
            ps.popPose();
        }
    }

    private int[] staticOrder(List<ModelPart> roots, int cubeCount) {
        if (staticOrder != null && staticOrder.length == cubeCount) {
            return staticOrder;
        }
        List<ModelPart> all = new ArrayList<>();
        for (ModelPart root : roots) {
            root.getAllParts().forEach(all::add);
        }
        List<net.minecraft.client.model.geom.PartPose> saved = new ArrayList<>(all.size());
        for (ModelPart p : all) {
            saved.add(p.storePose());
        }

        float[] centerY = new float[cubeCount];
        try {
            for (ModelPart p : all) {
                p.resetPose();
            }
            final int[] k = {0};
            org.joml.Vector3f probePoint = new org.joml.Vector3f();
            PoseStack probe = new PoseStack();
            for (ModelPart root : roots) {
                root.visit(probe, (pose, path, index, cube) -> {
                    if (k[0] < centerY.length) {
                        probePoint.set((cube.minX + cube.maxX) / 32.0F,
                                (cube.minY + cube.maxY) / 32.0F,
                                (cube.minZ + cube.maxZ) / 32.0F);
                        probePoint.mulPosition(pose.pose());
                        centerY[k[0]] = probePoint.y;
                    }
                    k[0]++;
                });
            }
        } finally {
            for (int i = 0; i < all.size(); i++) {
                all.get(i).loadPose(saved.get(i));
            }
        }

        Integer[] boxed = new Integer[cubeCount];
        for (int i = 0; i < cubeCount; i++) {
            boxed[i] = i;
        }

        java.util.Arrays.sort(boxed, (a, b) -> Float.compare(centerY[a], centerY[b]));
        staticOrder = new int[cubeCount];
        for (int i = 0; i < cubeCount; i++) {
            staticOrder[i] = boxed[i];
        }
        return staticOrder;
    }

    private static List<ModelPart> rootsOf(EntityModel<?> model) {
        if (model instanceof HierarchicalModel<?> hm) {
            return List.of(hm.root());
        }
        List<ModelPart> out = new ArrayList<>();
        for (Field f : FIELD_CACHE.computeIfAbsent(model.getClass(), NeedleStuckLayer::collectFields)) {
            try {
                ModelPart p = (ModelPart) f.get(model);
                if (p != null) {
                    out.add(p);
                }
            } catch (IllegalAccessException ignored) {

            }
        }
        return out;
    }

    private static List<Field> collectFields(Class<?> clazz) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (ModelPart.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        out.add(f);
                    } catch (RuntimeException ignored) {

                    }
                }
            }
        }
        return out;
    }
}
