package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class AuditService {
    private static final Logger LOG = LogManager.getLogger(AuditService.class);

    private static final String PERMISSION = "ROLE_MANAGE";
    private final AuditRepository repository = new AuditRepository();

    public Map<String, Object> load(
            Actor actor, String fromDate, String toDate, Long userId, int page)
            throws SQLException {
        LOG.info("Service flow started: load()");
        java.time.LocalDate from = parseDate(fromDate);
        java.time.LocalDate to = parseDate(toDate);
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("From date must not be after To date");
        return ScreenService.read(
                actor,
                PERMISSION,
                (c, data) -> {
                    repository.load(
                            c, data, blankToNull(fromDate), blankToNull(toDate), userId, page);
                });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private java.time.LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(value);
        } catch (java.time.DateTimeException invalid) {
            throw new IllegalArgumentException("Enter a valid audit date");
        }
    }
}
