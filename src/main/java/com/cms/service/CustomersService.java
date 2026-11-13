package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class CustomersService {
    private static final Logger LOG = LogManager.getLogger(CustomersService.class);

    private static final String PERMISSION = "COLLECT";
    private final CustomersRepository repository = new CustomersRepository();

    public Map<String, Object> load(
            Actor actor,
            int page,
            String q,
            String risk,
            String productFilter,
            String promiseDate,
            String nextActionDate,
            String outcome)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(
                            c,
                            data,
                            actor.id(),
                            page,
                            q,
                            risk,
                            productFilter,
                            promiseDate,
                            nextActionDate,
                            outcome);
                });
    }
}
