package com.databazoo.devmodeler.conn;


import java.util.regex.Pattern;

/**
 * String builder wrapper for SQL strings.
 */
class SQLBuilder {
	private static final Pattern PATTERN_ENDS_WITH_SEMICOLON = Pattern.compile(ConnectionUtils.ENDS_WITH_SEMICOLON, Pattern.DOTALL);

	private final StringBuilder sb;
	private final SQLOutputConfig config;

	SQLBuilder() {
		this(null);
	}

	SQLBuilder(SQLOutputConfig config) {
		this.sb = new StringBuilder();
		this.config = config != null ? config : SQLOutputConfig.DEFAULT;
	}

	SQLBuilder(String initialSQL, SQLOutputConfig config) {
		this.sb = new StringBuilder(initialSQL);
		this.config = config != null ? config : SQLOutputConfig.DEFAULT;
	}

	/**
	 * Append a value.
	 *
	 * @param object a string to append
	 * @return SQLBuilder
	 */
	SQLBuilder a(Object object) {
		sb.append(object);
		return this;
	}

	/**
	 * Append an escaped quoted value.
	 *
	 * @param object a string to append
	 * @return SQLBuilder
	 */
	SQLBuilder quotedEscaped(Object object) {
		sb.append("'").append(object.toString().replace("'", "''")).append("'");
		return this;
	}

	/**
	 * Append an escaped quoted value.
	 *
	 * @param object a string to append
	 * @return SQLBuilder
	 */
	SQLBuilder quotedEscapedOrNull(Object object) {
		if (object.toString().isEmpty()) {
			sb.append("NULL");
		} else {
			sb.append("'").append(object.toString().replace("'", "''")).append("'");
		}
		return this;
	}

	/**
	 * Append a comment line.
	 *
	 * @param lenght required count of "*" chars
	 * @return SQLBuilder
	 */
	SQLBuilder commentLine(int lenght) {
		sb.append("/");
		for(int i=0; i<lenght; i++){
			sb.append("*");
		}
		sb.append("/");
		return this;
	}

	/**
	 * Append a comment line.
	 *
	 * @param originalString calculate required count of "*" chars according to this string
	 * @return SQLBuilder
	 */
	SQLBuilder commentLine(String originalString) {
		return commentLine(originalString.length()-2);
	}

	/**
	 * Append a semi-colon.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder semicolon() {
		sb.append(";");
		return this;
	}

	/**
	 * Append a semi-colon in case given source does not end with one.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder conditionalSemicolon(String source) {
		if (!PATTERN_ENDS_WITH_SEMICOLON.matcher(source).matches()) {
			semicolon();
		}
		return this;
	}

	/**
	 * Append given source and a semi-colon in case given source does not end with one.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder withConditionalSemicolon(String source) {
		sb.append(source);
		return conditionalSemicolon(source);
	}

	/**
	 * Append a space.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder space() {
		sb.append(" ");
		return this;
	}

	/**
	 * Append a new line.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder nl() {
		sb.append(config.getNL());
		return this;
	}

	/**
	 * Append a new line.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder n() {
		sb.append("\n");
		return this;
	}

	/**
	 * Append a blank line if some content already exists.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder nlIfNotEmpty() {
		if(sb.length() > 0) {
			nl();
		}
		return this;
	}

	/**
	 * Append a new line + tab.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder nt() {
		sb.append("\n\t");
		return this;
	}

	/**
	 * Append a new line + tab + tab.
	 *
	 * @return SQLBuilder
	 */
	SQLBuilder ntt() {
		sb.append("\n\t\t");
		return this;
	}

	/**
	 * Get the result.
	 *
	 * @return String result
	 */
	@Override
	public String toString() {
		return sb.toString();
	}

	/**
	 * Determines if current content is empty.
	 *
	 * @return is empty?
	 */
	public boolean isEmpty() {
		return sb.length() == 0;
	}
}
