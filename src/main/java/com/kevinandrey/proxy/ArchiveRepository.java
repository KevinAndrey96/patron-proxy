package com.kevinandrey.proxy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The real subject: an in-memory archive.
 *
 * <p>In production this would open a connection and read from storage, which is what makes it worth
 * deferring, caching and guarding. Every lookup is counted so tests and the demo can show how many
 * requests actually reached it.
 */
public final class ArchiveRepository implements DocumentRepository {

    private final Map<String, Document> documents;
    private final AtomicInteger lookups = new AtomicInteger();

    /**
     * Builds an archive.
     *
     * @param documents contents, ids must be unique
     */
    public ArchiveRepository(Collection<Document> documents) {
        Map<String, Document> byId = new LinkedHashMap<>();
        for (Document document : Objects.requireNonNull(documents, "documents")) {
            if (byId.putIfAbsent(document.id(), document) != null) {
                throw new IllegalArgumentException("duplicate document id " + document.id());
            }
        }
        this.documents = Map.copyOf(byId);
    }

    /**
     * Builds the archive used by the demo.
     *
     * @return three documents, one of them confidential
     */
    public static ArchiveRepository sample() {
        return new ArchiveRepository(List.of(
                new Document("INF-2018", "Informe anual 2018", Classification.PUBLIC),
                new Document("NOM-2018", "Nómina de diciembre 2018", Classification.CONFIDENTIAL),
                new Document("MAN-001", "Manual de usuario", Classification.PUBLIC)));
    }

    @Override
    public Optional<Document> findById(String id) {
        lookups.incrementAndGet();

        return Optional.ofNullable(documents.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * Counts the lookups served so far.
     *
     * @return number of {@link #findById(String)} calls
     */
    public int lookups() {
        return lookups.get();
    }
}
