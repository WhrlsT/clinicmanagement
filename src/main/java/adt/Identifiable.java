package adt;

/**
 * Interface for entries that have unique identifiers
 * Used for ID-based lookup operations
 */
public interface Identifiable {
    /**
     * Get the unique identifier for this entry
     * @return the unique ID
     */
    String getId();
}
