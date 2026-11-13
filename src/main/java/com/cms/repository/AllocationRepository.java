package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;

import java.sql.*;
import java.util.*;

public class AllocationRepository {
    private static final Logger LOG = LogManager.getLogger(AllocationRepository.class);

    public record AllocationResult(int processed, int allocated, int skipped, int failed) {}

    public void load(Connection c, Map<String, Object> data) throws SQLException {
        LOG.info("Repository flow started: load()");
        String customerBranch = customerBranchExpression(c, "cu0");
        String customerBranchMap =
                "(SELECT cu0.id customer_id, " + customerBranch
                        + " customer_branch_id FROM cms_customer cu0)";
        data.put(
                "rows",
                Db.rows(
                        c,
                "SELECT b.id,b.name,d.name district_name,r.name region_name,"
                                + "NVL(ac.collectors,'Unassigned') collector_name,"
                                + "NVL(ac.assigned_cases,0) assigned_cases,NVL(bt.customers,0) customers,"
                                + "NVL(bt.allocated,0) allocated,"
                                + "NVL(bt.customers,0)-NVL(bt.allocated,0) unassigned "
                                + "FROM cms_branch b JOIN cms_district d ON d.id=b.district_id "
                                + "JOIN cms_region r ON r.id=b.region_id "
                                + "LEFT JOIN (SELECT customer_branch_map.customer_branch_id branch_id,"
                                + "COUNT(*) customers,COUNT(a.customer_id) allocated FROM "
                                + customerBranchMap + " customer_branch_map "
                                + "LEFT JOIN cms_allocation a ON a.customer_id=customer_branch_map.customer_id "
                                + "GROUP BY customer_branch_map.customer_branch_id) bt ON bt.branch_id=b.id "
                                + "LEFT JOIN (SELECT collector_counts.customer_branch_id branch_id,"
                                + "LISTAGG(u.display_name||' ('||collector_counts.assigned_cases||')', ', ') "
                                + "WITHIN GROUP (ORDER BY u.display_name) collectors,"
                                + "SUM(collector_counts.assigned_cases) assigned_cases FROM ("
                                + "SELECT customer_branch_map.customer_branch_id,a.collector_id,"
                                + "COUNT(*) assigned_cases FROM " + customerBranchMap
                                + " customer_branch_map JOIN cms_allocation a "
                                + "ON a.customer_id=customer_branch_map.customer_id "
                                + "GROUP BY customer_branch_map.customer_branch_id,a.collector_id) collector_counts "
                                + "JOIN cms_user u ON u.id=collector_counts.collector_id "
                                + "GROUP BY collector_counts.customer_branch_id) ac ON ac.branch_id=b.id "
                                + "ORDER BY r.name,d.name,b.name"));
    }

    public void loadSummary(Connection c, Map<String, Object> data) throws SQLException {
        LOG.info("Repository flow started: loadSummary()");
        String customerBranch = customerBranchExpression(c, "cu0");
        String customerBranchMap =
                "(SELECT cu0.id customer_id, " + customerBranch
                        + " customer_branch_id FROM cms_customer cu0)";
        data.put(
                "allocationRows",
                Db.rows(
                        c,
                        "SELECT b.id,b.name,d.name district_name,r.name region_name,"
                                + "NVL(mapped.customers,0) customers,NVL(mapped.allocated,0) allocated FROM"
                                + " cms_branch b JOIN cms_district d ON d.id=b.district_id"
                                + " JOIN cms_region r ON r.id=b.region_id LEFT JOIN ("
                                + "SELECT customer_branch_map.customer_branch_id,COUNT(*) customers,"
                                + "COUNT(a.customer_id) allocated FROM " + customerBranchMap
                                + " customer_branch_map LEFT JOIN cms_allocation a "
                                + "ON a.customer_id=customer_branch_map.customer_id "
                                + "GROUP BY customer_branch_map.customer_branch_id"
                                + ") branch_totals ON branch_totals.customer_branch_id=b.id "
                                + "ORDER BY r.name,d.name,b.name"));
    }

    public AllocationResult run(Connection c, String scope, Long branchId) throws SQLException {
        LOG.info("Repository flow started: run()");
        if ("BRANCH".equals(scope) && branchId == null)
            throw new IllegalArgumentException("Choose a branch for selected-branch allocation");
        if (!List.of("BRANCH", "ALL").contains(scope))
            throw new IllegalArgumentException("Choose a valid allocation scope");
        String customerBranch = customerBranchExpression(c, "cu0");
        List<Map<String, Object>> branches;
        if ("ALL".equals(scope)) {
            branches = Db.rows(c, "SELECT id FROM cms_branch ORDER BY id FOR UPDATE");
        } else {
            branches = Db.rows(c, "SELECT id FROM cms_branch WHERE id=? FOR UPDATE", branchId);
            if (branches.isEmpty()) throw new IllegalArgumentException("Unknown branch");
        }
        int processed = 0, allocated = 0, skipped = 0;
        for (Map<String, Object> branch : branches) {
            Object id = branch.get("ID");
            List<Map<String, Object>> customers =
                    Db.rows(
                            c,
                            "SELECT cu0.id FROM cms_customer cu0 WHERE " + customerBranch
                                    + "=? AND cu0.customer_status='ACTIVE'"
                                    + " AND EXISTS(SELECT 1 FROM cms_loan l WHERE l.customer_id=cu0.id) AND NOT"
                                    + " EXISTS(SELECT 1 FROM cms_allocation a WHERE"
                                    + " a.customer_id=cu0.id) ORDER BY cu0.id FOR UPDATE",
                            id);
            processed += customers.size();
            List<Map<String, Object>> collectors =
                    Db.rows(
                            c,
                            "SELECT u.id,(SELECT COUNT(*) FROM cms_allocation a WHERE"
                                + " a.collector_id=u.id) cnt FROM cms_user u JOIN cms_role r ON"
                                + " r.id=u.role_id AND r.active=1 JOIN cms_role_permission rp ON"
                                + " rp.role_id=u.role_id JOIN cms_permission p ON"
                                + " p.id=rp.permission_id AND p.code='COLLECT' WHERE u.active=1 AND"
                                + " u.branch_id=? ORDER BY u.id",
                            id);
            if (collectors.isEmpty()) {
                skipped += customers.size();
                continue;
            }
            for (Map<String, Object> customer : customers) {
                Map<String, Object> best = collectors.get(0);
                for (Map<String, Object> collector : collectors)
                    if (((Number) collector.get("CNT")).longValue()
                            < ((Number) best.get("CNT")).longValue()) best = collector;
                Db.update(
                        c,
                        "INSERT INTO cms_allocation(customer_id,collector_id) VALUES(?,?)",
                        customer.get("ID"),
                        best.get("ID"));
                best.put("CNT", ((Number) best.get("CNT")).longValue() + 1);
                allocated++;
            }
        }
        return new AllocationResult(processed, allocated, skipped, 0);
    }

    /**
     * Newer schemas store the branch directly on CMS_CUSTOMER. Older CMS data
     * identifies generated sample customers by the branch code in CUSTOMER_NO.
     * Match the longest code so e.g. MVP does not shadow MVP_100.
     */
    private String customerBranchExpression(Connection connection, String customerAlias)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS "
                        + "WHERE TABLE_NAME='CMS_CUSTOMER' AND COLUMN_NAME='BRANCH_ID'")) {
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) > 0) {
                    return customerAlias + ".branch_id";
                }
            }
        }

        LOG.warn("CMS_CUSTOMER.BRANCH_ID is absent; resolving generated customer branch from branch code.");
        return "(SELECT mapped_branch.id FROM cms_branch mapped_branch WHERE "
                + "SUBSTR(UPPER(" + customerAlias + ".customer_no),1,6)='CMS22-' AND "
                + "SUBSTR(UPPER(" + customerAlias + ".customer_no),7,LENGTH(mapped_branch.code))="
                + "UPPER(mapped_branch.code) AND "
                + "SUBSTR(UPPER(" + customerAlias + ".customer_no),7+LENGTH(mapped_branch.code),1) IN ('-','_') "
                + "ORDER BY LENGTH(mapped_branch.code) DESC FETCH FIRST 1 ROW ONLY)";
    }
}
