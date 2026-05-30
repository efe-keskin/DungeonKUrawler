package model;

/**
 * Adapter that lets empty searchable wall fixtures participate in target-item
 * missions without overwriting a key or other hidden item.
 */
public final class SearchableObjectHidingPlace implements HidingPlace {

    private final SearchableObject searchableObject;

    public SearchableObjectHidingPlace(SearchableObject searchableObject) {
        if (searchableObject == null) {
            throw new IllegalArgumentException("searchableObject must not be null");
        }
        this.searchableObject = searchableObject;
    }

    @Override
    public boolean canHide(Item item) {
        return item != null && searchableObject.getHiddenItem() == null;
    }

    @Override
    public boolean hide(Item item) {
        if (!canHide(item)) {
            return false;
        }
        searchableObject.setHiddenItem(item);
        return true;
    }

    @Override
    public String describe() {
        return searchableObject.getName();
    }

    public SearchableObject getSearchableObject() {
        return searchableObject;
    }
}
