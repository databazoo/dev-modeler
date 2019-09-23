package com.databazoo.devmodeler.tools.formatter;

import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import difflib.Patch;
import difflib.myers.MyersDiff;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class FormatterTest {

    @Before
    public void setProjectUp() {
        Settings.init();

        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", ""));
    }

    @Test
    public void formatDataType() throws Exception {
        String text = "SELECT hash FROM btree";
        FormattedTextField input = new FormattedTextField(new FormatterDataType());

        input.setText(text);
        input.formatImmediately();

        assertEquals(text, input.getText());
    }

    @Test
    public void formatDiff() throws Exception {
        String forwardSQL = "/** new **/\n/** COMMENT **/\nSELECT\n\tid,\n\tvalue\nFROM myTable\nORDER BY id";
        String revertSQL = "/** COMMENT **/\nSELECT *\nFROM myTable\nWHERE id IN (\n\t1, 2, 3\n)\nORDER BY id\nLIMIT 100;";

        Patch<String> patch = new MyersDiff<String>().diff(Arrays.asList(revertSQL.split("\\r?\\n")), Arrays.asList(forwardSQL.split("\\r?\\n")));

        FormattedTextField query1 = new FormattedClickableTextField(Project.getCurrent(), forwardSQL, new FormatterDiff(patch.getDeltas(), false));
        FormattedTextField query2 = new FormattedClickableTextField(Project.getCurrent(), revertSQL, new FormatterDiff(patch.getDeltas(), true));

        query1.setEditable(false);
        query2.setEditable(false);

        query1.formatImmediately();
        query2.formatImmediately();

        assertEquals(forwardSQL.replace("ORDER", "\n\n\nORDER"), query1.getText());
        assertEquals(revertSQL.replace("FROM", "\n\nFROM").replace("/** COMMENT **/", "\n/** COMMENT **/"), query2.getText());
    }

    @Test
    public void formatSettings() throws Exception {
        String text = "%uc_database%";
        FormattedTextField input = new FormattedTextField(new FormatterSettings());

        input.setText(text);
        input.formatImmediately();

        assertEquals(text, input.getText());
    }

}
