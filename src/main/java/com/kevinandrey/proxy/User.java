package com.kevinandrey.proxy;

import java.util.Objects;
import java.util.Set;

/**
 * The caller on whose behalf a {@link ProtectedRepository} answers.
 *
 * @param name display name used in error messages
 * @param roles granted roles, copied defensively
 */
public record User(String name, Set<Role> roles) {

    /** Validates the components and freezes the roles. */
    public User {
        Objects.requireNonNull(name, "name");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("user name must not be blank");
        }
    }

    /**
     * Tells whether the user holds a role.
     *
     * @param role role to look for
     * @return {@code true} when granted
     */
    public boolean has(Role role) {
        return roles.contains(role);
    }
}
