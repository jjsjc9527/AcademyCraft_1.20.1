package cn.academy.datapart;

import cn.academy.ability.vanilla.mentalout.advanced.CognitionTamper;
import cn.academy.ability.vanilla.mentalout.advanced.MentalMastery;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.config.AbilityConfig;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

@RegDataPart(Player.class)
public class RemoteData extends DataPart<Player> {

    public static final int MAX_PROGRAMS = 6;

    public static final int MAX_SLOTS = 6;

    public static final int MIN_RANGE = 2, MIN_COUNT = 1;

    public static final int MAX_ALLIES = 4096;

    private static final String
            MSG_SET_SLOT = "rc_slot",
            MSG_SET_NUM = "rc_num",
            MSG_SWITCH = "rc_switch",
            MSG_SET_SYNC = "rc_sync";

    public static final class Book {

        private static final String NBT = "ac_remote";

        private int current = 0;
        private final Program[] programs = new Program[MAX_PROGRAMS];

        private Book() {
            for (int i = 0; i < MAX_PROGRAMS; ++i) {
                programs[i] = new Program();
            }
        }

        public static Book of(net.minecraft.world.item.ItemStack stack) {
            Book b = new Book();
            CompoundTag root = stack == null ? null : stack.getTagElement(NBT);
            if (root != null) {
                b.fromNBT(root);
            }
            return b;
        }

        public void save(net.minecraft.world.item.ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            CompoundTag root = new CompoundTag();
            toNBT(root);
            stack.getOrCreateTag().put(NBT, root);
        }

        public int getCurrentID() {
            return current;
        }

        public Program getProgram(int id) {
            return programs[id < 0 || id >= MAX_PROGRAMS ? 0 : id];
        }

        public Program getCurrent() {
            return getProgram(current);
        }

        void switchTo(int pid) {
            if (pid >= 0 && pid < MAX_PROGRAMS) {
                current = pid;
            }
        }

        void applySync(int pid, boolean on) {
            if (pid >= 0 && pid < MAX_PROGRAMS) {
                programs[pid].syncMind = on;
            }
        }

        void toNBT(CompoundTag tag) {
            tag.putByte("cur", (byte) current);
            for (int i = 0; i < MAX_PROGRAMS; ++i) {
                CompoundTag t = new CompoundTag();
                t.putIntArray("c", programs[i].catIDs.clone());
                t.putIntArray("s", programs[i].skillIDs.clone());
                t.putByteArray("m", programs[i].cmds.clone());
                t.putInt("r", programs[i].range);
                t.putInt("n", programs[i].count);
                t.putBoolean("y", programs[i].syncMind);
                tag.put("p" + i, t);
            }
        }

        void fromNBT(CompoundTag tag) {
            current = tag.getByte("cur");
            if (current < 0 || current >= MAX_PROGRAMS) current = 0;
            for (int i = 0; i < MAX_PROGRAMS; ++i) {
                Program p = new Program();
                if (tag.contains("p" + i)) {
                    CompoundTag t = tag.getCompound("p" + i);
                    copyInto(p.catIDs, t.getIntArray("c"));
                    copyInto(p.skillIDs, t.getIntArray("s"));
                    byte[] m = t.getByteArray("m");
                    for (int j = 0; j < MAX_SLOTS && j < m.length; ++j) p.cmds[j] = m[j];
                    p.range = Math.max(MIN_RANGE, Math.min(HARD_MAX_RANGE, t.getInt("r")));
                    p.count = Math.max(MIN_COUNT, Math.min(HARD_MAX_COUNT, t.getInt("n")));
                    p.syncMind = t.getBoolean("y");
                }
                programs[i] = p;
            }
        }
    }

    private final java.util.List<Ally> allies = new java.util.ArrayList<>();

    private final java.util.Set<java.util.UUID> enraged = new java.util.LinkedHashSet<>();

    private static final int HARD_MAX_ENRAGED = 512;

    private final java.util.Set<java.util.UUID> jammed = new java.util.LinkedHashSet<>();

    private static final int JAM_SCAN_INTERVAL = 20;

    private int jamScanTimer;

    public RemoteData() {
        setNBTStorage();
        setClientNeedSync();

        setTick(true);
    }

    public Book book() {
        return Book.of(getEntity() == null ? null : getEntity().getMainHandItem());
    }

    public static int maxRange(float wideCastExp) {
        return Math.max(MIN_RANGE, (int) AbilityConfig.stat("wide_cast", "range_cap", wideCastExp));
    }

    public static int maxCount(float wideCastExp) {
        return Math.max(MIN_COUNT, (int) AbilityConfig.stat("wide_cast", "count_cap", wideCastExp));
    }

    public void clear() {
        checkSide(LogicalSide.SERVER);
        enraged.clear();
        jammed.clear();
        if (!allies.isEmpty()) {
            allies.clear();
            sync();
        }
    }

    @Override
    public void tick() {
        if (isClient()) {
            return;
        }
        if (++jamScanTimer < JAM_SCAN_INTERVAL) {
            return;
        }
        jamScanTimer = 0;
        scanJammed();
    }

    private void scanJammed() {
        Player self = getEntity();
        if (allies.isEmpty() || self == null || self.getServer() == null) {
            if (!jammed.isEmpty()) {
                jammed.clear();
                sync();
            }
            return;
        }
        java.util.Set<java.util.UUID> now = null;
        for (Ally a : allies) {
            if (!PLAYER_TYPE.equals(a.type)) {
                continue;
            }
            Player p = self.getServer().getPlayerList().getPlayer(a.id);
            if (p == null || !isDeviating(p)) {
                continue;
            }
            if (now == null) {
                now = new java.util.LinkedHashSet<>();
            }
            now.add(a.id);
        }
        java.util.Set<java.util.UUID> next =
                now == null ? java.util.Collections.emptySet() : now;
        if (jammed.equals(next)) {
            return;
        }
        jammed.clear();
        jammed.addAll(next);
        sync();
    }

    private static boolean isDeviating(Player p) {
        return cn.academy.ability.context.ContextManager.instance
                .find(cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.DeviationContext.class, p)
                .isPresent();
    }

    private static final net.minecraft.resources.ResourceLocation PLAYER_TYPE =
            net.minecraft.world.entity.EntityType.getKey(net.minecraft.world.entity.EntityType.PLAYER);

    public void setSlotFromClient(int pid, int slot, int skillID, int cmd) {
        checkSide(LogicalSide.CLIENT);
        edit(b -> applySlot(b, pid, slot, skillID, cmd));
        sendMessage(MSG_SET_SLOT, pid, slot, skillID, cmd);
    }

    public void setNumbersFromClient(int pid, int range, int count) {
        checkSide(LogicalSide.CLIENT);
        edit(b -> applyNumbers(b, pid, range, count));
        sendMessage(MSG_SET_NUM, pid, range, count);
    }

    public void switchFromClient(int pid) {
        checkSide(LogicalSide.CLIENT);
        edit(b -> b.switchTo(pid));
        sendMessage(MSG_SWITCH, pid);
    }

    public void setSyncFromClient(int pid, boolean on) {
        checkSide(LogicalSide.CLIENT);
        edit(b -> b.applySync(pid, on));
        sendMessage(MSG_SET_SYNC, pid, on ? 1 : 0);
    }

    public java.util.Set<java.util.UUID> getEnraged() {
        return java.util.Collections.unmodifiableSet(enraged);
    }

    public boolean addEnraged(java.util.UUID id) {
        checkSide(net.minecraftforge.fml.LogicalSide.SERVER);
        return id != null && enraged.add(id);
    }

    public boolean removeEnraged(java.util.UUID id) {
        checkSide(net.minecraftforge.fml.LogicalSide.SERVER);
        return id != null && enraged.remove(id);
    }

    public boolean isJammed(java.util.UUID id) {
        return id != null && jammed.contains(id);
    }

    public java.util.List<Ally> getAllies() {
        return java.util.Collections.unmodifiableList(allies);
    }

    public boolean addAlly(java.util.UUID id, net.minecraft.world.entity.EntityType<?> type) {
        checkSide(LogicalSide.SERVER);
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.world.entity.EntityType.getKey(type);
        for (Ally a : allies) {
            if (a.id.equals(id)) {
                return false;
            }
        }

        if (allies.size() >= MAX_ALLIES) {
            return false;
        }
        allies.add(new Ally(id, key));
        return true;
    }

    public boolean isAlly(java.util.UUID id) {
        if (id == null) {
            return false;
        }
        for (Ally a : allies) {
            if (a.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeAlly(java.util.UUID id) {
        checkSide(LogicalSide.SERVER);
        return allies.removeIf(a -> a.id.equals(id));
    }

    public static final class Ally {

        public final java.util.UUID id;
        public final net.minecraft.resources.ResourceLocation type;

        Ally(java.util.UUID id, net.minecraft.resources.ResourceLocation type) {
            this.id = id;
            this.type = type;
        }
    }

    @Listener(channel = MSG_SET_SLOT, side = LogicalSide.SERVER)
    private void s_setSlot(Integer pid, Integer slot, Integer skillID, Integer cmd) {
        edit(b -> applySlot(b, pid == null ? -1 : pid, slot == null ? -1 : slot,
                skillID == null ? -1 : skillID, cmd == null ? 0 : cmd));
    }

    @Listener(channel = MSG_SET_NUM, side = LogicalSide.SERVER)
    private void s_setNumbers(Integer pid, Integer range, Integer count) {
        edit(b -> applyNumbers(b, pid == null ? -1 : pid, range == null ? MIN_RANGE : range,
                count == null ? MIN_COUNT : count));
    }

    @Listener(channel = MSG_SWITCH, side = LogicalSide.SERVER)
    private void s_switch(Integer pid) {
        edit(b -> b.switchTo(pid == null ? -1 : pid));
    }

    @Listener(channel = MSG_SET_SYNC, side = LogicalSide.SERVER)
    private void s_setSync(Integer pid, Integer on) {

        boolean want = on != null && on != 0;
        if (want && !CognitionTamper.isLearned(getEntity())) {
            return;
        }
        edit(b -> b.applySync(pid == null ? -1 : pid, want));
    }

    private void edit(java.util.function.Consumer<Book> action) {
        Player p = getEntity();
        if (p == null) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof cn.academy.item.RemoteControlItem)) {
            return;
        }
        Book b = Book.of(stack);
        action.accept(b);
        b.save(stack);
    }

    private void applySlot(Book b, int pid, int slot, int skillID, int cmd) {

        if (pid < 0 || pid >= MAX_PROGRAMS || slot < 0 || slot >= MAX_SLOTS
                || !MentalMastery.slotUnlocked(getEntity(), slot)) {
            return;
        }
        Program p = b.programs[pid];
        Skill skill = resolve(skillID);

        if (!(skill instanceof WideCastable)) {
            skill = null;
        }

        if (skill instanceof WideCastable wc2 && wc2.wideUniquePerProgram()) {
            for (int i = 0; i < MAX_SLOTS; ++i) {
                if (i != slot && p.getSkill(i) == skill) {
                    return;
                }
            }
        }
        p.catIDs[slot] = skill == null ? -1 : skill.getCategory().getCategoryID();
        p.skillIDs[slot] = skill == null ? -1 : skill.getID();
        p.cmds[slot] = (byte) Math.max(0, cmd);
    }

    private void applyNumbers(Book b, int pid, int range, int count) {
        if (pid < 0 || pid >= MAX_PROGRAMS) {
            return;
        }

        b.programs[pid].range = Math.max(MIN_RANGE, Math.min(HARD_MAX_RANGE, range));
        b.programs[pid].count = Math.max(MIN_COUNT, Math.min(HARD_MAX_COUNT, count));
    }

    private static final int HARD_MAX_RANGE = 128, HARD_MAX_COUNT = 128;

    private Skill resolve(int skillID) {
        if (skillID < 0) {
            return null;
        }
        Player p = getEntity();
        if (p == null || !EntityData.isReady(p)) {
            return null;
        }
        AbilityData aData = AbilityData.get(p);
        return aData == null || !aData.hasCategory() ? null : aData.getCategory().getSkill(skillID);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        net.minecraft.nbt.ListTag al = new net.minecraft.nbt.ListTag();
        for (Ally a : allies) {
            CompoundTag e = new CompoundTag();
            e.putUUID("u", a.id);
            e.putString("t", a.type.toString());
            al.add(e);
        }
        tag.put("ally", al);

        net.minecraft.nbt.ListTag en = new net.minecraft.nbt.ListTag();
        for (java.util.UUID id : enraged) {
            CompoundTag e = new CompoundTag();
            e.putUUID("u", id);
            en.add(e);
        }
        tag.put("enraged", en);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        enraged.clear();
        net.minecraft.nbt.ListTag en = tag.getList("enraged", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < en.size(); ++i) {
            CompoundTag e = en.getCompound(i);
            if (e.hasUUID("u")) {
                enraged.add(e.getUUID("u"));
            }
        }
        allies.clear();
        net.minecraft.nbt.ListTag al = tag.getList("ally", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < al.size() && allies.size() < MAX_ALLIES; ++i) {
            CompoundTag e = al.getCompound(i);

            net.minecraft.resources.ResourceLocation type =
                    net.minecraft.resources.ResourceLocation.tryParse(e.getString("t"));
            if (type != null && e.hasUUID("u")) {
                allies.add(new Ally(e.getUUID("u"), type));
            }
        }
    }

    private static void copyInto(int[] dst, int[] src) {
        for (int i = 0; i < dst.length && i < src.length; ++i) {
            dst[i] = src[i];
        }
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {

        buf.writeVarInt(allies.size());
        for (Ally a : allies) {
            buf.writeUUID(a.id);
            buf.writeResourceLocation(a.type);
        }

        buf.writeVarInt(enraged.size());
        for (java.util.UUID id : enraged) {
            buf.writeUUID(id);
        }

        buf.writeVarInt(jammed.size());
        for (java.util.UUID id : jammed) {
            buf.writeUUID(id);
        }
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        allies.clear();

        int n = Math.min(MAX_ALLIES, Math.max(0, buf.readVarInt()));
        for (int i = 0; i < n; ++i) {
            allies.add(new Ally(buf.readUUID(), buf.readResourceLocation()));
        }
        enraged.clear();

        int m = Math.min(HARD_MAX_ENRAGED, Math.max(0, buf.readVarInt()));
        for (int i = 0; i < m; ++i) {
            enraged.add(buf.readUUID());
        }
        jammed.clear();

        int k = Math.min(MAX_ALLIES, Math.max(0, buf.readVarInt()));
        for (int i = 0; i < k; ++i) {
            jammed.add(buf.readUUID());
        }
    }

    public static RemoteData get(Player player) {
        if (player == null || !EntityData.isReady(player)) {
            return null;
        }
        EntityData<Player> data = EntityData.get(player);
        return data == null ? null : data.getPart(RemoteData.class);
    }

    public static final class Program {

        final int[] catIDs = new int[MAX_SLOTS];
        final int[] skillIDs = new int[MAX_SLOTS];
        final byte[] cmds = new byte[MAX_SLOTS];

        int range = 10;
        int count = 1;

        boolean syncMind = false;

        public Program() {
            java.util.Arrays.fill(catIDs, -1);
            java.util.Arrays.fill(skillIDs, -1);
        }

        public Skill getSkill(int slot) {
            if (slot < 0 || slot >= MAX_SLOTS || skillIDs[slot] < 0 || catIDs[slot] < 0) {
                return null;
            }

            Category cat = CategoryManager.INSTANCE.getCategory(catIDs[slot]);
            return cat == null ? null : cat.getSkill(skillIDs[slot]);
        }

        public int getCommand(int slot) {
            return slot < 0 || slot >= MAX_SLOTS ? 0 : cmds[slot];
        }

        public boolean isEmpty(int limit) {
            for (int i = 0; i < Math.min(limit, MAX_SLOTS); ++i) {
                if (getSkill(i) != null) {
                    return false;
                }
            }
            return true;
        }

        public boolean isSyncMind() {
            return syncMind;
        }

        public int getRange() {
            return range;
        }

        public int getCount() {
            return count;
        }

        public int effectiveRange(float wideCastExp) {
            return Math.max(MIN_RANGE, Math.min(maxRange(wideCastExp), range));
        }

        public int effectiveCount(float wideCastExp) {
            return Math.max(MIN_COUNT, Math.min(maxCount(wideCastExp), count));
        }
    }

    public static final class RemoteEvents {

        @SubscribeEvent
        public void onCategorySwitched(CategoryChangeEvent event) {
            if (!SideUtils.isClient()) {
                RemoteData data = RemoteData.get(event.player);
                if (data != null) {
                    data.clear();
                }
            }
        }
    }
}
