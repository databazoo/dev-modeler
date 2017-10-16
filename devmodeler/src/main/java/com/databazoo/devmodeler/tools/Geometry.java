
package com.databazoo.devmodeler.tools;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.Attribute;

import java.awt.*;
import java.util.Arrays;

/**
 * Collection of geometric operations. Mostly used by Canvas and Line/Draggable Components.
 * @author bobus
 */
public interface Geometry {

	/**
	 * Takes a line component (it's size and flipped status) and decides whether a given clip intersects it.
	 *
	 * @param isFlipped is left-top-to-right-bottom type?
	 * @param componentSize size of the component
	 * @param bounds painted clip's bounds
	 * @return is there an intersection?
	 */
	static boolean shapeIntersectsDiagonal(boolean isFlipped, Dimension componentSize, Rectangle bounds) {
		if (!isFlipped) {
			return isColliding(0, componentSize.width, componentSize.height,
					bounds.x, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y);
		} else {
			return isColliding(componentSize.height, componentSize.width, 0,
					bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
		}
	}

	static boolean isColliding(int lineStartY, int lineEndX, int lineEndY, int clipStartX, int clipStartY, int clipEndX, int clipEndY) {
		int firstVector = (-lineEndX)*(clipStartY - lineStartY) - (lineStartY - lineEndY)*clipStartX;
		int secondVector = (-lineEndX)*(clipEndY - lineStartY) - (lineStartY - lineEndY)*clipEndX;

		boolean bothAboveZero = firstVector > 0 && secondVector > 0;
		boolean bothBelowZero = firstVector < 0 && secondVector < 0;
		return !(bothAboveZero || bothBelowZero);
	}

	/**
	 * Is given point found inside given rectangle?
	 *
	 * @param recLocation x-y of the rectangle
	 * @param recSize size of the rectangle
	 * @param point given point
	 * @return is given point found inside given rectangle?
	 */
	static boolean isPointInRectangle(Point recLocation, Dimension recSize, Point point) {
		return recLocation.x <= point.x &&
				recLocation.x + recSize.width >= point.x &&
				recLocation.y <= point.y &&
				recLocation.y + recSize.height >= point.y;
	}

	/**
	 * Escape a rectangle in closest opossible location.
	 *
	 * @param recLocation x-y of the rectangle
	 * @param recSize size of the rectangle
	 * @param point given point
	 * @return new Point outside the rectangle
	 */
	static Point pointEscapeRectangle(Point recLocation, Dimension recSize, Point point) {
		int x = point.x;
		int y = point.y;
		if(isPointInRectangle(recLocation, recSize, point)) {
			if (recLocation.x + recSize.width - point.x < recLocation.y + recSize.height - point.y) {
				x = recLocation.x + recSize.width + Canvas.GRID_SIZE;
			} else {
				y = recLocation.y + recSize.height + Canvas.GRID_SIZE;

			}
			return getSnappedPosition(x, y);
		}else{
			return point;
		}
	}

	/**
	 * Snap point to grid.
	 *
	 * @param x X of the point
	 * @param y Y of the point
	 * @return snapped Point
	 */
	static Point getSnappedPosition(int x, int y) {
		Point position = new Point(x, y);
		if(Canvas.SNAP_TO_GRID){
			position.x = Math.round(position.x / (float)Canvas.GRID_SIZE) * Canvas.GRID_SIZE;
			position.y = Math.round(position.y / (float)Canvas.GRID_SIZE) * Canvas.GRID_SIZE;
		}
		return position;
	}

	/**
	 * Does given point hit a line (with some tolerance)?
	 * @param click clicked point
	 * @param lineBegin start point of the line
	 * @param lineEnd end point of the line
	 * @return is hit?
	 */
	static boolean clickedOnLine(Point click, Point lineBegin, Point lineEnd){
		double distanceAB = Math.sqrt(Math.pow((double)(lineBegin.x-lineEnd.x), 2.0) + Math.pow((double)(lineBegin.y-lineEnd.y), 2.0));
		double distanceAC = Math.sqrt(Math.pow((double)(lineBegin.x-click.x), 2.0) + Math.pow((double)(lineBegin.y-click.y), 2.0));
		double distanceBC = Math.sqrt(Math.pow((double)(lineEnd.x-click.x), 2.0) + Math.pow((double)(lineEnd.y-click.y), 2.0));
		return distanceAC + distanceBC < distanceAB+1.25;
	}

	static String getReadableSize(long size){
		double step = 1024.0;
		String [] vals = {"B","K","M","G","T"};
		for(int i=vals.length-1; i>=0; i--){
			if(size / Math.pow(step, i) >= 1){
				double ret = Math.round(size * 100.0 / Math.pow(step, (double)i))/100.0;
				if(ret >= 5){
					return ((int)Math.round(ret)) + vals[i];
				}else {
					return ret + vals[i];
				}
			}
		}
		return "0B";
	}
	static String getReadableSizeColorized(long size, Attribute a){
		String ret = getReadableSize(size);
		if(ret.matches(".*[GT]+")){
			a.setTypeColor(UIConstants.COLOR_RED);
		}
		return ret;
	}
	static String getReadableCount(long size){
		double step = 1000.0;
		String [] vals = {"","k","m","g","t"};
		for(int i=vals.length-1; i>=0; i--){
			if(size / Math.pow(step, i) >= 1){
				if(vals[i].isEmpty()){
					return Long.toString(size);
				}else{
					return Math.round(size * 10.0 / Math.pow(step, i))/10.0 + vals[i];
				}
			}
		}
		return "0";
	}
	static String getReadableCountColorized(long size, Attribute a){
		String ret = getReadableCount(size);
		if(ret.matches(".*[mgt]+")){
			a.setTypeColor(UIConstants.COLOR_RED);
		}
		return ret;
	}

	static int getZoomed(int input){
		return (int)Math.round(input*Canvas.getZoom());
	}
	static int getZoomedFloored(int input){
		return (int)Math.floor(input*Canvas.getZoom());
	}
	static int getUnZoomed(int input){
		return (int)Math.round(input/Canvas.getZoom());
	}

	static Dimension getZoomed(Dimension input){
		return input == null ? null : new Dimension(getZoomed(input.width), getZoomed(input.height));
	}
	static Point getZoomed(Point input){
		return input == null ? null : new Point(getZoomed(input.x), getZoomed(input.y));
	}
	static Dimension getUnZoomed(Dimension input){
		return input == null ? null : new Dimension(getUnZoomed(input.width), getUnZoomed(input.height));
	}
	static Point getUnZoomed(Point input){
		return input == null ? null : new Point(getUnZoomed(input.x), getUnZoomed(input.y));
	}

	static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	static Point fitPointToLimits(Point newPos, Point min, Point max) {
		if (newPos.x < min.x) {
			newPos.x = min.x;
		}
		if (newPos.y < min.y) {
			newPos.y = min.y;
		}
		if (newPos.x > max.x) {
			newPos.x = max.x;
		}
		if (newPos.y > max.y) {
			newPos.y = max.y;
		}
		return newPos;
	}

	static Point fitPointToLimits(Point newPos, Point min, Dimension max) {
		return fitPointToLimits(newPos, min, new Point(max.width, max.height));
	}
}
