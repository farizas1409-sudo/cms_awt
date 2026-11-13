package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.config.SystemParameters;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class PageRepository {
    private static final Logger LOG = LogManager.getLogger(PageRepository.class);

  public void load(Connection c, long userId, Map<String, Object> data) throws SQLException {
        LOG.info("Repository flow started: load()");
    data.put("districts", Db.rows(c, "SELECT id,name FROM cms_district ORDER BY name"));
    data.put("branches", Db.rows(c, "SELECT id,name,district_id FROM cms_branch ORDER BY name"));
    data.put("regions", Db.rows(c, "SELECT id,name FROM cms_region ORDER BY name"));
    data.put("states", Db.rows(c, "SELECT id,name FROM cms_state ORDER BY name"));
    data.put("roles", Db.rows(c, "SELECT id,name FROM cms_role WHERE active=1 ORDER BY name"));
    data.put("portfolios", Db.rows(c, "SELECT id,name FROM cms_portfolio ORDER BY name"));
    Map<String, Object> branding = new java.util.HashMap<>();
    for (Map<String, Object> item :
        Db.rows(c, "SELECT config_key,config_value FROM cms_app_config"))
      branding.put((String) item.get("CONFIG_KEY"), item.get("CONFIG_VALUE"));
    data.put("theme", branding.getOrDefault("UI.THEME", SystemParameters.get("ui.default.theme")));
    data.put(
        "accent", branding.getOrDefault("UI.ACCENT", SystemParameters.get("ui.default.accent")));
    data.put(
        "fontSize",
        branding.getOrDefault("UI.FONT_SIZE", SystemParameters.get("ui.default.font-size")));
    data.put(
        "fontFamily",
        branding.getOrDefault("UI.FONT_FAMILY", SystemParameters.get("ui.default.font-family")));
    data.put("minimumFontSize", SystemParameters.getInt("ui.font-size.minimum"));
    data.put("maximumFontSize", SystemParameters.getInt("ui.font-size.maximum"));
    data.put("defaultTheme", SystemParameters.get("ui.default.theme"));
    data.put("defaultAccent", SystemParameters.get("ui.default.accent"));
    data.put("defaultFontSize", SystemParameters.get("ui.default.font-size"));
    data.put("defaultFontFamily", SystemParameters.get("ui.default.font-family"));
    data.put("themeOptions", SystemParameters.getCsvMap("ui.theme.options"));
    data.put("themeAccents", SystemParameters.getCsvMap("ui.theme.accents"));
    data.put("fontFamilyOptions", SystemParameters.getCsvMap("ui.font-family.options"));
    data.put("auditPageSize", SystemParameters.getInt("pagination.audit"));
    data.put("assetVersion", SystemParameters.get("assets.version"));
    data.put(
        "companyName",
        branding.getOrDefault("APP.COMPANY", SystemParameters.get("application.company-name")));
    data.put(
        "applicationName",
        branding.getOrDefault("APP.NAME", SystemParameters.get("application.name")));
    data.put(
        "footerName",
        branding.getOrDefault("APP.FOOTER", SystemParameters.get("application.footer-name")));
    data.put(
        "menuUsers",
        branding.getOrDefault("MENU.USERS", SystemParameters.get("application.menu.users")));
    data.put(
        "menuRoles",
        branding.getOrDefault("MENU.ROLES", SystemParameters.get("application.menu.roles")));
    data.put(
        "menuMasters",
        branding.getOrDefault("MENU.MASTERS", SystemParameters.get("application.menu.masters")));
    data.put(
        "menuRisk",
        branding.getOrDefault("MENU.RISK", SystemParameters.get("application.menu.risk")));
    data.put(
        "menuAudit",
        branding.getOrDefault("MENU.AUDIT", SystemParameters.get("application.menu.audit")));
    data.put("menuOwner", SystemParameters.get("application.menu.owner"));
    data.put(
        "menuCollectorPerformance", SystemParameters.get("application.menu.collector-performance"));
    data.put("menuAllocation", SystemParameters.get("application.menu.allocation"));
    data.put("menuDashboard", SystemParameters.get("application.menu.dashboard"));
    data.put("menuCustomers", SystemParameters.get("application.menu.customers"));
    data.put("menuReports", SystemParameters.get("application.menu.reports"));
    data.put("menuProfile", SystemParameters.get("application.menu.profile"));
    data.put("applicationShortName", SystemParameters.get("application.short-name"));
    data.put("defaultUserPassword", SystemParameters.get("security.default-user-password"));
  }

  public void audit(Connection c, long actorId, String action, String details) throws SQLException {
        LOG.info("Repository flow started: audit()");
    Db.update(
        c,
        "INSERT INTO cms_audit(id,actor_id,action,details)"
            + " VALUES(cms_audit_seq.NEXTVAL,?,?,?)",
        actorId,
        action,
        details);
  }
}
