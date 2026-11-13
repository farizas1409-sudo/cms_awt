package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class RiskService {
    private static final Logger LOG = LogManager.getLogger(RiskService.class);

    private static final String PERMISSION = "RISK_EXECUTE";
    private final RiskRepository repository = new RiskRepository();

    public Map<String, Object> load(
            Actor actor, String riskFilter, Long portfolioFilter, boolean search, Long editId)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(c, data, riskFilter, portfolioFilter, search, editId);
                });
    }

    public void save(
            Actor actor, Long id, String risk, Long portfolioId, String minDpd, String minOverdue)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                id == null ? "RISK_RULE_CREATED" : "RISK_RULE_UPDATED",
                c -> {
                    repository.save(c, id, risk, portfolioId, minDpd, minOverdue);
                    return id == null ? "Risk rule created." : "Risk rule updated.";
                });
    }

    public String delete(Actor actor, Long id) throws SQLException {
        LOG.info("Service flow started: delete()");
        return ScreenService.write(
                actor,
                PERMISSION,
                "RISK_RULE_DELETED",
                c -> {
                    repository.delete(c, id);
                    return "Risk rule deleted.";
                });
    }

    public String run(Actor actor) throws SQLException {
        LOG.info("Service flow started: run()");
        return ScreenService.write(
                actor,
                PERMISSION,
                "RISK_RUN",
                c -> {
                    repository.run(c);
                    return "Risk rules executed.";
                });
    }
}
