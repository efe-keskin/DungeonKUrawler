package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.Column;
import model.Arch;
import model.Ring;
import model.RingEffectType;
import model.ValuableItem;
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

    @Test
    void breakableHiddenMissionTargetRoundTripPreservesObjectiveIdentity() {
        ItemDtoFactory factory = new ItemDtoFactory();
        ValuableItem target = new ValuableItem("Golden Idol", "/items/valuable_items/golden_idol_64x64.png");
        Column column = new Column();
        column.setHiddenItem(target);
        ItemDtoFactory.RestoreContext context = new ItemDtoFactory.RestoreContext();

        ItemDto dto = factory.toDto(column, target);
        Column restored = assertInstanceOf(Column.class, factory.fromDto(dto, context));

        assertInstanceOf(ValuableItem.class, restored.getHiddenItem());
        assertSame(restored.getHiddenItem(), context.missionTarget);
    }

    @Test
    void keyedOpenArchRoundTripPreservesExitState() {
        ItemDtoFactory factory = new ItemDtoFactory();
        Arch arch = new Arch("exit-bent-silver");
        arch.open();

        ItemDto dto = factory.toDto(arch, null);
        Arch restored = assertInstanceOf(Arch.class, factory.fromDto(dto, null));

        assertEquals("exit-bent-silver", restored.getRequiredKeyId());
        assertTrue(restored.isOpen());
    }
}
