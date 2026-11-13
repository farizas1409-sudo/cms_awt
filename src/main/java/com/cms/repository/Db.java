package com.cms.repository;

import com.cms.config.SystemParameters;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Db {
    private static final Logger LOG = LogManager.getLogger(Db.class);

    private Db() {}

    public static Connection open() throws SQLException {
        try {
            javax.sql.DataSource dataSource =
                    (javax.sql.DataSource)
                            new InitialContext()
                                    .lookup(SystemParameters.get("database.datasource.jndi"));
            Connection connection = dataSource.getConnection();
            LOG.debug("Opened database connection using configured JNDI datasource");
            return connection;
        } catch (NamingException notOnJboss) {
            LOG.warn("JNDI datasource unavailable; opening configured direct JDBC connection");
            try {
                Class.forName(SystemParameters.get("database.jdbc.driver"));
            } catch (ClassNotFoundException missingDriver) {
                LOG.error("Configured JDBC driver is unavailable", missingDriver);
                throw new SQLException("Configured JDBC driver is unavailable", missingDriver);
            }
            oracle.jdbc.pool.OracleDataSource source = new oracle.jdbc.pool.OracleDataSource();
            source.setURL(SystemParameters.get("database.jdbc.url"));
            Connection connection = source.getConnection(
                    SystemParameters.get("database.jdbc.username"),
                    SystemParameters.get("database.jdbc.password"));
            LOG.debug("Opened direct JDBC connection using configured Oracle datasource");
            return connection;
        }
    }

    public static void bind(PreparedStatement p, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
    }

    public static java.util.List<java.util.Map<String, Object>> rows(
            Connection c, String sql, Object... args) throws SQLException {
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        Logger queryLogger = queryLogger();
        String executableSql = executableSql(sql, args);
        long started = System.nanoTime();
        queryLogger.info("Database SELECT started; bindCount={} SQL: {}", args.length, executableSql);
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            try (ResultSet r = p.executeQuery()) {
                ResultSetMetaData m = r.getMetaData();
                while (r.next()) {
                    java.util.Map<String, Object> x = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        int columnType = m.getColumnType(i);
                        Object value =
                                switch (columnType) {
                                    case Types.DATE -> r.getDate(i);
                                    case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                                            r.getTimestamp(i);
                                    default -> r.getObject(i);
                                };
                        x.put(m.getColumnLabel(i).toUpperCase(), value);
                    }
                    out.add(x);
                }
            }
        } catch (SQLException failure) {
            queryLogger.error("Database SELECT failed; SQL: {}", executableSql, failure);
            throw failure;
        }
        queryLogger.info(
                "Database SELECT completed; rows={} elapsedMs={} SQL: {}",
                out.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                executableSql);
        return out;
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        Logger queryLogger = queryLogger();
        String executableSql = executableSql(sql, args);
        long started = System.nanoTime();
        queryLogger.info("Database change started; bindCount={} SQL: {}", args.length, executableSql);
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            int affected = p.executeUpdate();
            queryLogger.info(
                    "Database change completed; rowsAffected={} elapsedMs={} SQL: {}",
                    affected,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                    executableSql);
            return affected;
        } catch (SQLException failure) {
            queryLogger.error("Database change failed; SQL: {}", executableSql, failure);
            throw failure;
        }
    }

    /** Logs with the repository initiating the operation, so the complete category is actionable. */
    private static Logger queryLogger() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(
                        frames ->
                                frames
                                        .map(frame -> frame.getDeclaringClass())
                                        .filter(
                                                type ->
                                                        type.getName().startsWith("com.cms.repository.")
                                                                && type != Db.class)
                                        .findFirst()
                                        .map(LogManager::getLogger)
                                        .orElse(LOG));
    }

    /** Produces a directly executable SQL statement for diagnostics, with credentials redacted. */
    private static String executableSql(String sql, Object... args) {
        boolean[] sensitive = sensitiveParameters(sql, args.length);
        StringBuilder rendered = new StringBuilder(sql.length() + args.length * 16);
        int parameter = 0;
        boolean quoted = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                rendered.append(current);
                if (quoted && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    rendered.append(sql.charAt(++i));
                    continue;
                }
                quoted = !quoted;
            } else if (current == '?' && !quoted && parameter < args.length) {
                rendered.append(
                        sensitive[parameter]
                                ? "'<REDACTED-SECRET>'"
                                : sqlLiteral(args[parameter]));
                parameter++;
            } else {
                rendered.append(current);
            }
        }
        return rendered.toString().strip();
    }

    private static String sqlLiteral(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Date date) return "DATE '" + date.toLocalDate() + "'";
        if (value instanceof Timestamp timestamp) {
            return "TIMESTAMP '" + timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")) + "'";
        }
        if (value instanceof java.util.Date date) {
            return "TIMESTAMP '" + new Timestamp(date.getTime()).toLocalDateTime() + "'";
        }
        if (value instanceof LocalDate date) return "DATE '" + date + "'";
        if (value instanceof LocalDateTime dateTime) return "TIMESTAMP '" + dateTime + "'";
        if (value instanceof OffsetDateTime dateTime) {
            return "TIMESTAMP '" + dateTime.toLocalDateTime() + "'";
        }
        if (value instanceof Number || value instanceof BigDecimal) return value.toString();
        if (value instanceof Boolean flag) return flag ? "1" : "0";
        if (value instanceof byte[] bytes) return "HEXTORAW('" + toHex(bytes) + "')";
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) hex.append(String.format("%02X", value));
        return hex.toString();
    }

    private static boolean[] sensitiveParameters(String sql, int parameterCount) {
        boolean[] sensitive = new boolean[parameterCount];
        String[] assignments = sql.split("\\?");
        for (int i = 0; i < parameterCount && i < assignments.length - 1; i++) {
            String prefix = assignments[i].toLowerCase(java.util.Locale.ROOT).stripTrailing();
            java.util.regex.Matcher secret = java.util.regex.Pattern
                    .compile("(?is)(?:^|\\b(?:and|or|set)\\b|,)\\s*(?:[\\w.]+\\.)?\\b(password|passwd|secret|credential|token|csrf)\\b\\s*(?:=|<>|!=|like|in\\s*\\()\\s*$")
                    .matcher(prefix);
            sensitive[i] = secret.find();
        }
        // INSERT column lists do not sit immediately before their values; correlate their order.
        java.util.regex.Matcher insert = java.util.regex.Pattern
                .compile("(?is)insert\\s+into\\s+[^\\s(]+\\s*\\(([^)]*)\\)\\s*values\\s*\\(([^)]*)\\)")
                .matcher(sql);
        if (insert.find()) {
            List<String> columns = Arrays.stream(insert.group(1).split(","))
                    .map(String::trim)
                    .toList();
            String[] values = insert.group(2).split(",");
            int bindIndex = 0;
            for (int i = 0; i < columns.size() && i < values.length; i++) {
                boolean secretColumn = columns.get(i).toLowerCase(java.util.Locale.ROOT)
                        .matches(".*(password|passwd|secret|credential|token|csrf).*");
                int bindsInValue = (int) values[i].chars().filter(ch -> ch == '?').count();
                if (secretColumn && bindsInValue > 0 && bindIndex < sensitive.length) {
                    sensitive[bindIndex] = true;
                }
                bindIndex += bindsInValue;
            }
        }
        return sensitive;
    }
}
