package com.cms.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.cms.repository.QueryValues.*;

import com.cms.model.*;

import java.sql.*;
import java.util.*;

public class MastersRepository {
    private static final Logger LOG = LogManager.getLogger(MastersRepository.class);

    public void load(Connection c, Map<String, Object> data, String entity, boolean fetch)
            throws SQLException {
        LOG.info("Repository flow started: load()");
        String type = validEntity(entity);
        data.put("masterType", type);
        data.put("fetched", fetch);
        if (!fetch) return;
        String sql =
                switch (type) {
                    case "district" ->
                            "SELECT d.id,d.name,s.name state_name FROM cms_district d"
                                    + " JOIN cms_state s ON s.id=d.state_id ORDER BY d.name";
                    case "branch" ->
                            "SELECT b.id,b.code,b.name,d.name district_name,r.name region_name"
                                    + " FROM cms_branch b JOIN cms_district d ON d.id=b.district_id"
                                    + " JOIN cms_region r ON r.id=b.region_id ORDER BY b.name";
                    case "region" -> "SELECT id,name FROM cms_region ORDER BY name";
                    default -> "SELECT id,name FROM cms_portfolio ORDER BY name";
                };
        data.put("rows", Db.rows(c, sql));
    }

    public void save(
            Connection c,
            String entity,
            Long districtId,
            Long regionId,
            Long stateId,
            String reference,
            String name)
            throws SQLException {
        LOG.info("Repository flow started: save()");
        switch (validEntity(entity)) {
            case "district" ->
                    Db.update(
                            c,
                            "INSERT INTO cms_district(id,state_id,name)"
                                    + " VALUES(cms_district_seq.NEXTVAL,?,?)",
                            stateId,
                            name);
            case "branch" ->
                    Db.update(
                            c,
                            "INSERT INTO cms_branch(id,code,name,district_id,region_id)"
                                    + " VALUES(cms_branch_seq.NEXTVAL,?,?,?,?)",
                            reference,
                            name,
                            districtId,
                            regionId);
            case "region" ->
                    Db.update(
                            c,
                            "INSERT INTO cms_region(id,name) VALUES(cms_region_seq.NEXTVAL,?)",
                            name);
            case "portfolio" ->
                    Db.update(
                            c,
                            "INSERT INTO cms_portfolio(id,name)"
                                    + " VALUES(cms_portfolio_seq.NEXTVAL,?)",
                            name);
            default -> throw new IllegalArgumentException("Select a master type");
        }
    }

    private String validEntity(String entity) {
        if (List.of("district", "branch", "region", "portfolio").contains(entity)) return entity;
        throw new IllegalArgumentException("Select a valid master type");
    }
}
