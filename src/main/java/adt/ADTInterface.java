/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt;

/**
 *
 * @author Whrl
 */

public interface ADTInterface<T> {

    // List Implementation
    void add(T element);                // Add element to end
    void add(int index, T element);     // Add element at index
    T get(int index);                   // Get element at index
    T set(int index, T element);        // Replace element at index
    void swap(int i, int j);            // Swap elements at indices i and j
    T remove(int index);                // Remove element at index
    boolean remove(T element);          // Remove first occurrence
    int size();                         // Number of elements
    boolean isEmpty();                  // Is list empty?
    void clear();                       // Remove all elements

    // TODO: Any other methods?
}