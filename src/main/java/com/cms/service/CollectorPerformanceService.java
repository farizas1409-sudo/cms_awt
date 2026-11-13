package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.CollectorPerformanceRepository;
import java.sql.SQLException;
import java.util.Map;

public class CollectorPerformanceService {
    private static final Logger LOG = LogManager.getLogger(CollectorPerformanceService.class);

    private static final String PERMISSION = "OWNER_REPORT";
    private final CollectorPerformanceRepository repository = new CollectorPerformanceRepository();

    public Map<String, Object> load(
            Actor actor,
            String fromDate,
            String toDate,
            String district,
            String branch,
            String collector,
            String portfolio,
            String risk,
            int page,
            boolean searched)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (connection, data) -> repository.load(
                        connection, data, fromDate, toDate, district, branch, collector, portfolio, risk, page, searched));
    }
}
