package com.databazoo.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SQLUtilsTest {

    private static final String SQL1 = "SELECT 1";
    private static final String SQL2 = "SELECT ';'";
    private static final String SQL_SELECT_QM = "SELECT *\nFROM pcc_file_upload\nWHERE file_type ? 'POSITION'\nORDER BY id DESC\nLIMIT 200;\n\n";

    @Test
    public void explodeBasic() throws Exception {
        String[] res;

        res = SQLUtils.explode("");
        assertEquals(0, res.length);

        res = SQLUtils.explode(SQL1);
        assertEquals(1, res.length);
        assertEquals(SQL1, res[0]);

        res = SQLUtils.explode(SQL1 + ";" + SQL2);
        assertEquals(2, res.length);
        assertEquals(SQL1 + ";", res[0]);
        assertEquals(SQL2, res[1]);
    }

    @Test
    public void explode() throws Exception {
        String[] res = SQLUtils.explode("SELECT 1;\n\nSELECT ';';\n\n-- COMMENTED ;;;; OUT\n\nUPDATE /** ;;; **/ SET \"key;key\" = 'val;val';\n");
        assertEquals(3, res.length);
        assertEquals("SELECT 1;", res[0]);
        assertEquals("\n\nSELECT ';';", res[1]);
        assertEquals("\n\n-- COMMENTED ;;;; OUT\n\nUPDATE /** ;;; **/ SET \"key;key\" = 'val;val';", res[2]);
    }

    @Test
    public void explodeWithSlash() throws Exception {
        String[] res;

        res = SQLUtils.explodeWithSlash("SELECT 1;\n\nSELECT ';';\n\n-- COMMENTED ;;;; OUT\n\nUPDATE ... SET \"key;key\" = 'val;val';\n");
        assertEquals(3, res.length);
        assertEquals("SELECT 1;", res[0]);
        assertEquals("\n\nSELECT ';';", res[1]);
        assertEquals("\n\n-- COMMENTED ;;;; OUT\n\nUPDATE ... SET \"key;key\" = 'val;val';", res[2]);

        res = SQLUtils.explodeWithSlash("SELECT 1;\n\nSELECT ';';\n/\n-- COMMENTED ;;;; OUT\n\nUPDATE /** ;;; **/ SET \"key;key\" = 'val;val';/\n");
        assertEquals(2, res.length);
        assertEquals("SELECT 1;\n\nSELECT ';';\n/", res[0]);
        assertEquals("\n-- COMMENTED ;;;; OUT\n\nUPDATE /** ;;; **/ SET \"key;key\" = 'val;val';/", res[1]);
    }

    @Test
    public void escapeQM() throws Exception {
        assertEquals(SQL1, SQLUtils.escapeQM(SQL1));
        assertEquals("SELECT 'Will this work?';", SQLUtils.escapeQM("SELECT 'Will this work?';"));
        assertEquals("SELECT 1 FROM a WHERE b ?? 'abc?';", SQLUtils.escapeQM("SELECT 1 FROM a WHERE b ? 'abc?';"));
        assertEquals("-- Just a test?\r\nSELECT 1 FROM a WHERE b ?? 'abc?';\n\n/** anything more here? **/",
                SQLUtils.escapeQM("-- Just a test?\r\nSELECT 1 FROM a WHERE b ? 'abc?';\n\n/** anything more here? **/"));
        assertEquals(SQL_SELECT_QM.replace("?", "??"), SQLUtils.escapeQM(SQL_SELECT_QM));
    }

    @Test
    public void escapeQmFunction() throws Exception {
        String sqlSelectOut = SQL_SELECT_QM.replace("?", "??");
        assertEquals(sqlSelectOut, SQLUtils.escapeQM(SQL_SELECT_QM));

        String sqlFunction = "CREATE OR REPLACE FUNCTION public.new_function_1()\n" +
                "\tRETURNS TABLE () AS\n" +
                "$BODY$\n" +
                "\tSELECT *\n" +
                "\tFROM pcc_file_upload\n" +
                "\tWHERE file_type ? 'POSITION' + $1\n" +
                "\tORDER BY file_type ? 'POSITION';\n" +
                "$BODY$\n" +
                "\tLANGUAGE sql VOLATILE\n" +
                "\tCOST 100;\n\n";
        assertEquals(sqlSelectOut + sqlFunction + sqlSelectOut,
                SQLUtils.escapeQM(SQL_SELECT_QM + sqlFunction + SQL_SELECT_QM));

        sqlFunction = "CREATE OR REPLACE FUNCTION public.new_function_1()\n" +
                "\tRETURNS void AS\n" +
                "$$\n" +
                "\tSELECT *\n" +
                "\tFROM pcc_file_upload\n" +
                "\tWHERE file_type ? 'POSITION' + $1\n" +
                "\tORDER BY file_type ? 'POSITION';\n" +
                "$$\n" +
                "\tLANGUAGE sql VOLATILE\n" +
                "\tCOST 100;\n\n";
        assertEquals(sqlSelectOut + sqlFunction + sqlSelectOut,
                SQLUtils.escapeQM(SQL_SELECT_QM + sqlFunction + SQL_SELECT_QM));

        sqlFunction = "CREATE OR REPLACE FUNCTION public.new_function_1()\n" +
                "\tRETURNS void AS\n" +
                "$$";
        assertEquals(sqlSelectOut + sqlFunction,
                SQLUtils.escapeQM(SQL_SELECT_QM + sqlFunction));

        sqlFunction = "CREATE OR REPLACE FUNCTION public.new_function_1()\n" +
                "\tRETURNS void AS\n" +
                "$BODY$\nSELECT ? $1;";
        assertEquals(sqlSelectOut + sqlFunction,
                SQLUtils.escapeQM(SQL_SELECT_QM + sqlFunction));
    }

}