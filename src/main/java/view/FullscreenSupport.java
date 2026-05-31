package view;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * Adds an F11 fullscreen toggle to any JFrame. Single static method:
 * each window calls install(this) once.
 *
 * <p>Uses Swing's input/action map on the root pane so the binding is
 * window-local and doesn't conflict with focused-component handlers.
 *
 * <p>Stores the previous bounds on the root pane's client property
 * before going fullscreen so toggling back restores the exact size.
 * Pixel-art Swing doesn't scale, so fullscreen displays the game
 * centered at its native size with black space around.
 */
public final class FullscreenSupport {

    private FullscreenSupport() {}

    public static void install(JFrame frame) {
        JComponent rootPane = frame.getRootPane();
        KeyStroke f11 = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(f11, "toggleFullscreen");
        rootPane.getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggle(frame);
            }
        });
    }

    private static void toggle(JFrame frame) {
        Object stateObj = frame.getRootPane().getClientProperty("fullscreen.bounds");
        if (stateObj instanceof Rectangle savedBounds) {
            frame.dispose();
            frame.setUndecorated(false);
            frame.setBounds(savedBounds);
            frame.setVisible(true);
            frame.getRootPane().putClientProperty("fullscreen.bounds", null);
        } else {
            Rectangle saved = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            Rectangle screen = device.getDefaultConfiguration().getBounds();
            frame.setBounds(screen);
            frame.setVisible(true);
            frame.getRootPane().putClientProperty("fullscreen.bounds", saved);
        }
    }
}
