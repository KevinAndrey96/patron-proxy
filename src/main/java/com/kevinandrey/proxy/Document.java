package com.kevinandrey.proxy;

import java.util.Objects;

/**
 * A document kept in the archive.
 *
 * @param id unique identifier, never blank
 * @param title human readable title
 * @param classification who may read it
 */
public record Document(String id, String title, Classification classification) {

    /** Validates the components. */
    public Document {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(classification, "classification");
        if (id.isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
    }
}
