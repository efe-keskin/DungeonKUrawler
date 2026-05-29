package save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import save.SaveDtos.SaveDescriptor;
import save.SaveDtos.SaveGameDto;

/**
 * Repository: owns save-file naming, listing, and safe disk writes.
 */
public final class SaveFileRepository {

    private static final String SAVE_EXTENSION = ".json";
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path saveDirectory;
    private final SaveSerializer serializer;

    public SaveFileRepository(SaveSerializer serializer) {
        this(defaultSaveDirectory(), serializer);
    }

    public SaveFileRepository(Path saveDirectory, SaveSerializer serializer) {
        this.saveDirectory = Objects.requireNonNull(saveDirectory);
        this.serializer = Objects.requireNonNull(serializer);
    }

    public static Path defaultSaveDirectory() {
        return Path.of(System.getProperty("user.dir")).resolve("saves");
    }

    public SaveDescriptor write(SaveGameDto saveGame) throws SaveGameException {
        return writeTo(nextPath(saveGame.saveName), saveGame);
    }

    /**
     * Overwrites an existing save in place (same file path), without consuming
     * a new save slot. Used to persist progress back into the slot the player
     * loaded from. Falls back to a fresh slot if the descriptor has no path.
     */
    public SaveDescriptor overwrite(SaveDescriptor descriptor, SaveGameDto saveGame) throws SaveGameException {
        if (descriptor == null || descriptor.getPath() == null) {
            return write(saveGame);
        }
        return writeTo(descriptor.getPath(), saveGame);
    }

    private SaveDescriptor writeTo(Path destination, SaveGameDto saveGame) throws SaveGameException {
        try {
            Files.createDirectories(saveDirectory);
            Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
            try {
                Files.writeString(temp, serializer.serialize(saveGame), StandardCharsets.UTF_8);
                moveIntoPlace(temp, destination);
                return new SaveDescriptor(saveGame.saveName, saveGame.savedAt, destination);
            } catch (IOException | SaveGameException ex) {
                Files.deleteIfExists(temp);
                throw ex;
            }
        } catch (IOException ex) {
            throw new SaveGameException("Game could not be saved.", ex);
        }
    }

    public List<SaveDescriptor> list() throws SaveGameException {
        if (!Files.isDirectory(saveDirectory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(saveDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(SAVE_EXTENSION))
                    .map(this::descriptorIfReadable)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(SaveDescriptor::getSavedAt).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new SaveGameException("Saved games could not be listed.", ex);
        }
    }

    public SaveGameDto read(SaveDescriptor descriptor) throws SaveGameException {
        if (descriptor == null) {
            throw new SaveGameException("No save file was selected.");
        }
        return read(descriptor.getPath());
    }

    public SaveGameDto read(Path path) throws SaveGameException {
        try {
            return serializer.deserialize(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new SaveGameException("Save file could not be read.", ex);
        }
    }

    public void delete(SaveDescriptor descriptor) throws SaveGameException {
        if (descriptor == null || descriptor.getPath() == null) {
            throw new SaveGameException("No save file was selected.");
        }
        try {
            Files.deleteIfExists(descriptor.getPath());
        } catch (IOException ex) {
            throw new SaveGameException("Save file could not be deleted.", ex);
        }
    }

    private SaveDescriptor descriptorIfReadable(Path path) {
        try {
            SaveGameDto dto = serializer.deserialize(Files.readString(path, StandardCharsets.UTF_8));
            String name = dto.saveName == null || dto.saveName.isBlank()
                    ? path.getFileName().toString()
                    : dto.saveName;
            String savedAt = dto.savedAt == null || dto.savedAt.isBlank()
                    ? "unknown date"
                    : dto.savedAt;
            return new SaveDescriptor(name, savedAt, path);
        } catch (IOException | SaveGameException ignored) {
            return null;
        }
    }

    private Path nextPath(String saveName) {
        String safeName = sanitize(saveName);
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
        Path candidate = saveDirectory.resolve(safeName + "-" + timestamp + SAVE_EXTENSION);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = saveDirectory.resolve(safeName + "-" + timestamp + "-" + suffix + SAVE_EXTENSION);
            suffix++;
        }
        return candidate;
    }

    private static String sanitize(String saveName) {
        String source = saveName == null || saveName.isBlank() ? "save" : saveName;
        String sanitized = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "save" : sanitized;
    }

    private static void moveIntoPlace(Path temp, Path destination) throws IOException {
        try {
            Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
