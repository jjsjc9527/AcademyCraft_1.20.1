package cn.academy.ability;

import cn.academy.ACConfig;
import cn.academy.Resources;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.SideUtils;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private final List<Skill> skillList = new ArrayList<>();
    private final List<Controllable> ctrlList = new ArrayList<>();

    private final String name;

    int catID = -1;

    protected ResourceLocation icon, overlay, developerIcon;

    private Color colorStyle;

    public Category(String _name) {
        name = _name;
        icon = initIcon();
        overlay = initOverlayIcon();
        developerIcon = initDeveloperIcon();
        if (SideUtils.isClient()) {
            colorStyle = Colors.white();
        }
    }

    public void setColorStyle(int r, int g, int b) {
        setColorStyle(r, g, b, 0);
    }

    public void setColorStyle(int r, int g, int b, int a) {
        if (SideUtils.isClient()) {
            colorStyle.set(r, g, b, a);
        }
    }

    public Color getColorStyle() {
        return colorStyle;
    }

    public void addSkill(Skill skill) {
        if (getSkill(skill.getName()) != null)
            throw new RuntimeException("Duplicating skill " + skill.getName() + "!!");

        skillList.add(skill);
        addControllable(skill);

        skill.addedSkill(this, skillList.size() - 1);
    }

    public int getSkillID(Skill s) {
        return skillList.indexOf(s);
    }

    public int getSkillCount() {
        return skillList.size();
    }

    public Skill getSkill(int id) {
        return (id >= skillList.size() || id < 0) ? null : skillList.get(id);
    }

    public boolean containsSkill(Skill skill) {
        return skill == getSkill(skill.getID());
    }

    public Skill getSkill(String name) {
        for (Skill s : skillList)
            if (s.getName().equals(name))
                return s;
        return null;
    }

    public List<Skill> getSkillList() {
        return ImmutableList.copyOf(skillList);
    }

    public List<Skill> getSkillsOfLevel(int level) {
        List<Skill> ret = new ArrayList<>();
        for (Skill s : skillList)
            if (s.getLevel() == level)
                ret.add(s);
        return ret;
    }

    public int getCategoryID() {
        return catID;
    }

    public void addControllable(Controllable c) {
        ctrlList.add(c);
        c.addedControllable(this, ctrlList.size() - 1);
    }

    public Controllable getControllable(int id) {
        if (id < 0)
            return null;
        if (ctrlList.size() > id)
            return ctrlList.get(id);
        return null;
    }

    public float getProgIncrRate() {
        return ACConfig.getFloat("ac.ability.category." + name + ".common.prog_incr_rate", 1.0f);
    }

    public List<Controllable> getControllableList() {
        return ImmutableList.copyOf(ctrlList);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public ResourceLocation getDeveloperIcon() {
        return developerIcon;
    }

    public ResourceLocation getOverlayIcon() {
        return overlay;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return Component.translatable("ability.academy." + name + ".name").getString();
    }

    protected ResourceLocation initIcon() {
        return Resources.getTexture("abilities/" + name + "/icon");
    }

    protected ResourceLocation initOverlayIcon() {
        return Resources.getTexture("abilities/" + name + "/icon_overlay");
    }

    protected ResourceLocation initDeveloperIcon() {
        return Resources.getTexture("gui/icon_" + name);
    }
}
