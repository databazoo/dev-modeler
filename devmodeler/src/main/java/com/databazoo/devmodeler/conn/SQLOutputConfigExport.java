
package com.databazoo.devmodeler.conn;


import com.databazoo.devmodeler.gui.window.ProgressWindow;

/**
 * Config for SQL generation that can hold the returned SQL.
 *
 * @author bobus
 */
public class SQLOutputConfigExport extends SQLOutputConfig{

	public static final int LINE_LENGHT = 120;
	public static final String LINE_SEPARATOR = "/*" + padCenter("", LINE_LENGHT - 4, '*') + "*/";
	public static final int PREVIEW_DATA_ROWS_LIMIT = 10;

	/**
	 * Export string builder
	 */
	private final StringBuilder text = new StringBuilder();
	private Integer previewLimit;
	public final IConnection conn;
	public final ProgressWindow progressWindow;
	public final boolean exportDatabases;
	public final boolean exportSchemata;
	public final boolean exportTables;
	public final boolean exportIndexes;
	public final boolean exportTriggers;
	public final boolean exportConstraints;
	public final boolean exportFunctions;
	public final boolean exportViews;
	public final boolean exportSequences;


	private SQLOutputConfigExport(
			IConnection conn,
			ProgressWindow progressWindow,
			Integer previewLimit,
			boolean exportDatabases,
			boolean exportSchemata,
			boolean exportTables,
			boolean exportIndexes,
			boolean exportTriggers,
			boolean exportConstraints,
			boolean exportFunctions,
			boolean exportViews,
			boolean exportSequences,
			boolean exportComments,
			boolean exportOriginal,
			boolean exportSequenceStarts,
			boolean exportData,
			boolean exportDrop,
			boolean exportDropCommentedOut,
			boolean exportEmptyLines,
			boolean exportSkipTriggersConstraints
	){
		super(exportComments, exportOriginal, exportSequenceStarts, exportData, exportDrop, exportDropCommentedOut, exportEmptyLines, exportSkipTriggersConstraints);
		this.previewLimit = previewLimit;
		this.exportDatabases = exportDatabases;
		this.exportSchemata = exportSchemata;
		this.exportTables = exportTables;
		this.exportIndexes = exportIndexes;
		this.exportTriggers = exportTriggers;
		this.exportConstraints = exportConstraints;
		this.exportFunctions = exportFunctions;
		this.exportViews = exportViews;
		this.exportSequences = exportSequences;
		this.progressWindow = progressWindow;
		this.conn = conn;
	}

	public /*synchronized*/ void append(String appString){
		if(text.length() > 0) {
			text.append(getNL());
		}
		if(exportComments){
			text.append(LINE_SEPARATOR);
			text.append(getNL());
		}
		text.append(appString);
	}

	public String getText(){
		return text.toString();
	}

	boolean isPreviewMode(){
		return previewLimit != null;
	}

	public /*synchronized*/ void updateLimit() throws LimitReachedException {
		if(isPreviewMode()){
			if(previewLimit < 1){
				throw new LimitReachedException();
			}else{
				previewLimit--;
			}
		}
	}

	public static String padCenter(String input, int len, char character){
		boolean left = true;
		for(int i=input.length(); i<len; i++){
			if(left){
				input = character + input;
			}else{
				input += character;
			}
			left = !left;
		}
		return input;
	}

	public static final class LimitReachedException extends Exception {}

	public static class Builder extends SQLOutputConfig.Builder {
		Integer previewLimit;
		IConnection conn;
		ProgressWindow progressWindow;

		boolean exportDatabases, exportSchemata, exportTables, exportIndexes, exportTriggers, exportConstraints, exportFunctions, exportViews, exportSequences;

		/**
		 * Default config
		 */
		public Builder() {
			this.exportDatabases = false;
			this.exportSchemata = true;
			this.exportTables = true;
			this.exportIndexes = true;
			this.exportTriggers = true;
			this.exportConstraints = true;
			this.exportFunctions = true;
			this.exportViews = true;
			this.exportSequences = true;
		}

		/**
		 * Cut the SQLs after for example 50 objects. Leave null for no limit.
		 */
		public Builder withPreviewLimit(Integer previewLimit) {
			this.previewLimit = previewLimit;
			return this;
		}

		/**
		 * Use this connection for dialect?
		 */
		public Builder withConn(IConnection conn) {
			this.conn = conn;
			return this;
		}

		/**
		 * Progress window to report progress from data export.
		 */
		public Builder withProgressWindow(ProgressWindow progressWindow) {
			this.progressWindow = progressWindow;
			return this;
		}

		/**
		 * Export database DDL?
		 */
		public Builder withDatabases(boolean exportDatabases) {
			this.exportDatabases = exportDatabases;
			return this;
		}

		/**
		 * Export schema DDL?
		 */
		public Builder withSchemata(boolean exportSchemata) {
			this.exportSchemata = exportSchemata;
			return this;
		}

		/**
		 * Export table DDL?
		 */
		public Builder withTables(boolean exportTables) {
			this.exportTables = exportTables;
			return this;
		}

		/**
		 * Export index DDL?
		 */
		public Builder withIndexes(boolean exportIndexes) {
			this.exportIndexes = exportIndexes;
			return this;
		}

		/**
		 * Export trigger DDL?
		 */
		public Builder withTriggers(boolean exportTriggers) {
			this.exportTriggers = exportTriggers;
			return this;
		}

		/**
		 * Export constraint DDL?
		 */
		public Builder withConstraints(boolean exportConstraints) {
			this.exportConstraints = exportConstraints;
			return this;
		}

		/**
		 * Export function DDL?
		 */
		public Builder withFunctions(boolean exportFunctions) {
			this.exportFunctions = exportFunctions;
			return this;
		}

		/**
		 * Export view DDL?
		 */
		public Builder withViews(boolean exportViews) {
			this.exportViews = exportViews;
			return this;
		}

		/**
		 * Export standalone sequence DDL?
		 */
		public Builder withSequences(boolean exportSequences) {
			this.exportSequences = exportSequences;
			return this;
		}

		@Override
		public SQLOutputConfigExport build(){
			return new SQLOutputConfigExport(
					conn,
					progressWindow,
					previewLimit,
					exportDatabases,
					exportSchemata,
					exportTables,
					exportIndexes,
					exportTriggers,
					exportConstraints,
					exportFunctions,
					exportViews,
					exportSequences,
					exportComments,
					exportOriginal,
					exportSequenceStarts,
					exportData,
					exportDrop,
					exportDropCommentedOut,
					exportEmptyLines,
					exportSkipTriggersConstraints);
		}
	}
}
