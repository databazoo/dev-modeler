package com.databazoo.devmodeler.model;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.databazoo.devmodeler.TestProjectSetup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DBTest extends TestProjectSetup {

    @Test
    public void getTreeViewPositive() throws Exception {
        DefaultMutableTreeNode databaseTreeView = database.getTreeView(".*test.*", false, false);
        assertEquals("test 1", databaseTreeView.toString());
        assertEquals(1, databaseTreeView.getChildCount());

        TreeNode schemaTreeView = databaseTreeView.getFirstChild();
        assertEquals("test 2", schemaTreeView.toString());
        assertEquals(7, schemaTreeView.getChildCount());

        // Table test 3
        TreeNode table3TreeView = schemaTreeView.getChildAt(0);
        assertEquals("test 3", table3TreeView.toString());
        assertEquals(3, table3TreeView.getChildCount());

        TreeNode table3Attributes = table3TreeView.getChildAt(0);
        assertEquals("Attributes", table3Attributes.toString());
        assertEquals(2, table3Attributes.getChildCount());

        TreeNode table3Constraints = table3TreeView.getChildAt(1);
        assertEquals("Constraints", table3Constraints.toString());
        assertEquals(3, table3Constraints.getChildCount());

        TreeNode table3Indexes = table3TreeView.getChildAt(2);
        assertEquals("Indexes", table3Indexes.toString());
        assertEquals(1, table3Indexes.getChildCount());

        // Table test 4
        TreeNode table4TreeView = schemaTreeView.getChildAt(1);
        assertEquals("test 4", table4TreeView.toString());
        assertEquals(4, table4TreeView.getChildCount());

        TreeNode table4Attributes = table4TreeView.getChildAt(0);
        assertEquals("Attributes", table4Attributes.toString());
        assertEquals(2, table4Attributes.getChildCount());

        TreeNode table4Constraints = table4TreeView.getChildAt(1);
        assertEquals("Constraints", table4Constraints.toString());
        assertEquals(1, table4Constraints.getChildCount());

        TreeNode table4Indexes = table4TreeView.getChildAt(2);
        assertEquals("Indexes", table4Indexes.toString());
        assertEquals(1, table4Indexes.getChildCount());

        TreeNode table4Triggers = table4TreeView.getChildAt(3);
        assertEquals("Triggers", table4Triggers.toString());
        assertEquals(1, table4Triggers.getChildCount());

        // Other objects
        TreeNode funcTreeView = schemaTreeView.getChildAt(2);
        assertEquals("test function(int input)", funcTreeView.toString());
        assertEquals(0, funcTreeView.getChildCount());

        TreeNode triggerFunctionTreeView = schemaTreeView.getChildAt(3);
        assertEquals("test trigger function()", triggerFunctionTreeView.toString());
        assertEquals(0, triggerFunctionTreeView.getChildCount());

        TreeNode packageTreeView = schemaTreeView.getChildAt(4);
        assertEquals("test package", packageTreeView.toString());
        assertEquals(0, packageTreeView.getChildCount());

        TreeNode viewTreeView = schemaTreeView.getChildAt(5);
        assertEquals("test view", viewTreeView.toString());
        assertEquals(0, viewTreeView.getChildCount());

        TreeNode sequenceTreeView = schemaTreeView.getChildAt(6);
        assertEquals("test sequence", sequenceTreeView.toString());
        assertEquals(0, sequenceTreeView.getChildCount());
    }

    @Test
    public void getTreeViewNegative() throws Exception {
        DefaultMutableTreeNode databaseTreeView = database.getTreeView(".*2.*", false, true);
        assertEquals("test 1", databaseTreeView.toString());
        assertEquals(1, databaseTreeView.getChildCount());

        databaseTreeView = database.getTreeView(".*3.*", false, true);
        assertEquals("test 1", databaseTreeView.toString());
        assertEquals(2, databaseTreeView.getChildCount());

        TreeNode schemaTreeView = databaseTreeView.getChildAt(1);
        assertEquals("test 2", schemaTreeView.toString());
        assertEquals(6, schemaTreeView.getChildCount());

        // Table test 4
        TreeNode table4TreeView = schemaTreeView.getChildAt(0);
        assertEquals("test 4", table4TreeView.toString());
        assertEquals(4, table4TreeView.getChildCount());

        TreeNode table4Attributes = table4TreeView.getChildAt(0);
        assertEquals("Attributes", table4Attributes.toString());
        assertEquals(1, table4Attributes.getChildCount());

        TreeNode table4Constraints = table4TreeView.getChildAt(1);
        assertEquals("Constraints", table4Constraints.toString());
        assertEquals(1, table4Constraints.getChildCount());

        TreeNode table4Indexes = table4TreeView.getChildAt(2);
        assertEquals("Indexes", table4Indexes.toString());
        assertEquals(1, table4Indexes.getChildCount());

        TreeNode table4Triggers = table4TreeView.getChildAt(3);
        assertEquals("Triggers", table4Triggers.toString());
        assertEquals(1, table4Triggers.getChildCount());

        // Other objects
        TreeNode funcTreeView = schemaTreeView.getChildAt(1);
        assertEquals("test function(int input)", funcTreeView.toString());
        assertEquals(0, funcTreeView.getChildCount());

        TreeNode triggerFunctionTreeView = schemaTreeView.getChildAt(2);
        assertEquals("test trigger function()", triggerFunctionTreeView.toString());
        assertEquals(0, triggerFunctionTreeView.getChildCount());

        TreeNode packageTreeView = schemaTreeView.getChildAt(3);
        assertEquals("test package", packageTreeView.toString());
        assertEquals(0, packageTreeView.getChildCount());

        TreeNode viewTreeView = schemaTreeView.getChildAt(4);
        assertEquals("test view", viewTreeView.toString());
        assertEquals(0, viewTreeView.getChildCount());

        TreeNode sequenceTreeView = schemaTreeView.getChildAt(5);
        assertEquals("test sequence", sequenceTreeView.toString());
        assertEquals(0, sequenceTreeView.getChildCount());
    }

}