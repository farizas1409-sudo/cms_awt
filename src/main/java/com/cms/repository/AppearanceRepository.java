package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.config.SystemParameters;
import com.cms.model.*;
import java.sql.*;
import java.util.*;

public class AppearanceRepository {
    private static final Logger LOG = LogManager.getLogger(AppearanceRepository.class);

  public void save(Connection c, String theme, String accent, String fontSize, String fontFamily)
      throws SQLException {
        LOG.info("Repository flow started: save()");
    if (!SystemParameters.getCsvMap("ui.theme.options").containsKey(theme)
        || !key(accent).matches("#[0-9a-fA-F]{6}")
        || !isConfiguredFontSize(fontSize)
        || !SystemParameters.getCsvMap("ui.font-family.options").containsKey(fontFamily))
      throw new IllegalArgumentException("Invalid preferences");
    saveSetting(c, "UI.THEME", theme);
    saveSetting(c, "UI.ACCENT", accent);
    saveSetting(c, "UI.FONT_SIZE", fontSize);
    saveSetting(c, "UI.FONT_FAMILY", fontFamily);
  }

  private boolean isConfiguredFontSize(String value) {
    try {
      int size = Integer.parseInt(value);
      return size >= SystemParameters.getInt("ui.font-size.minimum")
          && size <= SystemParameters.getInt("ui.font-size.maximum");
    } catch (NumberFormatException invalidSize) {
      return false;
    }
  }

  private void saveSetting(Connection c, String key, String value) throws SQLException {
    Db.update(
        c,
        "MERGE INTO cms_app_config t USING(SELECT ? config_key, ? config_value FROM dual) s"
            + " ON(t.config_key=s.config_key) WHEN MATCHED THEN UPDATE SET"
            + " t.config_value=s.config_value,t.updated_at=SYSTIMESTAMP"
            + " WHEN NOT MATCHED THEN INSERT(config_key,config_value)"
            + " VALUES(s.config_key,s.config_value)",
        key,
        value);
  }
}
