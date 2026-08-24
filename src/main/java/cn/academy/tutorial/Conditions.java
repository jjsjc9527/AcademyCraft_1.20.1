package cn.academy.tutorial;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class Conditions {

    private Conditions() {}

    private static final Condition ALWAYS_TRUE = player -> true;

    private static final List<Condition> indexedConditions = new ArrayList<>();
    private static final Multimap<Item, ItemInfo>
            craftConds = ArrayListMultimap.create(),
            smeltConds = ArrayListMultimap.create(),
            pickupConds = ArrayListMultimap.create();

    public static Condition alwaysTrue() {
        return ALWAYS_TRUE;
    }

    public static Condition itemCrafted(Item item) {
        return itemCrafted(item, -1);
    }

    public static Condition itemCrafted(Item item, int meta) {
        return createItemMapped(craftConds, item, meta);
    }

    public static Condition itemSmelted(Item item) {
        return itemSmelted(item, -1);
    }

    public static Condition itemSmelted(Item item, int meta) {
        return createItemMapped(smeltConds, item, meta);
    }

    public static Condition itemPickup(Item item) {
        return itemPickup(item, -1);
    }

    public static Condition itemPickup(Item item, int meta) {
        return createItemMapped(pickupConds, item, meta);
    }

    public static Condition itemObtained(Item item) {
        return itemCrafted(item).or(itemPickup(item)).or(itemSmelted(item));
    }

    public static Condition itemObtained(Item item, int meta) {
        return itemCrafted(item, meta).or(itemPickup(item, meta)).or(itemSmelted(item, meta));
    }

    public static Condition itemObtained(Block block) {
        return itemObtained(block.asItem());
    }

    public static Condition itemObtained(Block block, int meta) {
        return itemObtained(block.asItem(), meta);
    }

    private static IndexedCondition indexed() {
        int idx = indexedConditions.size();
        IndexedCondition ret = new IndexedCondition(idx);
        indexedConditions.add(ret);
        return ret;
    }

    private static Condition createItemMapped(Multimap<Item, ItemInfo> map, Item item, int meta) {
        IndexedCondition ret = indexed();
        map.put(item, new ItemInfo(ret, item, meta));
        return ret;
    }

    private static class IndexedCondition implements Condition {
        final int index;

        IndexedCondition(int idx) {
            index = idx;
        }

        @Override
        public boolean test(Player player) {
            return TutorialData.get(player).isCondActivate(index);
        }
    }

    private static class ItemInfo {
        public final IndexedCondition cond;
        public final Item item;
        public final int meta;

        public ItemInfo(IndexedCondition cond, Item item, int meta) {
            this.cond = cond;
            this.item = item;
            this.meta = meta;
        }

        public boolean metaSensitive() {
            return meta != -1;
        }
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Conditions());
    }

    @SubscribeEvent
    public void onItemSmelt(PlayerEvent.ItemSmeltedEvent evt) {
        trigger(smeltConds, evt.getSmelting(), evt.getEntity());
    }

    @SubscribeEvent
    public void onItemCraft(PlayerEvent.ItemCraftedEvent evt) {
        trigger(craftConds, evt.getCrafting(), evt.getEntity());
    }

    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent evt) {
        trigger(pickupConds, evt.getStack(), evt.getEntity());
    }

    private void trigger(Multimap<Item, ItemInfo> map, ItemStack stack, Player player) {
        if (!player.level().isClientSide) {
            TutorialData tdata = TutorialData.get(player);
            map.get(stack.getItem())
                    .stream()
                    .filter(info -> !info.metaSensitive() || stack.getDamageValue() == info.meta)
                    .forEach(info -> tdata.setCondActivate(info.cond.index));
        }
    }

}
