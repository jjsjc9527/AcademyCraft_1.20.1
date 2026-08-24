package cn.academy.client.gui.config;

import cn.academy.client.gui.config.ACConfigNode.Entry;
import cn.academy.client.gui.config.ACConfigNode.Kind;
import cn.lambdalib2.input.KeyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ACConfigScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int TOP = 46;
    private static final int BOTTOM_PAD = 42;
    private static final int COLOR_OK = 0xE0E0E0;
    private static final int COLOR_BAD = 0xFF5555;
    private static final int COLOR_LABEL = 0xFFFFFF;
    private static final int COLOR_DIM = 0xA0A0A0;

    private final Screen parent;
    private final ACConfigSources.Source source;

    private ACConfigNode current;
    private final Deque<ACConfigNode> trail = new ArrayDeque<>();

    private int page;
    private int rowsPerPage = 1;

    private Entry capturing;

    private final List<ACConfigNode> pageRows = new ArrayList<>();
    private int contentLeft, contentWidth;

    public ACConfigScreen(Screen parent, ACConfigSources.Source source) {
        super(source.title);
        this.parent = parent;
        this.source = source;
        this.current = source.root;
    }

    @Override
    protected void init() {
        pageRows.clear();

        contentWidth = Math.min(this.width - 40, 420);
        contentLeft = (this.width - contentWidth) / 2;

        int avail = this.height - TOP - BOTTOM_PAD;
        rowsPerPage = Math.max(1, avail / ROW_H);

        List<ACConfigNode> all = current.children();
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));

        int from = page * rowsPerPage;
        int to = Math.min(all.size(), from + rowsPerPage);

        for (int i = from; i < to; i++) {
            ACConfigNode node = all.get(i);
            int y = TOP + (i - from) * ROW_H;
            pageRows.add(node);
            if (node.isCategory()) {
                buildCategoryRow(node, y);
            } else {
                buildEntryRow(node, y);
            }
        }

        buildBottomBar(pages);
    }

    private void buildCategoryRow(ACConfigNode node, int y) {
        Component label = Component.literal(node.display())
                .append(Component.literal("   " + node.countLeaves()).withStyle(ChatFormatting.GRAY));
        addRenderableWidget(new IconButton(contentLeft, y, contentWidth, 20, label, b -> {
            trail.push(current);
            current = node;
            page = 0;
            rebuildWidgets();
        }, node.icon()));
    }

    private static final class IconButton extends Button {
        private final net.minecraft.resources.ResourceLocation icon;

        IconButton(int x, int y, int w, int h, Component msg, OnPress onPress,
                   net.minecraft.resources.ResourceLocation icon) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        public void renderWidget(GuiGraphics gg, int mx, int my, float pt) {
            super.renderWidget(gg, mx, my, pt);
            if (icon != null) {
                gg.blit(icon, getX() + 4, getY() + 2, 16, 16, 0F, 0F, 16, 16, 16, 16);
            }
        }
    }

    private void buildEntryRow(ACConfigNode node, int y) {
        Entry entry = node.entry();
        Entry second = node.second();
        int resetW = 20;

        int ctrlW = second != null
                ? Math.min(200, contentWidth * 3 / 5)
                : Math.min(150, contentWidth / 2);
        int resetX = contentLeft + contentWidth - resetW;
        int ctrlX = resetX - 4 - ctrlW;

        if (second != null) {
            int half = (ctrlW - 4) / 2;
            addRenderableWidget(makeBox(entry, node.name(), ctrlX, y, half));
            addRenderableWidget(makeBox(second, node.name(), ctrlX + half + 4, y, half));
        } else if (entry.isKeyBinding()) {
            addRenderableWidget(Button.builder(keyLabel(entry), b -> {
                capturing = entry;
                rebuildWidgets();
            }).bounds(ctrlX, y, ctrlW, 20).build());
        } else if (entry.kind() == Kind.BOOLEAN) {
            boolean cur = Boolean.TRUE.equals(entry.get());
            addRenderableWidget(Button.builder(onOff(cur), b -> {
                entry.set(!Boolean.TRUE.equals(entry.get()));
                rebuildWidgets();
            }).bounds(ctrlX, y, ctrlW, 20).build());
        } else if (choicesOf(node) != null) {

            var src = choicesOf(node);
            int n = ACListPickerScreen.toList(entry.get()).size();
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.academy.config.picker.open", n),
                            b -> this.minecraft.setScreen(new ACListPickerScreen(
                                    this, src, ACListPickerScreen.toList(entry.get()),

                                    list -> {

                                        if (entry.accepts(list)) {
                                            entry.set(list);
                                        } else {
                                            cn.academy.AcademyCraft.LOGGER.warn(
                                                    "config entry {} rejected the value written by the picker, ignored: {}",
                                                    node.path(), list);
                                        }
                                    })))
                    .bounds(ctrlX, y, ctrlW, 20).build());
        } else {
            addRenderableWidget(makeBox(entry, node.name(), ctrlX, y, ctrlW));
        }

        Object def = entry.defaultValue();
        Object def2 = second == null ? null : second.defaultValue();
        Button reset = Button.builder(Component.literal("↺"), b -> {
            if (def != null) {
                entry.set(def);
            }
            if (def2 != null) {
                second.set(def2);
            }
            rebuildWidgets();
        }).bounds(resetX, y, resetW, 20).build();
        reset.active = (def != null && !ACConfigText.same(entry.get(), def))
                || (def2 != null && !ACConfigText.same(second.get(), def2));
        addRenderableWidget(reset);
    }

    private cn.academy.config.ConfigChoices.Source choicesOf(ACConfigNode node) {
        if (node.entry() == null || node.entry().kind() != Kind.LIST_STRING) {
            return null;
        }
        return cn.academy.config.ConfigChoices.get(node.path());
    }

    private EditBox makeBox(Entry entry, String name, int x, int y, int w) {
        EditBox box = new EditBox(font, x + 1, y + 1, w - 2, 18, Component.literal(name));
        box.setMaxLength(512);
        box.setValue(ACConfigText.format(entry.kind(), entry.get()));

        box.setCursorPosition(0);
        box.setHighlightPos(0);
        box.setResponder(text -> {
            Object v = ACConfigText.parse(entry.kind(), text);
            boolean ok = v != null && entry.accepts(v);
            box.setTextColor(ok ? COLOR_OK : COLOR_BAD);
            if (ok) {
                entry.set(v);
            }
        });
        return box;
    }

    private static final int PAGE_BTN = 20;

    private static final int PAGE_GAP = 44;

    private int pagerY() {
        return this.height - 28;
    }

    private int pagerNextX() {
        return contentLeft + contentWidth - PAGE_BTN;
    }

    private int pagerPrevX() {
        return pagerNextX() - PAGE_GAP - PAGE_BTN;
    }

    private int pagerLabelCx() {
        return pagerPrevX() + PAGE_BTN + PAGE_GAP / 2;
    }

    private int pagerLabelY() {
        return (2 * pagerY() + PAGE_BTN - 9) / 2 + 1;
    }

    private int pageCount() {
        return Math.max(1, (current.children().size() + rowsPerPage - 1) / rowsPerPage);
    }

    private void buildBottomBar(int pages) {
        int y = pagerY();

        boolean top = trail.isEmpty();
        addRenderableWidget(Button.builder(
                Component.translatable(top ? "gui.academy.config.done" : "gui.academy.config.up"),
                b -> {
                    if (trail.isEmpty()) {
                        onClose();
                    } else {
                        current = trail.pop();
                        page = 0;
                        rebuildWidgets();
                    }
                }).bounds(contentLeft, y, 100, 20).build());

        if (pages > 1) {
            Button next = Button.builder(Component.literal("▶"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(pagerNextX(), y, PAGE_BTN, PAGE_BTN).build();
            next.active = page < pages - 1;
            addRenderableWidget(next);

            Button prev = Button.builder(Component.literal("◀"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(pagerPrevX(), y, PAGE_BTN, PAGE_BTN).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        renderBackground(gg);

        gg.drawCenteredString(font, this.title, this.width / 2, 12, COLOR_LABEL);
        gg.drawCenteredString(font, Component.literal(breadcrumb()).withStyle(ChatFormatting.GRAY),
                this.width / 2, 26, COLOR_DIM);

        for (int i = 0; i < pageRows.size(); i++) {
            ACConfigNode node = pageRows.get(i);
            if (node.isCategory()) {
                continue;
            }
            int y = TOP + i * ROW_H + 6;
            boolean isCapturing = capturing != null && capturing == node.entry();
            Component name = isCapturing
                    ? Component.translatable("gui.academy.config.press_key").withStyle(ChatFormatting.YELLOW)
                    : Component.literal(node.display());
            gg.drawString(font, name, contentLeft + 2, y, COLOR_LABEL, false);
        }

        super.render(gg, mouseX, mouseY, partial);

        int pages = pageCount();
        if (pages > 1) {
            gg.drawCenteredString(font, (page + 1) + " / " + pages,
                    pagerLabelCx(), pagerLabelY(), COLOR_DIM);
        }

        renderRowTooltip(gg, mouseX, mouseY);
    }

    private void renderRowTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (mouseX < contentLeft || mouseX > contentLeft + contentWidth) {
            return;
        }

        int rel = mouseY - TOP;
        if (rel < 0) {
            return;
        }
        int idx = rel / ROW_H;
        if (idx >= pageRows.size() || rel % ROW_H > 20) {
            return;
        }
        ACConfigNode node = pageRows.get(idx);
        if (node.isCategory()) {
            return;
        }
        Entry entry = node.entry();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.path()).withStyle(ChatFormatting.YELLOW));
        for (String c : entry.comment()) {
            lines.add(Component.literal(c).withStyle(ChatFormatting.GRAY));
        }
        String range = entry.rangeHint();
        if (range != null) {
            lines.add(Component.translatable("gui.academy.config.range", range)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        Object def = entry.defaultValue();
        if (def != null) {
            String s = ACConfigText.format(entry.kind(), def);
            Entry sec = node.second();
            if (sec != null && sec.defaultValue() != null) {
                s += "  →  " + ACConfigText.format(sec.kind(), sec.defaultValue());
            }
            lines.add(Component.translatable("gui.academy.config.default", s)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (node.second() != null) {
            lines.add(Component.translatable("gui.academy.config.pair_hint")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        gg.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (capturing != null) {
            capturing.set(button - 100);
            capturing = null;
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (capturing != null) {

            if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                capturing.set(key);
            }
            capturing = null;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private String breadcrumb() {
        StringBuilder sb = new StringBuilder(source.fileName);
        List<ACConfigNode> chain = new ArrayList<>(trail);
        Collections.reverse(chain);
        chain.add(current);
        for (ACConfigNode n : chain) {
            String d = n.display();
            if (!d.isEmpty() && !d.equals(source.fileName)) {
                sb.append(" / ").append(d);
            }
        }
        return sb.toString();
    }

    private static Component onOff(boolean v) {
        return Component.translatable(v ? "gui.academy.config.on" : "gui.academy.config.off")
                .withStyle(v ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component keyLabel(Entry entry) {
        Object v = entry.get();
        int code = v instanceof Number n ? n.intValue() : 0;
        return Component.literal(KeyManager.getKeyName(code));
    }

    @Override
    public void onClose() {
        source.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
