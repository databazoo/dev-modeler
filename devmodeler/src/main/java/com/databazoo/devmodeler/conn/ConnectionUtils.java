package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.tools.Dbg;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * Provides connection getters outside the conn package.
 */
public interface ConnectionUtils {

    // DATABASE
    String ORIGINAL_DATABASE = "/** ORIGINAL DATABASE **/";
    String CREATE_DATABASE = "CREATE DATABASE ";
    String DROP_DATABASE = "DROP DATABASE ";
    String COMMENT_ON_DATABASE = "COMMENT ON DATABASE ";

    // SCHEMA
    String ORIGINAL_SCHEMA = "/** ORIGINAL SCHEMA **/";
    String CREATE_SCHEMA = "CREATE SCHEMA ";
    String ALTER_SCHEMA = "ALTER SCHEMA ";
    String DROP_SCHEMA = "DROP SCHEMA ";
    String COMMENT_ON_SCHEMA = "COMMENT ON SCHEMA ";

    // TABLE
    String ORIGINAL_TABLE = "/** ORIGINAL TABLE **/";
    String CREATE_TABLE = "CREATE TABLE ";
    String ALTER_TABLE = "ALTER TABLE ";
    String DROP_TABLE = "DROP TABLE ";
    String COMMENT_ON_TABLE = "COMMENT ON TABLE ";

    // COLUMN
    String ORIGINAL_ATTRIBUTE = "/** ORIGINAL ATTRIBUTE **/";
    String ADD_COLUMN = "ADD COLUMN ";
    String ALTER_COLUMN = "ALTER COLUMN ";
    String DROP_COLUMN = "DROP COLUMN ";
    String COMMENT_ON_COLUMN = "COMMENT ON COLUMN ";

    // SEQUENCE
    String ORIGINAL_SEQUENCE = "/** ORIGINAL SEQUENCE **/";
    String CREATE_SEQUENCE = "CREATE SEQUENCE ";
    String ALTER_SEQUENCE = "ALTER SEQUENCE ";
    String DROP_SEQUENCE = "DROP SEQUENCE ";
    String START_WITH = "START WITH ";
    String INCREMENT_BY = "INCREMENT BY ";
    String MINVALUE = "MINVALUE ";
    String MAXVALUE = "MAXVALUE ";
    String RESTART_WITH = " RESTART WITH ";
    String CYCLE = "CYCLE";
    String COMMENT_ON_SEQUENCE = "COMMENT ON SEQUENCE ";

    // VIEW
    String ORIGINAL_VIEW = "/** ORIGINAL VIEW **/";
    String CREATE_OR_REPLACE_VIEW = "CREATE OR REPLACE VIEW ";
    String CREATE_MATERIALIZED_VIEW = "CREATE MATERIALIZED VIEW ";
    String DROP_VIEW = "DROP VIEW ";
    String DROP_MATERIALIZED_VIEW = "DROP MATERIALIZED VIEW ";
    String COMMENT_ON_VIEW = "COMMENT ON VIEW ";

    // FUNCTION
    String ORIGINAL_FUNCTION = "/** ORIGINAL FUNCTION **/";
    String CREATE_OR_REPLACE_FUNCTION = "CREATE OR REPLACE FUNCTION ";
    String DROP_FUNCTION_IF_EXISTS = "DROP FUNCTION IF EXISTS ";
    String PROCEDURE = "PROCEDURE";
    String FUNCTION = "FUNCTION";
    String COMMENT_ON_FUNCTION = "COMMENT ON FUNCTION ";

    // INDEX
    String ALTER_INDEX = "ALTER INDEX ";
    String DROP_INDEX = "DROP INDEX ";
    String COMMENT_ON_INDEX = "COMMENT ON INDEX ";

    // CONSTRAINT
    String ORIGINAL_CONSTRAINT = "/** ORIGINAL CONSTRAINT **/";
    String ADD_CONSTRAINT = "ADD CONSTRAINT ";
    String DROP_CONSTRAINT = "DROP CONSTRAINT ";
    String COMMENT_ON_CONSTRAINT = "COMMENT ON CONSTRAINT ";
    String CONSTRAINT = "CONSTRAINT ";

    // TRIGGER
    String ORIGINAL_TRIGGER = "/** ORIGINAL TRIGGER **/";
    String CREATE_TRIGGER = "CREATE TRIGGER ";
    String EXECUTE_PROCEDURE = "EXECUTE PROCEDURE ";
    String DISABLE_TRIGGER = " DISABLE TRIGGER ";
    String ALTER_TRIGGER = "ALTER TRIGGER ";
    String DROP_TRIGGER = "DROP TRIGGER ";
    String COMMENT_ON_TRIGGER = "COMMENT ON TRIGGER ";

    // PACKAGE
    String ORIGINAL_PACKAGE = "/** ORIGINAL PACKAGE **/";
    String CREATE_OR_REPLACE_PACKAGE = "CREATE OR REPLACE PACKAGE ";
    String CREATE_OR_REPLACE_PACKAGE_BODY = "CREATE OR REPLACE PACKAGE BODY ";
    String DROP_PACKAGE = "DROP PACKAGE ";
    String COMMENT_ON_PACKAGE = "COMMENT ON PACKAGE ";

    // GENERAL STUFF
    String SELECT = "SELECT ";
    String FROM = "FROM ";
    String WHERE = "WHERE ";
    String ORDER_BY = "ORDER BY ";
    String UNION_ALL = "UNION ALL ";
    String ADD = "ADD ";
    String RENAME_TO = " RENAME TO ";
    String ON = " ON ";
    String IS = " IS ";
    String COMMENT = " COMMENT ";
    String COLLATE = " COLLATE ";
    String SET_SCHEMA = " SET SCHEMA ";
    String FULLTEXT_LC = "fulltext";
    String COMMA = ",";

    // SQL CONSTANTS
    String FULL_NAME = "FULL_NAME";
    String OBJECT_NAME = "OBJECT_NAME";
    String TABLE_SCHEMA = "TABLE_SCHEMA";
    String TABLE_NAME = "TABLE_NAME";
    String CON_NAME = "CON_NAME";
    String CON_TYPE = "CON_TYPE";
    String TYPE = "TYPE";
    String OWNER = "OWNER";
    String ATTRS = "ATTRS";
    String DESCRIPTION = "description";


    String ENDS_WITH_SEMICOLON = ".*;\\s*";
    String IS_NUMBER_REGEX = "\\-?([1-9]+[0-9]*\\.?[0-9]*|0|0\\.[0-9]+)";
    String FULL_NAME_REGEX = "(.+)\\.(.+)";
    String ESC_KEYWORDS = "(database|user|from|to|schema|table|relation|function|trigger|constraint|index|date|time|default|null|language)";

    static IConnection getCurrent(String name) {
        return Connection.getCurrent(name);
    }

    static void initConnectionChecker() {
        Connection.initConnectionChecker();
    }

    static String formatTime(Float time) {
        String ret = String.valueOf(time);
        if (time > 100) {
            return ret.replaceAll("(.+)\\..*", "$1") + "s";
        } else if (time > 10) {
            return ret.replaceAll("(.+\\..).*", "$1") + "s";
        } else if (ret.matches(".+\\..{4,}")) {
            return ret.replaceAll("(.+\\..{3}).*", "$1") + "s";
        } else {
            return ret + "s";
        }
    }

    static void checkDrivers() {
        String[] driv = {"org.postgresql.Driver", "com.mysql.jdbc.Driver", "oracle.jdbc.OracleDriver", "SQLServerDriver", "DB2Driver"};
        Enumeration<Driver> e = DriverManager.getDrivers();
        while (e.hasMoreElements()) {
            String val = e.nextElement().toString();
            Dbg.info(val);
            if (val.contains(driv[0])) {
                if(Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
                    DesignGUI.getInfoPanel().writeGray("PostgreSQL driver loaded");
                }
                Connection.POSTGRES_SUPPORTED = true;
            } else if (val.contains(driv[1])) {
                if(Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
                    DesignGUI.getInfoPanel().writeGray("MySQL driver loaded");
                }
                Connection.MYSQL_SUPPORTED = true;
            } else if (val.contains(driv[2])) {
                if(Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
                    DesignGUI.getInfoPanel().writeGray("ORACLE driver loaded");
                }
                Connection.ORACLE_SUPPORTED = true;
            } else if (val.contains(driv[3])) {
                if(Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
                    DesignGUI.getInfoPanel().writeGray("MS SQL Server driver loaded");
                }
                Connection.MSSQL_SUPPORTED = true;
            } else if (val.contains(driv[4])) {
                if(Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
                    DesignGUI.getInfoPanel().writeGray("DB2 driver loaded");
                }
                Connection.DB2_SUPPORTED = true;
            }
        }
    }
}
