package com.cms.service;

import com.cms.model.Actor;
import com.cms.repository.Db;
import com.cms.repository.PageRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.CloseableThreadContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class ScreenService {
    private static final Logger LOG = LogManager.getLogger(ScreenService.class);
    private static final PageRepository PAGES = new PageRepository();

    private ScreenService() {}

    @FunctionalInterface
    interface Query {
        void load(Connection connection, Map<String, Object> data) throws SQLException;
    }

    @FunctionalInterface
    interface Transaction {
        String execute(Connection connection) throws SQLException;
    }

    static Map<String, Object> read(Actor actor, String permission, Query query)
            throws SQLException {
        actor.require(permission);
        try (var context = userContext(actor); var connection = Db.open()) {
            LOG.info("Service read started permission={}", permission);
            Map<String, Object> data = new LinkedHashMap<>();
            PAGES.load(connection, actor.id(), data);
            query.load(connection, data);
            LOG.info("Service read completed permission={}", permission);
            return data;
        }
    }

    static void audit(Connection connection, long actorId, String action, String details)
            throws SQLException {
        PAGES.audit(connection, actorId, action, details);
    }

    static String write(Actor actor, String permission, String operation, Transaction transaction)
            throws SQLException {
        actor.require(permission);
        try (var context = userContext(actor); var connection = Db.open()) {
            connection.setAutoCommit(false);
            try {
                String message = transaction.execute(connection);
                PAGES.audit(connection, actor.id(), operation, message);
                connection.commit();
                LOG.info("CMS transaction committed actorId={} operation={}", actor.id(), operation);
                return message;
            } catch (SQLException | RuntimeException failure) {
                LOG.error(
                        "CMS transaction failed actorId={} operation={}",
                        actor.id(),
                        operation,
                        failure);
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    LOG.error(
                            "Failed to roll back CMS transaction for operation {}",
                            operation,
                            rollbackFailure);
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    private static CloseableThreadContext.Instance userContext(Actor actor) {
        return CloseableThreadContext.put("userId", Long.toString(actor.id()))
                .put("userName", actor.username())
                .put("displayName", actor.displayName())
                .put("role", actor.role());
    }
}
