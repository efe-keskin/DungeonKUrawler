package model;

/**
 * Role for any collectible whose contents can be viewed via {@link ItemAction#READ}.
 *
 * <p>Both {@link Book} (the win-condition clue) and {@link Scroll} (functional
 * readables like {@link ShadowCloneScroll}) implement this, so effects and the
 * UI can treat "readable" uniformly without conflating books and scrolls into a
 * single class hierarchy.
 */
public interface Readable {

    /** The text revealed when this object is read. */
    String read();
}
