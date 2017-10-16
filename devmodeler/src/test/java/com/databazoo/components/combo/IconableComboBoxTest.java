package com.databazoo.components.combo;

import java.awt.*;

import javax.swing.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

import junit.framework.Assert;

public class IconableComboBoxTest {
    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void setSelectedByNamePart() throws Exception {
        IconableComboBox combo = new IconableComboBox();
        combo.setRenderer(new ComboSeparatorsRenderer(combo.getRenderer()) {
            @Override protected boolean addSeparatorAfter(JList list, Object value, int index) {
                return index == 1;
            }
        });
        combo.addItem("TEST 1");
        combo.addItem("TEST 2", Theme.getSmallIcon(Theme.ICO_ABOUT_APP));
        combo.addItem("TEST 3", Theme.getSmallIcon(Theme.ICO_ABOUT_APP), Color.RED, Color.BLUE);
        combo.setSelectedByNamePart(".*3");
        Assert.assertEquals("TEST 3", combo.getSelectedItem().toString());
        combo.removeAllItems();
        Assert.assertEquals(0, combo.getItemCount());
    }
}
