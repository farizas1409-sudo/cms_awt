package com.cms.model;

import java.util.Set;

public record Actor(long id, String username, String displayName, String role, Set<String> permissions) {
    public Actor {
        permissions = Set.copyOf(permissions);
    }

    public void require(String permission) {
        if (permission != null && !permissions.contains(permission))
            throw new SecurityException("Permission denied");
    }
}
