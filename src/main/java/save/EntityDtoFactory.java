package save;

import model.AIState;
import model.Entity;
import model.Knight;
import model.Sorcerer;
import save.SaveDtos.EntityDto;

/**
 * Factory for serializing and restoring enemy entities.
 */
final class EntityDtoFactory {

    private static final String KNIGHT = "KNIGHT";
    private static final String SORCERER = "SORCERER";

    EntityDto toDto(Entity entity) {
        if (entity == null) {
            return null;
        }
        EntityDto dto = new EntityDto();
        dto.x = entity.getX();
        dto.y = entity.getY();
        dto.name = entity.getName();

        if (entity instanceof Knight knight) {
            dto.type = KNIGHT;
            dto.hp = knight.getHp();
            dto.str = knight.getStr();
            dto.def = knight.getDef();
            dto.visionRange = knight.getVisionRange();
            dto.aiState = knight.getAiState().name();
        } else if (entity instanceof Sorcerer sorcerer) {
            dto.type = SORCERER;
            dto.hp = sorcerer.getHp();
            dto.maxHp = sorcerer.getMaxHp();
            dto.mana = sorcerer.getMana();
            dto.def = sorcerer.getDef();
            dto.hasMagicRing = sorcerer.isHasMagicRing();
            dto.panicTeleportUsed = sorcerer.isPanicTeleportUsed();
            dto.aiState = sorcerer.getAiState().name();
        }
        return dto;
    }

    Entity fromDto(EntityDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (fallback(dto.type, "")) {
            case KNIGHT -> restoreKnight(dto);
            case SORCERER -> restoreSorcerer(dto);
            default -> null;
        };
    }

    private Knight restoreKnight(EntityDto dto) {
        Knight knight = new Knight(dto.x, dto.y, fallback(dto.name, "Knight"),
                positive(dto.hp, 20), dto.str, dto.def, positive(dto.visionRange, 5));
        knight.setAiState(parseAiState(dto.aiState));
        return knight;
    }

    private Sorcerer restoreSorcerer(EntityDto dto) {
        int maxHp = positive(dto.maxHp, positive(dto.hp, 10));
        Sorcerer sorcerer = new Sorcerer(dto.x, dto.y, fallback(dto.name, "Sorcerer"),
                maxHp, dto.mana, dto.def, dto.hasMagicRing);
        sorcerer.setHp(positive(dto.hp, maxHp));
        sorcerer.setPanicTeleportUsed(dto.panicTeleportUsed);
        sorcerer.setAiState(parseAiState(dto.aiState));
        return sorcerer;
    }

    private static AIState parseAiState(String value) {
        if (value == null || value.isBlank()) {
            return AIState.ROAMING;
        }
        try {
            return AIState.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return AIState.ROAMING;
        }
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
