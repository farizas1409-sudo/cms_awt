package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.config.SystemParameters;
import com.cms.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class CustomersRepository {
    private static final Logger LOG = LogManager.getLogger(CustomersRepository.class);

    public void load(
            Connection c,
            Map<String, Object> data,
            long userId,
            int page,
            String q,
            String risk,
            String productFilter,
            String promiseDate,
            String nextActionDate,
            String outcome)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        String search =
                "SELECT cu.id,cu.customer_no,cu.name,cu.phone,COUNT(l.id)"
                        + " loan_count,NVL(SUM(l.outstanding_paise),0)/100"
                        + " outstanding,NVL(SUM(l.overdue_paise),0)/100 overdue,MAX(l.dpd)"
                        + " dpd,CASE MAX(CASE l.risk WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN"
                        + " 2 ELSE 1 END) WHEN 3 THEN 'HIGH' WHEN 2 THEN 'MEDIUM' ELSE"
                        + " 'LOW' END risk FROM cms_customer cu JOIN cms_allocation a ON"
                        + " a.customer_id=cu.id JOIN cms_loan l ON l.customer_id=cu.id"
                        + " WHERE a.collector_id=? AND (LOWER(cu.name) LIKE ? OR"
                        + " LOWER(cu.customer_no) LIKE ? OR cu.phone LIKE ?)";
        String pattern = "%" + key(q).toLowerCase() + "%";
        List<Object> searchArgs = new ArrayList<>();
        Collections.addAll(searchArgs, userId, pattern, pattern, "%" + key(q) + "%");
        if (!key(productFilter).isEmpty()) {
            search +=
                    " AND EXISTS (SELECT 1 FROM cms_loan pl WHERE pl.customer_id=cu.id"
                            + " AND pl.portfolio_id=?)";
            searchArgs.add(Long.valueOf(productFilter));
        }
        if (!key(promiseDate).isEmpty()) {
            java.sql.Date.valueOf(promiseDate);
            search += " AND (EXISTS (SELECT 1 FROM cms_loan pl WHERE pl.customer_id=cu.id"
                    + " AND TRUNC(pl.promise_date)=TO_DATE(?,'YYYY-MM-DD')) OR EXISTS"
                    + " (SELECT 1 FROM cms_followup pf WHERE pf.customer_id=cu.id AND"
                    + " TRUNC(pf.promise_date)=TO_DATE(?,'YYYY-MM-DD')))";
            searchArgs.add(promiseDate);
            searchArgs.add(promiseDate);
        }
        if (!key(nextActionDate).isEmpty()) {
            java.sql.Date.valueOf(nextActionDate);
            search += " AND (EXISTS (SELECT 1 FROM cms_loan nl WHERE nl.customer_id=cu.id"
                    + " AND TRUNC(nl.next_action_date)=TO_DATE(?,'YYYY-MM-DD')) OR EXISTS"
                    + " (SELECT 1 FROM cms_followup nf WHERE nf.customer_id=cu.id AND"
                    + " TRUNC(nf.next_action)=TO_DATE(?,'YYYY-MM-DD')))";
            searchArgs.add(nextActionDate);
            searchArgs.add(nextActionDate);
        }
        if (!key(outcome).isEmpty()) {
            if (!Arrays.asList("Promise to Pay", "Reached", "No Answer", "Disputed", "Completed")
                    .contains(outcome))
                throw new IllegalArgumentException("Invalid follow-up outcome");
            search += " AND EXISTS (SELECT 1 FROM cms_followup lf WHERE lf.customer_id=cu.id"
                    + " AND lf.id=(SELECT MAX(lf2.id) FROM cms_followup lf2"
                    + " WHERE lf2.customer_id=cu.id) AND lf.outcome=?)";
            searchArgs.add(outcome);
        }
        search += " GROUP BY cu.id,cu.customer_no,cu.name,cu.phone HAVING 1=1";
        if (!key(risk).isEmpty()) {
            if (!Arrays.asList("LOW", "MEDIUM", "HIGH").contains(risk))
                throw new IllegalArgumentException("Invalid risk filter");
            search +=
                    " AND MAX(CASE l.risk WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE"
                            + " 1 END)=?";
            searchArgs.add("HIGH".equals(risk) ? 3 : "MEDIUM".equals(risk) ? 2 : 1);
        }
        long customerCount =
                ((Number)
                                Db.rows(
                                                c,
                                                "SELECT COUNT(*) total FROM (" + search + ")",
                                                searchArgs.toArray())
                                        .get(0)
                                        .get("TOTAL"))
                        .longValue();
        int pageSize = SystemParameters.getInt("pagination.customers");
        int pageCount = (int) Math.max(1, (customerCount + pageSize - 1) / pageSize);
        page = Math.min(Math.max(page, 1), pageCount);
        data.put("page", page);
        data.put("pageCount", pageCount);
        data.put("customerCount", customerCount);
        data.put("followupOutcomes", Db.rows(c, "SELECT DISTINCT outcome code FROM cms_followup ORDER BY outcome"));
        searchArgs.add((page - 1) * (long) pageSize);
        searchArgs.add(pageSize);
        data.put(
                "rows",
                Db.rows(
                        c,
                        "SELECT c.*, (SELECT MAX(p.created_at) FROM cms_payment p JOIN"
                                + " cms_loan pl ON pl.id=p.loan_id WHERE"
                                + " pl.customer_id=c.id AND p.status='SUCCESS')"
                                + " last_payment, (SELECT MAX(f.next_action) KEEP"
                                + " (DENSE_RANK LAST ORDER BY f.created_at,f.id) FROM"
                                + " cms_followup f WHERE f.customer_id=c.id) next_action"
                                + " FROM ("
                                + search
                                + ") c ORDER BY c.customer_no OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        searchArgs.toArray()));
    }
}
