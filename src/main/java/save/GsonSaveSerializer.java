package save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import save.SaveDtos.SaveGameDto;

/**
 * Strategy implementation for the required JSON save format.
 */
public final class GsonSaveSerializer implements SaveSerializer {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String serialize(SaveGameDto saveGame) throws SaveGameException {
        if (saveGame == null) {
            throw new SaveGameException("Cannot serialize an empty save.");
        }
        return gson.toJson(saveGame);
    }

    @Override
    public SaveGameDto deserialize(String content) throws SaveGameException {
        try {
            SaveGameDto dto = gson.fromJson(content, SaveGameDto.class);
            if (dto == null || dto.gameState == null) {
                throw new SaveGameException("Save file does not contain game state.");
            }
            return dto;
        } catch (JsonSyntaxException ex) {
            throw new SaveGameException("Save file is not valid JSON.", ex);
        }
    }
}
