
package com.databazoo.devmodeler.conn;


/**
 * Config for SQL generation.
 *
 * @author bobus
 */
public class SQLOutputConfig {

	public static final SQLOutputConfig DEFAULT = new SQLOutputConfig
			.Builder()
			.build();
	public static final SQLOutputConfig CREATE = new SQLOutputConfig
			.Builder()
			.withComments(false)
			.build();
	public static final SQLOutputConfig DROP_CREATE = new SQLOutputConfig
			.Builder()
			.withComments(false)
			.withDrop(true)
			.build();
	public static final SQLOutputConfig WIZARD = new SQLOutputConfig
			.Builder()
			.withOriginal(true)
			.withSequenceStarts(false)
			.withDrop(true)
			.build();

	final boolean exportComments;
	final boolean exportOriginal;
	final boolean exportSequenceStarts;
	final boolean exportSkipTriggersConstraints;
	final boolean exportData;
	final boolean exportDrop;
	final boolean exportDropCommentedOut;
	final boolean exportEmptyLines;

	protected SQLOutputConfig (boolean exportComments, boolean exportOriginal, boolean exportSequenceStarts, boolean exportData, boolean exportDrop, boolean exportDropCommentedOut, boolean exportEmptyLines, boolean exportSkipTriggersConstraints) {
		this.exportComments = exportComments;
		this.exportOriginal = exportOriginal;
		this.exportSequenceStarts = exportSequenceStarts;
		this.exportData = exportData;
		this.exportDrop = exportDrop;
		this.exportDropCommentedOut = exportDropCommentedOut;
		this.exportEmptyLines = exportEmptyLines;
		this.exportSkipTriggersConstraints = exportSkipTriggersConstraints;
	}

	String getNL(){
		return exportEmptyLines ? "\n\n" : "\n";
	}

	String getDropComment(String input){
		if(!exportDropCommentedOut){
			return input;
		}else{
			return "-- "+input.replace("\n", "\n-- ");
		}
	}

	public static class Builder {
		boolean exportComments, exportOriginal, exportSequenceStarts, exportSkipTriggersConstraints, exportData, exportDrop, exportDropCommentedOut, exportEmptyLines;

		/**
		 * Default config
		 */
		public Builder() {
			this.exportComments = true;
			this.exportOriginal = false;
			this.exportSequenceStarts = true;
			this.exportData = false;
			this.exportDrop = false;
			this.exportDropCommentedOut = false;
			this.exportEmptyLines = true;
			this.exportSkipTriggersConstraints = false;
		}

		/**
		 * Insert SQL comments?
		 */
		public Builder withComments(boolean exportComments) {
			this.exportComments = exportComments;
			return this;
		}

		/**
		 * Insert ORIGINAL comment?
		 */
		public Builder withOriginal(boolean exportOriginal) {
			this.exportOriginal = exportOriginal;
			return this;
		}

		/**
		 * Export sequence current values?
		 */
		public Builder withSequenceStarts(boolean exportSequenceStarts) {
			this.exportSequenceStarts = exportSequenceStarts;
			return this;
		}

		/**
		 * Skip constraints and triggers as they will be generated in a batch at the end of the export
		 */
		public Builder withSkipTriggersConstraints(boolean exportSkipTriggersConstraints) {
			this.exportSkipTriggersConstraints = exportSkipTriggersConstraints;
			return this;
		}

		/**
		 * Export data from tables?
		 */
		public Builder withData(boolean exportData) {
			this.exportData = exportData;
			return this;
		}

		/**
		 * Add DROP statement to CREATEs?
		 */
		public Builder withDrop(boolean exportDrop) {
			this.exportDrop = exportDrop;
			return this;
		}

		/**
		 * Add DROP statement to CREATEs?
		 */
		public Builder withDropCommentedOut(boolean exportDropComment) {
			this.exportDropCommentedOut = exportDropComment;
			return this;
		}

		/**
		 * Add extra newlines for better readability?
		 */
		public Builder withEmptyLines(boolean exportEmptyLines) {
			this.exportEmptyLines = exportEmptyLines;
			return this;
		}

		public SQLOutputConfig build(){
			return new SQLOutputConfig(exportComments, exportOriginal, exportSequenceStarts, exportData, exportDrop, exportDropCommentedOut, exportEmptyLines, exportSkipTriggersConstraints);
		}
	}
}
