package com.databazoo.devmodeler.conn;

import java.util.ArrayList;
import java.util.List;

class ResultRow {
	final List<Object> vals = new ArrayList<>();
	final List<ResultColumn> cols;

	ResultRow(List<ResultColumn> cols) {
		this.cols = cols;
	}

	void add(Object val) {
		vals.add(val);
	}
}