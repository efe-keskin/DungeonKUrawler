package engine;

import java.awt.event.KeyEvent;

/**
 * Logical move direction for {@link GameEngine#moveHero(Direction)}. Key-code mapping lives here
 * so the view only forwards raw input; it does not implement movement rules.
 */
public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    /**
     * Maps arrow keys and WASD to a direction; {@code null} if the key is not a movement key.
     */
    public static Direction fromKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> UP;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> DOWN;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> LEFT;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> RIGHT;
            default -> null;
        };
    }
}
