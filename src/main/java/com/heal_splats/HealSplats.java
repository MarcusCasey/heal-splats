package com.heal_splats;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Heal Splats",
	description = "Shows a green heal splat on your character when you eat food or drink a potion",
	tags = {"heal", "food", "splat", "hp", "hitpoints", "health"}
)
public class HealSplats extends Plugin
{
	/** Number of game ticks after an eat/drink in which HP increases are attributed to that action. */
	private static final int HEAL_WINDOW_TICKS = 4;

	@Inject
	private Client client;

	@Inject
	private HealSplatsConfig config;

	@Inject
	private HealSplatsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private int lastHp = -1;
	private int healWindowTicks = 0;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		lastHp = -1;
		healWindowTicks = 0;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlay.clearSplats();
		lastHp = -1;
		healWindowTicks = 0;
	}

	/** Open a heal window when the player clicks Eat or Drink. */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if ("Eat".equalsIgnoreCase(option) || "Drink".equalsIgnoreCase(option))
		{
			healWindowTicks = HEAL_WINDOW_TICKS;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (healWindowTicks > 0)
		{
			healWindowTicks--;
		}
	}

	/**
	 * When HP increases while a heal window is open, show a splat for the delta.
	 * The window is closed immediately so a single eat cannot fire multiple splats.
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.HITPOINTS)
		{
			return;
		}

		int currentHp = event.getBoostedLevel();

		if (lastHp >= 0 && healWindowTicks > 0 && currentHp > lastHp)
		{
			overlay.addSplat(currentHp - lastHp);
			healWindowTicks = 0;
		}

		lastHp = currentHp;
	}

	@Provides
	HealSplatsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HealSplatsConfig.class);
	}
}
