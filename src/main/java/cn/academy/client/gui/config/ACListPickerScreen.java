package cn.academy.client.gui.config;

import cn.academy.config.ConfigChoices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ACListPickerScreen extends Screen {

    private static final int ROW_H = 20;

    private static final int ICON = 18;

    private static final int TOP = 56;

    private static final int BOTTOM_PAD = 42;
    private static final int GAP = 8;
    private static final int PAD = 3;

    private static final int COLOR_TEXT = 0xE0E0E0;
    private static final int COLOR_DIM = 0x909090;
    private static final int COLOR_HOVER = 0xFFFFA0;
    private static final int COLOR_FRAME = 0xFF808080;
    private static final int COLOR_BG = 0xC0101010;

    private final Screen parent;
    private final ConfigChoices.Source source;
    private final List<String> original;
    private final Consumer<List<String>> onApply;

    private final Set<String> selected = new LinkedHashSet<>();

    private List<String> all = List.of();

    private int leftScroll;
    private int rightScroll;
    private int leftX, leftW, rightX, rightW, listY, listH, rows;

    private final java.util.Map<String, String> nameCache = new java.util.HashMap<>();

    private String hoveredId;

    public ACListPickerScreen(Screen parent, ConfigChoices.Source source,
                              List<String> current, Consumer<List<String>> onApply) {
        super(Component.literal(source.title()));
        this.parent = parent;
        this.source = source;
        this.original = List.copyOf(current);
        this.onApply = onApply;
        this.selected.addAll(current);
    }

    @Override
    protected void init() {

        nameCache.clear();

        List<String> raw = new ArrayList<>(source.choices().get());

        raw.sort(String::compareTo);
        all = raw;

        int totalW = Math.min(this.width - 24, 520);
        int x0 = (this.width - totalW) / 2;
        leftW = Math.max(120, totalW * 36 / 100);
        leftX = x0;
        rightX = x0 + leftW + GAP;
        rightW = totalW - leftW - GAP;

        listY = TOP;
        listH = Math.max(ROW_H * 4, this.height - TOP - BOTTOM_PAD);
        rows = Math.max(1, (listH - PAD * 2) / ROW_H);

        clampScroll();

        if (source.preset() != null) {
            addRenderableWidget(Button.builder(Component.literal(source.presetLabel()), b -> {
                selected.addAll(source.preset().get());
                clampScroll();
            }).bounds(rightX, TOP - 24, Math.min(190, rightW / 2 - 2), 20).build());
        }
        int clearW = Math.min(110, rightW / 3);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.academy.config.picker.clear"), b -> {
                    selected.clear();
                    clampScroll();
                })
                .bounds(rightX + rightW - clearW, TOP - 24, clearW, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.academy.config.picker.apply"), b -> {
                    onApply.accept(new ArrayList<>(selected));
                    this.minecraft.setScreen(parent);
                })
                .bounds(leftX, this.height - BOTTOM_PAD + 16, 100, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.academy.config.picker.back"), b -> onClose())
                .bounds(rightX + rightW - 100, this.height - BOTTOM_PAD + 16, 100, 20).build());
    }

    @Override
    public void onClose() {
        if (dirty()) {
            this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                    yes -> this.minecraft.setScreen(yes ? parent : this),
                    Component.translatable("gui.academy.config.picker.discard_title"),
                    Component.translatable("gui.academy.config.picker.discard_msg")));
            return;
        }
        this.minecraft.setScreen(parent);
    }

    private boolean dirty() {
        return !new LinkedHashSet<>(original).equals(selected);
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        renderBackground(gg);
        gg.drawCenteredString(font, this.title, this.width / 2, 16, 0xFFFFFF);

        hoveredId = null;
        drawList(gg, mx, my, leftX, leftW, visibleLeft(), leftScroll,
                Component.translatable("gui.academy.config.picker.available"));
        drawList(gg, mx, my, rightX, rightW, new ArrayList<>(selected), rightScroll,
                Component.translatable("gui.academy.config.picker.chosen"));

        if (hoveredId != null) {
            gg.drawCenteredString(font, hoveredId, this.width / 2, 30, COLOR_DIM);
        }

        super.render(gg, mx, my, pt);
    }

    private List<String> visibleLeft() {
        List<String> out = new ArrayList<>();
        for (String s : all) {
            if (!selected.contains(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private void drawList(GuiGraphics gg, int mx, int my, int x, int w,
                          List<String> items, int scroll, Component header) {
        gg.fill(x, listY, x + w, listY + listH, COLOR_BG);
        gg.renderOutline(x, listY, w, listH, COLOR_FRAME);

        gg.drawString(font, header, x + 1, listY + listH + 3, COLOR_DIM, false);

        int hovered = rowAt(mx, my, x, w);
        for (int i = 0; i < rows; i++) {
            int idx = scroll + i;
            if (idx >= items.size()) {
                break;
            }
            int y = listY + PAD + i * ROW_H;
            boolean hi = hovered == i;
            if (hi) {
                gg.fill(x + 1, y - 1, x + w - 1, y + ROW_H - 2, 0x40FFFFFF);
            }
            String id = items.get(idx);

            drawIcon(gg, id, x + PAD, y);
            int textX = x + PAD + ICON + 3;

            String label = shorten(displayName(id), x + w - PAD - textX);
            if (hi) {
                hoveredId = id;
            }

            gg.drawString(font, label, textX, y + (ROW_H - 10) / 2, hi ? COLOR_HOVER : COLOR_TEXT, false);
        }

        String count = (scroll + Math.min(rows, Math.max(0, items.size() - scroll)))
                + "/" + items.size();
        gg.drawString(font, count, x + w - font.width(count) - 1,
                listY + listH + 3, COLOR_DIM, false);
    }

    private String displayName(String id) {
        String cached = nameCache.get(id);
        if (cached != null) {
            return cached;
        }
        String name = resolveName(id);
        nameCache.put(id, name);
        return name;
    }

    private String resolveName(String id) {
        if (!ConfigChoices.IconType.MOB_EFFECT.equals(source.iconType())) {
            return id;
        }
        try {
            var loc = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (loc == null) {
                return id;
            }
            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(loc);
            if (effect == null) {
                return id;
            }
            String key = effect.getDescriptionId();
            if (key == null
                    || !net.minecraft.client.resources.language.I18n.exists(key)) {
                return id;
            }
            return net.minecraft.client.resources.language.I18n.get(key);
        } catch (Throwable ignored) {
            return id;
        }
    }

    private void drawIcon(GuiGraphics gg, String id, int x, int y) {
        if (!ConfigChoices.IconType.MOB_EFFECT.equals(source.iconType())) {
            return;
        }
        try {
            var loc = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (loc == null) {
                return;
            }
            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(loc);
            if (effect == null) {
                return;
            }
            var sprite = this.minecraft.getMobEffectTextures().get(effect);
            gg.blit(x, y, 0, ICON, ICON, sprite);
        } catch (Throwable ignored) {

        }
    }

    private String shorten(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        String cut = s;
        while (!cut.isEmpty() && font.width(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private int rowAt(int mx, int my, int x, int w) {
        if (mx < x || mx >= x + w || my < listY + PAD || my >= listY + listH - PAD) {
            return -1;
        }
        int r = (my - listY - PAD) / ROW_H;
        return r >= 0 && r < rows ? r : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lr = rowAt((int) mx, (int) my, leftX, leftW);
        if (lr >= 0) {
            List<String> items = visibleLeft();
            int idx = leftScroll + lr;
            if (idx < items.size()) {
                selected.add(items.get(idx));
                clampScroll();
                return true;
            }
        }
        int rr = rowAt((int) mx, (int) my, rightX, rightW);
        if (rr >= 0) {
            List<String> items = new ArrayList<>(selected);
            int idx = rightScroll + rr;
            if (idx < items.size()) {
                selected.remove(items.get(idx));
                clampScroll();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int step = (int) -Math.signum(delta) * 3;
        if (rowAt((int) mx, (int) my, leftX, leftW) >= 0) {
            leftScroll += step;
            clampScroll();
            return true;
        }
        if (rowAt((int) mx, (int) my, rightX, rightW) >= 0) {
            rightScroll += step;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void clampScroll() {
        leftScroll = clamp(leftScroll, visibleLeft().size());
        rightScroll = clamp(rightScroll, selected.size());
    }

    private int clamp(int scroll, int total) {
        int max = Math.max(0, total - rows);
        return Math.max(0, Math.min(scroll, max));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static List<String> toList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> l) {
            for (Object o : l) {
                if (o != null) {
                    out.add(String.valueOf(o).trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
    }
}
