package cn.academy.ability;

import cn.academy.ACConfig;
import cn.academy.Resources;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.IStateProvider;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.condition.DevConditionAdvancedTree;
import cn.academy.ability.develop.condition.DevConditionDep;
import cn.academy.ability.develop.condition.DevConditionDeveloperType;
import cn.academy.ability.develop.condition.DevConditionLevel;
import cn.academy.ability.develop.condition.IDevCondition;
import cn.academy.datapart.AbilityData;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Skill extends Controllable {

    private Category category;

    private final List<IDevCondition> learningConditions = new ArrayList<>();

    private String fullName;

    private Skill parent;

    private final List<Skill> extraTreeParents = new ArrayList<>();

    private int id;

    private final String name;
    private ResourceLocation icon;

    private final int level;

    public float guiX, guiY;

    public boolean expCustomized = false;

    protected boolean canControl = true;

    protected SkillTab tab = SkillTab.NORMAL;

    public SkillTab getTab() {
        return tab;
    }

    public boolean isGeneric() {
        return tab == SkillTab.GENERIC;
    }

    public Skill(String _name, int atLevel) {
        name = _name;
        level = atLevel;
        fullName = "<unassigned>." + name;

        addDevCondition(new DevConditionLevel());
    }

    public boolean isNeverLearnable() {
        for (IDevCondition c : learningConditions) {
            if (c instanceof cn.academy.ability.develop.condition.DevConditionNever) {
                return true;
            }
        }
        return false;
    }

    final void addedSkill(Category _category, int id) {
        category = _category;
        this.id = id;

        icon = initIcon();
        fullName = initFullName();

        if (tab == SkillTab.ADVANCED) {

            addDevCondition(new DevConditionAdvancedTree());
        } else {
            addDevCondition(new DevConditionDeveloperType(getMinimumDeveloperType()));
        }

        initSkill();
    }

    public void setPosition(float x, float y) {
        guiX = x;
        guiY = y;
    }

    protected void initSkill() {}

    public int getID() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        return getLocalized("name");
    }

    public String getDescription() {
        return getLocalized("desc");
    }

    public float getDamageScale() {
        return ACConfig.getFloat("skills." + fullName + ".damage_scale", 1.0f);
    }

    public boolean isEnabled() {
        return ACConfig.getBoolean("skills." + fullName + ".enabled", true);
    }

    public boolean shouldDestroyBlocks() {
        return ACConfig.getBoolean("skills." + fullName + ".destroy_blocks", true);
    }

    public float getCPConsumeSpeed() {
        return ACConfig.getFloat("skills." + fullName + ".cp_consume_speed", 1.0f);
    }

    public float getOverloadConsumeSpeed() {
        return ACConfig.getFloat("skills." + fullName + ".overload_consume_speed", 1.0f);
    }

    public float getExpIncrSpeed() {
        return ACConfig.getFloat("skills." + fullName + ".exp_incr_speed", 1.0f);
    }

    public boolean canControl() {
        return isEnabled() && canControl;
    }

    public boolean isPassive() {
        return !canControl;
    }

    @Override
    public ResourceLocation getHintIcon() {
        return icon;
    }

    @Override
    public String getHintText() {
        return getDisplayName();
    }

    public boolean hasAura() {
        return false;
    }

    protected String getLocalized(String key) {
        return Component.translatable("ability.academy." + getFullName() + "." + key).getString();
    }

    protected String getCategoryLocation() {
        return (isGeneric() ? "generic" : category.getName());
    }

    protected String initFullName() {
        return getCategoryLocation() + "." + name;
    }

    protected ResourceLocation initIcon() {
        return icon = Resources.getTexture("abilities/" + getCategoryLocation() + "/skills/" + name);
    }

    public float getSkillExp(AbilityData data) {
        return 0.0f;
    }

    public void setParent(Skill skill) {
        setParent(skill, 0.0f);
    }

    public void setParent(Skill skill, float requiredExp) {
        if (parent != null)
            throw new IllegalStateException("You can't set the parent twice!");
        if (skill.isEnabled()) {
            parent = skill;

            addDevCondition(new DevConditionDep(parent, requiredExp));
        }
    }

    public Skill getParent() {
        return parent;
    }

    public void addTreeParent(Skill skill, float requiredExp) {
        if (skill.isEnabled()) {
            extraTreeParents.add(skill);
            addDevCondition(new DevConditionDep(skill, requiredExp));
        }
    }

    public List<Skill> getTreeParents() {
        if (parent == null) {
            return ImmutableList.copyOf(extraTreeParents);
        }
        List<Skill> out = new ArrayList<>(extraTreeParents.size() + 1);
        out.add(parent);
        out.addAll(extraTreeParents);
        return out;
    }

    public boolean isRoot() {
        return parent == null && extraTreeParents.isEmpty();
    }

    public void addDevCondition(IDevCondition cond) {
        learningConditions.add(cond);
    }

    public void addSkillDep(Skill skill, float exp) {
        if (skill.isEnabled()) {
            addDevCondition(new DevConditionDep(skill, exp));
        }
    }

    public List<IDevCondition> getDevConditions() {
        return ImmutableList.copyOf(learningConditions);
    }

    public int getLearningStims() {
        return (int) (3 + level * level * 0.5f);
    }

    public DeveloperType getMinimumDeveloperType() {
        if (level <= 2) return DeveloperType.PORTABLE;
        if (level <= 3) return DeveloperType.NORMAL;
        else return DeveloperType.ADVANCED;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @OnlyIn(Dist.CLIENT)
    protected void activateSingleKey2(ClientRuntime rt, int keyID, Function<Player, Context> contextSupplier) {
        rt.addKey(keyID, new SingleKeyDelegate(contextSupplier));
    }

    @OnlyIn(Dist.CLIENT)
    public class SingleKeyDelegate extends KeyDelegate {
        private final Function<Player, Context> contextSupplier;
        Context context;

        public SingleKeyDelegate(Function<Player, Context> contextSupplier) {
            this.contextSupplier = contextSupplier;
        }

        @Override
        public void onKeyDown() {
            context = contextSupplier.apply(getPlayer());
            ContextManager.instance.activate(context);

            context.sendToSelf(Context.MSG_KEYDOWN);
        }

        @Override
        public void onKeyTick() {
            checkContext();

            if (context != null) {
                context.sendToSelf(Context.MSG_KEYTICK);
            }
        }

        @Override
        public void onKeyUp() {
            checkContext();

            if (context != null) {
                context.sendToSelf(Context.MSG_KEYUP);
            }

            context = null;
        }

        @Override
        public void onKeyAbort() {
            checkContext();

            if (context != null) {
                context.sendToSelf(Context.MSG_KEYABORT);
            }

            context = null;
        }

        private void checkContext() {
            if (context != null && context.getStatus() == Context.Status.TERMINATED) {
                context = null;
            }
        }

        @Override
        public DelegateState getState() {
            if (context == null) {
                return DelegateState.IDLE;
            } else if (context instanceof IStateProvider) {
                return ((IStateProvider) context).getState();
            } else {
                return DelegateState.ACTIVE;
            }
        }

        @Override
        public ResourceLocation getIcon() {
            return Skill.this.getHintIcon();
        }

        @Override
        public int createID() {
            return 0;
        }

        @Override
        public Skill getSkill() {
            return Skill.this;
        }
    }
}
