package com.kevinandrey.proxy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Caching proxy: remembers recent answers in a bounded least recently used cache.
 *
 * <p>Misses are cached too, so repeated lookups of an unknown id do not reach the real repository.
 */
public final class CachingRepository implements DocumentRepository {

    private final DocumentRepository target;
    private final Map<String, Optional<Document>> cache;
    private long hits;
    private long misses;

    /**
     * Wraps a repository.
     *
     * @param target repository to cache
     * @param capacity maximum number of remembered ids, at least 1
     */
    public CachingRepository(DocumentRepository target, int capacity) {
        this.target = Objects.requireNonNull(target, "target");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, got " + capacity);
        }
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Optional<Document>> eldest) {
                return size() > capacity;
            }
        };
    }

    @Override
    public synchronized Optional<Document> findById(String id) {
        Optional<Document> cached = cache.get(id);
        if (cached != null) {
            hits++;

            return cached;
        }
        misses++;
        Optional<Document> loaded = target.findById(id);
        cache.put(id, loaded);

        return loaded;
    }

    /**
     * Counts answers served from the cache.
     *
     * @return cache hits
     */
    public synchronized long hits() {
        return hits;
    }

    /**
     * Counts answers that had to be loaded.
     *
     * @return cache misses
     */
    public synchronized long misses() {
        return misses;
    }

    /**
     * Counts the ids currently remembered.
     *
     * @return cache size, never above the capacity
     */
    public synchronized int size() {
        return cache.size();
    }
}
