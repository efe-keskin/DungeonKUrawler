package engine;

import javax.swing.Timer;

/**
 * Engine-owned update loop for periodic gameplay ticks that should not live in the view.
 */
public class GameLoopController {

    private final Timer timer;

    public GameLoopController(GameEngine engine, int intervalMs) {
        this.timer = new Timer(intervalMs, e -> engine.tickEnergyRefill());
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }
}
