package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class HistoryService {
    private static final Logger LOG = LogManager.getLogger(HistoryService.class);

    private static final String PERMISSION = "COLLECT";

    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public Map<String, Object> load(
            Actor actor, Long customerId, int page, Long filterLoanId, String filterAccountNo)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    accounts.load(c, actor.id(), customerId, data);
                    if (filterLoanId == null) {
                        accounts.loadPaymentHistory(
                                c, actor.id(), customerId, page, filterAccountNo, data);
                    } else {
                        accounts.loadPaymentHistory(
                                c, actor.id(), customerId, page, filterLoanId, data);
                    }
                    data.put("filterLoanId", filterLoanId);
                });
    }
}
