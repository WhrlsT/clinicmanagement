package adt;

/**
 * Interface for entries that have priority levels
 * Used for priority-based operations in queue systems
 */
public interface Prioritizable {
    /**
     * Get the priority level of this entry
     * @return priority level (higher numbers = higher priority)
     */
    int getPriority();
}
