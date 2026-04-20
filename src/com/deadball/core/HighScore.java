package com.deadball.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persists the player's best campaign result as a single integer (total goals scored across all
 * {@code LEVEL_COUNT} levels when the campaign was completed). Lives in the user's home directory
 * so it survives between runs without bundling save data into the project.
 */
public final class HighScore {
    /** Persisted file (hidden dotfile in the user's home directory). */
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".deadball_highscore");

    private int bestCampaignGoals;

    public HighScore() {
        this.bestCampaignGoals = 0;
    }

    /** Load previously saved best score; on any error, silently fall back to zero. */
    public void load() {
        try {
            if (!Files.isRegularFile(FILE)) {
                bestCampaignGoals = 0;
                return;
            }
            String raw = new String(Files.readAllBytes(FILE)).trim();
            bestCampaignGoals = Math.max(0, Integer.parseInt(raw));
        } catch (IOException | NumberFormatException e) {
            bestCampaignGoals = 0;
        }
    }

    /**
     * Persist {@code goals} if it improves on the current best.
     *
     * @return true when a new best was written to disk.
     */
    public boolean submitCampaignResult(int goals) {
        if (goals <= bestCampaignGoals) {
            return false;
        }
        bestCampaignGoals = goals;
        try {
            Files.write(FILE, Integer.toString(bestCampaignGoals).getBytes());
            return true;
        } catch (IOException e) {
            // Best effort; keep the in-memory value even if disk write fails.
            return false;
        }
    }

    public int getBestCampaignGoals() {
        return bestCampaignGoals;
    }
}
