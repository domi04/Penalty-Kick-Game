package com.deadball.core;

import com.deadball.utils.GameConstants;

/**
 * Counts where the player aimed (by locked aim X) into goal-mouth thirds. The level 3 adaptive
 * keeper uses a campaign-wide instance; zones use the aim line, not where the ball lands.
 */
public class ShotHistory {
    public enum Zone { LEFT, CENTER, RIGHT }

    private final int[] shotsByZone = new int[3];
    private int totalShots;

    public void clear() {
        for (int i = 0; i < 3; i++) {
            shotsByZone[i] = 0;
        }
        totalShots = 0;
    }

    public void record(double aimX) {
        int z = zoneOf(aimX).ordinal();
        shotsByZone[z]++;
        totalShots++;
    }

    public int totalShots() {
        return totalShots;
    }

    /**
     * Share of shots aimed into a zone (0..1). Returns uniform 1/3 when no shots are recorded.
     */
    public double shareOfShots(Zone zone) {
        if (totalShots == 0) {
            return 1.0 / 3.0;
        }
        return (double) shotsByZone[zone.ordinal()] / totalShots;
    }

    /**
     * Classify an aim X into a goal-mouth third. Clamped so out-of-frame aims still pick a side.
     */
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
