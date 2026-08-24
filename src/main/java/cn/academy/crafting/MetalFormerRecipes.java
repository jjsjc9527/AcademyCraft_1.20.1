package cn.academy.crafting;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class MetalFormerRecipes {

    public static final MetalFormerRecipes INSTANCE = new MetalFormerRecipes();

    public enum Mode {
        PLATE, INCISE, ETCH, REFINE;

        public static Mode byOrdinal(int i) {
            Mode[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : PLATE;
        }
    }

    private final List<RecipeObject> objects = new ArrayList<>();

    private MetalFormerRecipes() {}

    public static void register() {
        if (!INSTANCE.objects.isEmpty()) return;
        MetalFormerRecipes m = INSTANCE;

        m.add(Mode.INCISE, ACItems.IMAG_SILICON_INGOT.get(), 1, ACItems.WAFER.get(), 2);
        m.add(Mode.INCISE, ACItems.WAFER.get(), 1, ACItems.IMAG_SILICON_PIECE.get(), 4);
        m.add(Mode.ETCH, ACItems.DATA_CHIP.get(), 1, ACItems.CALC_CHIP.get(), 1);
        m.add(Mode.PLATE, Items.IRON_INGOT, 1, ACItems.REINFORCED_IRON_PLATE.get(), 1);
        m.add(Mode.PLATE, ACItems.CONSTRAINT_INGOT.get(), 1, ACItems.CONSTRAINT_PLATE.get(), 1);

        m.add(Mode.PLATE, ACItems.REINFORCED_IRON_PLATE.get(), 2, ACItems.COIN.get(), 3);

        m.add(Mode.INCISE, ACItems.REINFORCED_IRON_PLATE.get(), 1, ACItems.NEEDLE.get(), 6);
        m.add(Mode.INCISE, Items.RAIL, 1, ACItems.NEEDLE.get(), 2);

        m.add(Mode.ETCH, ACItems.WAFER.get(), 1, ACItems.SILBARN.get(), 1);

        m.add(Mode.REFINE, ACBlocks.IMAGSIL_ORE.get(), 1, ACItems.IMAG_SILICON_INGOT.get(), 4);
        m.add(Mode.REFINE, ACBlocks.DEEPSLATE_IMAGSIL_ORE.get(), 1, ACItems.IMAG_SILICON_INGOT.get(), 4);
        m.add(Mode.REFINE, ACBlocks.CONSTRAINT_METAL.get(), 1, ACItems.CONSTRAINT_INGOT.get(), 2);
        m.add(Mode.REFINE, ACBlocks.DEEPSLATE_CONSTRAINT_METAL.get(), 1, ACItems.CONSTRAINT_INGOT.get(), 2);
        m.add(Mode.REFINE, ACBlocks.RESO_ORE.get(), 1, ACItems.RESO_CRYSTAL.get(), 3);
        m.add(Mode.REFINE, ACBlocks.DEEPSLATE_RESO_ORE.get(), 1, ACItems.RESO_CRYSTAL.get(), 3);
        m.add(Mode.REFINE, ACBlocks.CRYSTAL_ORE.get(), 1, ACItems.CRYSTAL_LOW.get(), 4);
        m.add(Mode.REFINE, ACBlocks.DEEPSLATE_CRYSTAL_ORE.get(), 1, ACItems.CRYSTAL_LOW.get(), 4);

        m.addTagRefine(forgeOre("gold"), Items.GOLD_INGOT, 2);
        m.addTagRefine(forgeOre("iron"), Items.IRON_INGOT, 2);

        m.addTagRefine(forgeRawMaterial("iron"), Items.IRON_INGOT, 2);
        m.addTagRefine(forgeRawMaterial("gold"), Items.GOLD_INGOT, 2);
        m.addTagRefine(forgeOre("emerald"), Items.EMERALD, 2);
        m.addTagRefine(forgeOre("quartz"), Items.QUARTZ, 2);
        m.addTagRefine(forgeOre("diamond"), Items.DIAMOND, 2);
        m.addTagRefine(forgeOre("redstone"), Items.REDSTONE_BLOCK, 1);

        m.addTagRefine(forgeOre("lapis"), Items.LAPIS_LAZULI, 12);
        m.addTagRefine(forgeOre("coal"), Items.COAL, 2);

        for (String metal : new String[]{"copper", "tin", "silver", "lead",
                "aluminum", "nickel", "platinum", "iridium", "mithril"}) {
            m.addTagRefine(forgeOre(metal), forgeIngot(metal), 2);
        }

        m.addTagRefine(forgeRawMaterial("copper"), forgeIngot("copper"), 2);
    }

    private static TagKey<Item> forgeOre(String name) {
        return ItemTags.create(new ResourceLocation("forge", "ores/" + name));
    }

    private static TagKey<Item> forgeIngot(String name) {
        return ItemTags.create(new ResourceLocation("forge", "ingots/" + name));
    }

    private static TagKey<Item> forgeRawMaterial(String name) {
        return ItemTags.create(new ResourceLocation("forge", "raw_materials/" + name));
    }

    public void add(Mode mode, ItemLike input, int inCount, ItemLike output, int outCount) {
        addRecipe(new RecipeObject(mode, input.asItem(), null, inCount,
                new ItemStack(output, outCount), null));
    }

    public void addTagRefine(TagKey<Item> inputTag, ItemLike output, int outCount) {
        addRecipe(new RecipeObject(Mode.REFINE, null, inputTag, 1,
                new ItemStack(output, outCount), null));
    }

    public void addTagRefine(TagKey<Item> inputTag, TagKey<Item> outputTag, int outCount) {
        addRecipe(new RecipeObject(Mode.REFINE, null, inputTag, 1,
                null, new TagOutput(outputTag, outCount)));
    }

    public void addRecipe(RecipeObject r) {
        r.id = objects.size();
        objects.add(r);
    }

    public RecipeObject getRecipe(ItemStack input, Mode mode) {
        for (RecipeObject r : objects) {
            if (r.accepts(input, mode)) return r;
        }
        return null;
    }

    public boolean isValidInput(ItemStack stack) {
        for (RecipeObject r : objects) {
            if (r.matchesItem(stack)) return true;
        }
        return false;
    }

    public RecipeObject byId(int id) {
        return id >= 0 && id < objects.size() ? objects.get(id) : null;
    }

    public List<RecipeObject> getAllRecipes() {
        return objects;
    }

    private record TagOutput(TagKey<Item> tag, int count) {
        ItemStack resolve() {
            var t = ForgeRegistries.ITEMS.tags();
            if (t == null) return ItemStack.EMPTY;
            var tag0 = t.getTag(tag);
            if (tag0.isEmpty()) return ItemStack.EMPTY;
            return new ItemStack(tag0.stream().findFirst().orElseThrow(), count);
        }
    }

    public static class RecipeObject {

        int id;
        public final Mode mode;

        private final Item inputItem;
        private final TagKey<Item> inputTag;
        private final int inputCount;

        private final ItemStack output;
        private final TagOutput outputTag;

        RecipeObject(Mode mode, Item inputItem, TagKey<Item> inputTag, int inputCount,
                     ItemStack output, TagOutput outputTag) {
            this.mode = mode;
            this.inputItem = inputItem;
            this.inputTag = inputTag;
            this.inputCount = inputCount;
            this.output = output;
            this.outputTag = outputTag;
        }

        public boolean matchesItem(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return inputTag != null ? stack.is(inputTag) : stack.getItem() == inputItem;
        }

        public boolean accepts(ItemStack stack, Mode mode2) {
            return mode == mode2 && matchesItem(stack) && stack.getCount() >= inputCount;
        }

        public int getInputCount() {
            return inputCount;
        }

        public ItemStack getOutput() {
            return outputTag != null ? outputTag.resolve() : output.copy();
        }

        public java.util.List<ItemStack> displayInputs() {
            if (inputTag != null) {
                return net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(inputTag)
                        .stream().map(i -> new ItemStack(i, inputCount)).toList();
            }
            return inputItem != null
                    ? java.util.List.of(new ItemStack(inputItem, inputCount))
                    : java.util.List.of();
        }

        public int getID() {
            return id;
        }
    }
}
