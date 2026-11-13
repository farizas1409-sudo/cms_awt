package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Identity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class IdentityRepository {
    private static final Logger LOG = LogManager.getLogger(IdentityRepository.class);

    public Optional<Map<String, Object>> findCredentials(Connection connection, String username)
            throws SQLException {
        LOG.info("Repository flow started: findCredentials()");
        return Db.rows(
                        connection,
                        """
                        SELECT u.id,u.password,u.must_change_password
                        FROM cms_user u JOIN cms_role r ON r.id=u.role_id
                        WHERE u.active=1 AND r.active=1 AND LOWER(u.username)=LOWER(?)
                        """,
                        username)
                .stream()
                .findFirst();
    }

    public Optional<Identity> findActiveIdentity(Connection connection, long id)
            throws SQLException {
        LOG.info("Repository flow started: findActiveIdentity()");
        var rows =
                Db.rows(
                        connection,
                        """
                        SELECT u.username,u.display_name,u.branch_id,u.must_change_password,r.name role_name,p.code
                        FROM cms_user u
                        JOIN cms_role r ON r.id=u.role_id
                        LEFT JOIN cms_role_permission rp ON rp.role_id=r.id
                        LEFT JOIN cms_permission p ON p.id=rp.permission_id
                        WHERE u.id=? AND u.active=1 AND r.active=1
                        """,
                        id);
        if (rows.isEmpty()) return Optional.empty();
        var permissions = new HashSet<String>();
        for (var row : rows) {
            if (row.get("CODE") instanceof String permission) permissions.add(permission);
        }
        var user = rows.get(0);
        return Optional.of(
                new Identity(
                        id,
                        (String) user.get("USERNAME"),
                        (String) user.get("DISPLAY_NAME"),
                        (String) user.get("ROLE_NAME"),
                        (Number) user.get("BRANCH_ID"),
                        ((Number) user.get("MUST_CHANGE_PASSWORD")).intValue() == 1,
                        permissions));
    }
}
