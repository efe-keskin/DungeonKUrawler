package main;

import javax.swing.SwingUtilities;

import view.MainMenuWindow;
import view.RetroTheme;

/**
 * Application entry: starts the Swing UI on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }
/*Starter of the code (TESTING) */ 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RetroTheme.installLookAndFeel();
            MainMenuWindow w = new MainMenuWindow();
            w.setVisible(true);
        });
    }
}
