package com.kevinandrey.proxy;

/** Confidentiality level of a {@link Document}. */
public enum Classification {
    /** Any authenticated user may read it. */
    PUBLIC,
    /** Only users with the {@link Role#AUDITOR} role may read it. */
    CONFIDENTIAL
}
