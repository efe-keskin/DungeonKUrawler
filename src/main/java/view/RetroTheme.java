package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Central place for retro dungeon styling: dark LAF defaults, shared fonts, and flat “arcade” buttons.
 */
public final class RetroTheme {

    /** Near-black stone background for frames and panels. */
    public static final Color BG_DUNGEON = new Color(12, 12, 18);
    /** Slightly lifted panel fill. */
    public static final Color BG_PANEL = new Color(22, 22, 30);
    /** Primary action (Start). */
    public static final Color BTN_PRIMARY = new Color(45, 90, 140);
    /** Secondary / info. */
    public static final Color BTN_SECONDARY = new Color(70, 65, 95);
    /** Warning / help. */
    public static final Color BTN_ACCENT = new Color(120, 85, 40);
    /** Danger (Exit). */
    public static final Color BTN_DANGER = new Color(130, 45, 45);

    public static Font UI_MONO;
    public static Font UI_MONO_SMALL;
    public static Font UI_TITLE_FONT;
    public static Font UI_SUBTITLE_FONT;

    private static final Border BTN_BORDER = BorderFactory.createEmptyBorder(6, 14, 6, 14);

    private RetroTheme() {
    }

    /**
     * Call once on the EDT before constructing any windows. Uses Metal with dark defaults for a
     * consistent retro shell; per-component colors still override where needed.
     */
    public static void installLookAndFeel() {
        try {
            try (InputStream fontStream = RetroTheme.class.getResourceAsStream("/fonts/Minecraft.ttf")) {
                if (fontStream != null) {
                    Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);
                    
                    UI_MONO = customFont.deriveFont(Font.PLAIN, 16f);
                    UI_MONO_SMALL = customFont.deriveFont(Font.PLAIN, 14f);
                    UI_TITLE_FONT = customFont.deriveFont(Font.PLAIN, 56f);
                    UI_SUBTITLE_FONT = customFont.deriveFont(Font.PLAIN, 20f);
                } else {
                    UI_MONO = new Font(Font.MONOSPACED, Font.BOLD, 14);
                    UI_MONO_SMALL = new Font(Font.MONOSPACED, Font.BOLD, 13);
                    UI_TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 42);
                    UI_SUBTITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);
                }
            } catch (Exception e) {
                System.err.println("Failed to load custom font: " + e.getMessage());
                UI_MONO = new Font(Font.MONOSPACED, Font.BOLD, 14);
                UI_MONO_SMALL = new Font(Font.MONOSPACED, Font.BOLD, 13);
                UI_TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 42);
                UI_SUBTITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);
            }

            UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.put("Panel.background", BG_DUNGEON);
            UIManager.put("Button.font", UI_MONO);
            UIManager.put("Label.font", UI_MONO);
            UIManager.put("TextArea.font", UI_MONO);
            UIManager.put("TextField.font", UI_MONO);
            UIManager.put("ToolTip.font", UI_MONO_SMALL);
            UIManager.put("Label.foreground", Color.WHITE);
        } catch (Exception e) {
            throw new IllegalStateException("Could not install retro LAF", e);
        }
    }

    public static void styleFrameDark(JFrame frame) {
        frame.getContentPane().setBackground(BG_DUNGEON);
    }

    public static void stylePanelDark(JPanel panel) {
        panel.setBackground(BG_PANEL);
    }

    /**
     * Flat retro button: bold white text, colored face, no focus ring, visible border.
     */
    public static void styleRetroButton(JButton button, Color background) {
        button.setFont(UI_MONO);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBorder(BTN_BORDER);
    }
}
