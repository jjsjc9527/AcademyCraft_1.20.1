package cn.academy.client.gui.tutorial;

import cn.academy.Resources;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.tutorial.ViewGroup;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class RecipeViews {

    public interface PreviewView {
        void render(float areaW, float areaH, double mx, double my, boolean hovering);
    }

    private RecipeViews() {}

    private static final ResourceLocation TEX_GRID   = Resources.getTexture("guis/tutorial/crafting_grid");
    private static final ResourceLocation TEX_FUSOR  = Resources.getTexture("guis/tutorial_fusor");
    private static final ResourceLocation TEX_FORMER = Resources.getTexture("guis/tutorial_metalformer");
    private static final ResourceLocation TEX_SMELT  = Resources.getTexture("guis/tutorial_smelting");
    private static final ResourceLocation PROG_FUSOR  = Resources.getTexture("gui/progress_fusor");
    private static final ResourceLocation PROG_FORMER = Resources.getTexture("gui/progress_metalformer");

    private static final double ALTERNATE_TIME = 2.0;
    private static final float ITEM_PX = 32f;

    public static List<PreviewView> buildFor(ViewGroup g) {
        ItemStack recipeTarget = g.recipeTarget();
        if (!recipeTarget.isEmpty()) return buildRecipes(recipeTarget);
        ItemStack stack = g.previewStack();
        if (!stack.isEmpty()) return List.of(new ItemView(stack));
        ResourceLocation icon = g.previewIcon();
        if (icon != null) return List.of(new IconView(icon));
        return List.of();
    }

    private static List<PreviewView> buildRecipes(ItemStack target) {
        List<PreviewView> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return out;
        RegistryAccess ra = level.registryAccess();
        RecipeManager rm = level.getRecipeManager();

        for (CraftingRecipe r : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = r.getResultItem(ra);
            if (!sameItem(result, target)) continue;
            if (r instanceof ShapedRecipe sr) {
                out.add(new CraftingView(remapShaped(sr), result, "shaped"));
            } else {
                out.add(new CraftingView(rowMajor(r.getIngredients()), result, "shapeless"));
            }
        }

        for (ImagFusorRecipes.IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
            if (sameItem(r.output, target)) {
                out.add(MachineView.fusor(new ItemStack[]{r.consumeType}, r.output, r.consumeLiquid));
            }
        }

        for (MetalFormerRecipes.RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
            ItemStack o = r.getOutput();
            if (o.isEmpty() || !sameItem(o, target)) continue;
            ItemStack[] ins = r.displayInputs().toArray(new ItemStack[0]);
            out.add(MachineView.former(ins, o, modeTex(r.mode)));
        }

        for (SmeltingRecipe r : rm.getAllRecipesFor(RecipeType.SMELTING)) {
            ItemStack result = r.getResultItem(ra);
            if (!sameItem(result, target)) continue;
            ItemStack[] ins = r.getIngredients().isEmpty()
                    ? new ItemStack[0] : r.getIngredients().get(0).getItems();
            out.add(MachineView.smelt(ins, result));
        }
        return out;
    }

    private static ResourceLocation modeTex(MetalFormerRecipes.Mode mode) {
        return Resources.getTexture("gui/icon_former_" + mode.name().toLowerCase());
    }

    private static ItemStack[][] remapShaped(ShapedRecipe sr) {
        int w = sr.getWidth();
        List<Ingredient> ing = sr.getIngredients();
        ItemStack[][] grid = new ItemStack[9][];
        for (int i = 0; i < ing.size(); i++) {
            int row = i / w, col = i % w;
            if (col < 3 && row < 3) grid[col + row * 3] = candidates(ing.get(i));
        }
        return grid;
    }

    private static ItemStack[][] rowMajor(List<Ingredient> ing) {
        ItemStack[][] grid = new ItemStack[9][];
        for (int i = 0; i < ing.size() && i < 9; i++) grid[i] = candidates(ing.get(i));
        return grid;
    }

    private static ItemStack[] candidates(Ingredient ing) {
        if (ing == null || ing.isEmpty()) return null;
        ItemStack[] items = ing.getItems();
        return items.length == 0 ? null : items;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItem(a, b);
    }

    private static Matrix4f contentMatrix(float areaW, float areaH, float cw, float ch, float scale) {
        float ox = (areaW - cw * scale) / 2f;
        float oy = (areaH - ch * scale) / 2f;
        return new Matrix4f(HudUtils.getMatrix()).translate(ox, oy, 0).scale(scale, scale, 1);
    }

    private static void renderStack(ItemStack stack, double cx, double cy, double sizePx) {
        if (stack == null || stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer ir = mc.getItemRenderer();
        BakedModel model = ir.getModel(stack, mc.level, null, 0);

        PoseStack ps = new PoseStack();
        ps.mulPoseMatrix(HudUtils.getMatrix());
        ps.translate(cx, cy, 150);
        ps.scale((float) sizePx, (float) -sizePx, (float) sizePx);

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        if (model.usesBlockLight()) Lighting.setupFor3DItems(); else Lighting.setupForFlatItems();
        ir.renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        buf.endBatch();
        Lighting.setupForFlatItems();
    }

    private static ItemStack pick(ItemStack[] cands) {
        if (cands == null || cands.length == 0) return ItemStack.EMPTY;
        int i = (int) (GameTimer.getAbsTime() / ALTERNATE_TIME) % cands.length;
        return cands[i];
    }

    private static void bindWhite() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void drawBg(ResourceLocation tex, float cw, float ch) {
        bindWhite();
        HudUtils.loadTexture(tex);
        HudUtils.rect(0, 0, cw, ch);
    }

    private static final double PROGRESS_PERIOD = 2.0;

    private static void drawProgress(ResourceLocation tex, float x, float y, float w, float h) {
        double disp = (GameTimer.getAbsTime() % PROGRESS_PERIOD) / PROGRESS_PERIOD;
        bindWhite();
        HudUtils.loadTexture(tex);
        HudUtils.rawRect(x, y, 0, 0, w * disp, h, disp, 1);
    }

    private static final class Hover {
        String text = null;

        void test(boolean hovering, double mx, double my,
                  float ox, float oy, float scale,
                  double slotCx, double slotCy, double half, ItemStack stack) {
            if (!hovering || stack == null || stack.isEmpty() || text != null) return;
            double lx = (mx - ox) / scale, ly = (my - oy) / scale;
            if (lx >= slotCx - half && lx <= slotCx + half && ly >= slotCy - half && ly <= slotCy + half) {
                text = stack.getHoverName().getString();
            }
        }

        void draw(Matrix4f base, double mx, double my) {
            if (text == null) return;
            HudUtils.setMatrix(base);
            IFont font = Resources.font();
            FontOption opt = new FontOption(9, Colors.white());
            float tw = font.getTextWidth(text, opt);
            double x = mx + 8, y = my - 4;
            double savedZ = HudUtils.zLevel;
            HudUtils.zLevel = 300;
            RenderSystem.setShaderColor(0, 0, 0, 0.75f);
            HudUtils.colorRect(x - 2, y - 2, tw + 4, 12);
            bindWhite();
            font.draw(text, (float) x, (float) y, opt);
            HudUtils.zLevel = savedZ;
        }
    }

    static final class ItemView implements PreviewView {
        private final ItemStack stack;
        ItemView(ItemStack stack) { this.stack = stack; }

        @Override public void render(float areaW, float areaH, double mx, double my, boolean hovering) {
            Minecraft mc = Minecraft.getInstance();
            PoseStack ps = new PoseStack();
            ps.mulPoseMatrix(HudUtils.getMatrix());
            ps.translate(areaW / 2f, areaH / 2f, 100);
            float s = areaH * 0.55f;
            ps.scale(s, -s, s);
            ps.mulPose(Axis.XP.rotationDegrees(30));

            ps.mulPose(Axis.YP.rotationDegrees((float) (-(GameTimer.getAbsTime() * 25.0) % 360.0)));
            MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
            Lighting.setupFor3DItems();
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
            buf.endBatch();
            Lighting.setupForFlatItems();
        }
    }

    static final class IconView implements PreviewView {
        private final ResourceLocation icon;
        IconView(ResourceLocation icon) { this.icon = icon; }

        @Override public void render(float areaW, float areaH, double mx, double my, boolean hovering) {
            bindWhite();
            HudUtils.loadTexture(icon);
            float sz = areaH * 0.6f;
            HudUtils.rect(areaW / 2f - sz / 2, areaH / 2f - sz / 2, sz, sz);
        }
    }

    static final class CraftingView implements PreviewView {
        static final float CW = 196, CH = 128, SCALE = 0.6f, STEP = 43;
        private final ItemStack[][] grid;
        private final ItemStack output;
        private final String desc;

        CraftingView(ItemStack[][] grid, ItemStack output, String desc) {
            this.grid = grid; this.output = output; this.desc = desc;
        }

        @Override public void render(float areaW, float areaH, double mx, double my, boolean hovering) {
            Matrix4f base = HudUtils.getMatrix();
            float ox = (areaW - CW * SCALE) / 2f, oy = (areaH - CH * SCALE) / 2f;
            HudUtils.setMatrix(contentMatrix(areaW, areaH, CW, CH, SCALE));

            drawBg(TEX_GRID, CW, CH);

            Hover hover = new Hover();

            for (int i = 0; i < 9; i++) {
                ItemStack s = pick(grid[i]);
                double cx = 5 + (i % 3) * STEP + 16, cy = 5 + (i / 3) * STEP + 16;
                renderStack(s, cx, cy, ITEM_PX);
                hover.test(hovering, mx, my, ox, oy, SCALE, cx, cy, 16, s);
            }

            renderStack(output, 169, 65, ITEM_PX);
            hover.test(hovering, mx, my, ox, oy, SCALE, 169, 65, 16, output);

            bindWhite();
            Resources.font().draw(Component.translatable("ac.gui.crafttype." + desc).getString(),
                    68, -28, new FontOption(24, FontAlign.CENTER, Colors.white()));

            hover.draw(base, mx, my);
            HudUtils.setMatrix(base);
        }
    }

    static final class MachineView implements PreviewView {
        private final ResourceLocation bg;
        private final float cw, ch, scale;
        private final ItemStack[] in;
        private final ItemStack out;
        private final float inCx, inCy, outCx, outCy;
        private final ResourceLocation modeTex;
        private final float modeX, modeY, modeSz;
        private final int amount;
        private final float amountCx, amountCy;
        private final ResourceLocation progTex;
        private final float progX, progY, progW, progH;

        private MachineView(ResourceLocation bg, float cw, float ch, float scale,
                            ItemStack[] in, ItemStack out, float inCx, float inCy, float outCx, float outCy,
                            ResourceLocation modeTex, float modeX, float modeY, float modeSz,
                            int amount, float amountCx, float amountCy,
                            ResourceLocation progTex, float progX, float progY, float progW, float progH) {
            this.bg = bg; this.cw = cw; this.ch = ch; this.scale = scale;
            this.in = in; this.out = out;
            this.inCx = inCx; this.inCy = inCy; this.outCx = outCx; this.outCy = outCy;
            this.modeTex = modeTex; this.modeX = modeX; this.modeY = modeY; this.modeSz = modeSz;
            this.amount = amount; this.amountCx = amountCx; this.amountCy = amountCy;
            this.progTex = progTex; this.progX = progX; this.progY = progY; this.progW = progW; this.progH = progH;
        }

        static MachineView former(ItemStack[] in, ItemStack out, ResourceLocation modeTex) {
            return new MachineView(TEX_FORMER, 192, 192, 0.5f,
                    in, out, 11.33f + 12.5f, 88.5f + 12.5f, 155.33f + 12.5f, 88.5f + 12.5f,
                    modeTex, 82.67f, 22.7f, 25f,
                    -1, 0, 0,
                    PROG_FORMER, 77.67f, 83.5f, 36.67f, 23.33f);
        }

        static MachineView fusor(ItemStack[] in, ItemStack out, int amount) {
            return new MachineView(TEX_FUSOR, 196, 128, 0.6f,
                    in, out, 19f + 16f, 62.5f + 15.5f, 147f + 16f, 62.5f + 15.5f,
                    null, 0, 0, 0,
                    amount, 81f + 26f, 14.5f + 7.5f,
                    PROG_FUSOR, 66f, 68.5f, 66f, 18f);
        }

        static MachineView smelt(ItemStack[] in, ItemStack out) {
            return new MachineView(TEX_SMELT, 192, 128, 0.6f,
                    in, out, 30f + 16f, 43.17f + 16f, 123.33f + 16f, 43.17f + 16f,
                    null, 0, 0, 0,
                    -1, 0, 0,
                    null, 0, 0, 0, 0);
        }

        @Override public void render(float areaW, float areaH, double mx, double my, boolean hovering) {
            Matrix4f base = HudUtils.getMatrix();
            float ox = (areaW - cw * scale) / 2f, oy = (areaH - ch * scale) / 2f;
            HudUtils.setMatrix(contentMatrix(areaW, areaH, cw, ch, scale));

            drawBg(bg, cw, ch);

            if (progTex != null) drawProgress(progTex, progX, progY, progW, progH);
            if (modeTex != null) {
                bindWhite();
                HudUtils.loadTexture(modeTex);
                HudUtils.rect(modeX, modeY, modeSz, modeSz);
            }
            if (amount >= 0) {
                bindWhite();
                Resources.font().draw(String.valueOf(amount), amountCx, amountCy - 7,
                        new FontOption(14, FontAlign.CENTER, Colors.white()));
            }

            ItemStack curIn = pick(in);
            renderStack(curIn, inCx, inCy, ITEM_PX);
            renderStack(out, outCx, outCy, ITEM_PX);

            Hover hover = new Hover();
            hover.test(hovering, mx, my, ox, oy, scale, inCx, inCy, 16, curIn);
            hover.test(hovering, mx, my, ox, oy, scale, outCx, outCy, 16, out);
            hover.draw(base, mx, my);

            HudUtils.setMatrix(base);
        }
    }
}
