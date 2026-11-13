package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class OwnerService {
    private static final Logger LOG = LogManager.getLogger(OwnerService.class);

    private static final String PERMISSION = "OWNER_REPORT";
    private final OwnerRepository repository = new OwnerRepository();

    public Map<String, Object> load(
            Actor actor, String district, String branch, String reportFrom, String reportTo, boolean search)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, district, branch, reportFrom, reportTo, search);
                });
    }
}
