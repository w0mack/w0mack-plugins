package net.unethicalite.plugins.chopper;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;

@ConfigGroup("w0mack-chopper")
public interface ChopperConfig extends Config
{
	@ConfigItem(
			keyName = "tree",
			name = "Tree type",
			description = "The type of tree to chop",
			position = 0
	)
	default Tree tree()
	{
		return Tree.REGULAR;
	}


	@ConfigItem(
			keyName = "makeFire",
			name = "Make fire",
			description = "Make fire while chopping",
			position = 2
	)
	default boolean makeFire()
	{
		return false;
	}

	@ConfigItem(
			keyName = "bankLogs",
			name = "Bank Logs",
			description = "Make fire while chopping",
			position = 1
	)
	default boolean bankLogs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "Start",
		name = "Start/Stop",
		description = "Start/Stop button",
		position = 3)
	default Button startStopButton()
	{
		return new Button();
	}
}
