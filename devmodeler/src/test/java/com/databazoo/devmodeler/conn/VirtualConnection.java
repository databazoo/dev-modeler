package com.databazoo.devmodeler.conn;

import java.util.ArrayList;
import java.util.List;

import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Relation;

public class VirtualConnection extends ConnectionPg {

    public static VirtualConnection prepareServerActivityData() throws DBCommException {
        return new VirtualConnection().query.prepareServerActivityData();
    }

    public static VirtualConnection prepareTableContentData(Relation relation) throws DBCommException {
        return new VirtualConnection().query.prepareTableContentData(relation);
    }

    private int callNumber = 0;

    private final VirtualQuery query = new VirtualQuery();
    private VirtualConnection() throws DBCommException {
        super("", "", "", "");
    }

    @Override
    public Result run(String sql) throws DBCommException {
        return new Result(query);
    }

    @Override
    public Result getAllRows(Relation rel) throws DBCommException {
        String pk = rel.getPKey();
        if(pk.isEmpty()){
            return null;
        }else{
            return new Result(query.getTableContent(callNumber++));
        }
    }

    public void reset() {
        callNumber = 0;
        query.pointer = 0;
    }

    private class VirtualQuery extends ConnectionBase.Query {
        private final List<ResultColumn> cols = new ArrayList<>();
        private final List<ResultRow> rows = new ArrayList<>();
        private int pointer = 0;

        VirtualQuery() throws DBCommException {}

        protected List<ResultColumn> getColumns() {
            return cols;
        }

        VirtualConnection prepareServerActivityData(){
            cols.clear();
            cols.add(new ResultColumn("procpid", "varchar"));
            cols.add(new ResultColumn("datname", "varchar"));
            cols.add(new ResultColumn("waiting", "varchar"));
            cols.add(new ResultColumn("start_time", "varchar"));
            cols.add(new ResultColumn("run_time", "varchar"));
            cols.add(new ResultColumn("current_query", "varchar"));

            ResultRow row = new ResultRow(cols);
            row.add("procpid");
            row.add("datname");
            row.add("f");
            row.add("start_time");
            row.add("10");
            row.add("current_query");
            rows.add(row);
            return VirtualConnection.this;
        }

        VirtualConnection prepareTableContentData(Relation relation) {
            cols.clear();
            for(Attribute attribute : relation.getAttributes()) {
                cols.add(new ResultColumn(attribute.getName(), attribute.getBehavior().getAttType()));
            }
            return VirtualConnection.this;
        }

        private VirtualQuery getTableContent(int callNumber) throws DBCommException {
            ResultRow row;
            switch (callNumber) {
                // Dataset 1
                case 0:
                    rows.clear();
                    row = new ResultRow(cols);
                    row.add("AAA");
                    row.add("AAA");
                    rows.add(row);
                    break;
                case 1:
                    rows.clear();
                    break;

                // Dataset 2
                case 2:
                    rows.clear();
                    row = new ResultRow(cols);
                    row.add("BBB");
                    row.add("BBB");
                    rows.add(row);
                    row = new ResultRow(cols);
                    row.add("BBB-1");
                    row.add("BBB-1");
                    rows.add(row);
                    break;
                case 3:
                    rows.clear();
                    row = new ResultRow(cols);
                    row.add("BBB");
                    row.add("BB-BB");
                    rows.add(row);
                    row = new ResultRow(cols);
                    row.add("BBB-1");
                    row.add("BBB-1");
                    rows.add(row);
                    break;

                // Dataset 3
                case 4:
                    rows.clear();
                    break;
                case 5:
                    rows.clear();
                    break;

                // Dataset 4
                case 6:
                    rows.clear();
                    row = new ResultRow(cols);
                    row.add("DD");
                    row.add("DD");
                    rows.add(row);
                    break;
                case 7:
                    throw new DBCommException("Just for fun", "No details");
                default:
                    throw new IllegalArgumentException("Rows are only available for 4 pairs tables");
            }
            pointer = 0;
            return this;
        }

        @Override
        public boolean next() {
            if(pointer < rows.size()){
                pointer++;
                return true;
            }
            return false;
        }

        public String getString(int i){
            return rows.get(pointer-1).vals.get(i-1).toString();
        }

    }
}
