
package com.databazoo.components.textInput;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.tools.formatter.FormatterBase;
import com.databazoo.tools.Dbg;

/**
 * Report SQL errors back to Formatted Text Field.
 * @author bobus
 */
public class QueryErrorPositionObserver {
	private static final Map<GCFrameWithObservers,QueryErrorPositionObserver> cache = new HashMap<>();

	public static QueryErrorPositionObserver get (GCFrameWithObservers parent) {
		QueryErrorPositionObserver ret = cache.get(parent);
		if(ret == null){
			ret = new QueryErrorPositionObserver();
			cache.put(parent, ret);
		}
		return ret;
	}

	public static QueryErrorPositionObserver getClean (GCFrameWithObservers parent) {
		QueryErrorPositionObserver ret = new QueryErrorPositionObserver();
		cache.put(parent, ret);
		return ret;
	}

	public static void remove (GCFrameWithObservers parent) {
		cache.remove(parent);
	}

	private FormattedTextField bodyInput;
	private int correction;

	private QueryErrorPositionObserver(){}

	public void registerObserver(FormattedTextField queryInput){
		this.bodyInput = queryInput;
	}

	public void setCorrection(int correction){
		this.correction = correction;
	}

	public void errorAt(Point location){
		try {
			//Dbg.info("Error is at "+location.x);
			location.x += correction;
			//Dbg.info("Translated to "+location.x);

			if(location.x >= 0 /*&& location.y >= 0*/){
				bodyInput.setCaretPosition(location.x);
				if(location.y <= 0){
					int eolPos = bodyInput.getText().replace("\r", "").indexOf('\n', location.x);
					location.y = eolPos > 0 ? eolPos : bodyInput.getText().length();
				}

				//Dbg.info("Error at "+location.x+"x"+location.y);

				bodyInput.getStyledDocument().setCharacterAttributes(location.x, location.y - location.x, bodyInput.getStyledDocument().getStyle(FormatterBase.STR_STYLE_ERROR), true);
			}
			bodyInput.requestFocusInWindow();
		} catch (IllegalArgumentException e){
			Dbg.notImportant("Error position could not be marked.", e);
		}
	}

}
