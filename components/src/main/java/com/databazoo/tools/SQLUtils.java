package com.databazoo.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A set of functions for SQL manipulation.
 */
public interface SQLUtils {

    Pattern PATTERN_SLASH = Pattern.compile(".*?;\\s*/.*", Pattern.DOTALL);
    Pattern PATTERN_EMPTY = Pattern.compile("\\s*");

    /**
     * Explode SQL into individual statements by semicolon.
     *
     * @param sql original SQL
     * @return individual statements
     */
    static String[] explode(String sql) {
        return explode(sql, false);
    }

    /**
     * Explode SQL into individual statements by semicolon and slash.
     *
     * @param sql original SQL
     * @return individual statements
     */
    static String[] explodeWithSlash(String sql) {
        return explode(sql, PATTERN_SLASH.matcher(sql).find());
    }

    /*private*/
    static String[] explode(String sql, boolean useSemicolonSlash) {
        List<String> output = new ArrayList<>();

        Character inString = null;
        boolean inLineComment = false;
        boolean inComment = false;
        char charAtPrev = '~';

        int currentStart = 0;
        final int length = sql.length();
        for (int i = 0; i < length; i++) {
            final char charAt = sql.charAt(i);

            if (inString != null) {
                inString = inString(inString, charAt);

            } else if (inLineComment) {
                inLineComment = inLineComment(charAt);

            } else if (inComment) {
                inComment = inComment(charAtPrev, charAt);

            } else {
                if (charAtPrev == '-' && charAt == '-') {
                    inLineComment = true;

                } else if (charAtPrev == '/' && charAt == '*') {
                    inComment = true;

                } else if (charAt == '\'' || charAt == '"' || charAt == '`') {
                    inString = charAt;

                } else if (charAt == ';') {
                    int posExtra = lookForSlash(sql, i, useSemicolonSlash);
                    if (posExtra >= 0) {
                        output.add(sql.substring(currentStart, i + posExtra));
                        currentStart = i + posExtra;
                    }
                }
            }
            charAtPrev = charAt;
        }
        final String substr = sql.substring(currentStart);
        if (!PATTERN_EMPTY.matcher(substr).matches()) {
            output.add(substr);
        }

        return output.toArray(new String[0]);
    }

    /*private*/
    static boolean inLineComment(char charAt) {
        return charAt != '\n';
    }

    /*private*/
    static boolean inComment(char charAtPrev, char charAt) {
        return charAtPrev != '*' || charAt != '/';
    }

    /*private*/
    static int lookForSlash(String sql, int i, boolean useSemicolonSlash) {
        if (useSemicolonSlash) {
            final int length = sql.length();
            for (int j = i + 1; j < length; j++) {
                final char charAt = sql.charAt(j);
                if (charAt == '/') {
                    return j - i + 1;
                } else if (charAt != ' ' && charAt != '\n' && charAt != '\r' && charAt != '\t') {
                    return -1;
                }
            }
        }
        return 1;
    }

    /*private*/
    static Character inString(Character inString, char charAt) {
        if (charAt == inString) {
            return null;
        }
        return inString;
    }

    static String escapeQM(String sql) {
        int pos = sql.indexOf('?');
        if (pos >= 0) {
            StringBuilder sb = new StringBuilder(sql.length() + 16);
            duplicateQMs(sql, sb);
            return sb.toString();
        }
        return sql;
    }

    /*private*/
    static void duplicateQMs(String sql, StringBuilder sb) {

        Character inString = null;
        boolean inLineComment = false;
        boolean inComment = false;
        boolean inBodyTag = false;
        StringBuilder bodyTag = new StringBuilder();
        boolean inBody = false;
        char charAtPrev = '~';

        final int length = sql.length();
        for (int i = 0; i < length; i++) {
            final char charAt = sql.charAt(i);

            if (inString != null) {
                inString = inString(inString, charAt);

            } else if (inLineComment) {
                inLineComment = inLineComment(charAt);

            } else if (inComment) {
                inComment = inComment(charAtPrev, charAt);

            } else if (inBody) {
                inBody = inBody(sql, i, bodyTag, charAt);

            } else if (inBodyTag) {
                inBodyTag = inBodyTag(bodyTag, charAt);
                if (!inBodyTag && bodyTag.length() > 0) {
                    inBody = true;
                }

            } else {
                if (charAtPrev == '-' && charAt == '-') {
                    inLineComment = true;

                } else if (charAtPrev == '/' && charAt == '*') {
                    inComment = true;

                } else if (charAt == '\'' || charAt == '"' || charAt == '`') {
                    inString = charAt;

                } else if (charAt == '$') {
                    inBodyTag = true;
                    bodyTag = new StringBuilder("$");

                } else if (charAt == '?') {
                    sb.append(charAt);
                }
            }
            sb.append(charAt);
            charAtPrev = charAt;
        }
    }

    /*private*/
    static boolean inBodyTag(StringBuilder bodyTag, char charAt) {
        boolean isUpperCase = charAt >= 65 && charAt <= 90;
        boolean isLowerCase = charAt >= 97 && charAt <= 122;
        boolean isNumber = charAt >= 48 && charAt <= 57;
        boolean stillInTag = isUpperCase || isLowerCase || isNumber;
        if (!stillInTag && charAt != '$') {
            bodyTag.delete(0, bodyTag.length());
        } else {
            bodyTag.append(charAt);
        }
        return stillInTag;
    }

    /*private*/
    static boolean inBody(String sql, int currentPos, StringBuilder bodyTag, char charAt) {
        if (charAt == '$') {
            int lengthSQL = sql.length();
            int lengthTag = bodyTag.length();
            if (lengthSQL > currentPos + lengthTag) {
                String substring = sql.substring(currentPos, currentPos + lengthTag);
                if (substring.equals(bodyTag.toString())) {
                    return false;
                }
            }
        }
        return true;
    }
}
