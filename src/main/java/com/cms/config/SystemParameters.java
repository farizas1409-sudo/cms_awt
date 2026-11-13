package com.cms.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Loads the application configuration once during web-application startup. */
public final class SystemParameters {
  private static final String CONFIG_RESOURCE = "Application.config";
  private static final String EXTERNAL_CONFIG_PROPERTY = "cms.config.path";
  private static volatile Properties parameters;

  private SystemParameters() {}

  public static synchronized void initialize() {
    if (parameters != null) {
      return;
    }

    Properties loaded = new Properties();
    String externalConfig = System.getProperty(EXTERNAL_CONFIG_PROPERTY);
    try (InputStream stream =
        externalConfig == null || externalConfig.isBlank()
            ? SystemParameters.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)
            : Files.newInputStream(Path.of(externalConfig))) {
      if (stream == null) {
        throw new IllegalStateException(
            "Application configuration was not found: " + CONFIG_RESOURCE);
      }
      loaded.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch (IOException failure) {
      throw new IllegalStateException("Unable to read application configuration", failure);
    }

    validate(loaded);
    configureLogging(loaded);
    parameters = loaded;
    LogManager.getLogger(SystemParameters.class)
        .info("Application configuration loaded from {}", externalConfig == null ? CONFIG_RESOURCE : externalConfig);
  }

  public static String get(String key) {
    return require(key);
  }

  public static String require(String key) {
    Properties current = parameters;
    if (current == null) {
      initialize();
      current = parameters;
    }
    String value = current.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Required application setting is missing: " + key);
    }
    return value.trim();
  }

  public static int getInt(String key) {
    try {
      return Integer.parseInt(require(key));
    } catch (NumberFormatException failure) {
      throw new IllegalStateException("Application setting must be an integer: " + key, failure);
    }
  }

  public static Set<String> getCsvSet(String key) {
    return new LinkedHashSet<>(
        Arrays.stream(require(key).split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList());
  }

  /** Reads ordered key:label choices for desktop selectors and validation. */
  public static Map<String, String> getCsvMap(String key) {
    return parseOptions(require(key), key);
  }

  private static Map<String, String> parseOptions(String value, String key) {
    Map<String, String> options = new LinkedHashMap<>();
    for (String item : value.split(",")) {
      String[] pair = item.trim().split(":", 2);
      if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
        throw new IllegalStateException("Invalid configured option in " + key + ": " + item);
      }
      options.put(pair[0].trim(), pair[1].trim());
    }
    return options;
  }

  private static void validate(Properties loaded) {
    String[] requiredKeys = {
      "application.name",
      "application.short-name",
      "application.company-name",
      "application.footer-name",
      "application.menu.users",
      "application.menu.roles",
      "application.menu.masters",
      "application.menu.risk",
      "application.menu.audit",
      "application.menu.owner",
      "application.menu.collector-performance",
      "application.menu.allocation",
      "application.menu.dashboard",
      "application.menu.customers",
      "application.menu.reports",
      "application.menu.profile",
      "assets.version",
      "database.datasource.jndi",
      "database.jdbc.driver",
      "database.jdbc.url",
      "database.jdbc.username",
      "database.jdbc.password",
      "security.default-user-password",
      "ui.default.theme",
      "ui.default.accent",
      "ui.default.font-size",
      "ui.default.font-family",
      "ui.font-size.minimum",
      "ui.font-size.maximum",
      "ui.theme.options",
      "ui.theme.accents",
      "ui.font-family.options",
      "session.timeout.minutes",
      "pagination.users",
      "pagination.customers",
      "pagination.audit",
      "pagination.collector-reports",
      "pagination.collector-performance",
      "pagination.customer-followups",
      "pagination.payment-history",
      "pagination.communication-history",
      "pagination.customer-followup-summary",
      "dashboard.recent-followups",
      "logging.directory",
      "logging.file-name",
      "logging.archive-pattern",
      "logging.pattern",
      "logging.application-level",
      "logging.root-level",
      "logging.rotation-size",
      "logging.retained-archives"
    };
    for (String key : requiredKeys) {
      String value = loaded.getProperty(key);
      if (value == null || value.isBlank()) {
        throw new IllegalStateException("Required application setting is missing: " + key);
      }
    }
    try {
      Map<String, String> themes =
          parseOptions(loaded.getProperty("ui.theme.options"), "ui.theme.options");
      Map<String, String> themeAccents =
          parseOptions(loaded.getProperty("ui.theme.accents"), "ui.theme.accents");
      Map<String, String> fonts =
          parseOptions(loaded.getProperty("ui.font-family.options"), "ui.font-family.options");
      int fontSize = Integer.parseInt(loaded.getProperty("ui.default.font-size"));
      int minimum = Integer.parseInt(loaded.getProperty("ui.font-size.minimum"));
      int maximum = Integer.parseInt(loaded.getProperty("ui.font-size.maximum"));
      if (!themes.keySet().equals(themeAccents.keySet())
          || !themes.containsKey(loaded.getProperty("ui.default.theme"))
          || !fonts.containsKey(loaded.getProperty("ui.default.font-family"))
          || fontSize < minimum
          || fontSize > maximum
          || minimum > maximum) {
        throw new IllegalStateException(
            "Default appearance settings do not match the configured options");
      }
    } catch (NumberFormatException invalidNumber) {
      throw new IllegalStateException(
          "Appearance number settings must be valid integers", invalidNumber);
    }
  }

  private static void configureLogging(Properties loaded) {
    setLogProperty(loaded, "logging.directory", "cms.logging.directory");
    setLogProperty(loaded, "logging.file-name", "cms.logging.file-name");
    setLogProperty(loaded, "logging.archive-pattern", "cms.logging.archive-pattern");
    setLogProperty(loaded, "logging.pattern", "cms.logging.pattern");
    setLogProperty(loaded, "logging.application-level", "cms.logging.application-level");
    setLogProperty(loaded, "logging.root-level", "cms.logging.root-level");
    setLogProperty(loaded, "logging.rotation-size", "cms.logging.rotation-size");
    setLogProperty(loaded, "logging.retained-archives", "cms.logging.retained-archives");
  }

  private static void setLogProperty(Properties loaded, String setting, String systemProperty) {
    System.setProperty(systemProperty, loaded.getProperty(setting));
  }
}
