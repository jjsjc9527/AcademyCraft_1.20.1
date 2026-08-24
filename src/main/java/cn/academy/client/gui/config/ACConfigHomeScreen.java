package cn.academy.client.gui.config;

import cn.academy.config.ServerConfigGate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ACConfigHomeScreen extends Screen {

    private final Screen parent;
    private List<ACConfigSources.Source> sources;

    public ACConfigHomeScreen(Screen parent) {
        super(Component.translatable("gui.academy.config.title"));
        this.parent = parent;

        ACConfigSources.beginSession();
    }

    @Override
    protected void init() {

        sources = ACConfigSources.all();

        int w = Math.min(this.width - 40, 300);
        int x = (this.width - w) / 2;
        int y = Math.max(60, this.height / 4 + 8);

        for (ACConfigSources.Source s : sources) {
            int leaves = s.root.countLeaves();
            Component label = Component.empty().append(s.title)
                    .append(Component.literal("   " + leaves).withStyle(ChatFormatting.GRAY));
            Button b = Button.builder(label,
                    btn -> this.minecraft.setScreen(new ACConfigScreen(this, s)))
                    .bounds(x, y, w, 20).build();

            b.active = leaves > 0;
            addRenderableWidget(b);
            y += 28;
        }

        int doneY = Math.min(this.height - 30, y + 16);
        if (showPushButton()) {

            int pushY = Math.min(this.height - 54, y + 16);
            addRenderableWidget(buildPushButton(x, pushY, w));
            doneY = pushY + 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.academy.config.done"),
                        b -> onClose())
                .bounds(x, doneY, w, 20).build());
    }

    private boolean showPushButton() {
        return this.minecraft != null
                && this.minecraft.player != null
                && !this.minecraft.hasSingleplayerServer();
    }

    private boolean isOperator() {
        return this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.player.hasPermissions(cn.academy.command.ACCommands.PERM_LEVEL);
    }

    private Button buildPushButton(int x, int y, int w) {
        boolean op = isOperator();

        int n = ACConfigSources.pendingChanges().size();

        Button b = Button.builder(
                        Component.translatable("gui.academy.config.push", n),
                        btn -> {
                            ACConfigSources.pushToServer();
                            rebuildWidgets();
                        })
                .bounds(x, y, w, 20).build();

        b.active = op && n > 0;
        b.setTooltip(Tooltip.create(Component.translatable(
                op ? "gui.academy.config.push.tip" : "gui.academy.config.push.noperm")));
        return b;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        renderBackground(gg);
        gg.drawCenteredString(font, this.title, this.width / 2, 24, 0xFFFFFF);
        gg.drawCenteredString(font,
                Component.translatable("gui.academy.config.hint").withStyle(ChatFormatting.GRAY),
                this.width / 2, 38, 0xA0A0A0);

        Component status = pushStatus();
        if (status != null) {
            gg.drawCenteredString(font, status, this.width / 2, 50, 0xFFFFFF);
        }

        super.render(gg, mouseX, mouseY, partial);
    }

    private Component pushStatus() {
        if (ACConfigSources.pushPending()) {
            return Component.translatable("gui.academy.config.push.sending")
                    .withStyle(ChatFormatting.GRAY);
        }

        ACConfigSources.PushResult r = ACConfigSources.lastPushResult();
        if (r == null) {
            return null;
        }
        if (r.reason() == ServerConfigGate.DENIED) {
            return Component.translatable("gui.academy.config.push.denied")
                    .withStyle(ChatFormatting.RED);
        }

        if (r.reason() == ServerConfigGate.SAVE_FAILED) {
            return Component.translatable("gui.academy.config.push.savefail", r.applied())
                    .withStyle(ChatFormatting.RED);
        }
        if (r.rejected() == 0) {
            return Component.translatable("gui.academy.config.push.ok", r.applied())
                    .withStyle(ChatFormatting.GREEN);
        }
        Component why = Component.translatable(r.reason() == ServerConfigGate.BAD_VALUE
                ? "gui.academy.config.push.reason.badvalue"
                : "gui.academy.config.push.reason.notallowed");
        return Component.translatable("gui.academy.config.push.partial",
                        r.applied(), r.rejected(), r.badPath(), why)
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
