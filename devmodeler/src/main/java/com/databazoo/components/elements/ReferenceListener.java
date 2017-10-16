package com.databazoo.components.elements;

/**
 * References may register to the main object as listeners. They will get notified of operations like resize, recache, etc.
 */
@FunctionalInterface
public interface ReferenceListener {

    void notifyReference();
}
