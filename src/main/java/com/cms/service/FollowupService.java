package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class FollowupService {
    private static final Logger LOG = LogManager.getLogger(FollowupService.class);

    private static final String PERMISSION = "COLLECT";
    private final FollowupRepository repository = new FollowupRepository();
    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public Map<String, Object> load(Actor actor, Long customerId, int page, Long filterLoanId)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    accounts.load(c, actor.id(), customerId, page, filterLoanId, data);
                    data.put("selectedLoanId", filterLoanId);
                });
    }

    public void save(
            Actor actor,
            String contactDate,
            String promiseDate,
            String promiseAmount,
            Long customerId,
            Long loanId,
            String channel,
            String outcome,
            String remarks,
            String nextAction)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "SAVE_followup",
                c -> {
                    repository.save(
                            c,
                            actor.id(),
                            contactDate,
                            promiseDate,
                            promiseAmount,
                            customerId,
                            loanId,
                            channel,
                            outcome,
                            remarks,
                            nextAction);
                    return "Changes saved.";
                });
    }
}
