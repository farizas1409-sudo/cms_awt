package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class DashboardService {
    private static final Logger LOG = LogManager.getLogger(DashboardService.class);

    private static final String PERMISSION = "COLLECT";
    private final DashboardRepository repository = new DashboardRepository();

    public Map<String, Object> load(Actor actor) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, actor.id());
                });
    }
}
