package save;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import engine.GameEngine;
import save.SaveDtos.SaveDescriptor;
import save.SaveDtos.SaveGameDto;

/**
 * Facade for the save/load use case.
 */
public final class SaveGameService {

    public static final int MAX_SAVE_FILES = 10;

    private final SaveFileRepository repository;
    private final GameStateMapper mapper;

    public SaveGameService() {
        this(new SaveFileRepository(new GsonSaveSerializer()), new GameStateMapper());
    }

    public SaveGameService(SaveFileRepository repository, GameStateMapper mapper) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public SaveDescriptor saveGame(GameEngine engine, String saveName) throws SaveGameException {
        if (saveName == null || saveName.isBlank()) {
            throw new SaveGameException("Save name is required.");
        }
        if (repository.list().size() >= MAX_SAVE_FILES) {
            throw new SaveLimitExceededException(MAX_SAVE_FILES);
        }
        SaveGameDto dto = new SaveGameDto();
        dto.saveName = saveName.trim();
        dto.savedAt = OffsetDateTime.now().toString();
        dto.gameState = mapper.toDto(engine);
        return repository.write(dto);
    }

    public List<SaveDescriptor> listSaves() throws SaveGameException {
        return repository.list();
    }

    public GameEngine loadGame(SaveDescriptor descriptor) throws SaveGameException {
        return mapper.toEngine(repository.read(descriptor));
    }

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        repository.delete(descriptor);
    }
}
