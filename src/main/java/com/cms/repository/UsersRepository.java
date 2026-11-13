package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.config.SystemParameters;
import com.cms.model.*;
import com.cms.service.Passwords;

import java.sql.*;
import java.util.*;

public class UsersRepository {
    private static final Logger LOG = LogManager.getLogger(UsersRepository.class);

    public boolean displayNameExists(Connection c, String name, Long excludeId) throws SQLException {
        LOG.info("Repository flow started: displayNameExists()");
        String sql = "SELECT COUNT(*) total FROM cms_user WHERE UPPER(TRIM(display_name))=UPPER(TRIM(?))";
        var rows = excludeId == null
                ? Db.rows(c, sql, name)
                : Db.rows(c, sql + " AND id<>?", name, excludeId);
        return ((Number) rows.get(0).get("TOTAL")).longValue() > 0;
    }

    public Long roleId(Connection c, Long id) throws SQLException {
        LOG.info("Repository flow started: roleId()");
        var rows = Db.rows(c, "SELECT role_id FROM cms_user WHERE id=?", id);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("ROLE_ID")).longValue();
    }

    public void load(Connection c, Map<String, Object> data, Long id, int page)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        int pageSize = SystemParameters.getInt("pagination.users");
        int pageNumber = Math.max(1, page);
        long total =
                ((Number) Db.rows(c, "SELECT COUNT(*) total FROM cms_user").get(0).get("TOTAL"))
                        .longValue();
        data.put(
                "rows",
                Db.rows(
                        c,
                        "SELECT u.id,u.username,u.display_name,u.active,r.name role_name,d.name"
                            + " district_name,b.name branch_name FROM cms_user u JOIN cms_role r ON"
                            + " r.id=u.role_id LEFT JOIN cms_branch b ON b.id=u.branch_id LEFT JOIN"
                            + " cms_district d ON d.id=NVL(u.district_id,b.district_id) ORDER BY"
                            + " u.id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        (pageNumber - 1) * pageSize,
                        pageSize));
        data.put("roleOptions", Db.rows(c, "SELECT id,name FROM cms_role WHERE active=1 ORDER BY name"));
        data.put("districtOptions", Db.rows(c, "SELECT id,name FROM cms_district ORDER BY name"));
        data.put("branchOptions", Db.rows(c, "SELECT id,name,district_id FROM cms_branch ORDER BY name"));
        data.put("totalUsers", total);
        data.put("userPage", pageNumber);
        data.put("userPages", Math.max(1, (total + pageSize - 1) / pageSize));
        if (id != null) {
            List<Map<String, Object>> selected =
                    Db.rows(
                            c,
                            "SELECT"
                                + " u.id,u.display_name,u.role_id,u.branch_id,u.active,NVL(u.district_id,b.district_id)"
                                + " district_id FROM cms_user u LEFT JOIN cms_branch b ON"
                                + " b.id=u.branch_id WHERE u.id=?",
                            id);
            if (!selected.isEmpty()) data.put("customer", selected.get(0));
        }
    }

    public void save(
            Connection c,
            String status,
            Long id,
            Long districtId,
            Long branchId,
            Long roleId,
            String name,
            String password,
            String username)
            throws SQLException {
        LOG.info("Repository flow started: save()");
        if (branchId != null
                && (districtId == null
                        || Db.rows(
                                        c,
                                        "SELECT id FROM cms_branch WHERE id=? AND district_id=?",
                                        branchId,
                                        districtId)
                                .isEmpty()))
            throw new IllegalArgumentException("Choose a branch in the selected district");
        if (roleId != null
                && branchId == null
                && !Db.rows(
                                c,
                                "SELECT name FROM cms_role WHERE id=? AND UPPER(name)='COLLECTOR'",
                                roleId)
                        .isEmpty())
            throw new IllegalArgumentException("Collector users must have a branch");
        if (id == null) {
            String initialPassword =
                    password == null || password.isBlank()
                            ? SystemParameters.get("security.default-user-password")
                            : password;
            Db.update(
                    c,
                    "INSERT INTO"
                        + " cms_user(id,username,display_name,password,role_id,district_id,branch_id,active,must_change_password)"
                        + " VALUES(cms_user_seq.NEXTVAL,?,?,?,?,?,?,1,0)",
                    username,
                    name,
                    Passwords.validatePlainText(initialPassword),
                    roleId,
                    districtId,
                    branchId);
        } else
            Db.update(
                    c,
                    "UPDATE cms_user SET"
                            + " display_name=?,role_id=?,district_id=?,branch_id=?,active=? WHERE"
                            + " id=?",
                    name,
                    roleId,
                    districtId,
                    branchId,
                    "0".equals(status) ? 0 : 1,
                    id);
    }
}
