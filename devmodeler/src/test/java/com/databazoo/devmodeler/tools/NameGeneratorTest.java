package com.databazoo.devmodeler.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.databazoo.devmodeler.TestProjectSetup;

public class NameGeneratorTest extends TestProjectSetup {
    @Test
    public void generate() throws Exception {
        assertEquals("new_schema_3", NameGenerator.createSchemaName(database));
        assertEquals("new_table_3", NameGenerator.createRelationName(schema));
        assertEquals("test3_new_column_3", NameGenerator.createAttributeName(relation));
        assertEquals("pk_test4", NameGenerator.createPrimaryKeyName(relation2));
        assertEquals("fk_test3_testattr1", NameGenerator.createForeignKeyName(attribute1));
        assertEquals("uc_test3_3", NameGenerator.createUniqueConstraintName(relation));
        assertEquals("cc_test3_3", NameGenerator.createCheckConstraintName(relation));
        assertEquals("ix_test3_3", NameGenerator.createIndexName(relation));
        assertEquals("new_sequence_2", NameGenerator.createSequenceName(schema));
        assertEquals("tr_test4_2", NameGenerator.createTriggerName(relation2));
        assertEquals("new_view_2", NameGenerator.createViewName(schema));
        assertEquals("new_function_3", NameGenerator.createFunctionName(schema));
        assertEquals("new_package_2", NameGenerator.createPackageName(schema));
    }

}