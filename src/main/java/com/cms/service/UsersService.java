package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class UsersService {
    private static final Logger LOG = LogManager.getLogger(UsersService.class);

    private static final String PERMISSION = "USER_MANAGE";
    private final UsersRepository repository = new UsersRepository();

    public Map<String, Object> load(Actor actor, Long id, int page) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, id, page);
                });
    }

    public void save(
            Actor actor,
            String status,
            Long id,
            Long districtId,
            Long branchId,
            Long roleId,
            String name,
            String password,
            String username)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                id == null ? "USER_CREATE" : "USER_UPDATE",
                c -> {
                    String normalizedName = name == null ? "" : name.trim();
                    if (normalizedName.isEmpty())
                        throw new IllegalArgumentException("Enter a display name");
                    if (repository.displayNameExists(c, normalizedName, id))
                        throw new IllegalArgumentException("Display name must be unique");
                    Long oldRoleId = id == null ? null : repository.roleId(c, id);
                    repository.save(
                            c,
                            status,
                            id,
                            districtId,
                            branchId,
                            roleId,
                            normalizedName,
                            password,
                            username);
                    if (id != null && oldRoleId != null && !oldRoleId.equals(roleId))
                        ScreenService.audit(
                                c,
                                actor.id(),
                                "USER_ROLE_CHANGE",
                                "userId=" + id + "; roleId=" + oldRoleId + " -> " + roleId);
                    return id == null
                            ? "Created user " + username + " with role " + roleId + "."
                            : "Updated user " + username + ".";
                });
    }
}
