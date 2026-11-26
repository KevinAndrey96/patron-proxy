package com.kevinandrey.proxy;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Virtual proxy: creates the real repository on the first lookup instead of at wiring time.
 *
 * <p>Initialization uses double-checked locking on a volatile field, so concurrent first calls still
 * build the target exactly once.
 */
public final class LazyRepository implements DocumentRepository {

    private final Supplier<? extends DocumentRepository> factory;
    private volatile DocumentRepository target;

    /**
     * Wraps a factory.
     *
     * @param factory builds the real repository; called at most once
     */
    public LazyRepository(Supplier<? extends DocumentRepository> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public Optional<Document> findById(String id) {
        return target().findById(id);
    }

    /**
     * Tells whether the real repository exists yet.
     *
     * @return {@code true} after the first lookup
     */
    public boolean isInitialized() {
        return target != null;
    }

    private DocumentRepository target() {
        DocumentRepository current = target;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (target == null) {
                target = Objects.requireNonNull(factory.get(), "factory returned null");
            }

            return target;
        }
    }
}
