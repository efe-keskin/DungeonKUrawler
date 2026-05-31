package view;

import java.awt.Window;

/**
 * GoF Strategy for leaving a gameplay window. Scenario floors return to the
 * tower map, while custom games return to the main menu.
 */
public interface GameReturnStrategy {

    String menuLabel();

    void returnFrom(Window gameplayWindow);
}
