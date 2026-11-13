package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Identity;
import com.cms.repository.Db;
import com.cms.repository.IdentityRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalLong;

public class IdentityService {
    private static final Logger LOG = LogManager.getLogger(IdentityService.class);

    private final IdentityRepository repository = new IdentityRepository();

    public OptionalLong authenticate(String username, String password) throws SQLException {
        LOG.info("Service flow started: authenticate()");
        try (var connection = Db.open()) {
            var credentials = repository.findCredentials(connection, username);
            if (credentials.isEmpty()) return OptionalLong.empty();
            var user = credentials.get();
            if (!Passwords.matches(password, (String) user.get("PASSWORD")))
                return OptionalLong.empty();
            long userId = ((Number) user.get("ID")).longValue();
            return OptionalLong.of(userId);
        }
    }

    public Optional<Identity> findActiveIdentity(long id) throws SQLException {
        LOG.info("Service flow started: findActiveIdentity()");
        try (var connection = Db.open()) {
            return repository.findActiveIdentity(connection, id);
        }
    }
}
