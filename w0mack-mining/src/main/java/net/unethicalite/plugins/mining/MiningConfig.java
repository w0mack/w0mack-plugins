package net.unethicalite.plugins.mining;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("w0mack-mining")
public interface MiningConfig extends Config {
    @ConfigItem(
            keyName = "rock",
            name = "Rock type",
            description = "The type of rock to mine",
            position = 0
    )
    default Rock rock() {
        return Rock.COPPER;
    }

    @ConfigItem(
            keyName = "bankLogs",
            name = "Bank Logs",
            description = "Make fire while chopping",
            position = 1
    )
    default boolean bankLogs() {
        return false;
    }

    @ConfigItem(
            keyName = "Start",
            name = "Start/Stop",
            description = "Start/Stop button",
            position = 2)
    default Button startStopButton() {
        return new Button();
    }
}
