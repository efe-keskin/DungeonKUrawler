package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import model.DungeonMap;
import model.HidingPlace;
import model.Item;
import model.ValuableItem;

/**
 * Tracks the "find this valuable" objective.
 *
 * <p>Pure Fabrication (GRASP): the gameplay objective lives in its own class,
 * decoupled from {@link model.Hero}, {@link DungeonMap}, and the view. The
 * mission is the Information Expert for its own win condition — outside code
 * only forwards pickup events through {@link #checkPickup(Item)}.
 *
 * <p>Observer (GoF): {@link MissionListener}s receive start/won callbacks so
 * the UI can react without polling.
 */
public final class TargetItemMission {

    private final List<MissionListener> listeners = new CopyOnWriteArrayList<>();

    private ValuableItem target;
    private HidingPlace hidingPlace;
    private boolean started;
    private boolean won;

    public void addListener(MissionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(MissionListener listener) {
        listeners.remove(listener);
    }

    public ValuableItem getTarget() {
        return target;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isWon() {
        return won;
    }

    /**
     * Picks a random hiding place from {@code provider}, stashes {@code target}
     * inside it, and flips the mission to "started." No-op when the mission has
     * already begun.
     *
     * @return true when the target was successfully placed.
     */
    public boolean start(HidingPlaceProvider provider, DungeonMap map,
                         Random random, ValuableItem target) {
        if (started || provider == null || map == null || target == null) {
            return false;
        }
        Random r = random == null ? new Random() : random;

        List<HidingPlace> candidates = new ArrayList<>();
        for (HidingPlace place : provider.collectHidingPlaces(map)) {
            if (place.canHide(target)) {
                candidates.add(place);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }

        HidingPlace pick = candidates.get(r.nextInt(candidates.size()));
        if (!pick.hide(target)) {
            return false;
        }
        this.target = target;
        this.hidingPlace = pick;
        this.started = true;
        System.out.println("[mission] target '" + target.getName()
                + "' hidden in " + pick.describe());
        for (MissionListener listener : listeners) {
            listener.onMissionStart(target);
        }
        return true;
    }

    /**
     * Called by the engine after every successful pickup (ground or container).
     * Identity comparison: the win fires only on the exact instance hidden at
     * start — a lookalike with the same name would not satisfy it.
     */
    public void checkPickup(Item picked) {
        if (won || !started || target == null || picked != target) {
            return;
        }
        won = true;
        System.out.println("[mission] target collected — victory");
        for (MissionListener listener : listeners) {
            listener.onMissionWon(target);
        }
    }

    public String hidingPlaceDescription() {
        return hidingPlace == null ? null : hidingPlace.describe();
    }
}
