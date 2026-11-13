-- CMS Version 2.2 complete Oracle 19c database setup.
-- DESTRUCTIVE: drops every CMS_-prefixed object owned by the connected schema, then recreates the CMS schema and loads fictional test data.
-- Run only in a dedicated disposable development/test schema. Never run in production or as SYS/SYSTEM.
-- Run this single file with SQL Developer F5, SQL*Plus, or SQLcl.
SET ECHO OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET PAGESIZE 100
SET TIMING ON
SET VERIFY OFF
SET DEFINE OFF
SET SQLBLANKLINES ON
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
PROMPT === CMS v2.2 full reset begins ===
PROMPT === Dropping prior CMS-owned objects ===
DECLARE
  PROCEDURE drop_objects(p_type VARCHAR2,p_ddl VARCHAR2) IS
  BEGIN
    FOR obj IN (SELECT object_name FROM user_objects WHERE object_type=p_type AND object_name LIKE 'CMS\_%' ESCAPE '\' ORDER BY object_name) LOOP
      EXECUTE IMMEDIATE p_ddl||DBMS_ASSERT.ENQUOTE_NAME(obj.object_name,FALSE);
      DBMS_OUTPUT.PUT_LINE('Dropped '||p_type||' '||obj.object_name);
    END LOOP;
  END;
BEGIN
  drop_objects('MATERIALIZED VIEW','DROP MATERIALIZED VIEW ');
  drop_objects('VIEW','DROP VIEW ');
  drop_objects('TRIGGER','DROP TRIGGER ');
  drop_objects('PACKAGE','DROP PACKAGE ');
  drop_objects('PROCEDURE','DROP PROCEDURE ');
  drop_objects('FUNCTION','DROP FUNCTION ');
  drop_objects('SYNONYM','DROP SYNONYM ');
  FOR obj IN (SELECT table_name FROM user_tables WHERE table_name LIKE 'CMS\_%' ESCAPE '\' ORDER BY table_name) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE '||DBMS_ASSERT.ENQUOTE_NAME(obj.table_name,FALSE)||' CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('Dropped TABLE '||obj.table_name||' and its data');
  END LOOP;
  drop_objects('SEQUENCE','DROP SEQUENCE ');
  FOR obj IN (SELECT object_name FROM user_objects WHERE object_type='TYPE' AND object_name LIKE 'CMS\_%' ESCAPE '\' ORDER BY object_name) LOOP
    EXECUTE IMMEDIATE 'DROP TYPE '||DBMS_ASSERT.ENQUOTE_NAME(obj.object_name,FALSE)||' FORCE';
  END LOOP;
END;
/

PROMPT === Creating CMS v2.2 schema ===
-- Run as dedicated CMS schema owner in Oracle 19c. Financial amounts are stored as INTEGER PAISE.
CREATE TABLE cms_state(id NUMBER(12) PRIMARY KEY,name VARCHAR2(100) NOT NULL UNIQUE);
CREATE TABLE cms_district(id NUMBER(12) PRIMARY KEY,state_id NUMBER(12) NOT NULL REFERENCES cms_state(id),name VARCHAR2(120) NOT NULL,CONSTRAINT uq_cms_district UNIQUE(state_id,name));
CREATE TABLE cms_region(id NUMBER(12) PRIMARY KEY,name VARCHAR2(120) NOT NULL UNIQUE);
CREATE TABLE cms_branch(id NUMBER(12) PRIMARY KEY,code VARCHAR2(20) NOT NULL UNIQUE,name VARCHAR2(120) NOT NULL,district_id NUMBER(12) NOT NULL REFERENCES cms_district(id),region_id NUMBER(12) NOT NULL REFERENCES cms_region(id));
CREATE TABLE cms_role(id NUMBER(12) PRIMARY KEY,name VARCHAR2(80) NOT NULL UNIQUE,active NUMBER(1) DEFAULT 1 NOT NULL);
CREATE TABLE cms_permission(id NUMBER(12) PRIMARY KEY,code VARCHAR2(80) NOT NULL UNIQUE);
CREATE TABLE cms_role_permission(role_id NUMBER(12) NOT NULL REFERENCES cms_role(id),permission_id NUMBER(12) NOT NULL REFERENCES cms_permission(id),PRIMARY KEY(role_id,permission_id));
CREATE TABLE cms_user(id NUMBER(12) PRIMARY KEY,username VARCHAR2(100) NOT NULL UNIQUE,display_name VARCHAR2(150) NOT NULL,password VARCHAR2(100) NOT NULL,role_id NUMBER(12) NOT NULL REFERENCES cms_role(id),district_id NUMBER(12) REFERENCES cms_district(id),branch_id NUMBER(12) REFERENCES cms_branch(id),active NUMBER(1) DEFAULT 1 NOT NULL,must_change_password NUMBER(1) DEFAULT 0 NOT NULL);
CREATE UNIQUE INDEX cms_user_display_name_uq ON cms_user(UPPER(TRIM(display_name)));
CREATE TABLE cms_user_preference(user_id NUMBER(12) PRIMARY KEY REFERENCES cms_user(id),theme VARCHAR2(20) DEFAULT 'SYSTEM' NOT NULL,accent VARCHAR2(12) DEFAULT '#2563eb' NOT NULL,font_size NUMBER(2) DEFAULT 11 NOT NULL);
CREATE TABLE cms_portfolio(id NUMBER(12) PRIMARY KEY,name VARCHAR2(120) NOT NULL UNIQUE);
CREATE TABLE cms_customer(id NUMBER(12) PRIMARY KEY,customer_no VARCHAR2(40) NOT NULL UNIQUE,name VARCHAR2(150) NOT NULL,phone VARCHAR2(20),email VARCHAR2(200),house_no VARCHAR2(100),street VARCHAR2(200),landmark VARCHAR2(200),village VARCHAR2(150),mandal VARCHAR2(150),district VARCHAR2(120),state VARCHAR2(80) DEFAULT 'Andhra Pradesh',pincode VARCHAR2(6),branch_id NUMBER(12) NOT NULL REFERENCES cms_branch(id),gender VARCHAR2(12),date_of_birth DATE,telephone_no VARCHAR2(30),whatsapp_no VARCHAR2(30),customer_status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL);
CREATE TABLE cms_loan(id NUMBER(12) PRIMARY KEY,account_no VARCHAR2(50) NOT NULL UNIQUE,customer_id NUMBER(12) NOT NULL REFERENCES cms_customer(id),portfolio_id NUMBER(12) NOT NULL REFERENCES cms_portfolio(id),loan_amount_paise NUMBER(18,0) DEFAULT 0 NOT NULL,outstanding_paise NUMBER(18,0) DEFAULT 0 NOT NULL,overdue_paise NUMBER(18,0) DEFAULT 0 NOT NULL,dpd NUMBER(6) DEFAULT 0 NOT NULL,risk VARCHAR2(10) DEFAULT 'LOW' NOT NULL,loan_status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,last_payment_date DATE,next_action_date DATE,promise_date DATE,followup_outcome VARCHAR2(80),CONSTRAINT ck_cms_loan_bal CHECK(outstanding_paise>=0 AND overdue_paise>=0 AND overdue_paise<=outstanding_paise));
CREATE TABLE cms_allocation(customer_id NUMBER(12) PRIMARY KEY REFERENCES cms_customer(id),collector_id NUMBER(12) NOT NULL REFERENCES cms_user(id),assigned_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL);
CREATE TABLE cms_payment(id NUMBER(12) PRIMARY KEY,loan_id NUMBER(12) NOT NULL REFERENCES cms_loan(id),amount_paise NUMBER(18,0) NOT NULL,applied_paise NUMBER(18,0) NOT NULL,overdue_applied_paise NUMBER(18,0) NOT NULL,reference VARCHAR2(100) NOT NULL UNIQUE,pymt_mode VARCHAR2(30) NOT NULL,status VARCHAR2(20) NOT NULL,created_by NUMBER(12) NOT NULL REFERENCES cms_user(id),created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,CONSTRAINT ck_cms_payment CHECK(amount_paise>0 AND status IN ('SUCCESS','FAILED','PENDING','REVERSED')));
CREATE TABLE cms_followup(id NUMBER(12) PRIMARY KEY,customer_id NUMBER(12) NOT NULL REFERENCES cms_customer(id),loan_id NUMBER(12) REFERENCES cms_loan(id),collector_id NUMBER(12) NOT NULL REFERENCES cms_user(id),channel VARCHAR2(30) NOT NULL,outcome VARCHAR2(80) NOT NULL,remarks VARCHAR2(2000),next_action DATE,created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,contact_date DATE,promise_paise NUMBER(18,0),promise_date DATE,CONSTRAINT ck_cms_followup_promise CHECK((promise_paise IS NULL AND promise_date IS NULL) OR (promise_paise IS NOT NULL AND promise_paise>0 AND promise_date IS NOT NULL)));
CREATE TABLE cms_communication(id NUMBER(12) PRIMARY KEY,customer_id NUMBER(12) NOT NULL REFERENCES cms_customer(id),loan_id NUMBER(12) REFERENCES cms_loan(id),channel VARCHAR2(30) NOT NULL,recipient VARCHAR2(200),email_used VARCHAR2(200),mobile_used VARCHAR2(30),telephone_used VARCHAR2(30),whatsapp_used VARCHAR2(30),status VARCHAR2(30) NOT NULL,details VARCHAR2(2000),created_by NUMBER(12) NOT NULL REFERENCES cms_user(id),created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL);
CREATE TABLE cms_risk_rule(id NUMBER(12) PRIMARY KEY,portfolio_id NUMBER(12) REFERENCES cms_portfolio(id),min_dpd NUMBER(6) NOT NULL,min_overdue_paise NUMBER(18,0) DEFAULT 0 NOT NULL,risk VARCHAR2(10) NOT NULL,active NUMBER(1) DEFAULT 1 NOT NULL);
CREATE TABLE cms_audit(id NUMBER(12) PRIMARY KEY,actor_id NUMBER(12),action VARCHAR2(80) NOT NULL,details VARCHAR2(2000),created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL);
CREATE INDEX idx_cms_cust_branch ON cms_customer(branch_id);
CREATE INDEX idx_cms_loan_customer ON cms_loan(customer_id);
CREATE INDEX idx_cms_alloc_collector ON cms_allocation(collector_id);
CREATE INDEX idx_cms_payment_loan ON cms_payment(loan_id,created_at);
CREATE INDEX idx_cms_followup_cust ON cms_followup(customer_id,created_at);
CREATE SEQUENCE cms_user_seq START WITH 1000;
CREATE SEQUENCE cms_customer_seq START WITH 100000;
CREATE SEQUENCE cms_loan_seq START WITH 100000;
CREATE SEQUENCE cms_role_seq START WITH 100;
CREATE SEQUENCE cms_region_seq START WITH 100;
CREATE SEQUENCE cms_district_seq START WITH 100;
CREATE SEQUENCE cms_branch_seq START WITH 100;
CREATE SEQUENCE cms_portfolio_seq START WITH 100;
CREATE SEQUENCE cms_risk_rule_seq START WITH 100;
CREATE SEQUENCE cms_followup_seq START WITH 1000;
CREATE SEQUENCE cms_payment_seq START WITH 1000;
CREATE SEQUENCE cms_communication_seq START WITH 1000;
CREATE SEQUENCE cms_audit_seq START WITH 1000;



PROMPT === Applying required objects and indexes ===
-- Oracle 19c additive CMS v2.1 migration. Existing business rows are retained.
-- Run after 01_schema_oracle19c.sql. Safe to rerun.
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  n NUMBER;
  next_id NUMBER;
  seq_value NUMBER;
  PROCEDURE add_column_if_missing(p_table VARCHAR2, p_column VARCHAR2, p_ddl VARCHAR2) IS
    column_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO column_count FROM user_tab_columns
     WHERE table_name = UPPER(p_table) AND column_name = UPPER(p_column);
    IF column_count = 0 THEN EXECUTE IMMEDIATE p_ddl; END IF;
  END;
  PROCEDURE ensure_sequence(p_sequence VARCHAR2, p_table VARCHAR2, p_start NUMBER) IS
    sequence_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO sequence_count FROM user_sequences
     WHERE sequence_name = UPPER(p_sequence);
    EXECUTE IMMEDIATE 'SELECT NVL(MAX(id),0)+1 FROM ' || p_table INTO next_id;
    next_id := GREATEST(next_id, p_start);
    IF sequence_count = 0 THEN
      EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || p_sequence || ' START WITH ' || next_id || ' INCREMENT BY 1 NOCACHE';
    ELSE
      EXECUTE IMMEDIATE 'SELECT ' || p_sequence || '.NEXTVAL FROM dual' INTO seq_value;
      IF seq_value < next_id THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE ' || p_sequence || ' INCREMENT BY ' || (next_id-seq_value);
        EXECUTE IMMEDIATE 'SELECT ' || p_sequence || '.NEXTVAL FROM dual' INTO seq_value;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE ' || p_sequence || ' INCREMENT BY 1';
      END IF;
    END IF;
  END;
BEGIN
  add_column_if_missing('CMS_RISK_RULE','ACTIVE',
    'ALTER TABLE cms_risk_rule ADD (active NUMBER(1) DEFAULT 1 NOT NULL)');
  add_column_if_missing('CMS_RISK_RULE','MIN_OVERDUE_PAISE',
    'ALTER TABLE cms_risk_rule ADD (min_overdue_paise NUMBER(18,0) DEFAULT 0 NOT NULL)');
  add_column_if_missing('CMS_USER','DISTRICT_ID',
    'ALTER TABLE cms_user ADD (district_id NUMBER(12))');
  add_column_if_missing('CMS_USER','MUST_CHANGE_PASSWORD',
    'ALTER TABLE cms_user ADD (must_change_password NUMBER(1) DEFAULT 0 NOT NULL)');
  -- Check the actual FK relationship, not just its constraint name.
  -- Existing foreign keys may use a different name.
  SELECT COUNT(*) INTO n
    FROM user_constraints c
    JOIN user_cons_columns cc
      ON cc.constraint_name = c.constraint_name
     AND cc.table_name = c.table_name
    JOIN user_constraints parent
      ON parent.constraint_name = c.r_constraint_name
    JOIN user_cons_columns pc
      ON pc.constraint_name = parent.constraint_name
     AND pc.position = cc.position
   WHERE c.table_name = 'CMS_USER'
     AND c.constraint_type = 'R'
     AND cc.column_name = 'DISTRICT_ID'
     AND parent.table_name = 'CMS_DISTRICT'
     AND pc.column_name = 'ID'
     AND (SELECT COUNT(*) FROM user_cons_columns cols
           WHERE cols.constraint_name = c.constraint_name
             AND cols.table_name = c.table_name) = 1;
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE cms_user ADD CONSTRAINT fk_cms_user_district FOREIGN KEY (district_id) REFERENCES cms_district(id)';
  ELSE
    DBMS_OUTPUT.PUT_LINE('CMS_USER.DISTRICT_ID foreign key already exists; skipping.');
  END IF;

  add_column_if_missing('CMS_CUSTOMER','GENDER',
    'ALTER TABLE cms_customer ADD (gender VARCHAR2(12))');
  add_column_if_missing('CMS_CUSTOMER','DATE_OF_BIRTH',
    'ALTER TABLE cms_customer ADD (date_of_birth DATE)');
  add_column_if_missing('CMS_CUSTOMER','TELEPHONE_NO',
    'ALTER TABLE cms_customer ADD (telephone_no VARCHAR2(30))');
  add_column_if_missing('CMS_CUSTOMER','WHATSAPP_NO',
    'ALTER TABLE cms_customer ADD (whatsapp_no VARCHAR2(30))');
  add_column_if_missing('CMS_CUSTOMER','CUSTOMER_STATUS',
    'ALTER TABLE cms_customer ADD (customer_status VARCHAR2(20) DEFAULT ''ACTIVE'' NOT NULL)');

  add_column_if_missing('CMS_LOAN','LOAN_AMOUNT_PAISE',
    'ALTER TABLE cms_loan ADD (loan_amount_paise NUMBER(18,0) DEFAULT 0 NOT NULL)');
  add_column_if_missing('CMS_LOAN','LOAN_STATUS',
    'ALTER TABLE cms_loan ADD (loan_status VARCHAR2(20) DEFAULT ''ACTIVE'' NOT NULL)');
  add_column_if_missing('CMS_LOAN','LAST_PAYMENT_DATE',
    'ALTER TABLE cms_loan ADD (last_payment_date DATE)');
  add_column_if_missing('CMS_LOAN','NEXT_ACTION_DATE',
    'ALTER TABLE cms_loan ADD (next_action_date DATE)');
  add_column_if_missing('CMS_LOAN','PROMISE_DATE',
    'ALTER TABLE cms_loan ADD (promise_date DATE)');
  add_column_if_missing('CMS_LOAN','FOLLOWUP_OUTCOME',
    'ALTER TABLE cms_loan ADD (followup_outcome VARCHAR2(80))');

  SELECT COUNT(*) INTO n FROM user_tab_columns
   WHERE table_name='CMS_PAYMENT' AND column_name='PYMT_MODE';
  IF n=0 THEN
    SELECT COUNT(*) INTO n FROM user_tab_columns
     WHERE table_name='CMS_PAYMENT' AND column_name='MODE';
    IF n=1 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE cms_payment RENAME COLUMN mode TO pymt_mode';
    ELSE
      EXECUTE IMMEDIATE 'ALTER TABLE cms_payment ADD (pymt_mode VARCHAR2(30) DEFAULT ''UPI'' NOT NULL)';
    END IF;
  END IF;

  add_column_if_missing('CMS_FOLLOWUP','CONTACT_DATE',
    'ALTER TABLE cms_followup ADD (contact_date DATE)');
  add_column_if_missing('CMS_FOLLOWUP','PROMISE_PAISE',
    'ALTER TABLE cms_followup ADD (promise_paise NUMBER(18,0))');
  add_column_if_missing('CMS_FOLLOWUP','PROMISE_DATE',
    'ALTER TABLE cms_followup ADD (promise_date DATE)');
  SELECT COUNT(*) INTO n FROM user_constraints
   WHERE table_name='CMS_FOLLOWUP' AND constraint_name='CK_CMS_FOLLOWUP_PROMISE';
  IF n=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE cms_followup ADD CONSTRAINT ck_cms_followup_promise CHECK ((promise_paise IS NULL AND promise_date IS NULL) OR (promise_paise IS NOT NULL AND promise_paise > 0 AND promise_date IS NOT NULL))';
  END IF;

  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='CMS_USER_PREFERENCE';
  IF n=0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE cms_user_preference (user_id NUMBER(12) PRIMARY KEY REFERENCES cms_user(id), theme VARCHAR2(20) DEFAULT ''SYSTEM'' NOT NULL, accent VARCHAR2(12) DEFAULT ''#2563eb'' NOT NULL, font_size NUMBER(2) DEFAULT 11 NOT NULL)';
  END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='CMS_APP_CONFIG';
  IF n=0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE cms_app_config (config_key VARCHAR2(100) PRIMARY KEY, config_value VARCHAR2(1000) NOT NULL, updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL)';
  END IF;
  SELECT COUNT(*) INTO n FROM user_indexes WHERE index_name='IDX_CMS_AUDIT_FILTER';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_cms_audit_filter ON cms_audit(created_at, actor_id, id)'; END IF;
  SELECT COUNT(*) INTO n FROM user_indexes WHERE index_name='IDX_CMS_RISK_FILTER';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_cms_risk_filter ON cms_loan(risk, portfolio_id)'; END IF;

  ensure_sequence('CMS_PERMISSION_SEQ','CMS_PERMISSION',100);
  ensure_sequence('CMS_CUSTOMER_SEQ','CMS_CUSTOMER',100000);
  ensure_sequence('CMS_LOAN_SEQ','CMS_LOAN',100000);
  ensure_sequence('CMS_USER_SEQ','CMS_USER',1000);
END;
/
COMMIT;
/

PROMPT === Applying application configuration ===
-- CMS defaults and branding upserts for Oracle 19c. Does not overwrite user preferences or
-- existing risk thresholds. Default permission mappings are inserted only when absent.
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

MERGE INTO cms_app_config t USING (SELECT 'APP.NAME' k, 'CMS – Collections Management System' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'APP.COMPANY' k, 'Bharat Small Finance Company' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'APP.FOOTER' k, 'Bharat Small Finance Company' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'UI.THEME' k, 'SYSTEM' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'UI.ACCENT' k, '#2563eb' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'UI.FONT_SIZE' k, '11' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'UI.FONT_FAMILY' k, 'INTER' v FROM dual) s
ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v, t.updated_at=SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'MENU.USERS' k, 'User Management' v FROM dual) s ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v,t.updated_at=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'MENU.ROLES' k, 'Roles & Permissions' v FROM dual) s ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v,t.updated_at=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'MENU.MASTERS' k, 'Application Masters' v FROM dual) s ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v,t.updated_at=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'MENU.RISK' k, 'Risk Rules' v FROM dual) s ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v,t.updated_at=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);
MERGE INTO cms_app_config t USING (SELECT 'MENU.AUDIT' k, 'Audit Logs' v FROM dual) s ON (t.config_key=s.k) WHEN MATCHED THEN UPDATE SET t.config_value=s.v,t.updated_at=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(config_key,config_value) VALUES(s.k,s.v);

MERGE INTO cms_role t USING (SELECT 'OWNER' name FROM dual) s ON (t.name=s.name)
WHEN NOT MATCHED THEN INSERT(id,name,active) VALUES(cms_role_seq.NEXTVAL,s.name,1);
MERGE INTO cms_role t USING (SELECT 'ADMIN' name FROM dual) s ON (t.name=s.name)
WHEN NOT MATCHED THEN INSERT(id,name,active) VALUES(cms_role_seq.NEXTVAL,s.name,1);
MERGE INTO cms_role t USING (SELECT 'COLLECTOR' name FROM dual) s ON (t.name=s.name)
WHEN NOT MATCHED THEN INSERT(id,name,active) VALUES(cms_role_seq.NEXTVAL,s.name,1);

MERGE INTO cms_permission t USING (SELECT 'OWNER_REPORT' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'USER_MANAGE' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'ROLE_MANAGE' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'MASTER_MANAGE' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'RISK_EXECUTE' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'ALLOCATE' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'COLLECT' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);
MERGE INTO cms_permission t USING (SELECT 'PAYMENT' code FROM dual) s ON (t.code=s.code) WHEN NOT MATCHED THEN INSERT(id,code) VALUES(cms_permission_seq.NEXTVAL,s.code);

MERGE INTO cms_role_permission t USING (SELECT r.id role_id,p.id permission_id FROM cms_role r CROSS JOIN cms_permission p WHERE r.name='OWNER' AND p.code='OWNER_REPORT') s ON (t.role_id=s.role_id AND t.permission_id=s.permission_id) WHEN NOT MATCHED THEN INSERT(role_id,permission_id) VALUES(s.role_id,s.permission_id);
MERGE INTO cms_role_permission t USING (SELECT r.id role_id,p.id permission_id FROM cms_role r CROSS JOIN cms_permission p WHERE r.name='ADMIN' AND p.code IN ('USER_MANAGE','ROLE_MANAGE','MASTER_MANAGE','RISK_EXECUTE','ALLOCATE')) s ON (t.role_id=s.role_id AND t.permission_id=s.permission_id) WHEN NOT MATCHED THEN INSERT(role_id,permission_id) VALUES(s.role_id,s.permission_id);
MERGE INTO cms_role_permission t USING (SELECT r.id role_id,p.id permission_id FROM cms_role r CROSS JOIN cms_permission p WHERE r.name='COLLECTOR' AND p.code IN ('COLLECT','PAYMENT')) s ON (t.role_id=s.role_id AND t.permission_id=s.permission_id) WHEN NOT MATCHED THEN INSERT(role_id,permission_id) VALUES(s.role_id,s.permission_id);


-- Default thresholds are added only when the corresponding category/all-portfolio rule is absent.
INSERT INTO cms_risk_rule(id,portfolio_id,min_dpd,risk,active)
SELECT cms_risk_rule_seq.NEXTVAL,NULL,x.min_dpd,x.risk,1 FROM
(SELECT 0 min_dpd,'LOW' risk FROM dual UNION ALL SELECT 31,'MEDIUM' FROM dual UNION ALL SELECT 61,'HIGH' FROM dual) x
WHERE NOT EXISTS (SELECT 1 FROM cms_risk_rule r WHERE r.portfolio_id IS NULL AND r.risk=x.risk AND r.min_dpd=x.min_dpd);

-- On a fresh empty schema only, insert starter branches and portfolios.
DECLARE
  n NUMBER;
  v_state_id NUMBER;
  PROCEDURE ensure_district(p_name VARCHAR2) IS
    found NUMBER;
  BEGIN
    SELECT COUNT(*) INTO found FROM cms_district WHERE state_id=v_state_id AND name=p_name;
    IF found=0 THEN INSERT INTO cms_district(id,state_id,name) VALUES(cms_district_seq.NEXTVAL,v_state_id,p_name); END IF;
  END;
  PROCEDURE ensure_region(p_name VARCHAR2) IS
    found NUMBER;
  BEGIN
    SELECT COUNT(*) INTO found FROM cms_region WHERE name=p_name;
    IF found=0 THEN INSERT INTO cms_region(id,name) VALUES(cms_region_seq.NEXTVAL,p_name); END IF;
  END;
  PROCEDURE ensure_branch(p_code VARCHAR2,p_name VARCHAR2,p_district VARCHAR2,p_region VARCHAR2) IS
    found NUMBER;
    district_key NUMBER;
    region_key NUMBER;
  BEGIN
    SELECT COUNT(*) INTO found FROM cms_branch WHERE code=p_code;
    IF found=0 THEN
      SELECT id INTO district_key FROM cms_district WHERE state_id=v_state_id AND name=p_district;
      SELECT id INTO region_key FROM cms_region WHERE name=p_region;
      INSERT INTO cms_branch(id,code,name,district_id,region_id) VALUES(cms_branch_seq.NEXTVAL,p_code,p_name,district_key,region_key);
    END IF;
  END;
BEGIN
  SELECT COUNT(*) INTO n FROM cms_branch;
  IF n=0 THEN
    SELECT MIN(id) INTO v_state_id FROM cms_state WHERE name='Andhra Pradesh';
    IF v_state_id IS NULL THEN
      SELECT NVL(MAX(id),0)+1 INTO v_state_id FROM cms_state;
      INSERT INTO cms_state(id,name) VALUES(v_state_id,'Andhra Pradesh');
    END IF;
    ensure_district('Visakhapatnam'); ensure_district('NTR'); ensure_district('Guntur');
    ensure_district('Tirupati'); ensure_district('Kurnool');
    ensure_region('North Coastal'); ensure_region('Central Andhra'); ensure_region('Rayalaseema');
    ensure_branch('MVP','Dwaraka Nagar','Visakhapatnam','North Coastal');
    ensure_branch('GJW','Madhurawada','Visakhapatnam','North Coastal');
    ensure_branch('VJA','Benz Circle','NTR','Central Andhra');
    ensure_branch('GNT','Brodipet','Guntur','Central Andhra');
    ensure_branch('TPT','Renigunta','Tirupati','Rayalaseema');
    ensure_branch('KRN','Adoni','Kurnool','Rayalaseema');
  END IF;
  SELECT COUNT(*) INTO n FROM cms_portfolio;
  IF n=0 THEN
    INSERT INTO cms_portfolio(id,name) VALUES(cms_portfolio_seq.NEXTVAL,'Personal Loan');
    INSERT INTO cms_portfolio(id,name) VALUES(cms_portfolio_seq.NEXTVAL,'Gold Loan');
    INSERT INTO cms_portfolio(id,name) VALUES(cms_portfolio_seq.NEXTVAL,'Business Loan');
    INSERT INTO cms_portfolio(id,name) VALUES(cms_portfolio_seq.NEXTVAL,'Two-Wheeler Loan');
    INSERT INTO cms_portfolio(id,name) VALUES(cms_portfolio_seq.NEXTVAL,'Microfinance Loan');
  END IF;
END;
/
-- Keep the known branch labels aligned with their town/locality names on existing schemas too.
UPDATE cms_branch SET name=CASE code
  WHEN 'MVP' THEN 'Dwaraka Nagar'
  WHEN 'GJW' THEN 'Madhurawada'
  WHEN 'VJA' THEN 'Benz Circle'
  WHEN 'GNT' THEN 'Brodipet'
  WHEN 'TPT' THEN 'Renigunta'
  WHEN 'KRN' THEN 'Adoni'
  ELSE name
END
WHERE code IN ('MVP','GJW','VJA','GNT','TPT','KRN');
COMMIT;

SELECT config_key,config_value FROM cms_app_config ORDER BY config_key;
SELECT r.name role_name, LISTAGG(p.code,', ') WITHIN GROUP(ORDER BY p.code) permissions
FROM cms_role r LEFT JOIN cms_role_permission rp ON rp.role_id=r.id LEFT JOIN cms_permission p ON p.id=rp.permission_id
GROUP BY r.name ORDER BY r.name;
/

PROMPT === Creating application accounts and fictional test data ===
-- CMS v2.2 development data. Integrated into the single complete installer.
-- Sample customers, loans, payments, follow-ups and communications use CMS22 identifiers.
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
  v_load_test_data VARCHAR2(10) := 'YES';
  v_owner_role NUMBER;
  v_admin_role NUMBER;
  v_collector_role NUMBER;
  v_owner_id NUMBER;
  v_admin_id NUMBER;
  v_demo_collector_id NUMBER;
  v_default_branch_id NUMBER;
  v_default_district_id NUMBER;
  v_customer_id NUMBER;
  v_customer_count NUMBER := 0;
  v_branch_count NUMBER;
  v_branch_index NUMBER := 0;
  v_customer_index NUMBER;
  v_global_index NUMBER := 0;
  v_per_branch NUMBER;
  v_collector_slot NUMBER;
  v_collector_id NUMBER;
  v_branch_code VARCHAR2(40);
  v_username VARCHAR2(100);
  v_display_name VARCHAR2(150);
  v_customer_no VARCHAR2(40);
  v_account_no VARCHAR2(50);
  v_district_name VARCHAR2(120);
  v_portfolio_id NUMBER;
  v_portfolio_count NUMBER := 0;
  v_risk_index NUMBER;
  v_risk VARCHAR2(10);
  v_dpd NUMBER;
  v_medium_dpd NUMBER;
  v_high_dpd NUMBER;
  v_loan_amount NUMBER;
  v_payment_amount NUMBER;
  v_outstanding NUMBER;
  v_overdue_before NUMBER;
  v_overdue NUMBER;
  v_last_payment DATE;
  v_next_action DATE;
  v_loan_status VARCHAR2(20);
  v_loan_id NUMBER;
  v_loan_count NUMBER;
  v_payment_mode VARCHAR2(30);
  v_payment_status VARCHAR2(20);
  v_outcome VARCHAR2(80);
  v_promise_amount NUMBER;
  v_promise_date DATE;
  v_channel VARCHAR2(30);
  v_communication_status VARCHAR2(20);
  v_recipient VARCHAR2(200);
  v_collector_key VARCHAR2(120);
  v_first_name VARCHAR2(50);
  v_last_name VARCHAR2(50);
  v_telephone VARCHAR2(30) := '0000000000';
  v_i NUMBER;
  v_j NUMBER;
  v_k NUMBER;
  v_n NUMBER;
  v_gender VARCHAR2(12);
  v_status VARCHAR2(20);
  v_house VARCHAR2(100);
  v_street VARCHAR2(200);
  v_town VARCHAR2(120);
  v_has_existing NUMBER;
  TYPE t_number_list IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
  TYPE t_string_list IS TABLE OF VARCHAR2(50) INDEX BY PLS_INTEGER;
  TYPE t_collector_map IS TABLE OF NUMBER INDEX BY VARCHAR2(120);
  v_portfolio_ids t_number_list;
  v_collector_ids t_collector_map;
  v_collector_first t_string_list;
  v_collector_display_names t_string_list;
  v_first_names t_string_list;
  v_last_names t_string_list;
BEGIN
  IF v_load_test_data NOT IN ('YES','NO') THEN
    RAISE_APPLICATION_ERROR(-20001,'Enter YES or NO for CMS test data.');
  END IF;

  IF v_load_test_data = 'NO' THEN
    DBMS_OUTPUT.PUT_LINE('Development/test records were not loaded. Existing business data is unchanged.');
  ELSE
    -- Existing owner/admin rows keep their IDs and role relationships. This is test-only provisioning.
    SELECT id INTO v_owner_role FROM cms_role WHERE UPPER(name)='OWNER' AND active=1;
    SELECT id INTO v_admin_role FROM cms_role WHERE UPPER(name)='ADMIN' AND active=1;
    SELECT id INTO v_collector_role FROM cms_role WHERE UPPER(name)='COLLECTOR' AND active=1;

    v_collector_first(1):='Suresh';
    v_collector_first(2):='Ramesh';
    v_collector_first(3):='Lakshmi';
    v_collector_first(4):='Venkatesh';
    v_collector_first(5):='Srikanth';
    -- Unique Andhra Pradesh collector names: five collectors for each configured branch.
    v_collector_display_names(1):='Suresh Babu';
    v_collector_display_names(2):='Anusha Reddy';
    v_collector_display_names(3):='Kiran Kumar';
    v_collector_display_names(4):='Bhavani Devi';
    v_collector_display_names(5):='Jagadeesh Naidu';
    v_collector_display_names(6):='Vamsi Krishna';
    v_collector_display_names(7):='Madhavi Chowdary';
    v_collector_display_names(8):='Ravi Teja';
    v_collector_display_names(9):='Haritha Varma';
    v_collector_display_names(10):='Gopi Reddy';
    v_collector_display_names(11):='Sravani Naidu';
    v_collector_display_names(12):='Pavan Kumar';
    v_collector_display_names(13):='Padmaja Rao';
    v_collector_display_names(14):='Sai Kiran';
    v_collector_display_names(15):='Lalitha Devi';
    v_collector_display_names(16):='Mohan Rao';
    v_collector_display_names(17):='Kavitha Reddy';
    v_collector_display_names(18):='Srinivas Babu';
    v_collector_display_names(19):='Divya Prasad';
    v_collector_display_names(20):='Arjun Chowdary';
    v_collector_display_names(21):='Mahesh Naidu';
    v_collector_display_names(22):='Ramya Varma';
    v_collector_display_names(23):='Prathyusha Reddy';
    v_collector_display_names(24):='Venkat Sai';
    v_collector_display_names(25):='Sujatha Kumari';
    v_collector_display_names(26):='Naveen Babu';
    v_collector_display_names(27):='Vijayalakshmi Devi';
    v_collector_display_names(28):='Rajesh Kumar';
    v_collector_display_names(29):='Chandra Sekhar';
    v_collector_display_names(30):='Tejaswi Rao';
    v_last_names(1):='Reddy';
    v_last_names(2):='Naidu';
    v_last_names(3):='Devi';
    v_last_names(4):='Rao';
    v_last_names(5):='Kumar';
    v_last_names(6):='Chowdary';
    v_last_names(7):='Varma';
    v_last_names(8):='Prasad';
    v_last_names(9):='Lakshmi';
    v_last_names(10):='Sharma';
    v_first_names(1):='Anusha'; v_first_names(2):='Kiran'; v_first_names(3):='Madhavi';
    v_first_names(4):='Ravi'; v_first_names(5):='Kavitha'; v_first_names(6):='Prasad';
    v_first_names(7):='Sravani'; v_first_names(8):='Arjun'; v_first_names(9):='Divya';
    v_first_names(10):='Naveen'; v_first_names(11):='Meena'; v_first_names(12):='Srinivas';
    v_first_names(13):='Pavani'; v_first_names(14):='Mahesh'; v_first_names(15):='Sandhya';
    v_first_names(16):='Gopi'; v_first_names(17):='Bhavani'; v_first_names(18):='Vijay';
    v_first_names(19):='Haritha'; v_first_names(20):='Mohan';

    -- The development accounts use the clear-text default requested for this installation.
    -- Demo users can access the screens immediately after signing in.
    SELECT COUNT(*) INTO v_has_existing FROM cms_user WHERE UPPER(username)='OWNER' AND role_id<>v_owner_role;
    IF v_has_existing>0 THEN RAISE_APPLICATION_ERROR(-20002,'OWNER username already belongs to a different role.'); END IF;
    v_owner_id := NULL;
    SELECT MIN(id) INTO v_owner_id FROM cms_user WHERE role_id=v_owner_role AND UPPER(username)='OWNER';
    IF v_owner_id IS NULL THEN SELECT MIN(id) INTO v_owner_id FROM cms_user WHERE role_id=v_owner_role; END IF;
    IF v_owner_id IS NULL THEN
      v_owner_id := cms_user_seq.NEXTVAL;
      INSERT INTO cms_user(id,username,display_name,password,role_id,active,must_change_password)
      VALUES(v_owner_id,'OWNER','OWNER','PAss@1',v_owner_role,1,0);
    ELSE
      UPDATE cms_user SET username='OWNER',display_name='OWNER',password='PAss@1',active=1,must_change_password=0 WHERE id=v_owner_id;
    END IF;

    SELECT COUNT(*) INTO v_has_existing FROM cms_user WHERE UPPER(username)='ADMIN' AND role_id<>v_admin_role;
    IF v_has_existing>0 THEN RAISE_APPLICATION_ERROR(-20003,'ADMIN username already belongs to a different role.'); END IF;
    v_admin_id := NULL;
    SELECT MIN(id) INTO v_admin_id FROM cms_user WHERE role_id=v_admin_role AND UPPER(username)='ADMIN';
    IF v_admin_id IS NULL THEN SELECT MIN(id) INTO v_admin_id FROM cms_user WHERE role_id=v_admin_role; END IF;
    IF v_admin_id IS NULL THEN
      v_admin_id := cms_user_seq.NEXTVAL;
      INSERT INTO cms_user(id,username,display_name,password,role_id,active,must_change_password)
      VALUES(v_admin_id,'ADMIN','ADMIN','PAss@1',v_admin_role,1,0);
    ELSE
      UPDATE cms_user SET username='ADMIN',display_name='ADMIN',password='PAss@1',active=1,must_change_password=0 WHERE id=v_admin_id;
    END IF;

    SELECT MIN(id) INTO v_default_branch_id FROM cms_branch;
    SELECT district_id INTO v_default_district_id FROM cms_branch WHERE id=v_default_branch_id;
    SELECT COUNT(*) INTO v_has_existing FROM cms_user WHERE UPPER(username)='COLLECTOR' AND role_id<>v_collector_role;
    IF v_has_existing>0 THEN RAISE_APPLICATION_ERROR(-20007,'COLLECTOR username already belongs to a different role.'); END IF;
    v_demo_collector_id := NULL;
    SELECT MIN(id) INTO v_demo_collector_id FROM cms_user WHERE role_id=v_collector_role AND UPPER(username)='COLLECTOR';
    IF v_demo_collector_id IS NULL THEN
      v_demo_collector_id := cms_user_seq.NEXTVAL;
      INSERT INTO cms_user(id,username,display_name,password,role_id,district_id,branch_id,active,must_change_password)
      VALUES(v_demo_collector_id,'COLLECTOR','COLLECTOR','PAss@1',v_collector_role,v_default_district_id,v_default_branch_id,1,0);
    ELSE
      UPDATE cms_user SET username='COLLECTOR',display_name='COLLECTOR',password='PAss@1',active=1,must_change_password=0 WHERE id=v_demo_collector_id;
    END IF;

    -- Deactivate only the old generic demo collector; leave all other collector accounts and history intact.
    UPDATE cms_user SET active=0 WHERE UPPER(username)='DEMO.COLLECTOR' AND role_id=v_collector_role;


    SELECT COUNT(*) INTO v_branch_count FROM cms_branch;
    SELECT COUNT(*) INTO v_portfolio_count FROM cms_portfolio;
    IF v_branch_count=0 THEN RAISE_APPLICATION_ERROR(-20004,'No configured branches. Load branch masters before test data.'); END IF;
    IF v_portfolio_count=0 THEN RAISE_APPLICATION_ERROR(-20005,'No configured portfolios. Load portfolios before test data.'); END IF;
    v_per_branch := GREATEST(3,CEIL(1000/v_branch_count));

    v_j := 0;
    FOR p IN (SELECT id FROM cms_portfolio ORDER BY id) LOOP
      v_j := v_j+1;
      v_portfolio_ids(v_j) := p.id;
    END LOOP;

    v_branch_index := 0;
    FOR b IN (SELECT b.id,b.code,b.district_id,d.name district_name FROM cms_branch b JOIN cms_district d ON d.id=b.district_id ORDER BY b.id) LOOP
      v_branch_index := v_branch_index+1;
      v_branch_code := SUBSTR(REGEXP_REPLACE(UPPER(b.code),'[^A-Z0-9]',''),1,15)||'_'||TO_CHAR(b.id);

      FOR v_collector_slot IN 1..5 LOOP
      v_username := v_branch_code || '_C' || LPAD(v_collector_slot,2,'0');
        IF v_branch_index <= 6 THEN
          v_display_name := v_collector_display_names(((v_branch_index-1)*5)+v_collector_slot);
        ELSE
          v_display_name := v_collector_first(v_collector_slot) || ' ' || v_last_names(v_collector_slot) || ' (' || b.code || ')';
        END IF;
        SELECT COUNT(*) INTO v_has_existing FROM cms_user WHERE UPPER(username)=v_username;
        IF v_has_existing=0 THEN
          v_collector_id := cms_user_seq.NEXTVAL;
          INSERT INTO cms_user(id,username,display_name,password,role_id,district_id,branch_id,active,must_change_password)
          VALUES(v_collector_id,v_username,v_display_name,'PAss@1',v_collector_role,b.district_id,b.id,1,0);
        ELSE
          SELECT id INTO v_collector_id FROM cms_user WHERE UPPER(username)=v_username;
          UPDATE cms_user SET display_name=v_display_name,password='PAss@1',role_id=v_collector_role,district_id=b.district_id,branch_id=b.id,active=1,must_change_password=0 WHERE id=v_collector_id;
        END IF;
        v_collector_ids(TO_CHAR(b.id)||':'||TO_CHAR(v_collector_slot)) := v_collector_id;
      END LOOP;

      FOR v_customer_index IN 1..v_per_branch LOOP
        v_global_index := v_global_index+1;
        v_customer_no := 'CMS22-' || v_branch_code || '-' || LPAD(v_customer_index,5,'0');
        v_collector_slot := MOD(v_customer_index-1,5)+1;
        v_collector_id := v_collector_ids(TO_CHAR(b.id)||':'||TO_CHAR(v_collector_slot));
        SELECT COUNT(*) INTO v_has_existing FROM cms_customer WHERE customer_no=v_customer_no;
        IF v_has_existing=0 THEN
          v_customer_id := cms_customer_seq.NEXTVAL;
          v_first_name := v_first_names(MOD(v_customer_index+v_branch_index-2,20)+1);
          v_last_name := v_last_names(MOD(v_customer_index+v_branch_index-2,10)+1);
          v_gender := CASE WHEN MOD(v_customer_index+v_branch_index,2)=0 THEN 'FEMALE' ELSE 'MALE' END;
          v_house := 'Test House ' || LPAD(MOD(v_customer_index*13+v_branch_index,999)+1,3,'0');
          v_street := CASE MOD(v_customer_index+v_branch_index,6)
            WHEN 0 THEN 'Dwaraka Nagar Road' WHEN 1 THEN 'MVP Colony Main Road'
            WHEN 2 THEN 'Gajuwaka Market Road' WHEN 3 THEN 'Station Road'
            WHEN 4 THEN 'Temple Street' ELSE 'Market Yard Road' END;
          v_town := CASE MOD(v_customer_index+v_branch_index,6)
            WHEN 0 THEN 'Visakhapatnam' WHEN 1 THEN 'Vijayawada'
            WHEN 2 THEN 'Guntur' WHEN 3 THEN 'Tirupati'
            WHEN 4 THEN 'Kurnool' ELSE 'Rajahmundry' END;
          INSERT INTO cms_customer(id,customer_no,name,phone,email,house_no,street,landmark,village,mandal,district,state,pincode,branch_id,gender,date_of_birth,telephone_no,whatsapp_no,customer_status)
          VALUES(v_customer_id,v_customer_no,v_first_name||' '||v_last_name,v_telephone,
            LOWER(REPLACE(v_first_name||'.'||v_last_name,' ',''))||'.'||LOWER(v_branch_code)||'.'||LPAD(v_customer_index,5,'0')||'@example.invalid',
            v_house,v_street,'Near '||v_town||' RTC Depot',v_town,b.district_name,b.district_name,'Andhra Pradesh',
            '000000',b.id,v_gender,ADD_MONTHS(DATE '1958-01-01',MOD(v_customer_index*17+v_branch_index*13,564)),v_telephone,v_telephone,
            CASE WHEN MOD(v_customer_index,23)=0 THEN 'INACTIVE' ELSE 'ACTIVE' END);
        ELSE
          SELECT id INTO v_customer_id FROM cms_customer WHERE customer_no=v_customer_no;
        END IF;

        -- Reconcile only generated test allocations; customers always stay within their own branch.
        MERGE INTO cms_allocation a
        USING (SELECT v_customer_id customer_id,v_collector_id collector_id FROM dual) s
           ON (a.customer_id=s.customer_id)
        WHEN MATCHED THEN UPDATE SET a.collector_id=s.collector_id
        WHEN NOT MATCHED THEN INSERT(customer_id,collector_id) VALUES(s.customer_id,s.collector_id);

        v_loan_count := CASE WHEN MOD(v_customer_index,7)=0 THEN 2 ELSE 1 END;
        FOR v_k IN 1..v_loan_count LOOP
          v_risk_index := MOD(v_branch_index+v_customer_index+v_k-3,3);
          v_portfolio_id := v_portfolio_ids(MOD(v_global_index+v_k-2,v_portfolio_count)+1);
          SELECT NVL(MAX(CASE WHEN risk='MEDIUM' THEN min_dpd END),31),
                 NVL(MAX(CASE WHEN risk='HIGH' THEN min_dpd END),61)
            INTO v_medium_dpd,v_high_dpd
            FROM cms_risk_rule
           WHERE active=1 AND (portfolio_id IS NULL OR portfolio_id=v_portfolio_id);
          IF v_high_dpd<=v_medium_dpd THEN
            RAISE_APPLICATION_ERROR(-20006,'Risk rules need HIGH DPD above MEDIUM DPD for portfolio '||v_portfolio_id);
          END IF;
          IF v_risk_index=0 THEN
            v_risk:='LOW'; v_dpd:=MOD(v_customer_index*17+v_k*7,GREATEST(v_medium_dpd,1));
          ELSIF v_risk_index=1 THEN
            v_risk:='MEDIUM'; v_dpd:=v_medium_dpd+MOD(v_customer_index*17+v_k*7,GREATEST(v_high_dpd-v_medium_dpd,1));
          ELSE
            v_risk:='HIGH'; v_dpd:=v_high_dpd+MOD(v_customer_index*17+v_k*7,90);
          END IF;
          v_account_no := 'CMS22-'||v_branch_code||'-'||LPAD(v_customer_index,5,'0')||'-'||v_k;
          SELECT COUNT(*) INTO v_has_existing FROM cms_loan WHERE account_no=v_account_no;
          IF v_has_existing=0 THEN
            v_loan_id := cms_loan_seq.NEXTVAL;
            v_loan_amount := 5000000+MOD(v_global_index*765431+v_k*93421,45000000);
            v_payment_amount := CASE WHEN MOD(v_global_index+v_k,4)=0 THEN LEAST(v_loan_amount,100000+MOD(v_global_index*173+v_k*911,490000)) ELSE 0 END;
            v_outstanding := v_loan_amount-v_payment_amount;
            IF v_dpd=0 THEN v_overdue_before:=0;
            ELSIF v_risk='LOW' THEN v_overdue_before:=TRUNC(v_loan_amount*DBMS_RANDOM.VALUE(0,0.04));
            ELSIF v_risk='MEDIUM' THEN v_overdue_before:=TRUNC(v_loan_amount*DBMS_RANDOM.VALUE(0.04,0.22));
            ELSE v_overdue_before:=TRUNC(v_loan_amount*DBMS_RANDOM.VALUE(0.18,0.45)); END IF;
            v_overdue:=GREATEST(v_overdue_before-v_payment_amount,0);
            v_last_payment := CASE WHEN v_payment_amount>0 THEN TRUNC(SYSDATE)-MOD(v_global_index+v_k,180) ELSE NULL END;
            v_loan_status := CASE WHEN v_dpd=0 THEN 'CURRENT' WHEN v_dpd<61 THEN 'OVERDUE' ELSE 'DELINQUENT' END;
            v_outcome := CASE MOD(v_global_index+v_k,5) WHEN 0 THEN 'Promise to Pay' WHEN 1 THEN 'No Answer' WHEN 2 THEN 'Reached' WHEN 3 THEN 'Completed' ELSE 'Promise to Pay' END;
            v_promise_amount := CASE WHEN v_outcome='Promise to Pay' THEN 100000+MOD(v_global_index*113,900000) ELSE NULL END;
            v_promise_date := CASE WHEN v_promise_amount IS NOT NULL THEN TRUNC(SYSDATE)+5+MOD(v_global_index,21) ELSE NULL END;
            v_next_action := CASE WHEN v_outcome IN ('Promise to Pay','No Answer','Reached') THEN TRUNC(SYSDATE)+MOD(v_global_index,14) ELSE NULL END;
            INSERT INTO cms_loan(id,account_no,customer_id,portfolio_id,loan_amount_paise,outstanding_paise,overdue_paise,dpd,risk,loan_status,last_payment_date,next_action_date,promise_date,followup_outcome)
            VALUES(v_loan_id,v_account_no,v_customer_id,v_portfolio_id,v_loan_amount,v_outstanding,v_overdue,v_dpd,v_risk,v_loan_status,v_last_payment,v_next_action,v_promise_date,v_outcome);

            IF v_payment_amount>0 THEN
              v_payment_mode := CASE MOD(v_global_index+v_k,4) WHEN 0 THEN 'UPI' WHEN 1 THEN 'NEFT' WHEN 2 THEN 'CASH' ELSE 'OTHER' END;
              SELECT COUNT(*) INTO v_has_existing FROM cms_payment WHERE reference=v_account_no||'-PAY-01';
              IF v_has_existing=0 THEN
                INSERT INTO cms_payment(id,loan_id,amount_paise,applied_paise,overdue_applied_paise,reference,pymt_mode,status,created_by,created_at)
                VALUES(cms_payment_seq.NEXTVAL,v_loan_id,v_payment_amount,v_payment_amount,LEAST(v_payment_amount,v_overdue_before),v_account_no||'-PAY-01',v_payment_mode,'SUCCESS',v_collector_id,CAST(v_last_payment AS TIMESTAMP));
              END IF;
            END IF;
            IF MOD(v_global_index+v_k,9)=0 THEN
              v_payment_status := CASE WHEN MOD(v_global_index,2)=0 THEN 'PENDING' ELSE 'FAILED' END;
              v_payment_mode := CASE MOD(v_global_index,4) WHEN 0 THEN 'UPI' WHEN 1 THEN 'NEFT' WHEN 2 THEN 'CASH' ELSE 'OTHER' END;
              SELECT COUNT(*) INTO v_has_existing FROM cms_payment WHERE reference=v_account_no||'-PAY-02';
              IF v_has_existing=0 THEN
                INSERT INTO cms_payment(id,loan_id,amount_paise,applied_paise,overdue_applied_paise,reference,pymt_mode,status,created_by,created_at)
                VALUES(cms_payment_seq.NEXTVAL,v_loan_id,50000+MOD(v_global_index*97,250000),0,0,v_account_no||'-PAY-02',v_payment_mode,v_payment_status,v_collector_id,SYSTIMESTAMP);
              END IF;
            END IF;

            SELECT COUNT(*) INTO v_has_existing FROM cms_followup WHERE loan_id=v_loan_id AND remarks='CMS22_TEST:'||v_account_no;
            IF v_has_existing=0 THEN
              INSERT INTO cms_followup(id,customer_id,loan_id,collector_id,channel,outcome,remarks,next_action,contact_date,promise_paise,promise_date)
              VALUES(cms_followup_seq.NEXTVAL,v_customer_id,v_loan_id,v_collector_id,
                CASE MOD(v_global_index+v_k,4) WHEN 0 THEN 'Telephone' WHEN 1 THEN 'SMS' WHEN 2 THEN 'Email' ELSE 'WhatsApp' END,
                v_outcome,'CMS22_TEST:'||v_account_no||' | Test collection activity',
                v_next_action,
                TRUNC(SYSDATE)-MOD(v_global_index,120),v_promise_amount,v_promise_date);
            END IF;

            v_channel := CASE MOD(v_global_index+v_k,4) WHEN 0 THEN 'EMAIL' WHEN 1 THEN 'SMS' WHEN 2 THEN 'Telephone' ELSE 'WhatsApp' END;
            v_communication_status := 'LOGGED';
            v_recipient := CASE WHEN v_channel='EMAIL'
              THEN 'test.'||LOWER(v_branch_code)||'.'||LPAD(v_customer_index,5,'0')||'@example.invalid'
              ELSE v_telephone END;
            SELECT COUNT(*) INTO v_has_existing FROM cms_communication WHERE loan_id=v_loan_id AND details='CMS22_TEST:'||v_account_no||':'||v_channel;
            IF v_has_existing=0 THEN
              INSERT INTO cms_communication(id,customer_id,loan_id,channel,recipient,email_used,mobile_used,telephone_used,whatsapp_used,status,details,created_by)
              SELECT cms_communication_seq.NEXTVAL,c.id,v_loan_id,v_channel,v_recipient,
                CASE WHEN v_channel='EMAIL' THEN v_recipient ELSE c.email END,
                CASE WHEN v_channel='SMS' THEN v_recipient ELSE c.phone END,
                CASE WHEN v_channel='Telephone' THEN v_recipient ELSE c.telephone_no END,
                CASE WHEN v_channel='WhatsApp' THEN v_recipient ELSE c.whatsapp_no END,
                v_communication_status,'CMS22_TEST:'||v_account_no||':'||v_channel||' | Fictional test activity',v_collector_id
              FROM cms_customer c WHERE c.id=v_customer_id;
            END IF;
          END IF;
        END LOOP;
        v_customer_count := v_customer_count+1;
      END LOOP;
    END LOOP;

    INSERT INTO cms_user_preference(user_id,theme,accent,font_size)
    SELECT u.id,'SYSTEM','#2563eb',11 FROM cms_user u
    WHERE NOT EXISTS (SELECT 1 FROM cms_user_preference p WHERE p.user_id=u.id);
    INSERT INTO cms_audit(id,actor_id,action,details)
    SELECT cms_audit_seq.NEXTVAL,v_admin_id,'TEST_DATA_SEEDED','CMS22_TEST branch sample data generated or reconciled.'
      FROM dual WHERE NOT EXISTS (SELECT 1 FROM cms_audit WHERE action='TEST_DATA_SEEDED' AND details='CMS22_TEST branch sample data generated or reconciled.');
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('CMS22_TEST customers in this seed pass: '||v_customer_count||'; target per branch: '||v_per_branch||'; configured branches: '||v_branch_count);
    DBMS_OUTPUT.PUT_LINE('Demo login: ADMIN, OWNER, COLLECTOR, and generated collectors use PAss@1.');
  END IF;
END;
/
PROMPT === Applying the project-wide default password ===
-- Integrated from Reset_All_User_Passwords_To_Default.sql and
-- Update_Branch_Names_And_Default_Password.sql. Keep every account aligned
-- with the development default after initial and repeatable data seeding.
UPDATE cms_user
SET password='PAss@1', must_change_password=0;
COMMIT;
SELECT username,display_name,active FROM cms_user ORDER BY id;

PROMPT === Test-data verification ===
SELECT COUNT(*) sample_customers FROM cms_customer WHERE customer_no LIKE 'CMS22-%';
SELECT b.code branch_code,b.name branch_name,COUNT(c.id) sample_customers
FROM cms_branch b LEFT JOIN cms_customer c ON c.branch_id=b.id AND c.customer_no LIKE 'CMS22-%'
GROUP BY b.code,b.name ORDER BY b.code;
SELECT l.risk,COUNT(DISTINCT c.id) sample_customers
FROM cms_customer c JOIN cms_loan l ON l.customer_id=c.id
WHERE c.customer_no LIKE 'CMS22-%' GROUP BY l.risk ORDER BY l.risk;
SELECT b.code,l.risk,COUNT(DISTINCT c.id) sample_customers
FROM cms_branch b JOIN cms_customer c ON c.branch_id=b.id JOIN cms_loan l ON l.customer_id=c.id
WHERE c.customer_no LIKE 'CMS22-%' GROUP BY b.code,l.risk ORDER BY b.code,l.risk;
SELECT b.code,COUNT(u.id) branch_collectors
FROM cms_branch b LEFT JOIN cms_user u ON u.branch_id=b.id AND u.active=1
 AND REGEXP_LIKE(u.username,'^'||SUBSTR(REGEXP_REPLACE(UPPER(b.code),'[^A-Z0-9]',''),1,15)||'_'||TO_CHAR(b.id)||'_C[0-9]{2}$')
GROUP BY b.code ORDER BY b.code;
SELECT u.username,COUNT(DISTINCT a.customer_id) assigned_customers
FROM cms_user u LEFT JOIN cms_allocation a ON a.collector_id=u.id
WHERE REGEXP_LIKE(u.username,'^[A-Z0-9]{1,15}_[0-9]+_C[0-9]{2}$') GROUP BY u.username ORDER BY u.username;
SELECT p.name portfolio,COUNT(*) loan_accounts
FROM cms_loan l JOIN cms_customer c ON c.id=l.customer_id JOIN cms_portfolio p ON p.id=l.portfolio_id
WHERE c.customer_no LIKE 'CMS22-%' GROUP BY p.name ORDER BY p.name;
SELECT 'PAYMENT' data_type,COUNT(*) record_count FROM cms_payment WHERE reference LIKE 'CMS22-%'
UNION ALL SELECT 'FOLLOWUP',COUNT(*) FROM cms_followup WHERE remarks LIKE 'CMS22_TEST:%'
UNION ALL SELECT 'COMMUNICATION',COUNT(*) FROM cms_communication WHERE details LIKE 'CMS22_TEST:%';
SELECT COUNT(*) unallocated_test_customers FROM cms_customer c
WHERE c.customer_no LIKE 'CMS22-%' AND NOT EXISTS (SELECT 1 FROM cms_allocation a WHERE a.customer_id=c.id);
SELECT UPPER(username),COUNT(*) duplicate_count FROM cms_user GROUP BY UPPER(username) HAVING COUNT(*)>1;
SELECT c.customer_no,b.code branch_code,a.collector_id,u.branch_id collector_branch
FROM cms_customer c JOIN cms_allocation a ON a.customer_id=c.id JOIN cms_user u ON u.id=a.collector_id
JOIN cms_branch b ON b.id=c.branch_id
WHERE c.customer_no LIKE 'CMS22-%' AND (u.branch_id IS NULL OR u.branch_id<>c.branch_id);




PROMPT === Ensure unique Andhra collector display names ===
-- Keep this correction in the single setup script so seeded accounts remain unique.
UPDATE cms_user
   SET display_name = CASE UPPER(username)
       WHEN 'MVP_100_C01' THEN 'Suresh Babu'
       WHEN 'MVP_100_C02' THEN 'Anusha Reddy'
       WHEN 'MVP_100_C03' THEN 'Kiran Kumar'
       WHEN 'MVP_100_C04' THEN 'Bhavani Devi'
       WHEN 'MVP_100_C05' THEN 'Jagadeesh Naidu'
       WHEN 'GJW_101_C01' THEN 'Vamsi Krishna'
       WHEN 'GJW_101_C02' THEN 'Madhavi Chowdary'
       WHEN 'GJW_101_C03' THEN 'Ravi Teja'
       WHEN 'GJW_101_C04' THEN 'Haritha Varma'
       WHEN 'GJW_101_C05' THEN 'Gopi Reddy'
       WHEN 'VJA_102_C01' THEN 'Sravani Naidu'
       WHEN 'VJA_102_C02' THEN 'Pavan Kumar'
       WHEN 'VJA_102_C03' THEN 'Padmaja Rao'
       WHEN 'VJA_102_C04' THEN 'Sai Kiran'
       WHEN 'VJA_102_C05' THEN 'Lalitha Devi'
       WHEN 'GNT_103_C01' THEN 'Mohan Rao'
       WHEN 'GNT_103_C02' THEN 'Kavitha Reddy'
       WHEN 'GNT_103_C03' THEN 'Srinivas Babu'
       WHEN 'GNT_103_C04' THEN 'Divya Prasad'
       WHEN 'GNT_103_C05' THEN 'Arjun Chowdary'
       WHEN 'TPT_104_C01' THEN 'Mahesh Naidu'
       WHEN 'TPT_104_C02' THEN 'Ramya Varma'
       WHEN 'TPT_104_C03' THEN 'Prathyusha Reddy'
       WHEN 'TPT_104_C04' THEN 'Venkat Sai'
       WHEN 'TPT_104_C05' THEN 'Sujatha Kumari'
       WHEN 'KRN_105_C01' THEN 'Naveen Babu'
       WHEN 'KRN_105_C02' THEN 'Vijayalakshmi Devi'
       WHEN 'KRN_105_C03' THEN 'Rajesh Kumar'
       WHEN 'KRN_105_C04' THEN 'Chandra Sekhar'
       WHEN 'KRN_105_C05' THEN 'Tejaswi Rao'
       END
 WHERE UPPER(username) IN (
       'MVP_100_C01','MVP_100_C02','MVP_100_C03','MVP_100_C04','MVP_100_C05',
       'GJW_101_C01','GJW_101_C02','GJW_101_C03','GJW_101_C04','GJW_101_C05',
       'VJA_102_C01','VJA_102_C02','VJA_102_C03','VJA_102_C04','VJA_102_C05',
       'GNT_103_C01','GNT_103_C02','GNT_103_C03','GNT_103_C04','GNT_103_C05',
       'TPT_104_C01','TPT_104_C02','TPT_104_C03','TPT_104_C04','TPT_104_C05',
       'KRN_105_C01','KRN_105_C02','KRN_105_C03','KRN_105_C04','KRN_105_C05'
 );
COMMIT;
PROMPT === Final object and configuration verification ===

-- Read-only Oracle 19c verification checks. Run after base schema and CMS configuration scripts.
SET PAGESIZE 100
PROMPT CMS-owned tables
SELECT table_name FROM user_tables WHERE table_name LIKE 'CMS\_%' ESCAPE '\' ORDER BY table_name;
PROMPT Invalid Oracle objects
SELECT object_name,object_type,status FROM user_objects WHERE status<>'VALID' ORDER BY object_type,object_name;
PROMPT Role and permission mappings
SELECT r.name role_name,p.code permission_code FROM cms_role_permission rp JOIN cms_role r ON r.id=rp.role_id JOIN cms_permission p ON p.id=rp.permission_id ORDER BY r.name,p.code;
PROMPT Duplicate role-permission mappings
SELECT role_id,permission_id,COUNT(*) duplicate_count FROM cms_role_permission GROUP BY role_id,permission_id HAVING COUNT(*)>1;
PROMPT Risk thresholds
SELECT id,risk,portfolio_id,min_dpd,active FROM cms_risk_rule ORDER BY min_dpd,id;
PROMPT Branch setup
SELECT b.code,b.name,d.name district_name,r.name region_name FROM cms_branch b JOIN cms_district d ON d.id=b.district_id JOIN cms_region r ON r.id=b.region_id ORDER BY b.name;
PROMPT Audit log availability
SELECT COUNT(*) audit_count,MAX(created_at) latest_audit FROM cms_audit;
PROMPT User display preferences
SELECT COUNT(*) user_preference_count FROM cms_user_preference;
PROMPT Required indexes
SELECT index_name,table_name,status FROM user_indexes WHERE index_name IN ('IDX_CMS_AUDIT_FILTER','IDX_CMS_RISK_FILTER','IDX_CMS_CUST_BRANCH','IDX_CMS_LOAN_CUSTOMER') ORDER BY index_name;
PROMPT Required constraints
SELECT constraint_name,table_name,constraint_type,status FROM user_constraints WHERE table_name IN ('CMS_ROLE_PERMISSION','CMS_USER_PREFERENCE','CMS_RISK_RULE') ORDER BY table_name,constraint_name;
/

PROMPT === CMS v2.2 setup finished ===
EXIT SUCCESS

