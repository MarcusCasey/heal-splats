package com.heal_splats;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Renders a heal splat that visually matches the jagged OSRS damage hitsplat
 * from splat.png — dark red star-burst shape with a brighter red centre and a
 * white number.  No floating; the splat is anchored to the player's head and
 * tracks their position until it fades out.
 */
@Slf4j
public class HealSplatsOverlay extends Overlay
{
	/** Fade starts at this fraction of the lifetime. */
	private static final float FADE_START = 0.70f;

	/** Z-offset (world units) — ~half of a player's logical height puts the splat at mid-body. */
	private static final int Z_OFFSET = 90;

	// ── Geometry ────────────────────────────────────────────────────────────
	/** Number of points on the jagged star (each "tooth" = 2 vertices). */
	private static final int STAR_TEETH   = 9;
	/** Outer radius of the star-burst. */
	private static final int OUTER_R      = 17;
	/** Inner (valley) radius — controls how sharp the teeth look. */
	private static final int INNER_R      = 11;
	/** Radius of the bright centre circle. */
	private static final int CENTRE_R     = 10;

	// ── Fixed colours (not user-configurable) ───────────────────────────────
	private static final Color COLOR_TEXT   = Color.WHITE;
	private static final Color COLOR_SHADOW = new Color(0, 0, 0, 210);

	// ── Fallback defaults (forest green) used when a hex code is invalid ────
	private static final Color DEFAULT_OUTER  = new Color(26,  92, 26);   // #1A5C1A
	private static final Color DEFAULT_CENTRE = new Color(34, 139, 34);   // #228B22
	private static final Color DEFAULT_SHINE  = new Color(76, 175, 80);   // #4CAF50

	private static final Font FONT = new Font("Arial", Font.BOLD, 12);

	@Inject
	private Client client;

	@Inject
	private HealSplatsConfig config;

	private final List<HealSplat> splats = new ArrayList<>();

	/** Pre-built star polygon template (re-used every frame, shifted per draw). */
	private final int[] starXTemplate = new int[STAR_TEETH * 2];
	private final int[] starYTemplate = new int[STAR_TEETH * 2];

	@Inject
	HealSplatsOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		buildStarTemplate();
	}

	/**
	 * Pre-compute the star polygon centred on (0,0) so we only translate it
	 * each draw call instead of re-computing all sines/cosines every frame.
	 */
	private void buildStarTemplate()
	{
		int nVerts = STAR_TEETH * 2;
		// Rotate by -π/2 so the first outer point is straight up
		double offset = -Math.PI / 2.0;
		for (int i = 0; i < nVerts; i++)
		{
			double angle = offset + (Math.PI * i / STAR_TEETH);
			int r = (i % 2 == 0) ? OUTER_R : INNER_R;
			starXTemplate[i] = (int) Math.round(r * Math.cos(angle));
			starYTemplate[i] = (int) Math.round(r * Math.sin(angle));
		}
	}

	void addSplat(int amount)
	{
		splats.add(new HealSplat(amount, System.currentTimeMillis()));
	}

	void clearSplats()
	{
		splats.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		long now = System.currentTimeMillis();
		Iterator<HealSplat> it = splats.iterator();

		while (it.hasNext())
		{
			HealSplat splat = it.next();
			long age = now - splat.getCreatedAt();
			long durationMs = config.duration() * 1000L;

			if (age >= durationMs)
			{
				it.remove();
				continue;
			}

			float progress = (float) age / durationMs;
			float alpha = progress < FADE_START
				? 1f
				: 1f - (progress - FADE_START) / (1f - FADE_START);

			String text = String.valueOf(splat.getAmount());

			Point anchor = player.getCanvasTextLocation(graphics, text, Z_OFFSET);
			if (anchor == null)
			{
				continue;
			}

			Color outer  = parseHex(config.outerColor(),  DEFAULT_OUTER);
			Color centre = parseHex(config.centreColor(), DEFAULT_CENTRE);
			Color shine  = parseHex(config.shineColor(),  DEFAULT_SHINE);
			drawSplat(graphics, anchor.getX(), anchor.getY(), text, alpha, outer, centre, shine);
		}

		return null;
	}

	/**
	 * Parses a CSS-style hex colour string (#RRGGBB or #RGB).
	 * Returns {@code fallback} and logs a warning if the string is invalid.
	 */
	private static Color parseHex(String hex, Color fallback)
	{
		try
		{
			return Color.decode(hex);
		}
		catch (NumberFormatException e)
		{
			log.warn("Invalid colour hex '{}', using default", hex);
			return fallback;
		}
	}

	private void drawSplat(Graphics2D g, int cx, int cy, String text, float alpha,
		Color colorOuter, Color colorCentre, Color colorShine)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Composite original = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		// Translate template polygon to (cx, cy)
		int n = STAR_TEETH * 2;
		int[] px = new int[n];
		int[] py = new int[n];
		for (int i = 0; i < n; i++)
		{
			px[i] = cx + starXTemplate[i];
			py[i] = cy + starYTemplate[i];
		}
		Polygon star = new Polygon(px, py, n);

		// ── 1. Dark outer star-burst ─────────────────────────────────────────
		g.setColor(colorOuter);
		g.fillPolygon(star);

		// ── 2. Bright centre circle ───────────────────────────────────────────
		g.setColor(colorCentre);
		g.fillOval(cx - CENTRE_R, cy - CENTRE_R, CENTRE_R * 2, CENTRE_R * 2);

		// ── 3. Small highlight fleck (upper-left, ~1/4 of the centre) ────────
		int shineR = CENTRE_R / 2;
		g.setColor(colorShine);
		g.fillOval(cx - CENTRE_R + 2, cy - CENTRE_R + 2, shineR, shineR);

		// ── 4. Number centred on the splat ───────────────────────────────────
		g.setFont(FONT);
		FontMetrics fm = g.getFontMetrics();
		int textX = cx - fm.stringWidth(text) / 2;
		int textY = cy + fm.getAscent() / 2 - 1;

		g.setColor(COLOR_SHADOW);
		g.drawString(text, textX + 1, textY + 1);
		g.setColor(COLOR_TEXT);
		g.drawString(text, textX, textY);

		g.setComposite(original);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
	}
}
