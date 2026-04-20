package com.deadball.audio;

import javafx.scene.media.AudioClip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Thin audio wrapper around {@link AudioClip}. Scans {@code assets/sounds/} for a known set of
 * filenames; any missing file is simply skipped so the game still runs silently without audio
 * assets. Drop matching {@code .wav} / {@code .mp3} files into the sounds directory to enable SFX.
 */
public final class SoundManager {

    public enum Clip {
        KICK("kick.wav"),
        GOAL("goal.wav"),
        SAVE("save.wav"),
        MISS("miss.wav"),
        LEVEL_COMPLETE("level_complete.wav"),
        LEVEL_FAILED("level_failed.wav"),
        CAMPAIGN_COMPLETE("campaign_complete.wav");

        private final String filename;

        Clip(String filename) {
            this.filename = filename;
        }

        public String filename() {
            return filename;
        }
    }

    private final Map<Clip, AudioClip> loaded = new EnumMap<>(Clip.class);
    private boolean enabled = true;

    public SoundManager(Path assetsDir) {
        if (assetsDir != null && Files.isDirectory(assetsDir)) {
            for (Clip c : Clip.values()) {
                Path file = assetsDir.resolve(c.filename());
                if (Files.isRegularFile(file)) {
                    try {
                        loaded.put(c, new AudioClip(file.toUri().toString()));
                    } catch (RuntimeException ignored) {
                        // Codec / decode errors are non-fatal: the game stays silent for that clip.
                    }
                }
            }
        }
    }

    public void play(Clip clip) {
        if (!enabled) {
            return;
        }
        AudioClip ac = loaded.get(clip);
        if (ac != null) {
            ac.play();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasAnyLoaded() {
        return !loaded.isEmpty();
    }
}
