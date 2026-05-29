package engine.audio;

import engine.CombatManager;
import engine.GameEventListener;
import engine.MissionListener;
import model.Entity;
import model.Item;
import model.ValuableItem;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Observer that translates game events into sound effects via
 * javax.sound.sampled. Missing files log warnings and become silent no-ops.
 */
public class AudioManager implements GameEventListener, MissionListener {

    // Limited-scope singleton: the view layer needs a global handle for button
    // click sounds (JButtons don't naturally have access to the engine).
    // Engine-driven events still use listener wiring; this is only for UI clicks.
    private static AudioManager INSTANCE;

    private final Map<String, byte[]> samples = new HashMap<>();
    private Clip musicClip;
    private boolean musicMuted = false;
    private static final float MUSIC_GAIN_DB = -10.0f;

    public AudioManager() {
        INSTANCE = this;
        preload("button_click");
        preload("pickup");
        preload("hero_attack");
        preload("hit_taken");
        preload("enemy_defeat");
        preload("victory");
        preload("defeat");
        preloadMusic();
    }

    public static AudioManager shared() {
        if (INSTANCE == null) {
            INSTANCE = new AudioManager();
        }
        return INSTANCE;
    }

    private void preload(String name) {
        String path = "/audio/" + name + ".wav";
        try (InputStream in = AudioManager.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("[audio] missing: " + path);
                return;
            }
            samples.put(name, in.readAllBytes());
        } catch (IOException e) {
            System.err.println("[audio] could not read " + path + ": " + e.getMessage());
        }
    }

    private void preloadMusic() {
        String path = "/audio/menu_theme.wav";
        try (InputStream in = AudioManager.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("[audio] missing: " + path);
                return;
            }
            byte[] data = in.readAllBytes();
            AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            musicClip = AudioSystem.getClip();
            musicClip.open(stream);
            stream.close();
            applyMusicGain();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[audio] could not load menu music: " + e.getMessage());
            musicClip = null;
        }
    }

    public void play(String name) {
        byte[] data = samples.get(name);
        if (data == null) {
            return;
        }
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            stream.close();
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[audio] play(" + name + ") failed: " + e.getMessage());
        }
    }

    /** Starts the main menu theme on a loop. No-op if already playing or unavailable. */
    public void startMenuMusic() {
        if (musicClip == null || musicMuted || musicClip.isRunning()) {
            return;
        }
        musicClip.setFramePosition(0);
        musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** Stops the menu music if playing. No-op otherwise. */
    public void stopMenuMusic() {
        if (musicClip == null || !musicClip.isRunning()) {
            return;
        }
        musicClip.stop();
    }

    /** Returns whether music is currently muted (true) or audible (false). */
    public boolean isMusicMuted() {
        return musicMuted;
    }

    /** Returns true if a music track is loaded and available. */
    public boolean isMusicAvailable() {
        return musicClip != null;
    }

    /**
     * Toggles music on/off. The mute flag persists so re-entering the menu
     * while muted does not auto-resume.
     */
    public void toggleMusicMute() {
        musicMuted = !musicMuted;
        if (musicMuted && musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
        } else if (!musicMuted && musicClip != null) {
            musicClip.setFramePosition(0);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void applyMusicGain() {
        if (musicClip == null) {
            return;
        }
        try {
            FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(MUSIC_GAIN_DB);
        } catch (IllegalArgumentException e) {
            // System has no master gain control; skip volume adjustment.
        }
    }

    @Override
    public void onHeroAttack(CombatManager.AttackResult result) {
        play("hero_attack");
    }

    @Override
    public void onHeroTookDamage(CombatManager.AttackResult result) {
        play("hit_taken");
    }

    @Override
    public void onEnemyDefeated(Entity enemy) {
        play("enemy_defeat");
    }

    @Override
    public void onItemPickedUp(Item item) {
        play("pickup");
    }

    @Override
    public void onHeroDefeated() {
        play("defeat");
    }

    @Override
    public void onButtonClick() {
        play("button_click");
    }

    @Override
    public void onMissionWon(ValuableItem target) {
        play("victory");
    }
}
