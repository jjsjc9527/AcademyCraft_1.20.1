package cn.academy.tutorial;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TutorialRegistry {

    private static final Map<String, ACTutorial> tutorials = new LinkedHashMap<>();

    public static void addTutorials(ACTutorial... tutorial) {
        for (ACTutorial t : tutorial) {
            if (tutorials.containsKey(t.id))
                throw new RuntimeException("Already have a tutorial with this id:" + t.id);
            tutorials.put(t.id, t);
        }
    }

    public static ACTutorial addTutorial(String string) {
        ACTutorial t = new ACTutorial(string);
        addTutorials(t);
        return t;
    }

    public static ACTutorial getTutorial(String s) {
        if (!tutorials.containsKey(s))
            throw new RuntimeException("No such a tutorial: " + s);
        return tutorials.get(s);
    }

    public static Collection<ACTutorial> getLearned(Player player) {
        List<ACTutorial> ret = new ArrayList<>();
        for (ACTutorial t : tutorials.values()) {
            if (t.isActivated(player)) ret.add(t);
        }
        return ret;
    }

    public static Pair<List<ACTutorial>, List<ACTutorial>> groupByLearned(Player player) {
        List<ACTutorial> learned = new ArrayList<>();
        List<ACTutorial> unlearned = new ArrayList<>();
        for (ACTutorial tut : tutorials.values()) {
            if (tut.isActivated(player)) learned.add(tut);
            else unlearned.add(tut);
        }
        return Pair.of(learned, unlearned);
    }

    public static Collection<ACTutorial> enumeration() {
        return ImmutableList.copyOf(tutorials.values());
    }

}
