package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RolesService {
    private static final Logger LOG = LogManager.getLogger(RolesService.class);

    private static final String PERMISSION = "ROLE_MANAGE";
    private final RolesRepository repository = new RolesRepository();

    public Map<String, Object> load(Actor actor, Long selectedRoleId) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, selectedRoleId);
                });
    }

    public void save(Actor actor, String mode, Long roleId, String name, List<String> permissions)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "CREATE".equals(mode) ? "ROLE_CREATED" : "ROLE_PERMISSIONS_UPDATED",
                c -> {
                    return "CREATE".equals(mode)
                            ? repository.create(c, name)
                            : repository.update(c, roleId, permissions);
                });
    }
}
