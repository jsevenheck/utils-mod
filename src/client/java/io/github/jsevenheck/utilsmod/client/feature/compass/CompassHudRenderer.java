package io.github.jsevenheck.utilsmod.client.feature.compass;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

/**
 * Renders a horizontally scrolling navigation compass at the top-center of the HUD,
 * showing the player's current bearing with cardinal/intercardinal labels and tick marks.
 */
public final class CompassHudRenderer implements HudElement {

	private static final int HUD_WIDTH = 270;
	private static final int HUD_Y = 10;
	private static final int BAR_HEIGHT = 13;

	private static final float PIXELS_PER_DEGREE = 3.0f;
	private static final float VISIBLE_HALF_RANGE_DEGREES = HUD_WIDTH / 2.0f / PIXELS_PER_DEGREE;
	private static final float LABEL_EDGE_MARGIN_DEGREES = 8.0f;

	private static final int TICK_STEP_DEGREES = 5;
	private static final int MEDIUM_TICK_STEP_DEGREES = 15;
	private static final int MAJOR_TICK_STEP_DEGREES = 45;

	private static final int MINOR_TICK_HEIGHT = 2;
	private static final int MEDIUM_TICK_HEIGHT = 4;
	private static final int MAJOR_TICK_HEIGHT = 6;

	private static final int BACKGROUND_COLOR = 0x80000000;
	private static final int MINOR_TICK_COLOR = 0x66FFFFFF;
	private static final int MEDIUM_TICK_COLOR = 0x99FFFFFF;
	private static final int MAJOR_TICK_COLOR = 0xCCFFFFFF;
	private static final int LABEL_COLOR = 0xFFE0E0E0;
	private static final int CARDINAL_LABEL_COLOR = 0xFFFFD700;
	private static final int DEGREE_LABEL_COLOR = 0xFF999999;
	private static final int CENTER_MARKER_COLOR = 0xFFFFD700;

	private static final String CENTER_MARKER_GLYPH = "▲";
	private static final String[] DIRECTION_LABELS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private static final int WAYPOINT_DOT_SIZE = 9;
	private static final int WAYPOINT_ROW_Y = HUD_Y + (BAR_HEIGHT - WAYPOINT_DOT_SIZE) / 2;
	private static final int WAYPOINT_ARROW_WIDTH = 7;
	private static final int WAYPOINT_ARROW_HEIGHT = 5;
	private static final int WAYPOINT_ARROW_X_OFFSET = 1;
	private static final int WAYPOINT_ARROW_Y_OFFSET = 6;
	private static final Identifier WAYPOINT_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
	private static final Identifier WAYPOINT_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			return;
		}

		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float bearing = calculateBearing(player.getViewYRot(partialTick));

		Font font = minecraft.font;
		int centerX = graphics.guiWidth() / 2;
		int left = centerX - HUD_WIDTH / 2;
		int right = centerX + HUD_WIDTH / 2;

		graphics.fill(left, HUD_Y, right, HUD_Y + BAR_HEIGHT, BACKGROUND_COLOR);

		graphics.enableScissor(left, HUD_Y, right, HUD_Y + BAR_HEIGHT);
		renderTicks(graphics, centerX, bearing);
		renderDegreeLabels(graphics, font, centerX, bearing);
		renderDirectionLabels(graphics, font, centerX, bearing);
		graphics.disableScissor();

		renderCenterMarker(graphics, font, centerX);
		renderWaypoints(graphics, deltaTracker, minecraft, player, minecraft.level, centerX);
	}

	/**
	 * Converts Minecraft yaw (0 = south, 90 = west, 180 = north, 270 = east)
	 * into a standard compass bearing (0 = north, 90 = east, 180 = south, 270 = west).
	 */
	private static float calculateBearing(float yaw) {
		return wrapDegrees(yaw + 180.0f);
	}

	private static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360.0f;
		if (wrapped < 0.0f) {
			wrapped += 360.0f;
		}
		return wrapped;
	}

	/** Signed shortest angular distance from {@code bearing} to {@code angle}, in (-180, 180]. */
	private static float angularDifference(float angle, float bearing) {
		return wrapDegrees(angle - bearing + 180.0f) - 180.0f;
	}

	private static int angleToScreenX(float angle, float bearing, int centerX) {
		return centerX + Math.round(angularDifference(angle, bearing) * PIXELS_PER_DEGREE);
	}

	private static void renderTicks(GuiGraphicsExtractor graphics, int centerX, float bearing) {
		float halfRange = VISIBLE_HALF_RANGE_DEGREES + TICK_STEP_DEGREES;
		int tickBottom = HUD_Y + BAR_HEIGHT - 1;

		for (int degree = 0; degree < 360; degree += TICK_STEP_DEGREES) {
			if (Math.abs(angularDifference(degree, bearing)) > halfRange) {
				continue;
			}

			int height;
			int color;
			if (degree % MAJOR_TICK_STEP_DEGREES == 0) {
				height = MAJOR_TICK_HEIGHT;
				color = MAJOR_TICK_COLOR;
			} else if (degree % MEDIUM_TICK_STEP_DEGREES == 0) {
				height = MEDIUM_TICK_HEIGHT;
				color = MEDIUM_TICK_COLOR;
			} else {
				height = MINOR_TICK_HEIGHT;
				color = MINOR_TICK_COLOR;
			}

			int x = angleToScreenX(degree, bearing, centerX);
			graphics.fill(x, tickBottom - height, x + 1, tickBottom, color);
		}
	}

	/** Draws the numeric degree markings (e.g. 15, 30, 330, 345) between the cardinal/intercardinal letters. */
	private static void renderDegreeLabels(GuiGraphicsExtractor graphics, Font font, int centerX, float bearing) {
		float halfRange = VISIBLE_HALF_RANGE_DEGREES + LABEL_EDGE_MARGIN_DEGREES;

		for (int degree = 0; degree < 360; degree += MEDIUM_TICK_STEP_DEGREES) {
			if (degree % MAJOR_TICK_STEP_DEGREES == 0) {
				continue;
			}
			if (Math.abs(angularDifference(degree, bearing)) > halfRange) {
				continue;
			}

			int x = angleToScreenX(degree, bearing, centerX);
			graphics.centeredText(font, String.valueOf(degree), x, HUD_Y + 1, DEGREE_LABEL_COLOR);
		}
	}

	private static void renderDirectionLabels(GuiGraphicsExtractor graphics, Font font, int centerX, float bearing) {
		float halfRange = VISIBLE_HALF_RANGE_DEGREES + LABEL_EDGE_MARGIN_DEGREES;

		for (int i = 0; i < DIRECTION_LABELS.length; i++) {
			float angle = i * MAJOR_TICK_STEP_DEGREES;
			if (Math.abs(angularDifference(angle, bearing)) > halfRange) {
				continue;
			}

			boolean cardinal = i % 2 == 0;
			int x = angleToScreenX(angle, bearing, centerX);
			graphics.centeredText(font, DIRECTION_LABELS[i], x, HUD_Y + 1, cardinal ? CARDINAL_LABEL_COLOR : LABEL_COLOR);
		}
	}

	private static void renderCenterMarker(GuiGraphicsExtractor graphics, Font font, int centerX) {
		graphics.centeredText(font, CENTER_MARKER_GLYPH, centerX, HUD_Y + BAR_HEIGHT + 1, CENTER_MARKER_COLOR);
	}

	/**
	 * Draws other players (and any other transmitted waypoints, e.g. glowing mobs on a team) as a
	 * row of dots below the compass, mirroring the vanilla locator bar shown above the XP bar:
	 * same dot sprites/colors and up/down out-of-view arrows, driven by the same
	 * {@link ClientWaypointManager} the game already populates from the server. Waypoints are
	 * positioned via {@link TrackedWaypoint#yawAngleToCamera}, which returns the signed angle
	 * (in degrees, positive = clockwise) from the camera's current facing to the waypoint --
	 * exactly the quantity {@link #angularDifference} computes for compass ticks, so it plugs
	 * directly into the same {@code centerX + angle * PIXELS_PER_DEGREE} placement.
	 */
	private static void renderWaypoints(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Minecraft minecraft, LocalPlayer player, Level level, int centerX) {
		ClientWaypointManager waypointManager = player.connection.getWaypointManager();
		if (!waypointManager.hasWaypoints()) {
			return;
		}

		GameRenderer gameRenderer = minecraft.gameRenderer;
		Camera camera = gameRenderer.mainCamera();
		TickRateManager tickRateManager = level.tickRateManager();
		PartialTickSupplier partialTick = entity -> deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
		WaypointStyleManager waypointStyles = minecraft.gui.hud.getWaypointStyles();

		int left = centerX - HUD_WIDTH / 2;
		int right = centerX + HUD_WIDTH / 2;
		int top = WAYPOINT_ROW_Y - WAYPOINT_ARROW_Y_OFFSET - WAYPOINT_ARROW_HEIGHT;
		int bottom = WAYPOINT_ROW_Y + WAYPOINT_DOT_SIZE + WAYPOINT_ARROW_Y_OFFSET + WAYPOINT_ARROW_HEIGHT;

		graphics.enableScissor(left, top, right, bottom);
		waypointManager.forEachWaypoint(player, waypoint ->
			renderWaypoint(graphics, level, camera, gameRenderer, partialTick, waypointStyles, player, centerX, waypoint));
		graphics.disableScissor();
	}

	private static void renderWaypoint(GuiGraphicsExtractor graphics, Level level, Camera camera, GameRenderer gameRenderer,
			PartialTickSupplier partialTick, WaypointStyleManager waypointStyles, LocalPlayer player, int centerX, TrackedWaypoint waypoint) {
		if (waypoint.id().left().map(uuid -> uuid.equals(player.getUUID())).orElse(false)) {
			return;
		}

		double relativeAngle = waypoint.yawAngleToCamera(level, camera, partialTick);
		if (Math.abs(relativeAngle) > VISIBLE_HALF_RANGE_DEGREES) {
			return;
		}

		Waypoint.Icon icon = waypoint.icon();
		WaypointStyle style = waypointStyles.get(icon.style);
		float distance = Mth.sqrt((float) waypoint.distanceSquared(player));
		Identifier sprite = style.sprite(distance);
		int color = icon.color.orElseGet(() -> waypoint.id()
			.map(uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9f),
				name -> ARGB.setBrightness(ARGB.color(255, name.hashCode()), 0.9f)));

		int dotCenterX = centerX + Math.round((float) relativeAngle * PIXELS_PER_DEGREE);
		int dotLeft = dotCenterX - WAYPOINT_DOT_SIZE / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, dotLeft, WAYPOINT_ROW_Y, WAYPOINT_DOT_SIZE, WAYPOINT_DOT_SIZE, color);

		TrackedWaypoint.PitchDirection pitchDirection = waypoint.pitchDirectionToCamera(level, gameRenderer, partialTick);
		if (pitchDirection != TrackedWaypoint.PitchDirection.NONE) {
			boolean pointsDown = pitchDirection == TrackedWaypoint.PitchDirection.DOWN;
			Identifier arrowSprite = pointsDown ? WAYPOINT_ARROW_DOWN : WAYPOINT_ARROW_UP;
			int arrowOffset = pointsDown ? WAYPOINT_ARROW_Y_OFFSET : -WAYPOINT_ARROW_Y_OFFSET;
			int arrowY = WAYPOINT_ROW_Y + WAYPOINT_DOT_SIZE / 2 + arrowOffset - WAYPOINT_ARROW_HEIGHT / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, dotLeft + WAYPOINT_ARROW_X_OFFSET, arrowY, WAYPOINT_ARROW_WIDTH, WAYPOINT_ARROW_HEIGHT);
		}
	}
}
