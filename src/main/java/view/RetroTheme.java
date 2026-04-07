package view;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
    /** Primary action (Start, Login). */
    public static final Color BTN_PRIMARY = new Color(45, 90, 140);
    /** Secondary / info. */
    public static final Color BTN_SECONDARY = new Color(70, 65, 95);
    /** Warning / help. */
    public static final Color BTN_ACCENT = new Color(120, 85, 40);
    /** Danger (Exit). */
    public static final Color BTN_DANGER = new Color(130, 45, 45);

    public static final Font UI_MONO = new Font(Font.MONOSPACED, Font.BOLD, 14);
    public static final Font UI_MONO_SMALL = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private static final Border BTN_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 230), 2),
            BorderFactory.createEmptyBorder(6, 14, 6, 14));

    private RetroTheme() {
    }

    /**
     * Call once on the EDT before constructing any windows. Uses Metal with dark defaults for a
     * consistent retro shell; per-component colors still override where needed.
     */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.put("Panel.background", BG_DUNGEON);
            UIManager.put("Label.font", UI_MONO);
            UIManager.put("Label.foreground", Color.WHITE);
            UIManager.put("TextField.background", new Color(35, 35, 45));
            UIManager.put("TextField.foreground", Color.WHITE);
            UIManager.put("TextField.caretForeground", Color.WHITE);
            UIManager.put("PasswordField.background", new Color(35, 35, 45));
            UIManager.put("PasswordField.foreground", Color.WHITE);
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

    public static void styleLabel(JLabel label) {
        label.setFont(UI_MONO_SMALL);
        label.setForeground(Color.WHITE);
    }

    public static void styleTextField(JTextField field) {
        field.setFont(UI_MONO_SMALL);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
    }
}
