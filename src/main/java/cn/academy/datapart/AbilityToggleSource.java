package cn.academy.datapart;

public enum AbilityToggleSource {

    PLAYER_KEY(true),

    SKILL_KEY(true),

    SYSTEM(true),

    COMMAND(true),

    UNKNOWN(false);

    private final boolean authorized;

    AbilityToggleSource(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean authorized() {
        return authorized;
    }

    private static final AbilityToggleSource[] VALUES = values();

    public static AbilityToggleSource fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : UNKNOWN;
    }
}
