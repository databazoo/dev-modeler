package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;

class StaticRules {

    private final AbstractOptimizer optimizer;

    StaticRules(AbstractOptimizer optimizer) {
        this.optimizer = optimizer;
    }

    /**
     * Check whether the relation has a primary key defined.
     *
     * @param r relation to check
     */
    void checkPrimaryKeyExists (Relation r) {
        for(Index i : r.getIndexes()){
            if(i.getBehavior().isPrimary()){
                return;
            }
        }
        optimizer.addFlaw(ModelFlaw.error(r,
                "Missing primary key",
                "Tables should always use primary keys to uniquely identify rows in the table. "
                + "Tables without primary keys are carrying great performance risks."
        ));
    }

    /**
     * Check whether the relation has an acceptable index count.
     *
     * @param r relation to check
     */
    void checkIndexesCount (Relation r) {
        if(r.getIndexes().size() >= Relation.INDEX_COUNT_LIMIT){
            optimizer.addFlaw(ModelFlaw.warning(r,
                    "Too many indexes",
                    StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + value(r.getIndexes().size()) + " indexes. " +
                            "Data manipulation on tables with too many indexes may be slow."
            ));
        }
    }

    /**
     * Check whether the relation has an acceptable trigger count.
     *
     * @param r relation to check
     */
    void checkTriggersCount (Relation r) {
        if(r.getTriggers().size() >= Relation.TRIGGER_COUNT_LIMIT){
            optimizer.addFlaw(ModelFlaw.warning(r,
                    "Too many triggers",
                    StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + value(r.getTriggers().size()) + " triggers. " +
                            "Data manipulation on tables with too many triggers may be slow."
            ));
        }
    }

    /**
     * Check whether the relation has an acceptable attribute count.
     *
     * @param r relation to check
     */
    void checkAttributesCount (Relation r) {
        if(r.getAttributes().size() >= Relation.ATTRIBUTE_COUNT_LIMIT){
            optimizer.addFlaw(ModelFlaw.warning(r,
                    "Too many columns",
                    StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + value(r.getAttributes().size()) + " columns. " +
                            "Tables with too many columns have inefficient storage and may be performing slow."
            ));
        }
        if(r.getAttributes().size() < 2){
            optimizer.addFlaw(ModelFlaw.warning(r,
                    "Useless table",
                    StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + (r.getAttributes().isEmpty() ? "no columns" : "only 1 column") +
                            ". Tables with so few attributes are generally useless."
            ));
        }
    }

    /**
     * Check whether the relation does not have a primary key on time-based column.
     *
     * @param r relation to check
     */
    void checkPrimaryKeyOnTimeColumn (Relation r) {
        for(int i=0; i < r.getPkCols().length; i++){
            if(r.getPkCols()[i] > 0){
                for(Attribute attr: r.getAttributes()){
                    if(attr.getAttNum() == r.getPkCols()[i]){
                        if(attr.getBehavior().getAttType().matches(".*(Time|time|TIME).*")){
                            optimizer.addFlaw(ModelFlaw.error(r,
                                    "Primary key on time column",
                                    StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + " has primary key defined on time column. " +
                                            "Inserting data into such table will create duplicities under heavy load."
                            ));
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Check whether the relation does not have a unique key on time-based column.
     *
     * @param r relation to check
     */
    void checkUniqueOnTimeColumn (Relation r) {
        for(Index index : r.getIndexes()){
            if(index.getBehavior().isUnique() && !index.getBehavior().isPrimary()){
                for(Attribute attr: index.getAttributes()){
                    if(attr.getBehavior().getAttType().matches(".*(Time|time|TIME).*")){
                        optimizer.addFlaw(ModelFlaw.error(r,
                                "Unique key on time column",
                                StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + elementName(index.getName()) +
                                        " unique key defined on time column. Inserting data into such table " +
                                        "will create duplicities under heavy load."
                        ));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check all foreign keys are covered by an index.
     *
     * @param r relation to check
     */
    void checkIndexesOnReferences (Relation r) {
        for(Constraint c : r.getConstraints()){
            if(c.getRel1() == r && c.getAttr1() != null){

                // Find an index for constraint
                boolean found = false;
                for(Index i : r.getIndexes()){
                    if(i.getAttributes().contains(c.getAttr1())){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    optimizer.addFlaw(ModelFlaw.warning(r,
                            "Foreign key not covered by index",
                            StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + elementName(c.getName()) +
                                    " foreign key that is not covered by any index. Data manipulation on parent table may be slow."
                    ));
                }
            }
        }
    }

    /**
     * Check for columns named `something_id` without a foreign key.
     *
     * @param r relation to check
     */
    void checkNoReferenceOnId (Relation r) {
        Index pk = null;
        for(Index index : r.getIndexes()){
            if(index.getBehavior().isPrimary()){
                pk = index;
                break;
            }
        }
        for(Attribute a : r.getAttributes()){

            // Skip if is part of primary key
            if(pk != null && !pk.getAttributes().contains(a)){

                // Name contains ID
                String aName = a.getName();
                if (aName.startsWith("ID_") || aName.startsWith("id_") ||
                        aName.endsWith("_ID") || aName.endsWith("_id") || aName.endsWith("_Id")){

                    // Find a FK
                    boolean found = false;
                    for(Constraint c : r.getConstraints()){
                        if(c.getRel1() == r && c.getAttr1() != null && c.getAttr1() == a){
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        optimizer.addFlaw(ModelFlaw.warning(r,
                                "ID column has no foreign key",
                                StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + elementName(a.getName()) +
                                        " column with no foreign key. There should be a foreign key constraint defined."
                        ));
                    }
                }else if (aName.startsWith("ID") || aName.startsWith("id") || aName.startsWith("Id") ||
                        aName.endsWith("ID") || aName.endsWith("id") || aName.endsWith("Id")){

                    // Find a FK
                    boolean found = false;
                    for(Constraint c : r.getConstraints()){
                        if(c.getRel1() == r && c.getAttr1() != null && c.getAttr1() == a){
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        optimizer.addFlaw(ModelFlaw.notice(r,
                                "ID column has no foreign key",
                                StaticFlawOptimizer.L_TABLE + elementName(r.getName()) + StaticFlawOptimizer.L_HAS + elementName(a.getName()) +
                                        " column with no foreign key. There could be a foreign key constraint defined."
                        ));
                    }
                }
            }
        }
    }

    /**
     * Check for existence of back and forth foreign keys.
     *
     * @param r relation to check
     */
    void check2wayReferences(Relation r) {
        for(Constraint c : r.getConstraints()){

            // Skip self-references
            if(c.getRel1() == r && c.getAttr1() != null && c.getRel1() != c.getRel2()){

                // Find a backward FK
                Relation rel2 = (Relation) c.getRel2();
                for(Constraint c2 : rel2.getConstraints()){
                    if(c2.getRel2() == r){
                        optimizer.addFlaw(ModelFlaw.warning(r,
                                "Bi-directional reference",
                                "There is a 2-way reference between " + elementName(r.getName()) + " and " + elementName(rel2.getName()) +
                                ". 2-way references make models too complicated and are a sign of poor design."
                        ));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check for indexes operation on same columns.
     *
     * @param r relation to check
     */
    void checkDuplicateIndexes(Relation r) {
        for(Index index : r.getIndexes()){
            for(Index index2 : r.getIndexes()){
                if(index != index2 && index.getAttributes().containsAll(index2.getAttributes()) && index2.getAttributes().containsAll(index.getAttributes())){
                    optimizer.addFlaw(ModelFlaw.warning(r,
                            "Duplicate indexes",
                            StaticFlawOptimizer.L_TABLE + elementName(r.getName()) +
                            " has indexes " + elementName(index.getName()) + " and " + elementName(index2.getName()) +
                            " operating on same columns. One of the indexes should be removed."
                    ));
                    break;
                }
            }
        }
    }

    /**
     * Check whether the relation does not have a unique key on nullable column.
     *
     * @param r relation to check
     */
    void checkUniqueOnNullColumn (Relation r) {
        for(Index index : r.getIndexes()){
            if(index.getBehavior().isUnique()){
                for(Attribute attr: index.getAttributes()){
                    if(attr.getBehavior().isAttNull()){
                        optimizer.addFlaw(ModelFlaw.warning(r,
                                "Unique key on NULL column",
                                StaticFlawOptimizer.L_TABLE +
                                elementName(r.getName()) + StaticFlawOptimizer.L_HAS + elementName(index.getName()) + " unique key defined on nullable column " +
                                elementName(attr.getName()) + ". NULL values may easily result in duplicities."
                        ));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check for PK columns that are not on the beginning of the table.
     *
     * @param r relation to check
     */
    void checkPrimaryKeyOnFirstColumn(Relation r) {
        for(Index index : r.getIndexes()){
            if(index.getBehavior().isPrimary()){
                boolean found = false;
                for(Attribute attr: index.getAttributes()){
                    if(attr.getAttNum() == 1){
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    optimizer.addFlaw(ModelFlaw.notice(r,
                            "Primary key not on first column",
                            StaticFlawOptimizer.L_TABLE + elementName(r.getName()) +
                            " has primary key defined in the middle of the table. PK column should be moved to the beginning."
                    ));
                }
            }
        }
    }

    private String elementName(String name) {
        return "<font color=\"#" + Integer.toHexString(UIConstants.COLOR_GREEN.getRGB()).substring(2) + "\">" + name + "</font>";
    }

    private String value(int val) {
        return "<font color=\"#" + Integer.toHexString(UIConstants.COLOR_AMBER.getRGB()).substring(2) + "\">" + val + "</font>";
    }

    void checkPrimaryKeyWrongDatatype(Relation r) {
        // TODO
    }

    void checkAttributeDefaultAndNull(Relation r) {
        // TODO
    }

    void checkRepeatedAttributes(Relation r) {
        // TODO
    }

    void checkReservedKeywords(Relation r) {
        // TODO
    }
}
