package com.cms.model;

import java.util.Set;

public record Identity(
        long id,
        String username,
        String displayName,
        String role,
        Number branchId,
        boolean mustChangePassword,
        Set<String> permissions) {
    public Identity {
        permissions = Set.copyOf(permissions);
    }
}
