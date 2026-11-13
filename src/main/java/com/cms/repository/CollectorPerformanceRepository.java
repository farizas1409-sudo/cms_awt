package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.config.SystemParameters;

import static com.cms.repository.QueryValues.key;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CollectorPerformanceRepository {
    private static final Logger LOG = LogManager.getLogger(CollectorPerformanceRepository.class);

    public void load(
            Connection connection,
            Map<String, Object> data,
            String fromDate,
            String toDate,
            String district,
            String branch,
            String collector,
            String portfolio,
            String risk,
            int requestedPage,
            boolean searched)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        data.put("districtOptions", Db.rows(connection, "SELECT id,name FROM cms_district ORDER BY name"));
        data.put("selectedDistrict", district);
        data.put("selectedRisk", risk);
        data.put("searched", searched);
        data.put("summaryRows", List.of());
        data.put("activityRows", List.of());
        if (!searched) {
            data.put("activityTotal", 0);
            data.put("activityPage", 1);
            data.put("activityPages", 1);
            return;
        }
        if (key(district).isEmpty())
            throw new IllegalArgumentException("District is required");
        java.sql.Date from = key(fromDate).isEmpty() ? null : java.sql.Date.valueOf(fromDate);
        java.sql.Date to = key(toDate).isEmpty() ? null : java.sql.Date.valueOf(toDate);
        if (from != null && to != null && from.after(to))
            throw new IllegalArgumentException("From date must be before To date");
        if (!key(risk).isEmpty() && !Arrays.asList("LOW", "MEDIUM", "HIGH").contains(risk))
            throw new IllegalArgumentException("Choose a valid risk category");

        StringBuilder baseWhere = new StringBuilder(" WHERE r.name='COLLECTOR' AND d.id=?");
        List<Object> baseParams = new ArrayList<>();
        baseParams.add(Long.valueOf(district));
        if (!key(branch).isEmpty()) { baseWhere.append(" AND b.id=?"); baseParams.add(Long.valueOf(branch)); }
        if (!key(collector).isEmpty()) { baseWhere.append(" AND u.id=?"); baseParams.add(Long.valueOf(collector)); }

        String loanFilter = "";
        List<Object> loanParams = new ArrayList<>();
        if (!key(portfolio).isEmpty()) { loanFilter += " AND l.portfolio_id=?"; loanParams.add(Long.valueOf(portfolio)); }
        if (!key(risk).isEmpty()) { loanFilter += " AND l.risk=?"; loanParams.add(risk); }
        String activityDateFilter = "";
        List<Object> activityDateParams = new ArrayList<>();
        if (from != null) { activityDateFilter += " AND f.created_at>=?"; activityDateParams.add(from); }
        if (to != null) { activityDateFilter += " AND f.created_at<?"; activityDateParams.add(java.sql.Date.valueOf(to.toLocalDate().plusDays(1))); }

        List<Object> summaryParams = new ArrayList<>(baseParams);
        summaryParams.addAll(loanParams);
        summaryParams.addAll(activityDateParams);
        summaryParams.addAll(loanParams);
        summaryParams.addAll(activityDateParams);
        summaryParams.addAll(loanParams);
        data.put("summaryRows", Db.rows(connection,
                "WITH collector_base (collector_id,collector_name,branch_name) AS (SELECT u.id,u.display_name,b.name FROM cms_user u JOIN cms_role r ON r.id=u.role_id JOIN cms_branch b ON b.id=u.branch_id JOIN cms_district d ON d.id=b.district_id"
                        + baseWhere + "),loan_stats (collector_id,assigned_customers,assigned_loan_accounts,outstanding,overdue) AS (SELECT a.collector_id,COUNT(DISTINCT cu.id),COUNT(l.id),NVL(SUM(l.outstanding_paise),0)/100,NVL(SUM(l.overdue_paise),0)/100 FROM cms_allocation a JOIN cms_customer cu ON cu.id=a.customer_id JOIN cms_loan l ON l.customer_id=cu.id WHERE 1=1"
                        + loanFilter + " GROUP BY a.collector_id),payment_stats (collector_id,collections) AS (SELECT a.collector_id,NVL(SUM(p.amount_paise),0)/100 FROM cms_payment p JOIN cms_loan l ON l.id=p.loan_id JOIN cms_allocation a ON a.customer_id=l.customer_id WHERE p.status='SUCCESS'"
                        + (from == null ? "" : " AND p.created_at>=?") + (to == null ? "" : " AND p.created_at<?") + loanFilter
                        + " GROUP BY a.collector_id),follow_stats (collector_id,followups_completed,promises_taken,promises_kept,promises_broken,pending_followups,next_actions_due) AS (SELECT f.collector_id,SUM(CASE WHEN f.outcome='Completed' THEN 1 ELSE 0 END),SUM(CASE WHEN f.promise_paise IS NOT NULL THEN 1 ELSE 0 END),SUM(CASE WHEN f.promise_paise IS NOT NULL AND NVL((SELECT SUM(p.applied_paise) FROM cms_payment p WHERE p.loan_id=f.loan_id AND p.status='SUCCESS' AND p.created_at<CAST(f.promise_date+1 AS TIMESTAMP)),0)>=f.promise_paise THEN 1 ELSE 0 END),SUM(CASE WHEN f.promise_paise IS NOT NULL AND f.promise_date<TRUNC(SYSDATE) AND NVL((SELECT SUM(p.applied_paise) FROM cms_payment p WHERE p.loan_id=f.loan_id AND p.status='SUCCESS' AND p.created_at<CAST(f.promise_date+1 AS TIMESTAMP)),0)<f.promise_paise THEN 1 ELSE 0 END),SUM(CASE WHEN f.next_action IS NOT NULL AND f.next_action<TRUNC(SYSDATE) THEN 1 ELSE 0 END),SUM(CASE WHEN f.next_action=TRUNC(SYSDATE) THEN 1 ELSE 0 END) FROM cms_followup f JOIN cms_customer cu ON cu.id=f.customer_id LEFT JOIN cms_loan l ON l.id=f.loan_id WHERE 1=1"
                        + activityDateFilter + loanFilter + " GROUP BY f.collector_id) SELECT cb.collector_id collector_id,cb.collector_name collector_name,cb.branch_name,NVL(ls.assigned_customers,0) assigned_customers,NVL(ls.assigned_loan_accounts,0) assigned_loan_accounts,NVL(ls.outstanding,0) outstanding,NVL(ls.overdue,0) overdue,NVL(ps.collections,0) collections,NVL(fs.followups_completed,0) followups_completed,NVL(fs.promises_taken,0) promises_taken,NVL(fs.promises_kept,0) promises_kept,NVL(fs.promises_broken,0) promises_broken,NVL(fs.pending_followups,0) pending_followups,NVL(fs.next_actions_due,0) next_actions_due,CASE WHEN NVL(ls.outstanding,0)+NVL(ps.collections,0)=0 THEN 0 ELSE ROUND(NVL(ps.collections,0)*100/(NVL(ls.outstanding,0)+NVL(ps.collections,0)),1) END collection_efficiency FROM collector_base cb LEFT JOIN loan_stats ls ON ls.collector_id=cb.collector_id LEFT JOIN payment_stats ps ON ps.collector_id=cb.collector_id LEFT JOIN follow_stats fs ON fs.collector_id=cb.collector_id ORDER BY collections DESC,collector_name",
                summaryParams.toArray()));

        StringBuilder actionWhere = new StringBuilder(" WHERE 1=1");
        List<Object> actionParams = new ArrayList<>();
        if (from != null) { actionWhere.append(" AND f.created_at>=?"); actionParams.add(from); }
        if (to != null) { actionWhere.append(" AND f.created_at<?"); actionParams.add(java.sql.Date.valueOf(to.toLocalDate().plusDays(1))); }
        actionWhere.append(" AND cu.branch_id IN (SELECT id FROM cms_branch WHERE district_id=?)"); actionParams.add(Long.valueOf(district));
        if (!key(branch).isEmpty()) { actionWhere.append(" AND cu.branch_id=?"); actionParams.add(Long.valueOf(branch)); }
        if (!key(collector).isEmpty()) { actionWhere.append(" AND f.collector_id=?"); actionParams.add(Long.valueOf(collector)); }
        if (!key(portfolio).isEmpty()) { actionWhere.append(" AND l.portfolio_id=?"); actionParams.add(Long.valueOf(portfolio)); }
        if (!key(risk).isEmpty()) { actionWhere.append(" AND l.risk=?"); actionParams.add(risk); }
        long total = ((Number) Db.rows(connection,
                "SELECT COUNT(*) total FROM cms_followup f JOIN cms_customer cu ON cu.id=f.customer_id LEFT JOIN cms_loan l ON l.id=f.loan_id"
                        + actionWhere, actionParams.toArray()).get(0).get("TOTAL")).longValue();
        int pageSize = SystemParameters.getInt("pagination.collector-performance");
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(requestedPage, 1), pages);
        data.put("activityTotal", total);
        data.put("activityPage", page);
        data.put("activityPages", pages);
        actionParams.add((page - 1) * pageSize);
        actionParams.add(pageSize);
        data.put("activityRows", Db.rows(connection,
                "SELECT f.created_at,u.display_name collector_name,cu.customer_no,cu.name customer_name,l.account_no,f.channel,f.outcome,f.promise_paise/100 promise_amount,f.promise_date,f.next_action,f.remarks,CASE WHEN f.next_action<TRUNC(SYSDATE) THEN 'DUE' ELSE 'RECORDED' END status FROM cms_followup f JOIN cms_user u ON u.id=f.collector_id JOIN cms_customer cu ON cu.id=f.customer_id LEFT JOIN cms_loan l ON l.id=f.loan_id"
                        + actionWhere + " ORDER BY f.created_at DESC,f.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", actionParams.toArray()));
        if (key(branch).isEmpty()) {
            data.put("collectors", Db.rows(connection,
                    "SELECT u.id,u.display_name FROM cms_user u JOIN cms_role r ON r.id=u.role_id JOIN cms_branch b ON b.id=u.branch_id WHERE r.name='COLLECTOR' AND u.active=1 AND b.district_id=? ORDER BY u.display_name",
                    Long.valueOf(district)));
        } else {
            data.put("collectors", Db.rows(connection,
                    "SELECT u.id,u.display_name FROM cms_user u JOIN cms_role r ON r.id=u.role_id JOIN cms_branch b ON b.id=u.branch_id WHERE r.name='COLLECTOR' AND u.active=1 AND b.district_id=? AND u.branch_id=? ORDER BY u.display_name",
                    Long.valueOf(district), Long.valueOf(branch)));
        }
    }
}
