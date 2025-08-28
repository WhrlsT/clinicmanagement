/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt;

/**
 *
 * @author Whrl
 */
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
@SuppressWarnings("unchecked")
public class CustomADT<T> implements ADTInterface<T> {
    // ===== Inner minimal functional/iterable types =====
    public interface ADTPredicate<U> { boolean test(U value); }
    public interface ADTComparator<U> { int compare(U a, U b); }
    public interface ADTIterator<U> { boolean hasNext(); U next(); }

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

    // ===== QUEUE-STYLE HELPERS (generic) =====
    // Minimal helpers that remain generic and don't depend on domain classes.
    
    /**
     * Add an entry and automatically position it based on a `priority` field if present
     */
    @Override
    public void enqueue(T entry) {
        add(entry);
        bubbleUp(size - 1);
    }
    
    // Type-specific helpers like findNextIndex/reposition should be implemented outside this ADT.
    
    /** Find an entry by its ID (requires T to have getId()) */
    @Override
    public T findEntry(String id) {
        for (int i = 0; i < size; i++) {
            String curId = getIdValue(data[i]);
            if (curId != null && curId.equals(id)) return data[i];
        }
        return null;
    }
    
    /** Find the index of an entry by its ID (requires T to have getId()) */
    @Override
    public int indexOf(String id) {
        for (int i = 0; i < size; i++) {
            String curId = getIdValue(data[i]);
            if (curId != null && curId.equals(id)) return i;
        }
        return -1;
    }
    
    // No domain-specific repositioning in a generic ADT.
    
    /** Bubble up using `getPriority()` if T provides it; stable otherwise */
    @Override
    public void bubbleUp(int index) {
        if (index <= 0 || index >= size) return;
        while (index > 0) {
            int prev = index - 1;
            int prevPr = getPriorityValue(data[prev]);
            int curPr = getPriorityValue(data[index]);
            if (prevPr < curPr) { swap(prev, index); index = prev; } else break;
        }
    }

    // Try to extract a priority int via reflection (generic)
    private int getPriorityValue(Object o) {
        if (o == null) return Integer.MIN_VALUE;
        // Use reflection to find getPriority()
        try {
            java.lang.reflect.Method m = o.getClass().getMethod("getPriority");
            Object v = m.invoke(o);
            if (v instanceof Integer i) return i;
        } catch (Exception ignored) {}
        return Integer.MIN_VALUE;
    }

    // Try to extract an id String via reflection (generic)
    private String getIdValue(Object o) {
        if (o == null) return null;
        try {
            java.lang.reflect.Method m = o.getClass().getMethod("getId");
            Object v = m.invoke(o);
            if (v instanceof String s) return s;
        } catch (Exception ignored) {}
        return null;
    }

    // ===== Generic searching/sorting/iteration exposed by CustomADT =====
    /** Find first index where predicate is true; -1 if none. */
    public int findIndex(ADTPredicate<T> predicate) {
        if (predicate == null) return -1;
        for (int i = 0; i < size; i++) {
            if (predicate.test(data[i])) return i;
        }
        return -1;
    }

    /** JDK Predicate-based findIndex overload. */
    public int findIndex(Predicate<? super T> predicate) {
        if (predicate == null) return -1;
        for (int i = 0; i < size; i++) {
            if (predicate.test(data[i])) return i;
        }
        return -1;
    }

    /** In-place stable insertion sort using provided comparator. */
    public void sort(ADTComparator<T> comparator) {
        if (comparator == null || size <= 1) return;
        for (int i = 1; i < size; i++) {
            T key = data[i];
            int j = i - 1;
            while (j >= 0 && comparator.compare(data[j], key) > 0) {
                data[j + 1] = data[j];
                j--;
            }
            data[j + 1] = key;
        }
    }

    /** In-place stable insertion sort using java.util.Comparator. */
    public void sort(Comparator<? super T> comparator) {
        if (comparator == null) return;
        sort(new ADTComparator<T>() { 
            public int compare(T a, T b){ 
                return comparator.compare(a,b); 
            } 
        });
    }

    /** Default sort using natural ordering if elements implement Comparable. */
    @SuppressWarnings({"rawtypes"})
    public void sort() {
        if (size <= 1) return;
        sort(new ADTComparator<T>() {
            public int compare(T a, T b) {
                if (a == null && b == null) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                return ((Comparable) a).compareTo(b);
            }
        });
    }

    /** Simple bubble sort (stable), useful for teaching/demo. */
    public void bubbleSort(ADTComparator<T> comparator) {
        if (comparator == null || size <= 1) return;
        boolean swapped;
        for (int pass = 0; pass < size - 1; pass++) {
            swapped = false;
            for (int i = 0; i < size - 1 - pass; i++) {
                if (comparator.compare(data[i], data[i + 1]) > 0) {
                    T tmp = data[i]; data[i] = data[i + 1]; data[i + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    /** Bubble sort using java.util.Comparator. */
    public void bubbleSort(Comparator<? super T> comparator) {
        if (comparator == null) return;
        bubbleSort(new ADTComparator<T>() { public int compare(T a, T b){ return comparator.compare(a,b); } });
    }

    /** Selection sort (not stable) with comparator. */
    public void selectionSort(ADTComparator<T> comparator) {
        if (comparator == null || size <= 1) return;
        for (int i = 0; i < size - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < size; j++) {
                if (comparator.compare(data[j], data[minIdx]) < 0) minIdx = j;
            }
            if (minIdx != i) {
                T tmp = data[i]; data[i] = data[minIdx]; data[minIdx] = tmp;
            }
        }
    }

    /** Selection sort using java.util.Comparator. */
    public void selectionSort(Comparator<? super T> comparator) {
        if (comparator == null) return;
        selectionSort(new ADTComparator<T>() { public int compare(T a, T b){ return comparator.compare(a,b); } });
    }

    /** Stable merge sort using provided comparator (O(n log n)). */
    public void mergeSort(ADTComparator<T> comparator) {
        if (comparator == null || size <= 1) return;
        T[] aux = (T[]) new Object[size];
        mergeSortInternal(0, size - 1, comparator, aux);
    }

    /** Stable merge sort using java.util.Comparator. */
    public void mergeSort(Comparator<? super T> comparator) {
        if (comparator == null) return;
        mergeSort(new ADTComparator<T>() { public int compare(T a, T b){ return comparator.compare(a,b); } });
    }

    private void mergeSortInternal(int left, int right, ADTComparator<T> cmp, T[] aux) {
        if (left >= right) return;
        int mid = (left + right) >>> 1;
        mergeSortInternal(left, mid, cmp, aux);
        mergeSortInternal(mid + 1, right, cmp, aux);
        // Optimization: if already in order, skip merge
        if (cmp.compare(data[mid], data[mid + 1]) <= 0) return;
        merge(left, mid, right, cmp, aux);
    }

    private void merge(int left, int mid, int right, ADTComparator<T> cmp, T[] aux) {
        for (int k = left; k <= right; k++) aux[k] = data[k];
        int i = left, j = mid + 1, idx = left;
        while (i <= mid && j <= right) {
            if (cmp.compare(aux[i], aux[j]) <= 0) data[idx++] = aux[i++];
            else data[idx++] = aux[j++];
        }
        while (i <= mid) data[idx++] = aux[i++];
        while (j <= right) data[idx++] = aux[j++];
    }

    /** Sort using natural ordering when elements implement Comparable. */
    @SuppressWarnings({"rawtypes"})
    public void sortComparable() {
        if (size <= 1) return;
        for (int i = 1; i < size; i++) {
            T key = data[i];
            int j = i - 1;
            while (j >= 0 && ((Comparable) data[j]).compareTo(key) > 0) {
                data[j + 1] = data[j];
                j--;
            }
            data[j + 1] = key;
        }
    }

    /** Binary search on a sorted array according to the same comparator. Returns index or -insertionPoint-1. */
    public int binarySearch(T key, ADTComparator<T> comparator) {
        if (comparator == null || size == 0) return -1;
        int low = 0, high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = comparator.compare(data[mid], key);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -low - 1; // insertion point encoding
    }

    /** Binary search overload using java.util.Comparator. */
    public int binarySearch(T key, Comparator<? super T> comparator) {
        if (comparator == null) return -1;
        return binarySearch(key, new ADTComparator<T>() { public int compare(T a, T b){ return comparator.compare(a,b); } });
    }

    /** Lightweight iterator over the current elements (0..size-1). */
    public ADTIterator<T> iterator() {
        return new ADTIterator<T>() {
            private int idx = 0;
            public boolean hasNext() { return idx < size; }
            public T next() { return data[idx++]; }
        };
    }

    /** JDK Iterator over the current elements. */
    public Iterator<T> toIterator() {
        return new Iterator<T>() {
            private int idx = 0;
            public boolean hasNext() { return idx < size; }
            public T next() { return data[idx++]; }
        };
    }

    /** Convenience adapter for enhanced-for: for (T x : list.asIterable()) { ... } */
    public Iterable<T> asIterable() { return this::toIterator; }
    
    /** Return a new CustomADT containing elements that satisfy the predicate. */
    public CustomADT<T> filter(ADTPredicate<T> predicate) {
        CustomADT<T> out = new CustomADT<>();
        if (predicate == null) return out;
        for (int i = 0; i < size; i++) if (predicate.test(data[i])) out.add(data[i]);
        return out;
    }

    /** JDK Predicate-based filter overload. */
    public CustomADT<T> filter(Predicate<? super T> predicate) {
        CustomADT<T> out = new CustomADT<>();
        if (predicate == null) return out;
        for (int i = 0; i < size; i++) if (predicate.test(data[i])) out.add(data[i]);
        return out;
    }
    
}