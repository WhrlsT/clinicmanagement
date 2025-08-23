package adt;

import entity.PatientQueueEntry;
import entity.QueueStatus;

/**
 * Priority Queue interface specifically for PatientQueueEntry management
 * Extends basic ADT functionality with queue-specific operations
 */
public interface PriorityQueueInterface<T extends PatientQueueEntry> extends ADTInterface<T> {
    
    /**
     * Add an entry and automatically position it based on priority
     * Higher priority entries bubble up to the front
     */
    void enqueue(T entry);
    
    /**
     * Find the next available entry for a specific doctor or any doctor
     * @param doctorId specific doctor ID, or null for any doctor
     * @return index of next available entry, or -1 if none found
     */
    int findNextIndex(String doctorId);
    
    /**
     * Find an entry by its ID
     * @param id the queue entry ID
     * @return the entry, or null if not found
     */
    T findEntry(String id);
    
    /**
     * Find the index of an entry by its ID
     * @param id the queue entry ID
     * @return the index, or -1 if not found
     */
    int indexOf(String id);
    
    /**
     * Reposition an entry after it has been called to maintain queue order
     * Moves called entries after other called/in-progress entries
     * @param index the index of the entry to reposition
     */
    void repositionAfterCalled(int index);
    
    /**
     * Bubble up an entry based on priority
     * Higher priority entries move toward the front
     * @param index the index to start bubbling from
     */
    void bubbleUp(int index);
    
    /**
     * Count entries by status
     * @param status the status to count
     * @return number of entries with that status
     */
    int countByStatus(QueueStatus status);
}
