package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class ReportsService {
    private static final Logger LOG = LogManager.getLogger(ReportsService.class);

    private static final String PERMISSION = "COLLECT";
    private final ReportsRepository repository = new ReportsRepository();

    public Map<String, Object> load(
            Actor actor, int page, String query, String risk, String portfolio) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, actor.id(), page, query, risk, portfolio);
                });
    }
}
