package com.kevinandrey.proxy;

import java.util.Optional;

/**
 * Read access to archived documents.
 *
 * <p>This is the <em>subject</em> of the pattern: the real repository and every proxy implement it, so
 * clients never know which one they hold and proxies can be stacked in any order.
 */
public interface DocumentRepository {

    /**
     * Looks a document up.
     *
     * @param id document identifier
     * @return the document, or empty when the archive has no such id
     */
    Optional<Document> findById(String id);
}
