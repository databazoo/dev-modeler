
package com.databazoo.devmodeler.wizards;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.TableModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import com.databazoo.components.FileChooser;
import com.databazoo.components.text.SelectableText;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.FileFilterFactory;

/**
 * Export data from query window.
 *
 * @author bobus
 */
public class DataExportWizard extends MigWizard {

	public static DataExportWizard get(){
		return new DataExportWizard();
	}

	private DataExportWizard(){
		super();
	}

	@Override public void valueChanged(TreeSelectionEvent tse) {}
	@Override public void notifyChange (String elementName, String value) {}
	@Override public void notifyChange (String elementName, boolean value) {}
	@Override public void notifyChange (String elementName, boolean[] values) {}

	public void drawExport(TableModel model) {
		File file = FileChooser.showWithOverwrite(
				"Save data to",
				"Save",
				new File(System.getProperty("user.home"), "data.xls"),
				FileFilterFactory.getXlsCsvFilter(false)
		);
		if(file != null){
			if(file.getName().endsWith("csv")) {
				exportToCSV(file, model);
			}else{
				exportToXLS(file, model);
			}
		}
	}

	void exportToXLS(File file, TableModel model) {
		try {
			try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"))){
				StringBuilder sb = new StringBuilder();
				sb.append("<table>");

				// HEADER
				sb.append("<tr>\n");
				for(int col=0; col<model.getColumnCount(); col++){
					sb.append("\t<th>").append(model.getColumnName(col)).append("</th>\n");
				}
				sb.append("</tr>\n");

				// DATA
				for(int row=0; row<model.getRowCount()-1; row++){
					sb.append("<tr>\n");
					for(int col=0; col<model.getColumnCount(); col++){
						Object val = model.getValueAt(row, col);
						if(val != null){
							val = val.toString().replace("<", "&lt;").replace(">", "&gt;");
						} else{
							val = "";
						}
						sb.append("\t<td>").append(val).append("</td>\n");
					}
					sb.append("</tr>\n");
				}
				sb.append("</table>");

				out.write('\ufeff');
				out.write(sb.toString());
				out.close();
			}
		} catch (Exception ex) {
			Dbg.fixme("Could not export to XLS", ex);
			String message = "File could not be saved.\n\n"
					+ "Error details:\n"
					+ ex.getLocalizedMessage();
			JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING + file.getName(), JOptionPane.ERROR_MESSAGE);
		}
	}

	void exportToCSV (File file, TableModel model) {
		try {
			try(BufferedWriter out = new BufferedWriter(new FileWriter(file))){
				StringBuilder sb = new StringBuilder();

				// HEADER
				for(int col=0; col<model.getColumnCount(); col++){
					if(col > 0){
						sb.append(";");
					}
					sb.append(model.getColumnName(col));
				}
				sb.append("\n");

				// DATA
				for(int row=0; row<model.getRowCount()-1; row++){
					for(int col=0; col<model.getColumnCount(); col++){
						if(col > 0){
							sb.append(";");
						}
						Object val = model.getValueAt(row, col);
						if(val != null) {
							sb.append(val);
						}
					}
					sb.append("\n");
				}

				out.write(sb.toString());
				out.close();
			}
		} catch (Exception ex) {
			Dbg.fixme("Could not export to CSV", ex);
			String message = "File could not be saved.\n\n"
							+ "Error details:\n"
							+ ex.getLocalizedMessage();
			JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING + file.getName(), JOptionPane.ERROR_MESSAGE);
		}
	}

}
