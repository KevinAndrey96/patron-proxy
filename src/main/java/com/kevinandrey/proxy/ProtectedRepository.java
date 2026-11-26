package com.kevinandrey.proxy;

import java.util.Objects;
import java.util.Optional;

/**
 * Protection proxy: lets a user read public documents and keeps confidential ones for auditors.
 *
 * <p>The classification is only known once the document is loaded, so the check runs on the answer.
 * Unknown ids stay empty instead of being refused, which would leak nothing either way.
 */
public final class ProtectedRepository implements DocumentRepository {

    private final DocumentRepository target;
    private final User user;

    /**
     * Wraps a repository for one user.
     *
     * @param target repository to guard
     * @param user caller whose roles are checked
     */
    public ProtectedRepository(DocumentRepository target, User user) {
        this.target = Objects.requireNonNull(target, "target");
        this.user = Objects.requireNonNull(user, "user");
    }

    @Override
    public Optional<Document> findById(String id) {
        Optional<Document> document = target.findById(id);
        if (document.isPresent() && !mayRead(document.get())) {
            throw new AccessDeniedException(user.name(), id);
        }

        return document;
    }

    private boolean mayRead(Document document) {
        return document.classification() == Classification.PUBLIC || user.has(Role.AUDITOR);
    }
}
