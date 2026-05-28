package save;

import save.SaveDtos.SaveGameDto;

public interface SaveSerializer {

    String serialize(SaveGameDto saveGame) throws SaveGameException;

    SaveGameDto deserialize(String content) throws SaveGameException;
}
