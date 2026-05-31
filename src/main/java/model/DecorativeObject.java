package model;

import java.util.List;

/**
 * Non-interactive map dressing placed by build mode: rugs, signs, torches,
 * stairs, floor patches, and similar art-only fixtures.
 */
public class DecorativeObject extends StaticObject {

    private final String spriteResource;

    public DecorativeObject(String name, boolean blocking, String spriteResource) {
        super(name, blocking);
        this.spriteResource = spriteResource;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of();
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
