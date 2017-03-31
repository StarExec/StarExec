package org.starexec.data.to.tuples;

// Interface for primitives that have a filepath (can be "located")
public interface Locatable {
    String getPath();
    void setPath(String path);
}
