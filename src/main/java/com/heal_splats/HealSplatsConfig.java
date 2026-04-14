package com.heal_splats;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("healsplats")
public interface HealSplatsConfig extends Config
{
	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "duration",
		name = "Duration (seconds)",
		description = "How long the heal splat stays on screen"
	)
	default int duration()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "outerColor",
		name = "Outer colour",
		description = "Hex code for the jagged outer star-burst ring (e.g. #5A0505)"
	)
	default String outerColor()
	{
		return "#1A5C1A";
	}

	@ConfigItem(
		keyName = "centreColor",
		name = "Centre colour",
		description = "Hex code for the bright inner circle (e.g. #B91919)"
	)
	default String centreColor()
	{
		return "#228B22";
	}

	@ConfigItem(
		keyName = "shineColor",
		name = "Shine colour",
		description = "Hex code for the small highlight fleck (e.g. #DC503C)"
	)
	default String shineColor()
	{
		return "#4CAF50";
	}
}
