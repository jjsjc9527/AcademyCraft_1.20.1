package cn.academy.tutorial;

import cn.academy.Resources;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ACTutorial {

    public enum Tag {
        CRAFT, SMELT, VIEW;

        public final ResourceLocation icon = Resources.getTexture(
                "gui/icons/icon_" + this.name().toLowerCase(Locale.ROOT));
    }

    public static final boolean SHOW_ALL = false;

    public final String id;

    private Condition condition = Conditions.alwaysTrue();
    private boolean defaultInstalled = true;

    private final List<ViewGroup> previewHandlers = new ArrayList<>();

    public ACTutorial(String id) {
        this.id = id;
    }

    public ACTutorial addCondition(Condition condition) {
        defaultInstalled = false;
        if (this.condition == Conditions.alwaysTrue()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.or(condition);
        }
        return this;
    }

    public ACTutorial addPreview(ViewGroup... handlers) {
        for (ViewGroup h : handlers) previewHandlers.add(h);
        return this;
    }

    public List<ViewGroup> getPreview() {
        return previewHandlers;
    }

    @OnlyIn(Dist.CLIENT)
    public String getContent() {
        final String unknown = "![title]\nUNKNOWN \n![brief]\n![content]\n ";
        try {
            String lang = Minecraft.getInstance().getLanguageManager().getSelected();
            String s = readMd(lang);
            if (s == null) s = readMd("en_us");
            return s == null ? unknown : s;
        } catch (Exception e) {
            return unknown;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private String readMd(String lang) {
        try {
            Optional<Resource> opt = Minecraft.getInstance().getResourceManager().getResource(location(lang));
            if (opt.isEmpty()) return null;
            try (InputStream in = opt.get().open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public String getTitle() {
        String raw = getContent();
        int i1 = raw.indexOf("![title]"),
                i2 = raw.indexOf("![brief]");
        return raw.substring(i1 + 8, i2).trim();
    }

    private ResourceLocation location(String lang) {
        return new ResourceLocation("academy", "tutorials/" + lang + "/" + id + ".md");
    }

    public boolean isActivated(Player player) {
        if (SHOW_ALL)
            return true;
        return this.condition.test(player);
    }

    public boolean isDefaultInstalled() {
        return defaultInstalled;
    }

}
