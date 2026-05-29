package engine;

import java.util.Objects;

import model.DungeonMap;

/**
 * GRASP Controller for the "Run current map in Team Match mode" use case.
 *
 * <p>The view asks this controller to start Team Match, while game creation is
 * delegated to {@link TeamMatchGameFactory}. Later Team Match validation and
 * setup rules should be added here instead of in the Swing view.
 */
public final class TeamMatchController {

    private final TeamMatchGameFactory gameFactory;
    private final TeamMatchMapValidator mapValidator;

    public TeamMatchController() {
        this(new TeamMatchGameFactory(), new TeamMatchMapValidator());
    }

    TeamMatchController(TeamMatchGameFactory gameFactory, TeamMatchMapValidator mapValidator) {
        this.gameFactory = Objects.requireNonNull(gameFactory);
        this.mapValidator = Objects.requireNonNull(mapValidator);
    }

    public GameEngine startFromDesignMap(DungeonMap designMap) {
        TeamMatchMapValidator.Result validation = mapValidator.validate(designMap);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.message());
        }
        return gameFactory.createFromDesignMap(designMap);
    }
}
