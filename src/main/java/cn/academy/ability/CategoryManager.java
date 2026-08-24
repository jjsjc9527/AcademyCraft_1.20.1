package cn.academy.ability;

import cn.lambdalib2.util.Debug;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryManager {

    public static CategoryManager INSTANCE = new CategoryManager();

    private final List<Category> catList = new ArrayList<>();

    private boolean _baked = false;

    private CategoryManager() {}

    public void register(Category cat) {
        Debug.require(!_baked, "CategoryManager.register() may only be called before bake");
        catList.add(cat);
    }

    public Category getCategory(int id) {
        return (id < 0 || id >= catList.size()) ? null : catList.get(id);
    }

    public List<Category> getCategories() {
        return ImmutableList.copyOf(catList);
    }

    public int getCategoryCount() {
        return catList.size();
    }

    public Category getCategory(String name) {
        for (Category c : catList) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private static final List<String> CANONICAL_ORDER =
            ImmutableList.of("electromaster", "meltdowner", "teleporter", "vecmanip", "mentalout");

    public void bake() {
        _baked = true;
        catList.sort(Comparator
                .comparingInt((Category c) -> {
                    int i = CANONICAL_ORDER.indexOf(c.getName());
                    return i >= 0 ? i : CANONICAL_ORDER.size();
                })
                .thenComparing(Category::getName));
        for (int idx = 0; idx < catList.size(); ++idx) {
            catList.get(idx).catID = idx;
        }

        StringBuilder sb = new StringBuilder();
        for (Category c : catList) {
            sb.append(' ').append(c.catID).append('=').append(c.getName())
                    .append('(').append(c.getSkillList().size()).append(" skills)");
        }
        com.mojang.logging.LogUtils.getLogger().info(
                "[AC] Ability categories baked, {} total:{}", catList.size(), sb);
    }
}
