package com.kevinandrey.proxy;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Smart reference: counts how many times each id is requested, the job the 2018 version of this
 * repository did with two integer fields.
 */
public final class CountingRepository implements DocumentRepository {

    private final DocumentRepository target;
    private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();

    /**
     * Wraps a repository.
     *
     * @param target repository whose traffic is counted
     */
    public CountingRepository(DocumentRepository target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public Optional<Document> findById(String id) {
        counts.computeIfAbsent(Objects.requireNonNull(id, "id"), key -> new LongAdder()).increment();

        return target.findById(id);
    }

    /**
     * Counts the requests for one id.
     *
     * @param id document identifier
     * @return requests so far, zero when never asked
     */
    public long count(String id) {
        LongAdder adder = counts.get(id);

        return adder == null ? 0 : adder.sum();
    }

    /**
     * Counts every request.
     *
     * @return total requests
     */
    public long total() {
        return counts.values().stream().mapToLong(LongAdder::sum).sum();
    }

    /**
     * Copies the counters.
     *
     * @return requests per id, sorted by id
     */
    public Map<String, Long> snapshot() {
        Map<String, Long> copy = new TreeMap<>();
        counts.forEach((id, adder) -> copy.put(id, adder.sum()));

        return copy;
    }
}
