package adt;

/**
 * Interface for queue entries that can be in different states
 * Used for status-based operations and filtering
 */
public interface Statusable<S> {
    /**
     * Get the current status of this entry
     * @return the current status
     */
    S getStatus();
}
