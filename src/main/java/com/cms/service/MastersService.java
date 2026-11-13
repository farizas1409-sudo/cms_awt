package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class MastersService {
    private static final Logger LOG = LogManager.getLogger(MastersService.class);

    private static final String PERMISSION = "MASTER_MANAGE";
    private final MastersRepository repository = new MastersRepository();

    public Map<String, Object> load(Actor actor, String entity, boolean fetch) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, entity, fetch);
                });
    }

    public void save(
            Actor actor,
            String entity,
            Long districtId,
            Long regionId,
            Long stateId,
            String reference,
            String name)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "MASTER_CREATE",
                c -> {
                    repository.save(c, entity, districtId, regionId, stateId, reference, name);
                    return "Created " + entity + " master: " + name + ".";
                });
    }
}
