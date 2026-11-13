package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RolesRepository {
    private static final Logger LOG = LogManager.getLogger(RolesRepository.class);

    public void load(Connection c, Map<String, Object> data, Long selectedRoleId)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        List<Map<String, Object>> roles =
                Db.rows(c, "SELECT id,name,active FROM cms_role ORDER BY name");
        List<Map<String, Object>> permissions =
                Db.rows(c, "SELECT id,code FROM cms_permission ORDER BY code");
        for (Map<String, Object> permission : permissions)
            permission.put("LABEL", permissionLabel((String) permission.get("CODE")));
        Map<Object, Map<String, Boolean>> matrix = new LinkedHashMap<>();
        Map<String, java.util.Set<String>> selected = new LinkedHashMap<>();
        for (Map<String, Object> role : roles) {
            long id = ((Number) role.get("ID")).longValue();
            Map<String, Boolean> roleMatrix = new LinkedHashMap<>();
            for (Map<String, Object> permission : permissions)
                roleMatrix.put((String) permission.get("CODE"), Boolean.FALSE);
            List<Map<String, Object>> assigned =
                    Db.rows(
                            c,
                            "SELECT p.code FROM cms_role_permission rp JOIN cms_permission p"
                                    + " ON p.id=rp.permission_id WHERE rp.role_id=?",
                            id);
            java.util.Set<String> codes = new java.util.LinkedHashSet<>();
            for (Map<String, Object> row : assigned) {
                String code = (String) row.get("CODE");
                roleMatrix.put(code, Boolean.TRUE);
                codes.add(code);
            }
            matrix.put(role.get("ID"), roleMatrix);
            selected.put(Long.toString(id), codes);
        }
        data.put("roles", roles);
        data.put("permissions", permissions);
        data.put("permissionMatrix", matrix);
        data.put(
                "selectedPermissions",
                selectedRoleId == null
                        ? java.util.Set.of()
                        : selected.getOrDefault(Long.toString(selectedRoleId), java.util.Set.of()));
        data.put("selectedRoleId", selectedRoleId);
    }

    private String permissionLabel(String code) {
        return switch (code) {
            case "ALLOCATE" -> "Allocate Customers";
            case "COLLECT" -> "View Assigned Customers";
            case "MASTER_MANAGE" -> "Manage Application Masters";
            case "OWNER_REPORT" -> "View Performance Reports";
            case "PAYMENT" -> "Record Payments";
            case "RISK_EXECUTE" -> "Manage and Run Risk Rules";
            case "ROLE_MANAGE" -> "Manage Roles and Permissions";
            case "USER_MANAGE" -> "Manage Users";
            default -> code;
        };
    }

    public String create(Connection c, String name) throws SQLException {
        LOG.info("Repository flow started: create()");
        String normalized = name == null ? "" : name.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_ ]{1,79}"))
            throw new IllegalArgumentException("Enter a valid role name");
        Db.update(
                c,
                "INSERT INTO cms_role(id,name,active) VALUES(cms_role_seq.NEXTVAL,?,1)",
                normalized);
        return "Created role " + normalized + ".";
    }

    public String update(Connection c, Long roleId, List<String> permissionCodes)
            throws SQLException {
        LOG.info("Repository flow started: update()");
        if (roleId == null
                || Db.rows(c, "SELECT id FROM cms_role WHERE id=? FOR UPDATE", roleId).isEmpty())
            throw new IllegalArgumentException("Choose an existing role");
        List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
        for (String code : codes)
            if (Db.rows(c, "SELECT id FROM cms_permission WHERE code=?", code).isEmpty())
                throw new IllegalArgumentException("Unknown permission");
        Db.update(c, "DELETE FROM cms_role_permission WHERE role_id=?", roleId);
        for (String code : codes)
            Db.update(
                    c,
                    "INSERT INTO cms_role_permission(role_id,permission_id)"
                            + " SELECT ?,id FROM cms_permission WHERE code=?",
                    roleId,
                    code);
        return "Updated access for " + codes.size() + " permission(s).";
    }
}
