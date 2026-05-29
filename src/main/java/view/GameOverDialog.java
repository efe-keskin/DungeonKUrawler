package view;

import java.awt.Component;

/**
 * Custom retro-styled defeat dialog. Kept as a small facade so gameplay code
 * doesn't depend on Swing implementation details.
 */
public final class GameOverDialog {

    private GameOverDialog() {
    }

    public static void show(Component parent) {
        // Uses the same visual language as MissionSplashDialog/ItemActionMenuDialog.
        show(parent, "DEFEAT", "Your HP reached 0.");
    }

    public static void show(Component parent, String heading, String message) {
        // Uses the same visual language as MissionSplashDialog/ItemActionMenuDialog.
        ItemActionMenuDialog.show(parent, "Game Over", heading, message, "Return to Menu");
    }
}
