package com.databazoo.devmodeler.conn;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Requires a functioning DB setup")
public class WarningTest {

    @Test
    public void readWarnings() throws Exception {
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/pcc", "pcc_user", "ghbnghbn");
        createTestFunction(con);

        PreparedStatement statement = con.prepareStatement("SELECT test_function();");
        SQLWarning[] warnings = new SQLWarning[1];
        new Timer().schedule(new TimerTask() {
            @Override public void run() {
                try {
                    warnings[0] = statement.getWarnings();
                } catch (SQLException e) {
                    Assert.fail("Exception thrown: " + e.getMessage());
                }
            }
        }, 1500);
        statement.executeQuery();
        Thread.sleep(1000);
        statement.close();

        Assert.assertNotNull(warnings[0]);
        Assert.assertFalse(warnings[0].getMessage().isEmpty());
    }

    private void createTestFunction(Connection con) throws SQLException {
        final PreparedStatement statement = con.prepareStatement(
                "CREATE OR REPLACE FUNCTION test_function() RETURNS VOID AS\n"
                + "$BODY$\n"
                + "BEGIN\n"
                + "\tFOR i IN 1..3 LOOP \n"
                + "\t\tRAISE NOTICE 'Tick %', i;\n"
                + "\t\tEXECUTE pg_sleep(1);\n"
                + "\tEND LOOP;\n"
                + "END\n"
                + "$BODY$\n"
                + "\tLANGUAGE plpgsql STABLE;");
        statement.execute();
        statement.close();
    }
}
