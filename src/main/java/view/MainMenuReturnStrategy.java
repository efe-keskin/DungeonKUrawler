package view;

import java.awt.Window;

import javax.swing.SwingUtilities;

/** Default gameplay exit: close the game and reopen the main menu. */
public final class MainMenuReturnStrategy implements GameReturnStrategy {

    @Override
    public String menuLabel() {
        return "Return to Main Menu";
    }

    @Override
    public void returnFrom(Window gameplayWindow) {
        if (gameplayWindow != null) {
            gameplayWindow.dispose();
        }
        SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
    }
}
