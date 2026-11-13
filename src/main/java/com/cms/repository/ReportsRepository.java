package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.key;

import com.cms.config.SystemParameters;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReportsRepository {
    private static final Logger LOG = LogManager.getLogger(ReportsRepository.class);

    public void load(
            Connection connection,
            Map<String, Object> data,
            long userId,
            int requestedPage,
            String query,
            String risk,
            String portfolio)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        StringBuilder where = new StringBuilder(" WHERE a.collector_id=?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (!key(query).isEmpty()) {
            where.append(" AND (LOWER(cu.customer_no) LIKE ? OR LOWER(cu.name) LIKE ? OR cu.phone LIKE ?)");
            String pattern = "%" + key(query).toLowerCase() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add("%" + key(query) + "%");
        }
        if (!key(risk).isEmpty()) {
            if (!Arrays.asList("LOW", "MEDIUM", "HIGH").contains(risk))
                throw new IllegalArgumentException("Choose a valid risk");
            where.append(" AND l.risk=?");
            parameters.add(risk);
        }
        if (!key(portfolio).isEmpty()) {
            where.append(" AND l.portfolio_id=?");
            parameters.add(Long.valueOf(portfolio));
        }
        long total = ((Number) Db.rows(
                connection,
                "SELECT COUNT(*) total FROM cms_customer cu JOIN cms_allocation a ON a.customer_id=cu.id JOIN cms_loan l ON l.customer_id=cu.id"
                        + where,
                parameters.toArray()).get(0).get("TOTAL")).longValue();
        int pageSize = SystemParameters.getInt("pagination.collector-reports");
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(requestedPage, 1), pages);
        data.put("reportPage", page);
        data.put("reportPages", pages);
        data.put("reportTotal", total);
        parameters.add((page - 1) * pageSize);
        parameters.add(pageSize);
        data.put(
                "rows",
                Db.rows(
                        connection,
                        "SELECT cu.customer_no,cu.name,l.account_no,COUNT(*) OVER (PARTITION BY cu.id) loans,"
                                + "l.outstanding_paise/100 outstanding,l.overdue_paise/100 overdue,cu.phone,cu.email,"
                                + "TRIM(NVL(cu.house_no,'')||' '||NVL(cu.street,'')||' '||NVL(cu.village,'')||' '||NVL(cu.mandal,'')||' '||NVL(cu.district,'')||' '||NVL(cu.state,'')) address,"
                                + "cu.whatsapp_no,l.dpd,l.risk,(SELECT MAX(p.created_at) FROM cms_payment p WHERE p.loan_id=l.id AND p.status='SUCCESS') last_payment,"
                                + "NVL(l.next_action_date,(SELECT MAX(f.next_action) FROM cms_followup f WHERE f.loan_id=l.id)) next_action "
                                + "FROM cms_customer cu JOIN cms_allocation a ON a.customer_id=cu.id JOIN cms_loan l ON l.customer_id=cu.id"
                                + where
                                + " ORDER BY l.overdue_paise DESC,l.account_no OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        parameters.toArray()));
    }
}
