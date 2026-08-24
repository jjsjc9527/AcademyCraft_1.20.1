package cn.academy.datapart;

import cn.academy.config.AbilityConfig;
import cn.academy.event.ability.*;
import cn.academy.event.ability.CalcEvent.CPRecoverSpeed;
import cn.academy.event.ability.CalcEvent.OverloadRecoverSpeed;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.MathUtils;
import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@RegDataPart(Player.class)
public class CPData extends DataPart<Player> {

    private AbilityData abilityData;

    private static final String
        MSG_POST_EVENT = "post_event",
        MSG_ACTIVATE_SVR = "actv_svr";

    public interface IInterfSource {

        boolean interfering();
    }

    private final Map<String, IInterfSource> interfSources = new HashMap<>();

    private boolean activated = false;

    private float curCP;

    private float maxCP = 100.0f;
    private float addMaxCP = 0.0f;

    private float extraMaxCpRate = 0.0f;

    private float maxCpMultiplier = 1.0f;

    private boolean lifeGuarded = false;

    private int consumeSeq = 0;

    public int getConsumeSeq() {
        return consumeSeq;
    }

    private boolean lifeTakenOver = false;

    private float curOverload;

    private float maxOverload = 100.0f;
    private float addMaxOverload = 0.0f;

    private boolean overloadFine = true;
    private boolean interfering = false;

    private int untilRecover;

    private int untilOverloadRecover;

    private boolean dataDirty = false;

    private int tickSync;

    public CPData() {
        setTick(true);
        setClientNeedSync();
        setNBTStorage();
    }

    public static CPData get(Player player) {
        return EntityData.get(player).getPart(CPData.class);
    }

    @Override
    public void wake() {
        abilityData = AbilityData.get(getEntity());
    }

    @Override
    public void tick() {
        AbilityData aData = AbilityData.get(getEntity());

        boolean remote = isClient();

        if (aData.hasCategory()) {
            if (untilRecover == 0) {
                float recover = getCPRecoverSpeed();
                curCP += recover;
                if (curCP > getMaxCP())
                    curCP = getMaxCP();
            } else {
                untilRecover--;
            }

            if (untilOverloadRecover == 0) {
                float recover = getOverloadRecoverSpeed();

                curOverload -= recover;
                if (curOverload <= 0) {
                    overloadFine = true;
                    curOverload = 0;
                }
            } else {
                untilOverloadRecover--;
            }

            if (!remote) {
                Iterator<Entry<String, IInterfSource>> iter = interfSources.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<String, IInterfSource> entry = iter.next();
                    if (!entry.getValue().interfering()) {
                        iter.remove();
                    }
                }

                boolean newInterf = !interfSources.isEmpty();
                if (newInterf != interfering) {
                    dataDirty = true;
                }

                interfering = newInterf;
            }

            if (!remote) {
                int interval = (activated ? 1 : 3) * (dataDirty ? 4 : 10);

                ++tickSync;
                if (tickSync >= interval) {
                    dataDirty = false;
                    tickSync = 0;
                    sync();
                }
            }
        }
    }

    public boolean isActivated() {

        return abilityData.hasCategory() && activated;
    }

    public boolean canUseAbility() {
        return activated && overloadFine && !interfering;
    }

    @Deprecated
    public void setActivateState(boolean state) {
        setActivateState(state, AbilityToggleSource.UNKNOWN);
    }

    public void setActivateState(boolean state, AbilityToggleSource source) {

        if (!state && !source.authorized()) {
            refuseDeactivate();
            return;
        }

        if (!state) {
            try {
                if (cn.academy.util.ACDiag.ON)
                org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/CPData").warn(
                        "[ability-off] {} side | ability of {} was switched off | source={}\nstack:\n{}",
                        isClient() ? "client" : "server",
                        getEntity().getName().getString(), source,
                        StackWalker.getInstance().walk(s -> s.limit(20)
                                .map(f -> "    " + f.getClassName() + "." + f.getMethodName())
                                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b)));
            } catch (Throwable ignored) {

            }
        }

        if (isClient()) {
            activated = state;
            NetworkMessage.sendToServer(this, MSG_ACTIVATE_SVR, state, source.ordinal());
        } else {
            Preconditions.checkState(!state || AbilityData.get(getEntity()).hasCategory(),
                    "Trying to activate ability when player doesn't have one");

            if (activated != state) {

                activated = state;
                postEvent(activated);
                sendToLocal(MSG_POST_EVENT, activated);
            }

            markDirty();
        }
    }

    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/CPData");

    private static final Map<String, Long> REFUSE_LOG_AT = new HashMap<>();

    private static final long REFUSE_LOG_INTERVAL_MS = 10_000L;

    private static final int REFUSE_LOG_MAX = 64;

    private void refuseDeactivate() {
        String caller = academy$callerOutsideCPData();
        String who = getEntity() == null ? "?" : getEntity().getGameProfile().getName();
        String key = who + "|" + caller;

        long now = System.currentTimeMillis();
        Long last = REFUSE_LOG_AT.get(key);
        if (last != null && now - last < REFUSE_LOG_INTERVAL_MS) {
            return;
        }
        if (REFUSE_LOG_AT.size() > REFUSE_LOG_MAX) {
            REFUSE_LOG_AT.clear();
        }
        REFUSE_LOG_AT.put(key, now);

        LOG.warn("blocked an unauthorized ability shutdown: player={} caller={} -- "
                        + "if this is our own code, use setActivateState(state, AbilityToggleSource.XXX) to declare the source.",
                who, caller);
    }

    private static String academy$callerOutsideCPData() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        String self = CPData.class.getName();
        for (StackTraceElement e : st) {
            String cls = e.getClassName();
            if (cls.equals(self) || cls.equals(Thread.class.getName())) {
                continue;
            }
            return cls + "." + e.getMethodName() + ":" + e.getLineNumber();
        }
        return "<unknown>";
    }

    public void setCP(float cp) {
        curCP = MathUtils.clampf(0, getMaxCP(), cp);

        markDirty();
    }

    public void setOverload(float newOverload) {
        curOverload = MathUtils.clampf(0, getMaxOverload(), newOverload);

        markDirty();
    }

    private void markDirty() {
        if (!isClient()) {
            dataDirty = true;
        }
    }

    public float getCP() {
        return curCP;
    }

    public float getMaxCP() {
        return (maxCP + addMaxCP) * (1.0f + extraMaxCpRate) * maxCpMultiplier;
    }

    public float getRawMaxCP() {
        return maxCP;
    }

    public boolean isLifeTakenOver() {
        return lifeTakenOver;
    }

    public void setLifeTakenOver(boolean value) {
        if (value == lifeTakenOver) {
            return;
        }
        lifeTakenOver = value;
        markDirty();
    }

    public boolean isLifeGuarded() {
        return lifeGuarded;
    }

    public void setLifeGuarded(boolean value) {
        if (value == lifeGuarded) {
            return;
        }
        lifeGuarded = value;
        markDirty();
    }

    public float getExtraMaxCpRate() {
        return extraMaxCpRate;
    }

    public void setExtraMaxCpRate(float rate) {
        float v = Math.max(0.0f, rate);
        if (v == extraMaxCpRate) {
            return;
        }
        extraMaxCpRate = v;
        markDirty();
    }

    public float getMaxCpMultiplier() {
        return maxCpMultiplier;
    }

    public void setMaxCpMultiplier(float value) {
        float v = Math.max(1.0f, value);
        if (v == maxCpMultiplier) {
            return;
        }
        maxCpMultiplier = v;
        markDirty();
    }

    public float getAddMaxCP() {
        return addMaxCP;
    }

    public void setAddMaxCP(float value) {
        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddCP(aData.getLevel());
        addMaxCP = Math.min(max, value);

        markDirty();
    }

    public float getOverload() {
        return curOverload;
    }

    public float getMaxOverload() {
        return maxOverload + addMaxOverload;
    }

    public float getRawMaxOverload() {
        return maxOverload;
    }

    public float getAddMaxOverload() {
        return addMaxOverload;
    }

    public boolean perform(float overloadToAdd, float cpToAdd) {
        return performInternal(overloadToAdd, cpToAdd, false);
    }

    public void performWithForce(float overload, float cp) {
        performInternal(overload, cp, true);
    }

    private boolean performInternal(float overload, float cp, boolean force) {
        Pair<Float, Float> res = performData(overload, cp);
        overload = res.getLeft();
        cp = res.getRight();

        boolean result;

        if (!getEntity().getAbilities().instabuild) {

            // A normal skill use must also start the configured recovery cooldown.
            // The 1.12.2 implementation always did this; without it CP starts
            // regenerating on the very next tick and high-level players appear
            // to spend no CP at all.
            result = consumeCP(cp, force, true);
            if (result) {
                addOverload(overload);
            }
        } else {
            result = true;
        }

        if (result) {
            addMaxCP(cp);
            addMaxOverload(overload);
        }

        return result;
    }

    private Pair<Float, Float> performData(float overload, float cp) {
        CalcEvent.SkillPerform evt = new CalcEvent.SkillPerform(getEntity(), overload, cp);
        MinecraftForge.EVENT_BUS.post(evt);

        return Pair.of(evt.overload, evt.cp);
    }

    public boolean canPerform(float cp) {
        return getEntity().getAbilities().instabuild || this.getCP() >= cp;
    }

    public boolean isOverloadRecovering() {
        return !overloadFine;
    }

    private void addMaxCP(float consumedCP) {
        setAddMaxCP(addMaxCP + consumedCP * AbilityConfig.maxCpIncrRate());
    }

    private void addMaxOverload(float overload) {
        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddOverload(aData.getLevel());
        float add = MathUtils.clampf(0, 10, overload * AbilityConfig.maxOverloadIncrRate());
        addMaxOverload += add;
        if (addMaxOverload > max)
            addMaxOverload = max;
    }

    private float getCPRecoverSpeed() {
        float cap = getMaxCP();
        float ratio = cap > 0.0f ? curCP / cap : 0.0f;
        float raw = AbilityConfig.cpRecoverSpeed() *
                0.0003f * cap *
                MathUtils.lerpf(1, 2, ratio);

        return CalcEvent.calc(new CPRecoverSpeed(getEntity(), 1)) * raw;
    }

    private float getOverloadRecoverSpeed() {
        float raw = AbilityConfig.overloadRecoverSpeed() *
                Math.max(0.002f * maxOverload,
                        0.007f * maxOverload * MathUtils.lerpf(1, 0.5f, curOverload / maxOverload / 2));

        return CalcEvent.calc(new OverloadRecoverSpeed(getEntity(), 1)) * raw;
    }

    private boolean consumeCP(float amt, boolean force, boolean holdRecover) {
        if (!force && curCP < amt)
            return false;

        curCP = Math.max(0, curCP - amt);

        if (amt > 0.0f) {
            consumeSeq++;
        }
        if (holdRecover) {
            untilRecover = AbilityConfig.cpRecoverCooldown();
        }

        if (!isClient())
            dataDirty = true;

        return true;
    }

    public void drainCP(float amt) {
        consumeCP(amt, true, true);
    }

    private void addOverload(float amt) {
        if (getEntity().getAbilities().instabuild)
            return;

        curOverload = Math.min(getMaxOverload(), curOverload + amt);

        untilOverloadRecover = AbilityConfig.overloadRecoverCooldown();

        if (curOverload == getMaxOverload()) {
            MinecraftForge.EVENT_BUS.post(new OverloadEvent(getEntity()));
            overloadFine = false;
        }

        if (!isClient())
            dataDirty = true;
    }

    public boolean isOverloaded() {
        return !overloadFine && untilOverloadRecover > 0;
    }

    public void recalcMaxValue() {
        AbilityData data = AbilityData.get(getEntity());

        this.maxCP = getInitCP(data.getLevel());
        this.maxOverload = getInitOverload(data.getLevel());

        curCP = getMaxCP();
        curOverload = 0;

        if (!isClient())
            sync();
    }

    public void refreshMaxFromConfig() {
        if (isClient()) {
            return;
        }
        AbilityData data = AbilityData.get(getEntity());
        float newMaxCP = getInitCP(data.getLevel());
        float newMaxOverload = getInitOverload(data.getLevel());
        if (newMaxCP == maxCP && newMaxOverload == maxOverload) {
            return;
        }
        maxCP = newMaxCP;
        maxOverload = newMaxOverload;

        curCP = MathUtils.clampf(0, getMaxCP(), curCP);
        curOverload = MathUtils.clampf(0, getMaxOverload(), curOverload);
        sync();
    }

    public boolean isInterfering() {
        return interfering;
    }

    public boolean hasInterfSource(String name) {
        return interfSources.containsKey(name);
    }

    public void addInterf(String id, IInterfSource interferer) {
        checkSide(LogicalSide.SERVER);

        interfSources.put(id, interferer);
    }

    public void removeInterf() {
        checkSide(LogicalSide.SERVER);

        interfSources.clear();
    }

    public void removeInterf(String name) {
        checkSide(LogicalSide.SERVER);

        interfSources.remove(name);
    }

    public float getInitCP(int level) {
        return CalcEvent.calc(new CalcEvent.MaxCP(getEntity(), AbilityConfig.initCp(level)));
    }

    public float getInitOverload(int level) {
        return CalcEvent.calc(new CalcEvent.MaxOverload(getEntity(), AbilityConfig.initOverload(level)));
    }

    public float getMaxAddCP(int level) {
        return AbilityConfig.addCp(level);
    }

    public float getMaxAddOverload(int level) {
        return AbilityConfig.addOverload(level);
    }

    public void recoverAll() {
        if (!isClient()) {
            curCP = getMaxCP();
            curOverload = 0;
            overloadFine = false;
            sync();
        }
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.putBoolean("activated", activated);
        tag.putFloat("curCP", curCP);
        tag.putFloat("maxCP", maxCP);
        tag.putFloat("addMaxCP", addMaxCP);

        tag.putFloat("maxCpMultiplier", maxCpMultiplier);
        tag.putFloat("curOverload", curOverload);
        tag.putFloat("maxOverload", maxOverload);
        tag.putFloat("addMaxOverload", addMaxOverload);
        tag.putBoolean("overloadFine", overloadFine);
        tag.putBoolean("interfering", interfering);
        tag.putInt("untilRecover", untilRecover);
        tag.putInt("untilOverloadRecover", untilOverloadRecover);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        activated = tag.getBoolean("activated");
        curCP = tag.getFloat("curCP");
        maxCP = tag.getFloat("maxCP");
        addMaxCP = tag.getFloat("addMaxCP");

        maxCpMultiplier = tag.contains("maxCpMultiplier")
                ? Math.max(1.0f, tag.getFloat("maxCpMultiplier"))
                : 1.0f;
        curOverload = tag.getFloat("curOverload");
        maxOverload = tag.getFloat("maxOverload");
        addMaxOverload = tag.getFloat("addMaxOverload");
        overloadFine = tag.getBoolean("overloadFine");
        interfering = tag.getBoolean("interfering");
        untilRecover = tag.getInt("untilRecover");
        untilOverloadRecover = tag.getInt("untilOverloadRecover");
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeBoolean(activated);
        buf.writeFloat(curCP);
        buf.writeFloat(maxCP);
        buf.writeFloat(addMaxCP);
        buf.writeFloat(curOverload);
        buf.writeFloat(maxOverload);
        buf.writeFloat(addMaxOverload);
        buf.writeBoolean(overloadFine);
        buf.writeBoolean(interfering);
        buf.writeVarInt(untilRecover);
        buf.writeVarInt(untilOverloadRecover);

        buf.writeFloat(extraMaxCpRate);
        buf.writeBoolean(lifeGuarded);
        buf.writeBoolean(lifeTakenOver);
        buf.writeFloat(maxCpMultiplier);
        buf.writeVarInt(consumeSeq);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        activated = buf.readBoolean();
        curCP = buf.readFloat();
        maxCP = buf.readFloat();
        addMaxCP = buf.readFloat();
        curOverload = buf.readFloat();
        maxOverload = buf.readFloat();
        addMaxOverload = buf.readFloat();
        overloadFine = buf.readBoolean();
        interfering = buf.readBoolean();
        untilRecover = buf.readVarInt();
        untilOverloadRecover = buf.readVarInt();
        extraMaxCpRate = buf.readFloat();
        lifeGuarded = buf.readBoolean();
        lifeTakenOver = buf.readBoolean();
        maxCpMultiplier = buf.readFloat();
        consumeSeq = buf.readVarInt();
    }

    @Listener(channel = MSG_ACTIVATE_SVR, side = LogicalSide.SERVER)
    private void activateAtServer(boolean state, Integer sourceOrdinal) {
        setActivateState(state, sourceOrdinal == null
                ? AbilityToggleSource.UNKNOWN
                : AbilityToggleSource.fromOrdinal(sourceOrdinal));
    }

    @Listener(channel = MSG_POST_EVENT, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
    private void postEvent(boolean state) {
        MinecraftForge.EVENT_BUS.post(state ?
                new AbilityActivateEvent(getEntity()) :
                new AbilityDeactivateEvent(getEntity()));
    }

    public static class Events {

        @SubscribeEvent
        public void changedCategory(CategoryChangeEvent event) {
            CPData cpData = CPData.get(event.player);

            if (!AbilityData.get(event.player).hasCategory()) {

                cpData.setActivateState(false, AbilityToggleSource.SYSTEM);
            }
            cpData.recalcMaxValue();
        }

        @SubscribeEvent
        public void learnedSkill(SkillLearnEvent event) {
            CPData.get(event.player).recalcMaxValue();
        }

        @SubscribeEvent
        public void changedLevel(LevelChangeEvent event) {
            CPData cpData = CPData.get(event.player);
            cpData.addMaxCP = cpData.addMaxOverload = 0;
            cpData.recalcMaxValue();
        }

        @SubscribeEvent
        public void playerWakeup(PlayerWakeUpEvent event) {

            if (!event.wakeImmediately()) {
                CPData.get(event.getEntity()).recoverAll();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void playerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof Player player) {
                CPData cpData = CPData.get(player);

                cpData.recoverAll();

                cpData.setActivateState(false, AbilityToggleSource.SYSTEM);
            }
        }
    }
}
