package com.deadball.core;

import com.deadball.utils.GameConstants;

/**
 * Tracks where the player has aimed and whether the shot scored during the current level.
 * The adaptive goalkeeper (level 3) and the aim-zone heatmap both read from this. Zones are
 * classified by the locked aim X (what the human actually chose), not by where the ball landed.
 */
public class ShotHistory {
    public enum Zone { LEFT, CENTER, RIGHT }

    private final int[] shotsByZone = new int[3];
    private final int[] goalsByZone = new int[3];
    private int totalShots;
    private int totalGoals;

    /** Wipe all tracking (called when a new level starts). */
    public void clear() {
        for (int i = 0; i < 3; i++) {
            shotsByZone[i] = 0;
            goalsByZone[i] = 0;
        }
        totalShots = 0;
        totalGoals = 0;
    }

    /** Add one shot to the history. {@code goal} is true only when it counted as a scored goal. */
    public void record(double aimX, boolean goal) {
        int z = zoneOf(aimX).ordinal();
        shotsByZone[z]++;
        totalShots++;
        if (goal) {
            goalsByZone[z]++;
            totalGoals++;
        }
    }

    public int shotsIn(Zone zone) {
        return shotsByZone[zone.ordinal()];
    }

    public int goalsIn(Zone zone) {
        return goalsByZone[zone.ordinal()];
    }

    public int totalShots() {
        return totalShots;
    }

    public int totalGoals() {
        return totalGoals;
    }

    /** Share of shots aimed into a zone (0..1). Returns uniform 1/3 when no shots are recorded. */
    public double shareOfShots(Zone zone) {
        if (totalShots == 0) {
            return 1.0 / 3.0;
        }
        return (double) shotsByZone[zone.ordinal()] / totalShots;
    }

    /** Classify an aim X into a goal-mouth third. Clamped so out-of-frame aims still pick a side. */
    public static Zone zoneOf(double aimX) {
        double left = GameConstants.GOAL_LEFT;
        double right = GameConstants.GOAL_RIGHT;
        double span = Math.max(1.0, right - left);
        double t = Math.max(0.0, Math.min(1.0, (aimX - left) / span));
        if (t < 1.0 / 3.0) {
            return Zone.LEFT;
        }
        if (t > 2.0 / 3.0) {
            return Zone.RIGHT;
        }
        return Zone.CENTER;
    }
}
