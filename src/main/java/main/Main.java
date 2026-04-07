package main;

import javax.swing.SwingUtilities;

import view.LoginWindow;
import view.RetroTheme;

/**
 * Application entry: starts the Swing UI on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RetroTheme.installLookAndFeel();
            LoginWindow w = new LoginWindow();
            w.setVisible(true);
        });
    }
}
