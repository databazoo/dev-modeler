package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.project.Project;


public class SearchPanelTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() throws InterruptedException {
        Project.getCurrent().setCurrentDB(database);
        Canvas.instance.setScrolls(new JScrollPane());
        Canvas.instance.setOverview(Navigator.instance);
        DBTree.instance.checkDB(true);
    }

    @Test
    public void updateDbTree() throws Exception {
        SearchPanel.instance.searchText.setText(relation2.getName());
        SearchPanel.instance.triggerSearch();
        Thread.sleep(800);

        TreeModel model = DBTree.instance.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        assertEquals(2, root.getChildCount());

        DefaultMutableTreeNode db = (DefaultMutableTreeNode) root.getChildAt(1);
        assertEquals(1, db.getChildCount());

        DefaultMutableTreeNode schema = (DefaultMutableTreeNode) db.getFirstChild();
        assertEquals(1, schema.getChildCount());


        assertEquals(relation2.getName(), schema.getFirstChild().toString());
    }

}