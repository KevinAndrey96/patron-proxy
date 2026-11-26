package com.kevinandrey.proxy;

/** Roles checked by the {@link ProtectedRepository}. */
public enum Role {
    /** Reads public documents. */
    READER,
    /** Reads public and confidential documents. */
    AUDITOR
}
