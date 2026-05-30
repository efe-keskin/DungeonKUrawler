package view;

import java.awt.Component;

import model.ValuableItem;

/**
 * Mission-lifecycle splash screens. Implemented as a thin facade on
 * {@link ItemActionMenuDialog} so the retro styling matches every other
 * notice in the game and we keep one source of truth for popup chrome.
 */
public final class MissionSplashDialog {

    private MissionSplashDialog() {
    }

    /**
     * Opening splash that tells the player which valuable to chase. Modal, so
     * the caller is paused until the player dismisses it.
     */
    public static void showFindThis(Component parent, ValuableItem target) {
        String title = target == null ? "Mission" : target.getName();
        String body = target == null
                ? "Explore the dungeon and survive."
                : "Find this valuable hidden somewhere in the dungeon.\n"
                        + "Search chests and wall fixtures, claim it, then unlock the exit when one guards the way.";
        ItemActionMenuDialog.show(parent, "Mission Briefing", "FIND: " + title, body, "Begin");
    }

    /** Win screen shown after the hero exits through the opened arch. */
    public static void showVictory(Component parent, ValuableItem target) {
        String title = "Victory!";
        String body = target == null
                ? "You recovered the lost valuable. The dungeon yields its secret."
                : "You recovered the " + target.getName() + ".\n"
                        + "The dungeon yields its secret. The mission is complete.";
        ItemActionMenuDialog.show(parent, "Mission Complete", title, body, "Continue");
    }
}
