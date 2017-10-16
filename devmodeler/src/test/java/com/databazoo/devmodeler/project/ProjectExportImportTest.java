package com.databazoo.devmodeler.project;

import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ProjectExportImportTest extends TestProjectSetup {

    private static final File EXPORT_FILE = new File("/tmp/exportProjectTest.xml");
    private static final File IMPORT_FILE = new File("/tmp/importProjectTest.xml");

    @Before
    public void init() throws InterruptedException {
        setProjectUp();
        finalizeData();
        CanvasTest.initializeCanvas();
    }

    @Test
    public void runExport() throws Exception {
        assertEquals(1.0, Canvas.getZoom(), 0.0001);

        relation.setLocation(new Point(60, 60));
        relation2.setLocation(new Point(90, 90));

        schema.checkSize();
        publicSchema.checkSize();

        ProjectExportImport.getInstance().runExport(Project.getCurrent(), EXPORT_FILE);

        String content = new String(Files.readAllBytes(Paths.get(EXPORT_FILE.getAbsolutePath())));
        assertEquals("  <project name=\"test1\" type=\"2\">\n" +
                "    <server color=\"DEFAULT\" type=\"0\"/>\n" +
                "    <database name=\"test 1\">\n" +
                "      <locations>\n" +
                "        <loc name=\"test 2.test 4\" value=\"90,90\"/>\n" +
                "        <loc name=\"test 2.test 3\" value=\"60,60\"/>\n" +
                "      </locations>\n" +
                "      <schemata>\n" +
                "        <schema descr=\"public schema\" loc=\"0,0\" name=\"public\" size=\"216,30\"/>\n" +
                "        <schema descr=\"test 2\" loc=\"0,0\" name=\"test 2\" size=\"331,153\">\n" +
                "          <table loc=\"60,60\" name=\"test 2.test 3\" pk=\"1\" rows=\"0\" sizeI=\"0\" sizeT=\"0\" store=\"MyISAM\">\n" +
                "            <column def=\"'defval'\" descr=\"test attr 1\" name=\"test attr 1\" num=\"1\" store=\"x\" type=\"varchar\"/>\n" +
                "            <column descr=\"test attr 2\" name=\"test attr 2\" num=\"2\" store=\"a\" type=\"varchar\"/>\n" +
                "            <index con=\"1\" def=\"&quot;test attr 1&quot;\" descr=\"test PK\" name=\"test PK\" pkey=\"1\" ux=\"1\"/>\n" +
                "            <index def=\"&quot;test attr 2&quot;\" descr=\"test UX\" name=\"test UX\" ux=\"1\"/>\n" +
                "          </table>\n" +
                "          <table loc=\"90,90\" name=\"test 2.test 4\" rows=\"0\" sizeI=\"0\" sizeT=\"0\" store=\"MyISAM\">\n" +
                "            <column def=\"'defval'\" descr=\"test attr 3\" name=\"test attr 3\" num=\"1\" store=\"a\" type=\"varchar\"/>\n" +
                "            <column descr=\"test attr 4\" name=\"test attr 4\" null=\"1\" num=\"2\" store=\"a\" type=\"varchar\"/>\n" +
                "            <index con=\"1\" def=\"&quot;test attr 3&quot;\" descr=\"test UC\" name=\"test UC\" ux=\"1\"/>\n" +
                "            <index def=\"&quot;test attr 4&quot;\" descr=\"test index\" name=\"test index\" where=\"&quot;test attr 4&quot; IS NOT NULL\"/>\n" +
                "          </table>\n" +
                "          <package body=\"TEST PACKAGE BODY;&#10;&#10;\" descr=\"test package\" loc=\"0,0\" name=\"test package\" src=\"TEST PACKAGE SOURCE;&#10;&#10;\"/>\n" +
                "          <function args=\"int input\" cost=\"10\" descr=\"test function\" lang=\"plpgsql\" loc=\"0,0\" name=\"test function\" rows=\"100\" src=\"&#10;RETURN CAST(input AS varchar);&#10;\" type=\"varchar\" vlt=\"stable\"/>\n" +
                "          <function cost=\"0\" descr=\"test trigger function\" lang=\"plpgsql\" loc=\"0,0\" name=\"test trigger function\" rows=\"0\" src=\"&#10;RETURN NEW;&#10;\" type=\"trigger\" vlt=\"volatile\"/>\n" +
                "          <view descr=\"test view\" loc=\"0,0\" name=\"test view\" src=\"SELECT * FROM &quot;test 4&quot;\"/>\n" +
                "          <sequence current=\"5\" cycle=\"1\" descr=\"test sequence\" inc=\"1\" loc=\"0,0\" max=\"1000000\" min=\"0\" name=\"test sequence\"/>\n" +
                "        </schema>\n" +
                "      </schemata>\n" +
                "      <constraints>\n" +
                "        <constraint attr1=\"test attr 1\" attr2=\"test attr 3\" delete=\"c\" descr=\"test 5\" name=\"test 5\" rel1=\"test 2.test 3\" rel2=\"test 2.test 4\" update=\"c\"/>\n" +
                "        <constraint def=\"CHECK ( id &gt; 0 )\" descr=\"test 6\" name=\"test 6\" rel1=\"test 2.test 3\"/>\n" +
                "      </constraints>\n" +
                "      <triggers>\n" +
                "        <trigger attrs=\"test attr 3, test attr 4\" def=\"test trigger function\" descr=\"test trigger\" disabled=\"1\" events=\"u\" name=\"test trigger\" rel1=\"test 2.test 4\" timing=\"b\" when=\"&quot;test attr 4&quot; IS NOT NULL\"/>\n" +
                "      </triggers>\n" +
                "    </database>\n" +
                "  </project>\n" +
                "</projects>\n", content.substring(content.indexOf("\">")+3));  // Substring because of variable info in PROJECTS tag
    }

    @Test
    public void runImport() throws Exception {
        generateImportFile();

        ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS = true;
        ProjectExportImport.getInstance().runImport(IMPORT_FILE);

        java.util.List<Project> projectList = ProjectManager.getInstance().getProjectList();
        Project project1 = projectList.get(projectList.size() - 1);

        assertEquals("test import 1", project1.getProjectName());
        assertEquals(2, project1.getConnections().size());
        assertEquals(4, project1.getDedicatedConnections().size());
        assertEquals(2, project1.getDatabases().size());

        DB db1 = project1.getDatabaseByName("importDB1");
        DB db2 = project1.getDatabaseByName("importDB2");

        IConnection developmentConnection = project1.getConnectionByName("Development");
        assertEquals(1, developmentConnection.getType());
        assertEquals("localhost", developmentConnection.getHost());
        assertEquals("importUser1", developmentConnection.getUser());
        assertEquals("importPass1", developmentConnection.getPass());

        IConnection productionConnection = project1.getConnectionByName("Production");
        assertEquals(2, productionConnection.getType());
        assertEquals("127.0.0.1", productionConnection.getHost());
        assertEquals("importUser2", productionConnection.getUser());
        assertEquals("importPass2", productionConnection.getPass());

        IConnection dedicatedConnection1 = project1.getDedicatedConnection(db1.getName(), "Development");
        assertEquals(1, dedicatedConnection1.getType());
        assertEquals("localhost", dedicatedConnection1.getHost());
        assertEquals("importDB1alias1", dedicatedConnection1.getDbAlias());
        assertEquals("importDedicatedUser1", dedicatedConnection1.getUser());
        assertEquals("importDedicatedPass1", dedicatedConnection1.getPass());

        IConnection dedicatedConnection2 = project1.getDedicatedConnection(db1.getName(), "Production");
        assertEquals(2, dedicatedConnection2.getType());
        assertEquals("127.0.0.1", dedicatedConnection2.getHost());
        assertEquals("importDB1alias2", dedicatedConnection2.getDbAlias());
        assertEquals("importDedicatedUser2", dedicatedConnection2.getUser());
        assertEquals("importDedicatedPass2", dedicatedConnection2.getPass());

        IConnection dedicatedConnection3 = project1.getDedicatedConnection(db2.getName(), "Development");
        assertEquals(1, dedicatedConnection3.getType());
        assertEquals("localhost", dedicatedConnection3.getHost());
        assertEquals("importDB2alias1", dedicatedConnection3.getDbAlias());
        assertEquals("importDedicatedUser3", dedicatedConnection3.getUser());
        assertEquals("importDedicatedPass3", dedicatedConnection3.getPass());

        IConnection dedicatedConnection4 = project1.getDedicatedConnection(db2.getName(), "Production");
        assertEquals(2, dedicatedConnection4.getType());
        assertEquals("127.0.0.1", dedicatedConnection4.getHost());
        assertEquals("importDB2alias2", dedicatedConnection4.getDbAlias());
        assertEquals("importDedicatedUser4", dedicatedConnection4.getUser());
        assertEquals("importDedicatedPass4", dedicatedConnection4.getPass());

        ProjectManager.getInstance().setCurrentProject(project1);
        project1.setCurrentDB(db1);
        Canvas.instance.drawProject(true);

        assertEquals(new Point(150, 240), db1.getKnownLocation("importSchema2.importLocation1"));

        assertEquals(2, db1.getSchemas().size());
        assertEquals(1, db1.getConstraints().size());

        assertEquals(3, db2.getSchemas().size());
        assertEquals(1, db2.getTriggers().size());

        assertEquals(2, db1.getSchemaByFullName("importSchema1").getRelations().size());
        assertEquals(3, db1.getSchemaByFullName("importSchema2").getRelations().size());

        assertEquals(0, db2.getSchemaByFullName("importSchema3").getRelations().size());
        assertEquals(3, db2.getSchemaByFullName("importSchema4").getRelations().size());
        assertEquals(0, db2.getSchemaByFullName("importSchema5").getRelations().size());

        Schema importSchema1 = db1.getSchemaByFullName("importSchema1");
        Schema importSchema2 = db1.getSchemaByFullName("importSchema2");

        assertEquals(2, importSchema1.getRelations().size());
        assertEquals(1, importSchema1.getSequences().size());
        assertEquals(0, importSchema1.getFunctions().size());

        Relation importTable2 = importSchema1.getRelationByName("importTable2");
        Relation importTable3 = importSchema2.getRelationByName("importTable3");
        Relation importTable4 = importSchema2.getRelationByName("importTable4");

        assertEquals(importTable2, db1.getRelationByFullName("importSchema1.importTable2"));
        assertEquals("id", importTable2.getPKey());
        assertEquals(5, importTable2.getCountRows());
        assertEquals(24576, importTable2.getSizeIndexes());
        assertEquals(32768, importTable2.getSizeTotal());
        assertEquals("MyISAM", importTable2.getBehavior().getStorage());
        assertEquals("coll", importTable2.getBehavior().getCollation());
        assertEquals("import description 1", importTable2.getBehavior().getDescr());
        assertEquals(new Point(1350, 30), importTable2.getLocation());
        assertEquals(new Point(1350, 30), importTable2.getRememberedLocation());

        assertEquals(importTable4, db1.getRelationByFullName("importSchema2.importTable4"));
        assertEquals(1, importTable4.getInheritances().size());
        assertEquals("importSchema2.importTable5", importTable4.getInheritances().get(0).getRel2().getFullName());
        assertEquals(2, importTable4.getBehavior().getOptions().length);

        assertEquals(6, importTable2.getAttributes().size());
        assertEquals(2, importTable2.getIndexes().size());

        Attribute attr1 = importTable2.getAttributeByName("id");
        assertEquals(1, (int) attr1.getAttNum());
        assertEquals("serial", attr1.getBehavior().getAttType());
        assertEquals('p', attr1.getBehavior().getStorageChar());
        assertEquals(false, attr1.getBehavior().isAttNull());

        Attribute attr3 = importTable2.getAttributeByName("last_update_user");
        assertEquals(3, (int) attr3.getAttNum());
        assertEquals("varchar", attr3.getBehavior().getAttType());
        assertEquals("60", attr3.getBehavior().getAttPrecision());
        assertEquals('x', attr3.getBehavior().getStorageChar());
        assertEquals(false, attr3.getBehavior().isAttNull());

        Schema importSchema3 = db2.getSchemaByFullName("importSchema3");
        assertEquals(0, importSchema3.getRelations().size());
        assertEquals(0, importSchema3.getSequences().size());
        assertEquals(1, importSchema3.getFunctions().size());
        assertEquals(1, importSchema3.getViews().size());
        assertEquals(1, importSchema3.getPackages().size());

        View view1 = importSchema3.getViewByName("view1");
        assertEquals(view1, db2.getViewByFullName("importSchema3.view1"));
        assertEquals("SELECT CURRENT_DATE;", view1.getBehavior().getSrc());
        assertEquals("import view description", view1.getBehavior().getDescr());
        assertEquals(new Point(300, 90), view1.getLocation());
        assertEquals(new Point(300, 90), view1.getRememberedLocation());

        Package package1 = importSchema3.getPackageByName("package1");
        assertEquals(package1, db2.getPackageByFullName("importSchema3.package1"));
        assertEquals("/** package source **/", package1.getBehavior().getDefinition());
        assertEquals("/** package body **/", package1.getBehavior().getBody());
        assertEquals("import package description", package1.getBehavior().getDescr());
        assertEquals(new Point(330, 90), package1.getLocation());
        assertEquals(new Point(330, 90), package1.getRememberedLocation());

        Sequence sequence1 = importSchema1.getSequenceByName("importTable2_id_seq");
        assertEquals(sequence1, db1.getSequenceByFullName("importSchema1.importTable2_id_seq"));
        assertEquals("10", sequence1.getBehavior().getMin());
        assertEquals("9223372036854775807", sequence1.getBehavior().getMax());
        assertEquals("100", sequence1.getBehavior().getCurrent());
        assertEquals("10", sequence1.getBehavior().getIncrement());
        assertEquals("import description 1", sequence1.getBehavior().getDescr());

        Function triggerFunction1 = importSchema3.getFunctionByName("triggerFunction1");
        assertEquals(triggerFunction1, db2.getFunctionByFullName("importSchema3.triggerFunction1"));
        assertEquals("trigger", triggerFunction1.getBehavior().getRetType());
        assertEquals("/** no args **/", triggerFunction1.getBehavior().getArgs());
        assertEquals("\nBEGIN;\n\tRETURN NEW;\nEND;\n", triggerFunction1.getBehavior().getSrc());
        assertEquals("plpgsql", triggerFunction1.getBehavior().getLang());
        assertEquals("volatile", triggerFunction1.getBehavior().getVolatility());
        assertEquals("plpgsql", triggerFunction1.getBehavior().getLang());
        assertEquals(false, triggerFunction1.getBehavior().isSecDefiner());
        assertEquals(12, triggerFunction1.getBehavior().getCost());
        assertEquals(120, triggerFunction1.getBehavior().getRows());
        assertEquals("import function description", triggerFunction1.getBehavior().getDescr());
        assertEquals(1, triggerFunction1.getTriggers().size());
        assertEquals(1, importSchema3.getTriggerFunctionNames().length);
        assertEquals("importSchema3.triggerFunction1", importSchema3.getTriggerFunctionNames()[0]);

        Trigger importTrigger1 = db2.getTriggerByFullName("importSchema4.importTable6.importTrigger1");
        assertEquals(importTrigger1, triggerFunction1.getTriggers().get(0));
        assertEquals(importTrigger1, db2.getRelationByFullName("importSchema4.importTable6").getTriggers().get(0));
        assertEquals("EXECUTE PROCEDURE importSchema3.triggerFunction1()", importTrigger1.getBehavior().getDef());
        assertEquals("id", importTrigger1.getBehavior().getAttrs());
        assertEquals("id IS NOT NULL", importTrigger1.getBehavior().getWhen());
        assertEquals(true, importTrigger1.getBehavior().isRowType());
        assertEquals(false, importTrigger1.getBehavior().isEnabled());
        assertEquals("BEFORE INSERT OR UPDATE", importTrigger1.getBehavior().getTiming(" OR "));
        assertEquals("import trigger description", importTrigger1.getBehavior().getDescr());

        Constraint importConstraint1 = db1.getConstraintByFullName("importSchema1.importTable2.fk_ref_id");
        assertEquals(importConstraint1, importTable2.getConstraints().get(0));
        assertEquals(importConstraint1, importTable3.getConstraints().get(0));
        assertEquals("importTable2", importConstraint1.getRel1().getName());
        assertEquals("ref_id", importConstraint1.getBehavior().getAttr1().getName());
        assertEquals("importTable3", importConstraint1.getRel2().getName());
        assertEquals("id", importConstraint1.getBehavior().getAttr2().getName());

    }

    private void generateImportFile() throws IOException {
        FileOutputStream os = new FileOutputStream(IMPORT_FILE);
        os.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>".getBytes());
        os.write("<projects>".getBytes());

        os.write("<project name=\"test import 1\" type=\"2\">".getBytes());

        os.write("<server host=\"localhost\" name=\"Development\" type=\"1\" user=\"importUser1\" pass=\"importPass1\"/>".getBytes());
        os.write("<server host=\"127.0.0.1\" name=\"Production\" type=\"2\" user=\"importUser2\" pass=\"importPass2\"/>".getBytes());

        os.write("<database name=\"importDB1\">".getBytes());
        os.write("  <locations>".getBytes());
        os.write("    <loc name=\"importSchema2.importLocation1\" value=\"150,240\"/>".getBytes());
        os.write("  </locations>".getBytes());
        os.write("  <connection dbAlias=\"importDB1alias1\" defaultSchema=\"public\" host=\"localhost\" name=\"Development\" type=\"1\" user=\"importDedicatedUser1\" pass=\"importDedicatedPass1\" />".getBytes());
        os.write("  <connection dbAlias=\"importDB1alias2\" defaultSchema=\"importSchema2\" host=\"127.0.0.1\" name=\"Production\" type=\"2\" user=\"importDedicatedUser2\" pass=\"importDedicatedPass2\" />".getBytes());
        os.write("  <schemata>".getBytes());
        os.write("    <schema name=\"importSchema1\">".getBytes());
        os.write("      <table name=\"importSchema1.importTable1\">".getBytes());
        os.write("      </table>".getBytes());
        os.write("      <table name=\"importSchema1.importTable2\" loc=\"1350,30\" coll=\"coll\" pk=\"1\" rows=\"5\" sizeI=\"24576\" sizeT=\"32768\" store=\"MyISAM\" oids=\"1\" descr=\"import description 1\">".getBytes());
        os.write("        <column name=\"id\" num=\"1\" store=\"p\" type=\"serial\"/>".getBytes());
        os.write("        <column name=\"last_update_time\" num=\"2\" store=\"p\" type=\"timestamp\"/>".getBytes());
        os.write("        <column name=\"last_update_user\" num=\"3\" store=\"x\" type=\"varchar(60)\"/>".getBytes());
        os.write("        <column name=\"conf_key\" num=\"4\" store=\"x\" type=\"varchar(255)\"/>".getBytes());
        os.write("        <column name=\"conf_value\" null=\"1\" num=\"5\" store=\"x\" type=\"varchar(255)\"/>".getBytes());
        os.write("        <column name=\"ref_id\" num=\"6\" store=\"p\" type=\"int\"/>".getBytes());
        os.write("        <index con=\"1\" def=\"id\" name=\"configuration_pkey\" pkey=\"1\" ux=\"1\"/>".getBytes());
        os.write("        <index con=\"1\" def=\"conf_key\" name=\"uk_8ptevxr0bea85p74daxsjldoi\" ux=\"1\"/>".getBytes());
        os.write("        <sequence deps=\"importTable2.id\" inc=\"10\" loc=\"0,0\" max=\"9223372036854775807\" min=\"10\" name=\"importSchema1.importTable2_id_seq\" current=\"100\" descr=\"import description 1\"/>".getBytes());
        os.write("      </table>".getBytes());
        os.write("    </schema>".getBytes());
        os.write("    <schema name=\"importSchema2\">".getBytes());
        os.write("      <table name=\"importSchema2.importTable3\">".getBytes());
        os.write("        <column name=\"id\" num=\"1\" store=\"p\" type=\"serial\"/>".getBytes());
        os.write("      </table>".getBytes());
        os.write("      <table name=\"importSchema2.importTable4\" inherits=\"importSchema2.importTable5\" opts=\"opt1,opt2\">".getBytes());
        os.write("      </table>".getBytes());
        os.write("      <table name=\"importSchema2.importTable5\">".getBytes());
        os.write("      </table>".getBytes());
        os.write("    </schema>".getBytes());
        os.write("  </schemata>".getBytes());
        os.write("  <constraints>".getBytes());
        os.write("      <constraint attr1=\"ref_id\" attr2=\"id\" delete=\"c\" name=\"fk_ref_id\" rel1=\"importSchema1.importTable2\" rel2=\"importSchema2.importTable3\" update=\"c\"/>".getBytes());
        os.write("  </constraints>".getBytes());
        os.write("</database>".getBytes());

        os.write("<database name=\"importDB2\">".getBytes());
        os.write("  <connection dbAlias=\"importDB2alias1\" defaultSchema=\"importSchema3\" host=\"localhost\" name=\"Development\" type=\"1\" user=\"importDedicatedUser3\" pass=\"importDedicatedPass3\" />".getBytes());
        os.write("  <connection dbAlias=\"importDB2alias2\" defaultSchema=\"importSchema4\" host=\"127.0.0.1\" name=\"Production\" type=\"2\" user=\"importDedicatedUser4\" pass=\"importDedicatedPass4\" />".getBytes());
        os.write("  <schemata>".getBytes());
        os.write("    <schema name=\"importSchema3\">".getBytes());
        os.write("      <function name=\"importSchema3.triggerFunction1\" type=\"trigger\" args=\"/** no args **/\" src=\"&#10;BEGIN;&#10;&#09;RETURN NEW;&#10;END;&#10;\" lang=\"plpgsql\" vlt=\"volatile\" secDef=\"0\" cost=\"12\" rows=\"120\" descr=\"import function description\" />".getBytes());
        os.write("      <view name=\"importSchema3.view1\" src=\"SELECT CURRENT_DATE;\" loc=\"300,90\" descr=\"import view description\" />".getBytes());
        os.write("      <package name=\"importSchema3.package1\" src=\"/** package source **/\" body=\"/** package body **/\" loc=\"330,90\" descr=\"import package description\" />".getBytes());
        os.write("    </schema>".getBytes());
        os.write("    <schema name=\"importSchema4\">".getBytes());
        os.write("      <table name=\"importSchema4.importTable6\">".getBytes());
        os.write("        <column name=\"id\" num=\"1\" store=\"p\" type=\"serial\"/>".getBytes());
        os.write("      </table>".getBytes());
        os.write("      <table name=\"importSchema4.importTable7\">".getBytes());
        os.write("      </table>".getBytes());
        os.write("      <table name=\"importSchema4.importTable8\">".getBytes());
        os.write("      </table>".getBytes());
        os.write("    </schema>".getBytes());
        os.write("    <schema name=\"importSchema5\" />".getBytes());
        os.write("  </schemata>".getBytes());
        os.write("  <triggers>".getBytes());
        os.write("    <trigger name=\"importTrigger1\" rel=\"importSchema4.importTable6\" def=\"EXECUTE PROCEDURE importSchema3.triggerFunction1()\" attrs=\"id\" when=\"id IS NOT NULL\" statement=\"0\" disabled=\"1\" timing=\"b\" events=\"iu\" descr=\"import trigger description\" />".getBytes());
        os.write("  </triggers>".getBytes());
        os.write("</database>".getBytes());

        os.write("</project>".getBytes());

        os.write("</projects>".getBytes());

        os.flush();
        os.close();
    }

}