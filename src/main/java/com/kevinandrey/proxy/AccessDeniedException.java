package com.kevinandrey.proxy;

/** Thrown by the {@link ProtectedRepository} when a user may not read a document. */
public final class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String documentId;

    /**
     * Creates the exception.
     *
     * @param userName who asked
     * @param documentId what was refused
     */
    public AccessDeniedException(String userName, String documentId) {
        super("user " + userName + " may not read document " + documentId);
        this.userName = userName;
        this.documentId = documentId;
    }

    /**
     * Returns the refused user.
     *
     * @return user name
     */
    public String userName() {
        return userName;
    }

    /**
     * Returns the refused document.
     *
     * @return document id
     */
    public String documentId() {
        return documentId;
    }
}
