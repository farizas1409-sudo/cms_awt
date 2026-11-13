package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;
import com.cms.service.Passwords;

import java.sql.*;
import java.util.*;

public class ProfileRepository {
    private static final Logger LOG = LogManager.getLogger(ProfileRepository.class);

    public void save(Connection c, long userId, String currentPassword, String password)
            throws SQLException {
        LOG.info("Repository flow started: save()");
        String oldPassword =
                String.valueOf(
                        Db.rows(
                                        c,
                                        "SELECT password FROM cms_user WHERE" + " id=? FOR UPDATE",
                                        userId)
                                .get(0)
                                .get("PASSWORD"));
        if (!Passwords.matches(currentPassword, oldPassword))
            throw new IllegalArgumentException("Incorrect current password");
        Db.update(
                c,
                "UPDATE cms_user SET password=?,must_change_password=0 WHERE id=?",
                Passwords.validatePlainText(password),
                userId);
    }
}
