package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.key;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OwnerRepository {
    private static final Logger LOG = LogManager.getLogger(OwnerRepository.class);

    public void load(
            Connection connection,
            Map<String, Object> data,
            String district,
            String branch,
            String reportFrom,
            String reportTo,
            boolean searched)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        List<Object> scopeParameters = new ArrayList<>();
        StringBuilder scope = new StringBuilder(" WHERE 1=1");
        if (!key(district).isEmpty()) {
            scope.append(" AND d.id=?");
            scopeParameters.add(Long.parseLong(district));
        }
        if (!key(branch).isEmpty()) {
            scope.append(" AND b.id=?");
            scopeParameters.add(Long.parseLong(branch));
        }
        data.put("searched", searched);
        if (!searched) {
            data.put("rows", List.of());
            data.put("summary", Map.of("CUSTOMERS", 0, "ACCOUNTS", 0, "OUTSTANDING", 0, "OVERDUE", 0, "COLLECTED", 0));
            data.put("riskRows", List.of());
            data.put("portfolioRows", List.of());
            return;
        }

        java.sql.Date from = key(reportFrom).isEmpty() ? null : java.sql.Date.valueOf(reportFrom);
        java.sql.Date to = key(reportTo).isEmpty() ? null : java.sql.Date.valueOf(reportTo);
        if (from != null && to != null && from.after(to))
            throw new IllegalArgumentException("From date must be before To date");
        StringBuilder paymentDateFilter = new StringBuilder();
        List<Object> paymentParameters = new ArrayList<>();
        if (from != null) {
            paymentDateFilter.append(" AND p.created_at>=?");
            paymentParameters.add(from);
        }
        if (to != null) {
            paymentDateFilter.append(" AND p.created_at<?");
            paymentParameters.add(java.sql.Date.valueOf(to.toLocalDate().plusDays(1)));
        }
        List<Object> reportParameters = new ArrayList<>(paymentParameters);
        reportParameters.addAll(scopeParameters);
        String reportScope =
                " FROM cms_customer cu JOIN cms_branch b ON b.id=cu.branch_id JOIN"
                        + " cms_district d ON d.id=b.district_id LEFT JOIN cms_loan l ON"
                        + " l.customer_id=cu.id LEFT JOIN (SELECT p.loan_id,SUM(p.amount_paise) collected"
                        + " FROM cms_payment p WHERE p.status='SUCCESS'"
                        + paymentDateFilter
                        + " GROUP BY p.loan_id) paid ON paid.loan_id=l.id"
                        + scope;
        String totals =
                "COUNT(DISTINCT cu.id) customers,COUNT(l.id) accounts,NVL(SUM(l.outstanding_paise),0)/100 outstanding,"
                        + "NVL(SUM(l.overdue_paise),0)/100 overdue,NVL(SUM(paid.collected),0)/100 collected";
        data.put(
                "rows",
                Db.rows(
                        connection,
                        "SELECT b.name branch_name,d.name district_name," + totals + reportScope
                                + " GROUP BY b.id,b.name,d.id,d.name ORDER BY d.name,b.name",
                        reportParameters.toArray()));
        data.put("summary", Db.rows(connection, "SELECT " + totals + reportScope, reportParameters.toArray()).get(0));
        data.put("riskRows", Db.rows(connection,
                "SELECT l.risk,COUNT(*) accounts,NVL(SUM(l.outstanding_paise),0)/100 outstanding,NVL(SUM(l.overdue_paise),0)/100 overdue FROM cms_customer cu JOIN cms_branch b ON b.id=cu.branch_id JOIN cms_district d ON d.id=b.district_id JOIN cms_loan l ON l.customer_id=cu.id"
                        + scope + " GROUP BY l.risk ORDER BY l.risk", scopeParameters.toArray()));
        data.put("portfolioRows", Db.rows(connection,
                "SELECT p.name portfolio_name,COUNT(*) accounts,NVL(SUM(l.outstanding_paise),0)/100 outstanding,NVL(SUM(l.overdue_paise),0)/100 overdue FROM cms_customer cu JOIN cms_branch b ON b.id=cu.branch_id JOIN cms_district d ON d.id=b.district_id JOIN cms_loan l ON l.customer_id=cu.id JOIN cms_portfolio p ON p.id=l.portfolio_id"
                        + scope + " GROUP BY p.name ORDER BY outstanding DESC", scopeParameters.toArray()));

    }
}
