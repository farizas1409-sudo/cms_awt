package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class FollowupRepository {
    private static final Logger LOG = LogManager.getLogger(FollowupRepository.class);

    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public void save(
            Connection c,
            long userId,
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
        LOG.info("Repository flow started: save()");
        accounts.checkCustomer(c, userId, customerId);
        accounts.checkLoanOwnership(c, customerId, loanId);
        java.sql.Date contacted = java.sql.Date.valueOf(contactDate);
        java.sql.Date promise =
                key(promiseDate).isEmpty() ? null : java.sql.Date.valueOf(promiseDate);
        Long promisePaise =
                key(promiseAmount).isEmpty()
                        ? null
                        : new BigDecimal(promiseAmount).movePointRight(2).longValueExact();
        if (!Arrays.asList("Telephone", "SMS", "Email", "WhatsApp", "Field Visit").contains(channel)
                || key(outcome).isEmpty()) throw new IllegalArgumentException("Invalid follow-up");
        if ((promise == null) != (promisePaise == null)
                || (promise != null && (promisePaise <= 0 || promise.before(contacted)))
                || ("Promise to Pay".equals(outcome) && promise == null))
            throw new IllegalArgumentException("Invalid promise");
        Db.update(
                c,
                "INSERT INTO"
                    + " cms_followup(id,customer_id,loan_id,collector_id,channel,outcome,remarks,next_action,contact_date,promise_paise,promise_date)"
                    + " VALUES(cms_followup_seq.NEXTVAL,?,?,?,?,?,?,?,?,?,?)",
                customerId,
                loanId,
                userId,
                channel,
                outcome,
                remarks,
                key(nextAction).isEmpty() ? null : java.sql.Date.valueOf(nextAction),
                contacted,
                promisePaise,
                promise);
    }
}
