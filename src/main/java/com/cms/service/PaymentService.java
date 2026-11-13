package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.CustomerAccountRepository;
import com.cms.repository.PaymentRepository;
import com.cms.repository.PaymentRepository.Balances;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

public class PaymentService {
    private static final Logger LOG = LogManager.getLogger(PaymentService.class);

    private static final String PERMISSION = "PAYMENT";
    private final PaymentRepository repository = new PaymentRepository();
    private final CustomerAccountRepository accounts = new CustomerAccountRepository();

    public Map<String, Object> load(
            Actor actor, Long customerId, Long loanId, Long filterLoanId, int page)
            throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(
                actor,
                PERMISSION,
                (connection, data) -> {
                    accounts.load(connection, actor.id(), customerId, data);
                    accounts.loadPaymentHistory(
                            connection, actor.id(), customerId, page, filterLoanId, data);
                    data.put("loanId", loanId);
                    data.put("filterLoanId", filterLoanId);
                });
    }

    public void save(
            Actor actor,
            Long customerId,
            Long loanId,
            String mode,
            String reference,
            BigDecimal paymentAmount)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "SAVE_payment",
                connection -> {
                    if (customerId == null
                            || loanId == null
                            || paymentAmount == null
                            || paymentAmount.signum() <= 0
                            || reference == null
                            || reference.isBlank()) {
                        throw new IllegalArgumentException("Invalid payment");
                    }
                    long paid = paymentAmount.movePointRight(2).longValueExact();
                    var balances =
                            repository
                                    .lockBalances(connection, actor.id(), customerId, loanId)
                                    .orElseThrow(() -> new SecurityException("Loan not allocated"));
                    if (paid > balances.outstanding())
                        throw new IllegalArgumentException("Payment exceeds outstanding");
                    long overdueApplied = Math.min(paid, balances.overdue());
                    repository.updateBalances(
                            connection,
                            loanId,
                            new Balances(
                                    balances.outstanding() - paid,
                                    balances.overdue() - overdueApplied));
                    repository.insertReceipt(
                            connection, loanId, paid, overdueApplied, reference, mode, actor.id());
                    return "Changes saved.";
                });
    }
}
