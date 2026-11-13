package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.config.SystemParameters;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;

import java.sql.*;
import java.util.*;

public class CustomerAccountRepository {
    private static final Logger LOG = LogManager.getLogger(CustomerAccountRepository.class);

    public void load(Connection c, long userId, Long customerId, Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        load(c, userId, customerId, 0, data);
    }

    public void load(
            Connection c, long userId, Long customerId, int requestedFollowupPage,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        load(c, userId, customerId, requestedFollowupPage, null, data);
    }

    public void load(
            Connection c, long userId, Long customerId, int requestedFollowupPage,
            Long filterLoanId, Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        if (customerId != null) {
            List<Map<String, Object>> cust =
                    Db.rows(
                            c,
                            "SELECT cu.* FROM cms_customer cu JOIN cms_allocation a ON"
                                    + " a.customer_id=cu.id WHERE cu.id=? AND"
                                    + " a.collector_id=?",
                            customerId,
                            userId);
            if (cust.isEmpty()) throw new SecurityException("Customer not allocated");
            data.put("customer", cust.get(0));
            data.put(
                    "loans",
                    Db.rows(
                            c,
                            "SELECT l.*,p.name portfolio_name FROM cms_loan l JOIN"
                                    + " cms_portfolio p ON p.id=l.portfolio_id WHERE"
                                    + " l.customer_id=? ORDER BY l.account_no",
                            customerId));
            data.put(
                    "summary",
                    Db.rows(
                                    c,
                                    "SELECT COUNT(*)"
                                            + " accounts,NVL(SUM(outstanding_paise),0)/100"
                                            + " outstanding,NVL(SUM(overdue_paise),0)/100"
                                            + " overdue,NVL(MAX(dpd),0) dpd,CASE MAX(CASE"
                                            + " risk WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN"
                                            + " 2 ELSE 1 END) WHEN 3 THEN 'HIGH' WHEN 2"
                                            + " THEN 'MEDIUM' ELSE 'LOW' END risk FROM"
                                            + " cms_loan WHERE customer_id=?",
                                    customerId)
                            .get(0));
            if (requestedFollowupPage > 0) {
                checkLoanOwnership(c, customerId, filterLoanId);
                loadFollowupPage(c, customerId, requestedFollowupPage, filterLoanId, data);
            } else {
                checkLoanOwnership(c, customerId, filterLoanId);
                String loanClause = filterLoanId == null ? "" : " AND f.loan_id=?";
                List<Object> followupArgs = new ArrayList<>();
                followupArgs.add(customerId);
                if (filterLoanId != null) followupArgs.add(filterLoanId);
                followupArgs.add(SystemParameters.getInt("pagination.customer-followup-summary"));
                data.put(
                        "secondary",
                        Db.rows(
                                c,
                                "SELECT"
                                    + " f.id,f.channel,f.outcome,f.remarks,f.next_action,COALESCE(f.contact_date,CAST(f.created_at"
                                    + " AS DATE)) contact_date,f.promise_paise/100"
                                    + " promise_amount,f.promise_date,l.account_no FROM cms_followup f"
                                    + " LEFT JOIN cms_loan l ON l.id=f.loan_id WHERE f.customer_id=?"
                                    + loanClause
                                    + " ORDER BY f.created_at DESC,f.id DESC FETCH FIRST ? ROWS ONLY",
                                followupArgs.toArray()));
            }
        }
    }

    private void loadFollowupPage(
            Connection c, Long customerId, int requestedPage, Long filterLoanId,
            Map<String, Object> data)
            throws SQLException {
        String loanClause = filterLoanId == null ? "" : " AND loan_id=?";
        List<Object> countArgs = new ArrayList<>();
        countArgs.add(customerId);
        if (filterLoanId != null) countArgs.add(filterLoanId);
        long total = ((Number) Db.rows(
                c,
                "SELECT COUNT(*) total FROM cms_followup WHERE customer_id=?" + loanClause,
                countArgs.toArray()).get(0).get("TOTAL")).longValue();
        int pageSize = SystemParameters.getInt("pagination.customer-followups");
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(1, requestedPage), pages);
        int first = total == 0 ? 0 : (page - 1) * pageSize + 1;
        int last = (int) Math.min((long) page * pageSize, total);

        data.put("followupPage", page);
        data.put("followupPages", pages);
        data.put("followupTotal", total);
        data.put("followupFirst", first);
        data.put("followupLast", last);
        data.put("filterLoanId", filterLoanId);
        List<Object> rowArgs = new ArrayList<>();
        rowArgs.add(customerId);
        if (filterLoanId != null) rowArgs.add(filterLoanId);
        rowArgs.add((page - 1) * pageSize);
        rowArgs.add(pageSize);
        data.put(
                "secondary",
                Db.rows(
                        c,
                        "SELECT f.id,f.channel,f.outcome,f.remarks,f.next_action,COALESCE(f.contact_date,CAST(f.created_at AS DATE)) contact_date,f.promise_paise/100 promise_amount,f.promise_date,l.account_no FROM cms_followup f LEFT JOIN cms_loan l ON l.id=f.loan_id WHERE f.customer_id=?"
                                + (filterLoanId == null ? "" : " AND f.loan_id=?")
                                + " ORDER BY f.created_at DESC,f.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        rowArgs.toArray()));
    }

    public void loadPaymentHistory(
            Connection c, long userId, Long customerId, int requestedPage,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: loadPaymentHistory()");
        loadPaymentHistory(c, userId, customerId, requestedPage, (String) null, data);
    }

    public void loadPaymentHistory(
            Connection c, long userId, Long customerId, int requestedPage, Long filterLoanId,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: loadPaymentHistory()");
        loadPaymentHistory(c, userId, customerId, requestedPage, filterLoanId, null, data);
    }

    public void loadPaymentHistory(
            Connection c, long userId, Long customerId, int requestedPage, String filterAccountNo,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: loadPaymentHistory()");
        loadPaymentHistory(c, userId, customerId, requestedPage, null, filterAccountNo, data);
    }

    private void loadPaymentHistory(
            Connection c,
            long userId,
            Long customerId,
            int requestedPage,
            Long filterLoanId,
            String filterAccountNo,
            Map<String, Object> data)
            throws SQLException {
        if (customerId == null) return;
        checkCustomer(c, userId, customerId);
        checkLoanOwnership(c, customerId, filterLoanId);
        filterAccountNo = normalizeAccountFilter(filterAccountNo);
        checkLoanAccountOwnership(c, customerId, filterAccountNo);
        String loanClause =
                filterLoanId != null
                        ? " AND l.id=?"
                        : filterAccountNo != null ? " AND l.account_no=?" : "";
        List<Object> queryArgs = new ArrayList<>();
        queryArgs.add(customerId);
        if (filterLoanId != null) queryArgs.add(filterLoanId);
        else if (filterAccountNo != null) queryArgs.add(filterAccountNo);
        long total = ((Number) Db.rows(
                c,
                "SELECT COUNT(*) total FROM cms_payment p JOIN cms_loan l ON l.id=p.loan_id WHERE l.customer_id=?"
                        + loanClause,
                queryArgs.toArray()).get(0).get("TOTAL")).longValue();
        int pageSize = SystemParameters.getInt("pagination.payment-history");
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(1, requestedPage), pages);
        data.put("paymentPage", page);
        data.put("paymentPages", pages);
        data.put("paymentTotal", total);
        data.put("filterLoanId", filterLoanId);
        data.put("filterAccountNo", filterAccountNo);
        queryArgs.add((page - 1) * pageSize);
        queryArgs.add(pageSize);
        data.put(
                "rows",
                Db.rows(
                        c,
                        "SELECT p.id,l.account_no,p.amount_paise/100"
                                + " amount,p.overdue_applied_paise/100"
                                + " overdue_applied,p.status,p.reference,p.pymt_mode payment_mode,p.created_at"
                                + " FROM cms_payment p JOIN cms_loan l ON"
                                + " l.id=p.loan_id WHERE l.customer_id=?"
                                + loanClause
                                + " ORDER BY p.created_at DESC,p.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        queryArgs.toArray()));
    }

    public void loadCommunications(
            Connection c, long userId, Long customerId, int requestedPage, String filterAccountNo,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: loadCommunications()");
        if (customerId == null) return;
        checkCustomer(c, userId, customerId);
        filterAccountNo = normalizeAccountFilter(filterAccountNo);
        checkLoanAccountOwnership(c, customerId, filterAccountNo);
        String loanClause = filterAccountNo == null ? "" : " AND l.account_no=?";
        List<Object> queryArgs = new ArrayList<>();
        queryArgs.add(customerId);
        if (filterAccountNo != null) queryArgs.add(filterAccountNo);
        long total = ((Number) Db.rows(
                c,
                "SELECT COUNT(*) total FROM cms_communication m LEFT JOIN cms_loan l ON l.id=m.loan_id WHERE m.customer_id=?"
                        + loanClause,
                queryArgs.toArray()).get(0).get("TOTAL")).longValue();
        int pageSize = SystemParameters.getInt("pagination.communication-history");
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(1, requestedPage), pages);
        data.put("communicationPage", page);
        data.put("communicationPages", pages);
        data.put("communicationTotal", total);
        data.put("filterAccountNo", filterAccountNo);
        queryArgs.add((page - 1) * pageSize);
        queryArgs.add(pageSize);
        data.put(
                "tertiary",
                Db.rows(
                        c,
                        "SELECT m.channel,m.recipient,m.status,m.details,m.created_at,l.account_no,"
                                + "m.email_used,m.mobile_used,m.telephone_used,m.whatsapp_used FROM cms_communication m "
                                + "JOIN cms_customer cu ON cu.id=m.customer_id LEFT JOIN cms_loan l ON l.id=m.loan_id "
                                + "WHERE m.customer_id=?" + loanClause
                                + " ORDER BY m.created_at DESC,m.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        queryArgs.toArray()));
    }

    public void loadCommunications(
            Connection c, long userId, Long customerId, int requestedPage, Long filterLoanId,
            Map<String, Object> data)
            throws SQLException {
        LOG.info("Repository flow started: loadCommunications()");
        String filterAccountNo = null;
        if (filterLoanId != null) {
            List<Map<String, Object>> selectedLoan =
                    Db.rows(
                            c,
                            "SELECT account_no FROM cms_loan WHERE id=? AND customer_id=?",
                            filterLoanId,
                            customerId);
            if (selectedLoan.isEmpty()) {
                throw new SecurityException("Loan does not belong to selected customer");
            }
            filterAccountNo = String.valueOf(selectedLoan.get(0).get("ACCOUNT_NO"));
        }
        loadCommunications(c, userId, customerId, requestedPage, filterAccountNo, data);
    }

    public void checkCustomer(Connection c, long userId, Long cid) throws SQLException {
        LOG.info("Repository flow started: checkCustomer()");
        if (cid == null
                || Db.rows(
                                c,
                                "SELECT cu.id FROM cms_customer cu JOIN cms_allocation a ON"
                                        + " a.customer_id=cu.id WHERE cu.id=? AND a.collector_id=?",
                                cid,
                                userId)
                        .isEmpty()) throw new SecurityException("Customer not allocated");
    }

    public void checkLoanOwnership(Connection c, Long cid, Long lid) throws SQLException {
        LOG.info("Repository flow started: checkLoanOwnership()");
        if (lid != null
                && Db.rows(c, "SELECT id FROM cms_loan WHERE id=? AND customer_id=?", lid, cid)
                        .isEmpty())
            throw new SecurityException("Loan does not belong to selected customer");
    }

    private String normalizeAccountFilter(String accountNo) {
        return accountNo == null || accountNo.isBlank() ? null : accountNo.trim();
    }

    private void checkLoanAccountOwnership(Connection c, Long customerId, String accountNo)
            throws SQLException {
        if (accountNo != null
                && Db.rows(
                                c,
                                "SELECT id FROM cms_loan WHERE account_no=? AND customer_id=?",
                                accountNo,
                                customerId)
                        .isEmpty()) {
            throw new SecurityException("Loan account does not belong to selected customer");
        }
    }
}
