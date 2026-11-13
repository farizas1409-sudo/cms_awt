package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class PaymentRepository {
    private static final Logger LOG = LogManager.getLogger(PaymentRepository.class);

    public record Balances(long outstanding, long overdue) {}

    public Optional<Balances> lockBalances(
            Connection connection, long collectorId, long customerId, long loanId)
            throws SQLException {
        LOG.info("Repository flow started: lockBalances()");
        return Db.rows(
                        connection,
                        """
                        SELECT l.outstanding_paise,l.overdue_paise
                        FROM cms_loan l JOIN cms_allocation a ON a.customer_id=l.customer_id
                        WHERE l.id=? AND a.collector_id=? AND l.customer_id=?
                        FOR UPDATE OF l.outstanding_paise
                        """,
                        loanId,
                        collectorId,
                        customerId)
                .stream()
                .findFirst()
                .map(
                        row ->
                                new Balances(
                                        ((Number) row.get("OUTSTANDING_PAISE")).longValue(),
                                        ((Number) row.get("OVERDUE_PAISE")).longValue()));
    }

    public void updateBalances(Connection connection, long loanId, Balances balances)
            throws SQLException {
        LOG.info("Repository flow started: updateBalances()");
        Db.update(
                connection,
                "UPDATE cms_loan SET outstanding_paise=?,overdue_paise=? WHERE id=?",
                balances.outstanding(),
                balances.overdue(),
                loanId);
    }

    public void insertReceipt(
            Connection connection,
            long loanId,
            long paid,
            long overdueApplied,
            String reference,
            String mode,
            long collectorId)
            throws SQLException {
        LOG.info("Repository flow started: insertReceipt()");
        Db.update(
                connection,
                """
INSERT INTO cms_payment(id,loan_id,amount_paise,applied_paise,overdue_applied_paise,reference,pymt_mode,status,created_by)
VALUES(cms_payment_seq.NEXTVAL,?,?,?,?,?,?,'SUCCESS',?)
""",
                loanId,
                paid,
                paid,
                overdueApplied,
                reference,
                mode,
                collectorId);
    }
}
