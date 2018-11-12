
package com.databazoo.devmodeler.tools.formatter;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Dbg;
import difflib.Delta;

/**
 * Formatter implementation for Formatted Text Field
 */
public abstract class FormatterBase implements Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	public static final String STR_STYLE_ERROR = "error";
	private static final Pattern NUMERIC_PATTERN = Pattern.compile("[0-9.]+");
	private static final Pattern WORD_PATTERN = Pattern.compile(".*[^a-z0-9_.].*");
    private static final Integer INTEGER_ONE = 1;
    private static final Integer INTEGER_TWO = 2;

	transient Style styleRegular;
	private transient Style styleKeyword;
	private transient Style styleTableCol;
	private transient Style styleLocalVar;
	private transient Style styleTableColUl;
	private transient Style styleDatatype;
	private transient Style styleString;
	private transient Style styleNumber;
	private transient Style styleComment;
	private transient Style styleParen;

	/*public static final HashMap<String, Integer> STYLE_USAGE = new HashMap<>();
	private static void usedStyle(String styleName){
		Integer cnt = STYLE_USAGE.get(styleName);
		if(cnt == null){
			STYLE_USAGE.put(styleName, 1);
		}else{
			STYLE_USAGE.put(styleName, cnt + 1);
		}
	}*/

	protected final transient Set<String> ELEMENT_NAMES = new HashSet<>();
	protected final transient Set<String> DATATYPE_NAMES = new HashSet<>();
	protected final transient Set<String> BEGIN_CLAUSES = new HashSet<>();
	protected final transient Set<String> END_CLAUSES = new HashSet<>();
	protected final transient Set<String> LOGICAL = new HashSet<>();
	protected final transient Set<String> QUANTIFIERS = new HashSet<>();
	protected final transient Set<String> DML = new HashSet<>();
	private final transient Set<String> LOCAL_KEYWORDS = new HashSet<>();

	transient List<Delta> deltaList;
	boolean isSource = false;
	public boolean isControlDown = false;
	private boolean isFirstFormat = true;

	private final boolean upperCaseKeyWords = Settings.getBool(Settings.L_FONT_FORCE_UPPER);

	private Integer parenAtPos;

	transient DefaultHighlighter hilighter;

	public DefaultHighlighter getHilighter(){
		return hilighter;
	}

	public Integer getParenAtPos() {
		return parenAtPos;
	}

	public void setParenAtPos(Integer parenAtPos) {
		this.parenAtPos = parenAtPos;
	}

	public void format(FormattedTextField pane) {
		FormatProcess fp = getFormatter(pane).perform();
		if(pane.isEditable()){
			Set<String> kwList = fp.getPossibleKeywords();
			pane.updateKeywordList(kwList);
			if(isFirstFormat){
				if(!kwList.isEmpty()){
					pane.format();
				}
				isFirstFormat = false;
			}
		}

		if(pane.getParent() != null && pane.getParent().getParent() != null){
			pane.getParent().getParent().repaint();
		}

		// Disable further formatting for disabled fields - releases a lot of resources
		if(!pane.isEditable()){
			pane.setFormatter(null);
		}
	}

	FormatProcess getFormatter(FormattedTextField pane){
		return new FormatProcess(pane);
	}


	public void addStylesToDocument(StyledDocument doc) {
		if (UIConstants.isLafWithDarkSkin()) {
			addDarkColorSet(doc);
		} else {
			addBrightColorSet(doc);
		}
	}

	private void addDarkColorSet(StyledDocument doc) {
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		styleRegular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(def, FontFactory.getMonospacedName());
		StyleConstants.setFontSize(def, Settings.getInt(Settings.L_FONT_MONO_SIZE));
		StyleConstants.setForeground(def, Color.LIGHT_GRAY);

		styleKeyword = doc.addStyle("kw", styleRegular);
		StyleConstants.setForeground(styleKeyword, UIConstants.COLOR_BLUE_GRAY);

		styleNumber = doc.addStyle("nums", styleRegular);
		StyleConstants.setForeground(styleNumber, UIConstants.COLOR_PINK);

		styleString = doc.addStyle("strings", styleRegular);
		StyleConstants.setForeground(styleString, UIConstants.COLOR_BROWN);

		styleTableCol = doc.addStyle("table_col", styleRegular);
		StyleConstants.setForeground(styleTableCol, UIConstants.COLOR_GREEN_BRIGHT);
		StyleConstants.setItalic(styleTableCol, true);

		styleLocalVar = doc.addStyle("local_var", styleRegular);
		StyleConstants.setForeground(styleLocalVar, UIConstants.COLOR_BLUE_GRAY);
		StyleConstants.setItalic(styleLocalVar, true);

		styleTableColUl = doc.addStyle("table_col_ul", styleRegular);
		StyleConstants.setForeground(styleTableColUl, UIConstants.COLOR_GREEN_BRIGHT);
		StyleConstants.setItalic(styleTableColUl, true);
		StyleConstants.setUnderline(styleTableColUl, true);

		styleDatatype = doc.addStyle("datatype", styleRegular);
		StyleConstants.setForeground(styleDatatype, UIConstants.COLOR_PINK);
		StyleConstants.setItalic(styleDatatype, true);

		styleComment = doc.addStyle("comment", styleRegular);
		StyleConstants.setForeground(styleComment, UIConstants.COLOR_GRAY);

		styleParen = doc.addStyle("paren", styleRegular);
		StyleConstants.setBackground(styleParen, UIConstants.COLOR_HILIGHT_CHANGE);
		StyleConstants.setBold(styleParen, true);

		Style styleError = doc.addStyle(STR_STYLE_ERROR, styleRegular);
		StyleConstants.setForeground(styleError, UIConstants.COLOR_RED);
		StyleConstants.setUnderline(styleError, true);
	}

	private void addBrightColorSet(StyledDocument doc) {
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		styleRegular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(def, FontFactory.getMonospacedName());
		StyleConstants.setFontSize(def, Settings.getInt(Settings.L_FONT_MONO_SIZE));
		StyleConstants.setForeground(def, Color.BLACK);

		styleKeyword = doc.addStyle("kw", styleRegular);
		StyleConstants.setForeground(styleKeyword, UIConstants.COLOR_BLUE);

		styleNumber = doc.addStyle("nums", styleRegular);
		StyleConstants.setForeground(styleNumber, UIConstants.COLOR_AMBER);

		styleString = doc.addStyle("strings", styleRegular);
		StyleConstants.setForeground(styleString, UIConstants.COLOR_BROWN);

		styleTableCol = doc.addStyle("table_col", styleRegular);
		StyleConstants.setForeground(styleTableCol, UIConstants.COLOR_GREEN);
		StyleConstants.setItalic(styleTableCol, true);

		styleLocalVar = doc.addStyle("local_var", styleRegular);
		StyleConstants.setForeground(styleLocalVar, UIConstants.COLOR_BLUE_DARK);
		StyleConstants.setItalic(styleLocalVar, true);

		styleTableColUl = doc.addStyle("table_col_ul", styleRegular);
		StyleConstants.setForeground(styleTableColUl, UIConstants.COLOR_GREEN);
		StyleConstants.setItalic(styleTableColUl, true);
		StyleConstants.setUnderline(styleTableColUl, true);

		styleDatatype = doc.addStyle("datatype", styleRegular);
		StyleConstants.setForeground(styleDatatype, UIConstants.COLOR_AMBER);
		StyleConstants.setItalic(styleDatatype, true);

		styleComment = doc.addStyle("comment", styleRegular);
		StyleConstants.setForeground(styleComment, UIConstants.COLOR_GRAY);

		styleParen = doc.addStyle("paren", styleRegular);
		StyleConstants.setBackground(styleParen, UIConstants.COLOR_HILIGHT_CHANGE);
		StyleConstants.setBold(styleParen, true);

		Style styleError = doc.addStyle(STR_STYLE_ERROR, styleRegular);
		StyleConstants.setForeground(styleError, UIConstants.COLOR_RED);
		StyleConstants.setUnderline(styleError, true);
	}

	class FormatProcess {
		private final boolean isEditable;
		StringTokenizer tokens;
		String lastToken;
		String token;
		String lcToken;
		int line = 0;
		StyledDocument doc;
		String sql;
		Map<String,Integer> possibleKeywords;

		FormatProcess(FormattedTextField pane) {
			this.doc = pane.getStyledDocument();
			this.isEditable = pane.isEditable();

			synchronized (doc) {
				if (isEditable) {
					possibleKeywords = new HashMap<>();
				}
				int docLength = doc.getLength();
				try {
					sql = doc.getText(0, docLength);
					doc.remove(0, docLength);
					tokens = new StringTokenizer(sql, "()+*/-=<>'`\"[],:;%| \r\n\f\t", true);
				} catch (Exception ex) {
					Dbg.fixme("Remove failed. Expected doc lenght was " + docLength + ", current is " + doc.getLength(), ex);
				}
			}
		}

		private Set<String> getPossibleKeywords(){
			LOCAL_KEYWORDS.clear();
			for(Map.Entry<String, Integer> data : possibleKeywords.entrySet()){
				if(data.getValue() > 1){
					LOCAL_KEYWORDS.add(data.getKey());
				}
			}
			possibleKeywords = null;
			return LOCAL_KEYWORDS;
		}

		FormatProcess perform(){
			checkDeltas();
			while (tokens != null && tokens.hasMoreTokens() ) {
				token = tokens.nextToken();
				lcToken = token.toLowerCase();

				// Preprocessing for old., new. and schema.
				if(lcToken.startsWith("new.") ||lcToken.startsWith("old.")){
					//usedStyle("new/old");
					String origToken = token;
					token = origToken.substring(0, 4);
					datatype();
					if(origToken.length() > 4){
						token = origToken.substring(4);
						lcToken = token.toLowerCase();
					}else{
						continue;
					}
				}else if(lcToken.contains(".") && !isNumeric(lcToken) && !ELEMENT_NAMES.contains(lcToken)){
					//usedStyle("dotted");
					String origToken = token;
					int pos = token.indexOf('.')+1;
					token = origToken.substring(0, pos-1);
					possibleKeyword();
					token = ".";
					misc();
					if(origToken.length() > pos){
						token = origToken.substring(pos);
						lcToken = token.toLowerCase();
					}else{
						continue;
					}
				}

				/*
				USAGE STATISTICS:
					whitespace		65531
					keyword			24724
					misc			19349
					element name	11194
					string			7496
					open paren		3864
					close paren		3860
					numeric			3783
					possibleKeyword	2518
					datatype		2086
					line comment	1460
					dotted			1423
					new/old			64
					block comment	62
				 */

				// Token processing
				if ( isWhitespace( lcToken ) ) {
					//usedStyle("whitespace");
					white();

				} else if ( BEGIN_CLAUSES.contains( lcToken ) || END_CLAUSES.contains( lcToken ) || DML.contains( lcToken ) || QUANTIFIERS.contains( lcToken ) || LOGICAL.contains( lcToken ) ) {
					//usedStyle("keyword");
					keyword();

				} else if ( ELEMENT_NAMES.contains(lcToken) ) {
					//usedStyle("element name");
					if(isControlDown){
						elementUnderlined();
					}else{
						element();
					}

				} else if ( isStringEscape(token) ) {
					//usedStyle("string");
					String escString = token;
					stringValue();
					do {
						if(tokens.hasMoreTokens()){
							token = tokens.nextToken();
							if(isEqual(token, '\n')){
								white();
							}else{
								stringValue();
							}
						}else{
							break;
						}
					}
					while ( !escString.equals( token ) );

				} else if ( isEqual(lcToken, '(') ) {
					//usedStyle("open paren");
					openParen();

				} else if ( isEqual(lcToken, ')') ) {
					//usedStyle("close paren");
					closeParen();

				} else if (isNumeric(lcToken)) {
					//usedStyle("numeric");
					numericValue();

				} else if ( DATATYPE_NAMES.contains(lcToken) ) {
					//usedStyle("datatype");
					datatype();

				} else if ( isWord(lcToken) ) {
					//usedStyle("possibleKeyword");
					possibleKeyword();

				} else if ( isEqual(lastToken, '/') && isEqual(token, '*') ) {
					//usedStyle("block comment");
					reComment(lastToken);
					comment();
					do {
						if(tokens.hasMoreTokens()){
							lastToken = lcToken = token;
							token = tokens.nextToken();
							if(isEqual(token, '\n')){
								white();
							}else{
								comment();
							}
						}else{
							break;
						}
					}
					while ( !(isEqual(lastToken, '*') && isEqual(token, '/')) );

				} else if ( isEqual(lastToken, '-') && isEqual(token, '-') ) {
					//usedStyle("line comment");
					reComment(lastToken);
					comment();
					do {
						if(tokens.hasMoreTokens()){
							lastToken = token;
							token = tokens.nextToken();
							if(isEqual(token, '\n')){
								white();
                                lastToken = lcToken = token;
							}else{
								comment();
							}
						}else{
							break;
						}
					}
					while ( !isEqual(token, '\n') );

				} else {
					//usedStyle("misc");
					misc();
				}

				if ( !isWhitespace( lcToken ) ) {
					lastToken = lcToken;
				}

			}
			checkDeltas();
			checkParentheses();
			return this;
		}

		private void checkParentheses(){
			if(parenAtPos != null){
				int parenLevel = 1;

				char openParen = '(';
				char closeParen = ')';
				boolean isOpening = true;
				switch(sql.charAt(parenAtPos)){
					case ')':
						isOpening = false;
						break;

					case '[':
						openParen = '[';
						closeParen = ']';
						isOpening = true;
						break;

					case ']':
						openParen = '[';
						closeParen = ']';
						isOpening = false;
						break;

					case '{':
						openParen = '{';
						closeParen = '}';
						isOpening = true;
						break;

					case '}':
						openParen = '{';
						closeParen = '}';
						isOpening = false;
						break;
				}

				// Look forward
				if(isOpening){
					int pos = parenAtPos+1;
					while(true){
						int openParenPos = sql.indexOf(openParen, pos);
						int closeParenPos = sql.indexOf(closeParen, pos);

						// No more closing parentheses, nothing to match
						if(closeParenPos < 0){
							break;
						}

						// Next paren is closing
						if(closeParenPos < openParenPos || openParenPos < 0){
							parenLevel--;
							pos = closeParenPos+1;

							// Match found
							if(parenLevel == 0){
								doc.setCharacterAttributes(parenAtPos, 1, styleParen, true);
								doc.setCharacterAttributes(closeParenPos, 1, styleParen, true);
								break;
							}

						// Next paren is opening
						}else{
							parenLevel++;
							pos = openParenPos+1;
						}
					}

				// Look backward
				}else{
					int pos = parenAtPos-1;
					while(true){
						int openParenPos = sql.lastIndexOf(openParen, pos);
						int closeParenPos = sql.lastIndexOf(closeParen, pos);

						// No more opening parentheses, nothing to match
						if(openParenPos < 0){
							break;
						}

						// Next paren is opening
						if(closeParenPos < openParenPos){
							parenLevel--;
							pos = openParenPos-1;

							// Match found
							if(parenLevel == 0){
								doc.setCharacterAttributes(parenAtPos, 1, styleParen, true);
								doc.setCharacterAttributes(openParenPos, 1, styleParen, true);
								break;
							}

						// Next paren is closing
						}else{
							parenLevel++;
							pos = closeParenPos-1;
						}
					}
				}
			}
		}

		private void possibleKeyword(){
			if(isEditable) {
				Integer cnt = possibleKeywords.get(token);
				if (cnt == null) {
					possibleKeywords.put(token, INTEGER_ONE);
				} else if(cnt == 1) {                       // Only write twice - do not generate more objects
					possibleKeywords.put(token, INTEGER_TWO);
				}
			}

			if(LOCAL_KEYWORDS.contains(token)){
				localVar();
			}else {
				misc();
			}
		}

		private void misc(){
			out(styleRegular);
		}

		private void white(){
			out(styleRegular);
			if(token.equals("\n")){
				line++;
				checkDeltas();
			}
		}

		protected void out(Style style) {
			try {
				doc.insertString(doc.getLength(), token, style);
			} catch (BadLocationException e) {
				Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
			}
		}
		private void outUpper(Style style) {
			token = token.toUpperCase();
			out(style);
		}

		private void keyword(){
			if(upperCaseKeyWords){
				outUpper(styleKeyword);
			}else{
				out(styleKeyword);
			}
		}

		private void closeParen(){
			out(styleRegular);
		}

		private void openParen(){
			out(styleRegular);
		}

		private void reComment(String inToken) {
			if(doc != null){
				try {
					doc.remove(doc.getLength()-1, 1);
					doc.insertString(doc.getLength(), inToken, styleComment);
				} catch (BadLocationException e) {
					Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
				}
			}
		}

		private void comment(){
			out(styleComment);
		}
		private void stringValue(){
			out(styleString);
		}
		private void numericValue(){
			out(styleNumber);
		}
		private void localVar(){
			out(styleLocalVar);
		}
		private void element(){
			out(styleTableCol);
		}
		private void elementUnderlined(){
			out(styleTableColUl);
		}
		private void datatype(){
			out(styleDatatype);
		}

		private boolean isNumeric(String token) {
			//return token.matches("[0-9.]+");
			return NUMERIC_PATTERN.matcher(token).matches();
		}

		private boolean isWord(String token) {
			//return token.matches("[a-z0-9_.]+");
			return !WORD_PATTERN.matcher(token).matches();
		}

		private boolean isWhitespace(String token) {
			return token != null && token.length() == 1 && (token.charAt(0) == ' ' || token.charAt(0) == '\n' || token.charAt(0) == '\r' || token.charAt(0) == '\f' || token.charAt(0) == '\t');
		}

		private boolean isStringEscape(String token) {
			return token != null && token.length() == 1 && (token.charAt(0) == '"' || token.charAt(0) == '\'' || token.charAt(0) == '`');
		}

		private boolean isEqual(String token, char expectedChar) {
			return token != null && token.length() == 1 && token.charAt(0) == expectedChar;
		}

		protected void checkDeltas() {}
	}
}
