/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt;

import entity.PatientQueueEntry;

/**
 *
 * @author Whrl
 */
@SuppressWarnings("unchecked")
public class CustomADT<T> implements ADTInterface<T> {
    private T[] data;
    private int size;
    private static final int INITIAL_CAPACITY = 10;

    public CustomADT() {
        data = (T[]) new Object[INITIAL_CAPACITY];
        size = 0;
    }

    private void ensureCapacity() {
        if (size >= data.length) {
            T[] newData = (T[]) new Object[data.length * 2];
            for (int i = 0; i < data.length; i++) {
                newData[i] = data[i];
            }
            data = newData;
        }
    }

    @Override
    public void add(T element) {
        ensureCapacity();
        data[size++] = element;
    }

    @Override
    public void add(int index, T element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        ensureCapacity();
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
        }
        data[index] = element;
        size++;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return data[index];
    }

    @Override
    public T set(int index, T element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        T old = data[index];
        data[index] = element;
        return old;
    }

    @Override
    public void swap(int i, int j) {
        if (i < 0 || i >= size || j < 0 || j >= size) {
            throw new IndexOutOfBoundsException();
        }
        T temp = data[i];
        data[i] = data[j];
        data[j] = temp;
    }

    @Override
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        T removed = data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        data[size - 1] = null;
        size--;
        return removed;
    }

    @Override
    public boolean remove(T element) {
        for (int i = 0; i < size; i++) {
            if ((data[i] == null && element == null) || (data[i] != null && data[i].equals(element))) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            data[i] = null;
        }
        size = 0;
    }

    // ===== QUEUE-SPECIFIC OPERATIONS =====
    // These operations work with any type that implements the appropriate interfaces:
    // - Prioritizable: for priority-based operations (enqueue, bubbleUp)
    // - Identifiable: for ID-based lookups (findEntry, indexOf) 
    // - Statusable<S>: for status-based operations (findNextIndex, repositionAfterCalled, countByStatus)
    //
    // Examples: PatientQueueEntry, TaskEntry, or any custom entry type
    
    /**
     * Add an entry and automatically position it based on priority
     * Works with any type that implements Prioritizable
     */
    @Override
    public void enqueue(T entry) {
        add(entry);
        if (entry instanceof Prioritizable) {
            bubbleUp(size - 1);
        }
    }
    
    /**
     * Find the next available entry for a specific doctor or any doctor
     * Works with types that have doctor preference and status
     * @param doctorId specific doctor ID, or null for any doctor
     * @return index of next available entry, or -1 if none found
     */
    @Override
    public int findNextIndex(String doctorId) {
        // Priority order already handled by array order (higher priority bubbled up)
        for (int i = 0; i < size; i++) {
            if (data[i] instanceof PatientQueueEntry) {
                PatientQueueEntry e = (PatientQueueEntry) data[i];
                if (e.getStatus().toString().equals("WAITING")) {
                    if (doctorId == null) return i; // any
                    // match doctor or ANY
                    if (e.getPreferredDoctorId() == null || e.getPreferredDoctorId().equals(doctorId)) return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Find an entry by its ID
     * Works with any type that implements Identifiable
     * @param id the entry ID
     * @return the entry, or null if not found
     */
    @Override
    public T findEntry(String id) {
        for (int i = 0; i < size; i++) {
            if (data[i] instanceof Identifiable) {
                Identifiable e = (Identifiable) data[i];
                if (e.getId().equals(id)) return data[i];
            }
        }
        return null;
    }
    
    /**
     * Find the index of an entry by its ID
     * Works with any type that implements Identifiable
     * @param id the entry ID
     * @return the index, or -1 if not found
     */
    @Override
    public int indexOf(String id) {
        for (int i = 0; i < size; i++) {
            if (data[i] instanceof Identifiable) {
                Identifiable e = (Identifiable) data[i];
                if (e.getId().equals(id)) return i;
            }
        }
        return -1;
    }
    
    /**
     * Reposition an entry after it has been started to maintain queue order
     * Moves in-progress entries after other in-progress entries
     * @param index the index of the entry to reposition
     */
    @Override
    public void repositionAfterCalled(int index) {
        if (index < 0 || index >= size) return;
        
        // Find target insert position - works for any status-based entry
        int target = 0;
        for (int i = 0; i < size; i++) {
            if (data[i] instanceof PatientQueueEntry) {
                PatientQueueEntry entry = (PatientQueueEntry) data[i];
                String status = entry.getStatus().toString();
                if (status.equals("IN_PROGRESS")) target = i + 1; 
                else break;
            }
        }
        if (index < target) return; // already in place
        
        // Extract and shift
        T temp = data[index];
        for (int i = index; i > target; i--) {
            data[i] = data[i - 1];
        }
        data[target] = temp;
    }
    
    /**
     * Bubble up an entry based on priority
     * Works with any type that implements Prioritizable
     * @param index the index to start bubbling from
     */
    @Override
    public void bubbleUp(int index) {
        if (index <= 0 || index >= size) return;
        
        while (index > 0) {
            int prev = index - 1;
            if (data[prev] instanceof Prioritizable && data[index] instanceof Prioritizable) {
                Prioritizable prevEntry = (Prioritizable) data[prev];
                Prioritizable currentEntry = (Prioritizable) data[index];
                
                if (prevEntry.getPriority() < currentEntry.getPriority()) {
                    swap(prev, index);
                    index = prev;
                } else break;
            } else break;
        }
    }
    
    /**
     * Count entries by status
     * Works with any type that implements Statusable
     * @param status the status to count
     * @return number of entries with that status
     */
    @Override
    public <S> int countByStatus(S status) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (data[i] instanceof Statusable) {
                Statusable<?> e = (Statusable<?>) data[i];
                if (e.getStatus().equals(status)) count++;
            }
        }
        return count;
    }
}