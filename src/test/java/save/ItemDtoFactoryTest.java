package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.Ring;
import model.RingEffectType;
import model.Vase;
import save.SaveDtos.ItemDto;

import org.junit.jupiter.api.Test;

class ItemDtoFactoryTest {

    @Test
    void brokenVaseRoundTripRestoresBrokenState() {
        ItemDtoFactory factory = new ItemDtoFactory();
        Vase vase = new Vase();
        vase.breakApart();

        ItemDto dto = factory.toDto(vase, null);
        Vase restored = assertInstanceOf(Vase.class, factory.fromDto(dto, null));

        assertTrue(restored.isBroken());
        assertEquals(Vase.BROKEN_SPRITE, restored.spriteResource());
        assertFalse(restored.isBlocking());
        assertTrue(restored.getInventoryActions().isEmpty());
    }

    @Test
    void ringRoundTripPreservesEffectTypeAndBonus() {
        ItemDtoFactory factory = new ItemDtoFactory();
        Ring ring = new Ring("Power Ring", RingEffectType.STRENGTH, 3,
                "/items/rings/10_ring_red_gem.png");

        ItemDto dto = factory.toDto(ring, null);
        Ring restored = assertInstanceOf(Ring.class, factory.fromDto(dto, null));

        assertEquals(RingEffectType.STRENGTH, restored.getEffectType());
        assertEquals(3, restored.getBonus());
        assertEquals("/items/rings/10_ring_red_gem.png", restored.spriteResource());
    }
}
