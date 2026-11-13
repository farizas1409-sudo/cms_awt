package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.config.SystemParameters;
import com.cms.model.*;

import java.sql.*;
import java.util.*;

public class AuditRepository {
    private static final Logger LOG = LogManager.getLogger(AuditRepository.class);

    public void load(
            Connection c,
            Map<String, Object> data,
            String fromDate,
            String toDate,
            Long userId,
            int page)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        int size = SystemParameters.getInt("pagination.audit");
        int pageNo = Math.max(1, page);
        String where =
                " WHERE (TO_DATE(?,'YYYY-MM-DD') IS NULL OR a.created_at>=TO_DATE(?,'YYYY-MM-DD'))"
                    + " AND (TO_DATE(?,'YYYY-MM-DD') IS NULL OR"
                    + " a.created_at<TO_DATE(?,'YYYY-MM-DD')+1) AND (? IS NULL OR a.actor_id=?)";
        Object[] filters = {fromDate, fromDate, toDate, toDate, userId, userId};
        var countRows = Db.rows(c, "SELECT COUNT(*) total FROM cms_audit a" + where, filters);
        long total = ((Number) countRows.get(0).get("TOTAL")).longValue();
        data.put(
                "rows",
                Db.rows(
                        c,
                        "SELECT a.id,a.created_at,a.action,a.details,u.username,"
                                + " REGEXP_SUBSTR(a.action,'^[^_]+') module,'RECORDED' status"
                                + " FROM cms_audit a LEFT JOIN cms_user u ON u.id=a.actor_id"
                                + where
                                + " ORDER BY a.created_at DESC,a.id DESC OFFSET ? ROWS"
                                + " FETCH NEXT ? ROWS ONLY",
                        filters[0],
                        filters[1],
                        filters[2],
                        filters[3],
                        filters[4],
                        filters[5],
                        (pageNo - 1) * size,
                        size));
        data.put("users", Db.rows(c, "SELECT id,username FROM cms_user ORDER BY username"));
        data.put("auditTotal", total);
        data.put("auditPage", pageNo);
        data.put("auditPages", Math.max(1, (total + size - 1) / size));
    }
}
