package view;

/**
 * Receives passive gameplay feedback that should not interrupt the player with
 * a modal dialog.
 */
@FunctionalInterface
public interface GameNoticeSink {

    void showNotice(String title, String message);
}
