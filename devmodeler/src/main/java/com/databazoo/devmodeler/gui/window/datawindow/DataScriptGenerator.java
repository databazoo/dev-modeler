package com.databazoo.devmodeler.gui.window.datawindow;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.databazoo.devmodeler.conn.Result;

import static com.databazoo.devmodeler.gui.window.datawindow.DataWindowOutputData.L_SELECTED_CELLS;
import static com.databazoo.devmodeler.gui.window.datawindow.DataWindowOutputData.L_SELECTED_COLUMNS_ALL_ROWS;
import static com.databazoo.devmodeler.gui.window.datawindow.DataWindowOutputData.L_SELECTED_ROWS_ALL_COLUMNS;

/**
 * Script generator delegate for {@link DataWindowOutputData}.
 */
public class DataScriptGenerator {

    private final DataWindowOutputData dataWindow;

    public DataScriptGenerator(DataWindowOutputData dataWindow) {
        this.dataWindow = dataWindow;
    }

    int[] getSelectedRows(String selectedMenu, int row) {
        int[] rows;
        switch(selectedMenu){
        case L_SELECTED_CELLS:
            rows = dataWindow.outputData.getSelectedRows();
            break;
        case L_SELECTED_ROWS_ALL_COLUMNS:
            rows = dataWindow.outputData.getSelectedRows().length > 0 ? dataWindow.outputData.getSelectedRows() : new int[]{row};
            break;
        default:
            rows = new int[dataWindow.outputData.getRowCount()];
            for(int i = 0; i < dataWindow.outputData.getRowCount(); i++){
                rows[i] = i;
            }
            break;
        }
        return rows.length > 0 ? rows : new int[]{row};
    }

    int[] getSelectedCols(String selectedMenu, int column) {
        int[] cols;
        switch(selectedMenu){
        case L_SELECTED_CELLS:
            cols = dataWindow.outputData.getSelectedColumns();
            break;
        case L_SELECTED_COLUMNS_ALL_ROWS:
            cols = dataWindow.outputData.getSelectedColumns().length > 0 ? dataWindow.outputData.getSelectedColumns() : new int[]{column};
            break;
        default:
            cols = new int[dataWindow.outputData.getColumnCount()];
            for(int i = 0; i < dataWindow.outputData.getColumnCount(); i++){
                cols[i] = i;
            }
            break;
        }
        return cols.length > 0 ? cols : new int[]{column};
    }

    String generateInsertScript(int[] selectedRows, int[] selectedColumns, Result model) {
        StringBuilder output = new StringBuilder();
        LinkedHashMap<String, String> values;
        for(int row : selectedRows){
            if(model.isNewLine(row)){
                continue;
            }
            values = new LinkedHashMap<>();
            for(int col : selectedColumns){
                values.put(model.getColumnName(col), (String)model.getValueAt(row, col));
            }
            final String queryInsert = dataWindow.rel != null ?
                    dataWindow.connection.getQueryInsert(dataWindow.rel, values) :
                    dataWindow.connection.getQueryInsert("table", values);
            output.append("\n").append(queryInsert);
        }
        return output.toString();
    }

    String generateUpdateScript(int[] selectedRows, int[] selectedColumns, Result model) {
        StringBuilder output = new StringBuilder();
        LinkedHashMap<String, String> values;
        for(int row : selectedRows){
            if(model.isNewLine(row)){
                continue;
            }

            HashMap<String, String> linePKeyValues = getLinePKValues(model, row);
            values = new LinkedHashMap<>();
            for(int col1 : selectedColumns){
                values.put(model.getColumnName(col1), (String)model.getValueAt(row, col1));
            }
            output.append("\n").append(
                    dataWindow.rel != null ?
                    dataWindow.connection.getQueryUpdate(dataWindow.rel, linePKeyValues, values) :
                    dataWindow.connection.getQueryUpdate("table", linePKeyValues, values)
            );
        }
        return output.toString();
    }

    private HashMap<String, String> getLinePKValues(Result model, int row) {
        HashMap<String,String> linePKeyValues = new LinkedHashMap<>();
        if(dataWindow.rel != null){
            for (int col = 0; col < model.getColumnCount(); col++) {
                if (dataWindow.rel.pKeyContains(model.getColumnName(col))) {
                    linePKeyValues.put(model.getColumnName(col), (String) model.getValueAt(row, col));
                }
            }
        }
        return linePKeyValues;
    }
}
