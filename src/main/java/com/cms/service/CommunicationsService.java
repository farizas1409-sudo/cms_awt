package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class CommunicationsService {
    private static final Logger LOG = LogManager.getLogger(CommunicationsService.class);

    private static final String PERMISSION = "COLLECT";
    private final CommunicationsRepository repository = new CommunicationsRepository();
    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public Map<String, Object> load(
            Actor actor,
            Long customerId,
            int page,
            Long filterLoanId,
            String filterAccountNo)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    accounts.load(c, actor.id(), customerId, data);
                    if (filterLoanId == null) {
                        accounts.loadCommunications(
                                c, actor.id(), customerId, page, filterAccountNo, data);
                    } else {
                        accounts.loadCommunications(
                                c, actor.id(), customerId, page, filterLoanId, data);
                    }
                    data.put("filterLoanId", filterLoanId);
                });
    }

    public void save(
            Actor actor, Long customerId, Long loanId, String channel, String remarks, String value)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "SAVE_communications",
                c -> {
                    repository.save(c, actor.id(), customerId, loanId, channel, remarks, value);
                    return "Changes saved.";
                });
    }
}
