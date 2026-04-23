package com.example.pr_1_file_dupe.utils;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {

    private static boolean soundEnabled = true;
    private static double volume = 0.5;

    public enum Sound {
        // 🔥 FIXED: Pointing to the .mp3 files you generated
        SCAN_START("scan_start.mp3"),
        SCAN_COMPLETE("scan_compleate.mp3"),
        DELETE_FILE("delete.mp3"),
        BUTTON_CLICK("click.mp3"),
        ERROR("error.mp3"),
        SUCCESS("succes.mp3"),
        NAVIGATION("nav.mp3");

        private final String filename;
        Sound(String filename) { this.filename = filename; }
        public String getFilename() { return filename; }
    }

    public static void play(Sound sound) {
        if (!soundEnabled) return;
        try {
<<<<<<< HEAD
            URL resource = SoundManager.class.getResource(
                    "/com/example/pr_1_file_dupe/sound/" + sound.getFilename()
            );

=======
            // 🔥 FIXED: Pointing to the "sound/" folder (no 's' at the end)
            URL resource = SoundManager.class.getResource("/com/example/pr_1_file_dupe/sound/" + sound.getFilename());
>>>>>>> 056546b (some sound work)
            if (resource == null) {
                System.out.println("❌ Sound file not found: " + sound.getFilename());
                return;
            }
            AudioClip clip = new AudioClip(resource.toExternalForm());
            clip.setVolume(volume);
            clip.play();
        } catch (Exception e) {
            System.out.println("❌ Sound error: " + sound.getFilename());
        }
    }

    public static void playAsync(Sound sound) { Platform.runLater(() -> play(sound)); }
    public static void setSoundEnabled(boolean enabled) { soundEnabled = enabled; }
    public static boolean isSoundEnabled() { return soundEnabled; }
    public static void setVolume(double vol) { volume = Math.max(0.0, Math.min(1.0, vol)); }
    public static double getVolume() { return volume; }
}