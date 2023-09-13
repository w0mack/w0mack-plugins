package net.unethicalite.plugins.chopper;

import lombok.Getter;

@Getter
public enum Rock {
    COPPER(1, "Copper"),
    TIN(1, "Tin"),
    IRON(15, "Iron"),
    COAL(30, "Coal"),
    GOLD(40, "Gold"),
    GEM(40, "Gem"),
    GRANITE(45, "Granite"),
    MITHRIL(55, "Mithril"),
    ADAMANTITE(70, "Adamant"),
    TE_SALT(72, "Te Salt")
    EFH_SALT(72, "Efh Salt")
    URT_SALT(72, "Urt Salt")
    BASALT(72, "Basalt");
    AMETHYST(92, "Amethyst");

    private final int level;
    private final String[] names;

    Tree(int level, String... names) {
        this.level = level;
        this.names = names;
    }
}
