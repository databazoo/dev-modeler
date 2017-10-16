package com.databazoo.devmodeler.conn;

public class ResultColumn {
	final String name;
	final String type;
	private int longest = 0;

	ResultColumn(String columnName, String columnTypeName) {
		name = columnName;
		type = columnTypeName;
	}

	void tryMaxW(int chars) {
		if (longest < chars) {
			longest = chars;
		}
	}

	public void setLongest(int longest) {
		this.longest = longest;
	}

	int getLongest() {
		return longest;
	}
}