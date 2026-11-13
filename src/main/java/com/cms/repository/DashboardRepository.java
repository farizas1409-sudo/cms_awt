package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.config.SystemParameters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class DashboardRepository {
    private static final Logger LOG = LogManager.getLogger(DashboardRepository.class);

    public void load(Connection connection, Map<String, Object> data, long userId) throws SQLException {
        LOG.info("Repository flow started: load()");
        Map<String, Object> summary =
                Db.rows(
                                connection,
                                "SELECT COUNT(DISTINCT cu.id) customers,COUNT(l.id) accounts,NVL(SUM(l.outstanding_paise),0)/100 outstanding,"
                                        + "NVL(SUM(l.overdue_paise),0)/100 overdue "
                                        + "FROM cms_customer cu JOIN cms_allocation a ON a.customer_id=cu.id LEFT JOIN cms_loan l ON l.customer_id=cu.id WHERE a.collector_id=?",
                                userId)
                        .get(0);
        summary.put(
                "COLLECTIONS",
                Db.rows(
                                connection,
                                "SELECT NVL(SUM(p.amount_paise),0)/100 collections FROM cms_payment p JOIN cms_loan l ON l.id=p.loan_id JOIN cms_allocation a ON a.customer_id=l.customer_id WHERE a.collector_id=? AND p.status='SUCCESS'",
                                userId)
                        .get(0)
                        .get("COLLECTIONS"));
        data.put("summary", summary);
        data.put(
                "riskRows",
                Db.rows(
                        connection,
                        "SELECT risk,COUNT(*) total FROM (SELECT cu.id,CASE MAX(CASE l.risk WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END) WHEN 3 THEN 'HIGH' WHEN 2 THEN 'MEDIUM' ELSE 'LOW' END risk FROM cms_customer cu JOIN cms_allocation a ON a.customer_id=cu.id JOIN cms_loan l ON l.customer_id=cu.id WHERE a.collector_id=? GROUP BY cu.id) GROUP BY risk ORDER BY risk",
                        userId));
        data.put(
                "portfolioRows",
                Db.rows(
                        connection,
                        "SELECT p.name portfolio_name,COUNT(*) accounts,NVL(SUM(l.outstanding_paise),0)/100 outstanding,NVL(SUM(l.overdue_paise),0)/100 overdue FROM cms_allocation a JOIN cms_loan l ON l.customer_id=a.customer_id JOIN cms_portfolio p ON p.id=l.portfolio_id WHERE a.collector_id=? GROUP BY p.name ORDER BY outstanding DESC",
                        userId));
        data.put(
                "trendRows",
                Db.rows(
                        connection,
                        "SELECT TRUNC(p.created_at) activity_date,SUM(p.amount_paise)/100 collected FROM cms_payment p JOIN cms_loan l ON l.id=p.loan_id JOIN cms_allocation a ON a.customer_id=l.customer_id WHERE a.collector_id=? AND p.status='SUCCESS' AND p.created_at>=TRUNC(SYSDATE)-13 GROUP BY TRUNC(p.created_at) ORDER BY activity_date",
                        userId));
        data.put(
                "promiseRows",
                Db.rows(
                        connection,
                        "SELECT f.outcome,COUNT(*) actions,NVL(SUM(f.promise_paise),0)/100 promised FROM cms_followup f JOIN cms_allocation a ON a.customer_id=f.customer_id WHERE a.collector_id=? GROUP BY f.outcome ORDER BY actions DESC",
                        userId));
        data.put(
                "recentRows",
                Db.rows(
                        connection,
                        "SELECT f.created_at,cu.customer_no,cu.name,l.account_no,f.channel,f.outcome,f.next_action FROM cms_followup f JOIN cms_customer cu ON cu.id=f.customer_id JOIN cms_allocation a ON a.customer_id=cu.id LEFT JOIN cms_loan l ON l.id=f.loan_id WHERE a.collector_id=? ORDER BY f.created_at DESC,f.id DESC FETCH FIRST ? ROWS ONLY",
                        userId,
                        SystemParameters.getInt("dashboard.recent-followups")));
    }
}
