package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class AllocationService {
    private static final Logger LOG = LogManager.getLogger(AllocationService.class);

    private static final String PERMISSION = "ALLOCATE";
    private final AllocationRepository repository = new AllocationRepository();

    public Map<String, Object> load(Actor actor) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data);
                });
    }

    public String run(Actor actor, String scope, Long branchId) throws SQLException {
        LOG.info("Service flow started: run()");
        return ScreenService.write(
                actor,
                PERMISSION,
                "ALLOCATE",
                c -> {
                    var result = repository.run(c, scope, branchId);
                    return "Allocation complete. Processed: "
                            + result.processed()
                            + ", allocated: "
                            + result.allocated()
                            + ", skipped: "
                            + result.skipped()
                            + ", failed: "
                            + result.failed()
                            + ".";
                });
    }
}
