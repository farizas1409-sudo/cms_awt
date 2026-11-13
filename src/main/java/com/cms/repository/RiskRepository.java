package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class RiskRepository {
    private static final Logger LOG = LogManager.getLogger(RiskRepository.class);

    public void load(
            Connection c,
            Map<String, Object> data,
            String riskFilter,
            Long portfolioFilter,
            boolean search,
            Long editId)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        List<Map<String, Object>> rules =
                Db.rows(
                        c,
                        "SELECT rr.id,rr.risk,rr.portfolio_id,p.name"
                            + " portfolio_name,rr.min_dpd,rr.min_overdue_paise,rr.active FROM cms_risk_rule rr LEFT JOIN"
                            + " cms_portfolio p ON p.id=rr.portfolio_id ORDER BY rr.min_dpd,rr.id");
        data.put("rows", rules);
        data.put(
                "riskCategories",
                Db.rows(c, "SELECT DISTINCT risk code FROM cms_risk_rule ORDER BY risk"));
        data.put("riskSearch", search);
        data.put("riskFilter", riskFilter);
        data.put("portfolioFilter", portfolioFilter);
        String selectedRisk = search ? blank(riskFilter) : null;
        Long selectedPortfolio = search ? portfolioFilter : null;
        data.put(
                "riskDistribution",
                Db.rows(
                        c,
                        "SELECT p.name portfolio_name,l.risk,COUNT(DISTINCT l.id) loans,"
                            + " NVL(SUM(l.outstanding_paise),0) outstanding,"
                            + " NVL(SUM(l.overdue_paise),0) overdue FROM cms_loan l"
                            + " JOIN cms_portfolio p ON p.id=l.portfolio_id WHERE (? IS NULL OR"
                            + " l.risk=?) AND (? IS NULL OR l.portfolio_id=?) GROUP BY"
                            + " p.name,l.risk ORDER BY p.name,l.risk",
                        selectedRisk,
                        selectedRisk,
                        selectedPortfolio,
                        selectedPortfolio));
        if (editId != null) {
            List<Map<String, Object>> selected =
                    Db.rows(
                            c,
                            "SELECT id,risk,portfolio_id,min_dpd,min_overdue_paise,active FROM cms_risk_rule WHERE"
                                    + " id=?",
                            editId);
            if (!selected.isEmpty()) data.put("editRule", selected.get(0));
        }
    }

    public void save(
            Connection c, Long id, String risk, Long portfolioId, String minDpd, String minOverdue)
            throws SQLException {
        LOG.info("Repository flow started: save()");
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(risk))
            throw new IllegalArgumentException("Choose a valid risk category");
        int threshold;
        try {
            threshold = Integer.parseInt(minDpd);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Enter a valid DPD threshold");
        }
        if (threshold < 0 || threshold > 99999)
            throw new IllegalArgumentException("DPD must be from 0 to 99999");
        long overdueThreshold;
        try {
            overdueThreshold =
                    new BigDecimal(minOverdue)
                            .setScale(2, RoundingMode.UNNECESSARY)
                            .movePointRight(2)
                            .longValueExact();
        } catch (RuntimeException invalidAmount) {
            throw new IllegalArgumentException("Enter a valid minimum overdue amount");
        }
        if (overdueThreshold < 0 || overdueThreshold > 999999999999999999L)
            throw new IllegalArgumentException("Minimum overdue amount is out of range");
        if (id == null) {
            Db.update(
                    c,
                    "INSERT INTO cms_risk_rule(id,portfolio_id,min_dpd,min_overdue_paise,risk,active)"
                            + " VALUES(cms_risk_rule_seq.NEXTVAL,?,?,?,?,1)",
                    portfolioId,
                    threshold,
                    overdueThreshold,
                    risk);
        } else {
            if (Db.rows(c, "SELECT id FROM cms_risk_rule WHERE id=? FOR UPDATE", id).isEmpty())
                throw new IllegalArgumentException("Risk rule no longer exists");
            Db.update(
                    c,
                    "UPDATE cms_risk_rule SET portfolio_id=?,min_dpd=?,min_overdue_paise=?,risk=? WHERE id=?",
                    portfolioId,
                    threshold,
                    overdueThreshold,
                    risk,
                    id);
        }
    }

    public void delete(Connection c, Long id) throws SQLException {
        LOG.info("Repository flow started: delete()");
        if (id == null || Db.update(c, "DELETE FROM cms_risk_rule WHERE id=?", id) != 1)
            throw new IllegalArgumentException("Risk rule no longer exists");
    }

    public void run(Connection c) throws SQLException {
        LOG.info("Repository flow started: run()");
        Db.update(
                c,
                "UPDATE cms_loan l SET risk=CASE WHEN EXISTS (SELECT 1 FROM cms_risk_rule rr"
                        + " WHERE rr.active=1 AND rr.risk='HIGH' AND"
                        + " (rr.portfolio_id=l.portfolio_id OR rr.portfolio_id IS NULL)"
                        + " AND l.dpd>=rr.min_dpd AND l.overdue_paise>=rr.min_overdue_paise)"
                        + " THEN 'HIGH' WHEN EXISTS (SELECT 1 FROM cms_risk_rule rr"
                        + " WHERE rr.active=1 AND rr.risk='MEDIUM' AND"
                        + " (rr.portfolio_id=l.portfolio_id OR rr.portfolio_id IS NULL)"
                        + " AND l.dpd>=rr.min_dpd AND l.overdue_paise>=rr.min_overdue_paise)"
                        + " THEN 'MEDIUM' ELSE 'LOW' END");
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
