package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;

import java.sql.*;
import java.util.*;

public class CommunicationsRepository {
    private static final Logger LOG = LogManager.getLogger(CommunicationsRepository.class);

    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public void save(
            Connection c,
            long userId,
            Long customerId,
            Long loanId,
            String channel,
            String remarks,
            String value)
            throws SQLException {
        LOG.info("Repository flow started: save()");
        accounts.checkCustomer(c, userId, customerId);
        accounts.checkLoanOwnership(c, customerId, loanId);
        if (!List.of("Telephone", "SMS", "Email", "WhatsApp", "Other").contains(channel)
                || key(value).isEmpty() || key(value).length() > 200
                || key(remarks).isEmpty() || key(remarks).length() > 1800)
            throw new IllegalArgumentException("Enter the actual recipient and a short contact summary");
        var contacts = Db.rows(
                c,
                "SELECT email,phone,telephone_no,whatsapp_no FROM cms_customer WHERE id=?",
                customerId).get(0);
        Db.update(
                c,
                "INSERT INTO"
                    + " cms_communication(id,customer_id,loan_id,channel,recipient,email_used,mobile_used,telephone_used,whatsapp_used,status,details,created_by)"
                    + " VALUES(cms_communication_seq.NEXTVAL,?,?,?,?,?,?,?,?,'LOGGED',?,?)",
                customerId,
                loanId,
                channel,
                value,
                contacts.get("EMAIL"),
                contacts.get("PHONE"),
                contacts.get("TELEPHONE_NO"),
                contacts.get("WHATSAPP_NO"),
                remarks,
                userId);
    }
}
