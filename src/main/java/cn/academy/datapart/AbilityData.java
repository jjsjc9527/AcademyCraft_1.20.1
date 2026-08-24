package cn.academy.datapart;

import cn.academy.ACConfig;
import cn.academy.ability.AwakenedCategories;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.SkillExpAddedEvent;
import cn.academy.event.ability.SkillExpChangedEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RegDataPart(Player.class)
public class AbilityData extends DataPart<Player> {

    public static AbilityData get(Player player) {
        return EntityData.get(player).getPart(AbilityData.class);
    }

    private static final String MSG_CAT_CHANGE = "cat_change", MSG_SKILL_LEARNED = "skill_learned";

    @SerializeIncluded
    private int catID = -1;

    private String catName = "";

    @SerializeIncluded
    private BitSet learnedSkills;
    @SerializeIncluded
    private float[] skillExps;
    @SerializeIncluded
    private int level;
    @SerializeIncluded
    private float expAddedThisLevel;

    private int updateTicker = 0;

    public AbilityData() {
        learnedSkills = new BitSet(32);
        skillExps = new float[32];

        setTick(true);
        setNBTStorage();
        setClientNeedSync();
    }

    public void setCategory(Category c) {
        checkSide(LogicalSide.SERVER);

        int id = c == null ? -1 : c.getCategoryID();
        if (id != catID) {
            catID = id;

            catName = c == null ? "" : c.getName();

            if (catID != -1 && level == 0) {
                level = 1;
            }
            if (catID == -1 && level != 0) {
                level = 0;
            }

            AwakenedCategories reg = AwakenedCategories.of(getEntity());
            if (reg != null) {
                reg.set(getEntity().getUUID(), c == null ? null : c.getName());
            }

            for (int i = 0; i < skillExps.length; ++i) {
                skillExps[i] = 0.0f;
            }
            learnedSkills.set(0, learnedSkills.size(), false);

            sync();

            informCategoryChange();
            sendMessage(MSG_CAT_CHANGE);
        }
    }

    public Category getCategory() {

        Preconditions.checkState(hasCategory(), "invalid catID: " + catID);
        return CategoryManager.INSTANCE.getCategory(catID);
    }

    public Category getCategoryNullable() {
        return hasCategory() ? getCategory() : null;
    }

    public boolean hasCategory() {
        return catID >= 0 && catID < CategoryManager.INSTANCE.getCategoryCount();
    }

    public static final int MAX_LEVEL = 5;

    public int getLevel() {
        return hasCategory() ? level : 0;
    }

    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    public void setLevel(int lv) {
        checkSide(LogicalSide.SERVER);
        checkLearned();

        if (level != lv) {
            level = lv;
            expAddedThisLevel = 0;
            MinecraftForge.EVENT_BUS.post(new LevelChangeEvent(getEntity()));
            sync();
        }
    }

    public void maxOutLevelProgress() {
        expAddedThisLevel = 100;
        sync();
    }

    public boolean setLevelProgress(float ratio) {
        checkSide(LogicalSide.SERVER);
        checkLearned();
        float threshold = levelProgressThreshold();
        if (threshold <= 0) {
            return false;
        }
        expAddedThisLevel = threshold * Math.max(0f, Math.min(1f, ratio));
        sync();
        return true;
    }

    public List<Skill> getLearnedSkillList() {
        return getSkillListFiltered(this::isSkillLearned);
    }

    public List<Skill> getControllableSkillList() {
        return getSkillListFiltered(s -> (s.canControl() && isSkillLearned(s)));
    }

    public void learnSkill(Skill s) {
        checkSide(LogicalSide.SERVER);
        checkSkill(s);

        setSkillLearnState(s, true);
    }

    public void setSkillLearnState(Skill s, boolean value) {
        checkSide(LogicalSide.SERVER);
        checkSkill(s);

        int id = s.getID();
        boolean prevState = learnedSkills.get(id);

        learnedSkills.set(id, value);

        if (!prevState && value) {
            fireSkillLearn(s);
            sendToLocal(MSG_SKILL_LEARNED, s);
        }

        sync();
    }

    @Listener(channel = MSG_SKILL_LEARNED, side = LogicalSide.CLIENT)
    private void fireSkillLearn(Skill s) {
        MinecraftForge.EVENT_BUS.post(new SkillLearnEvent(getEntity(), s));
    }

    public float getSkillExp(Skill skill) {
        if (!checkSkillSoft(skill)) {
            return 0.0f;
        } else {
            return skill.expCustomized ? skill.getSkillExp(this) : this.skillExps[skill.getID()];
        }
    }

    public void addSkillExp(Skill skill, float amt) {
        if (checkSideSoft(LogicalSide.SERVER)) {
            checkSide(LogicalSide.SERVER);
            if (!checkSkillSoft(skill)) {
                cn.academy.AcademyCraft.LOGGER.warn(
                        "addSkillExp: skill {} does not belong to the current ability category #{} of this player, skipped"
                                + " (common when a skill effect is taken over by another player, e.g. Vector Deviation reflecting a Shift Block)",
                        skill, catID);
                return;
            }

            if (!isSkillLearned(skill)) {
                return;
            }

            int id = skill.getID();
            float cur = skillExps[id];

            float scaled = cn.academy.ability.SkillExpCurve.apply(skill, cur, amt);
            float added = Math.min(1.0f - cur, scaled);
            skillExps[id] += added;

            addLevelProgress(scaled);

            MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
            MinecraftForge.EVENT_BUS.post(new SkillExpAddedEvent(getEntity(), skill, scaled));
            scheduleUpdate(25);
        }
    }

    public float getLevelProgress() {
        float threshold = levelProgressThreshold();
        return threshold == 0 ? 1 : Math.min(1, expAddedThisLevel / threshold);
    }

    private float levelProgressThreshold() {
        return getLevelTotalExp() * (level == 4 ? 1.333f : 0.666f);
    }

    public boolean canLevelUp() {
        return hasCategory() && getLevel() < MAX_LEVEL && getLevelProgress() == 1;
    }

    public void setSkillExp(Skill skill, float exp) {
        checkSide(LogicalSide.SERVER);
        checkSkill(skill);
        if (isSkillLearned(skill)) {
            skillExps[skill.getID()] = exp;
            if (!isClient()) {
                MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
                scheduleUpdate(25);
            }
        }
    }

    public void learnAllSkills() {
        checkSide(LogicalSide.SERVER);

        if (hasCategory()) {
            learnedSkills.set(0, getCategory().getSkillCount(), true);
            sync();
        }
    }

    public boolean isSkillLearned(Skill s) {
        return checkSkillSoft(s) && learnedSkills.get(s.getID());
    }

    private List<Skill> getSkillListFiltered(Predicate<Skill> predicate) {
        if (!hasCategory()) {
            return Collections.emptyList();
        } else {
            return getCategory().getSkillList()
                    .stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        }
    }

    private void scheduleUpdate(int ticks) {
        if (updateTicker == 0)
            updateTicker = ticks;
        else if (updateTicker != 1)
            updateTicker -= 1;
    }

    private void checkSkill(Skill s) {
        Preconditions.checkState(checkSkillSoft(s), "Skill " + s + " not in category #" + catID);
    }

    private float getLevelTotalExp() {
        if (hasCategory()) {
            List<Skill> testSkills = getCategory().getSkillList()
                    .stream()
                    .filter(skill -> skill.canControl() && skill.getLevel() == getLevel())
                    .collect(Collectors.toList());
            return testSkills.size();
        }
        return 0;
    }

    private boolean checkSkillSoft(Skill s) {
        return s.getCategory().getCategoryID() == catID;
    }

    private void addLevelProgress(float consumedExp) {
        float mul0 = getCategory().getProgIncrRate();
        float mul1 = (float) ACConfig.getDouble("ac.ability.data.prog_incr_rate", 1.0);
        expAddedThisLevel += consumedExp * mul0 * mul1;
    }

    private void checkLearned() {
        Preconditions.checkState(hasCategory(), "Player doesn't have category");
    }

    @Listener(channel = MSG_CAT_CHANGE, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
    private void informCategoryChange() {
        MinecraftForge.EVENT_BUS.post(new CategoryChangeEvent(getEntity()));
    }

    @Override
    public void tick() {
        if (!isClient()) {
            if (updateTicker > 0) {
                if (--updateTicker == 0) {
                    sync();
                }
            }
        }
    }

    @Override
    public void toNBT(CompoundTag tag) {

        tag.putString("cat", catName);
        tag.putInt("catID", catID);
        tag.putByteArray("learned", learnedSkills.toByteArray());
        int[] bits = new int[skillExps.length];
        for (int i = 0; i < skillExps.length; i++) {
            bits[i] = Float.floatToIntBits(skillExps[i]);
        }
        tag.putIntArray("exps", bits);
        tag.putInt("level", level);
        tag.putFloat("expThisLevel", expAddedThisLevel);
    }

    @Override
    public void fromNBT(CompoundTag tag) {

        if (tag.contains("cat")) {
            catName = tag.getString("cat");
            Category c = catName.isEmpty()
                    ? null : CategoryManager.INSTANCE.getCategory(catName);
            catID = c == null ? -1 : c.getCategoryID();
        } else {
            catID = tag.getInt("catID");
            Category c = CategoryManager.INSTANCE.getCategory(catID);
            catName = c == null ? "" : c.getName();
        }
        learnedSkills = BitSet.valueOf(tag.getByteArray("learned"));
        int[] bits = tag.getIntArray("exps");
        skillExps = new float[Math.max(32, bits.length)];
        for (int i = 0; i < bits.length; i++) {
            skillExps[i] = Float.intBitsToFloat(bits[i]);
        }
        level = tag.getInt("level");
        expAddedThisLevel = tag.getFloat("expThisLevel");
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeInt(catID);
        buf.writeByteArray(learnedSkills.toByteArray());
        buf.writeVarInt(skillExps.length);
        for (float f : skillExps) {
            buf.writeFloat(f);
        }
        buf.writeInt(level);
        buf.writeFloat(expAddedThisLevel);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        catID = buf.readInt();

        Category syncedCat = CategoryManager.INSTANCE.getCategory(catID);
        catName = syncedCat == null ? "" : syncedCat.getName();
        learnedSkills = BitSet.valueOf(buf.readByteArray());
        int n = buf.readVarInt();
        skillExps = new float[n];
        for (int i = 0; i < n; i++) {
            skillExps[i] = buf.readFloat();
        }
        level = buf.readInt();
        expAddedThisLevel = buf.readFloat();
    }
}
