package cn.academy.tutorial;

import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

public interface Condition extends Predicate<Player> {

    default Condition or(Condition other) {
        return new OrCondition(this, other);
    }

    default Condition and(Condition other) {
        return new AndCondition(this, other);
    }

    class OrCondition implements Condition {
        private final Condition lhs;
        private final Condition rhs;

        public OrCondition(Condition lhs, Condition rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean test(Player player) {
            return lhs.test(player) || rhs.test(player);
        }
    }

    class AndCondition implements Condition {
        private final Condition lhs;
        private final Condition rhs;

        public AndCondition(Condition lhs, Condition rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean test(Player player) {
            return lhs.test(player) && rhs.test(player);
        }
    }

}
