package com.cms.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cms.model.Actor;
import com.cms.repository.*;

import java.sql.SQLException;
import java.util.Map;

public class AppearanceService {
    private static final Logger LOG = LogManager.getLogger(AppearanceService.class);

    private static final String PERMISSION = null;
    private final AppearanceRepository repository = new AppearanceRepository();

    public Map<String, Object> load(Actor actor) throws SQLException {
        LOG.info("Service flow started: load()");
        return ScreenService.read(actor, PERMISSION, (c, data) -> {});
    }

    public void save(Actor actor, String theme, String accent, String fontSize, String fontFamily)
            throws SQLException {
        LOG.info("Service flow started: save()");
        ScreenService.write(
                actor,
                PERMISSION,
                "SAVE_appearance",
                c -> {
                    repository.save(c, theme, accent, fontSize, fontFamily);
                    return "Changes saved.";
                });
    }
}
