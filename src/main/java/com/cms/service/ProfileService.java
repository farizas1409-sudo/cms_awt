package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class ProfileService {
    private static final Logger LOG = LogManager.getLogger(ProfileService.class);

    private static final String PERMISSION = null;
    private final ProfileRepository repository = new ProfileRepository();

    public Map<String, Object> load(Actor actor) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(actor, PERMISSION, (c, data) -> {});
    }

    public void save(Actor actor, String currentPassword, String password) throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "SAVE_profile",
                c -> {
                    repository.save(c, actor.id(), currentPassword, password);
                    return "Changes saved.";
                });
    }
}
