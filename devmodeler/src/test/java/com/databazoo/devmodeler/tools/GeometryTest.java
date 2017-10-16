package com.databazoo.devmodeler.tools;

import com.databazoo.devmodeler.TestProjectSetup;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class GeometryTest extends TestProjectSetup {

    @Test
    public void shapeIntersectsDiagonal() throws Exception {
        Dimension componentSize = new Dimension(100, 50);
        assertTrue(Geometry.shapeIntersectsDiagonal(false, componentSize, new Rectangle(10, 5, 10, 10)));
        assertTrue(Geometry.shapeIntersectsDiagonal(false, componentSize, new Rectangle(45, 20, 10, 10)));
        assertTrue(Geometry.shapeIntersectsDiagonal(false, componentSize, new Rectangle(80, 35, 10, 10)));
        assertFalse(Geometry.shapeIntersectsDiagonal(false, componentSize, new Rectangle(80, 5, 10, 10)));
        assertFalse(Geometry.shapeIntersectsDiagonal(false, componentSize, new Rectangle(10, 35, 10, 10)));

        assertFalse(Geometry.shapeIntersectsDiagonal(true, componentSize, new Rectangle(10, 5, 10, 10)));
        assertTrue(Geometry.shapeIntersectsDiagonal(true, componentSize, new Rectangle(45, 20, 10, 10)));
        assertFalse(Geometry.shapeIntersectsDiagonal(true, componentSize, new Rectangle(80, 35, 10, 10)));
        assertTrue(Geometry.shapeIntersectsDiagonal(true, componentSize, new Rectangle(80, 5, 10, 10)));
        assertTrue(Geometry.shapeIntersectsDiagonal(true, componentSize, new Rectangle(10, 35, 10, 10)));
    }

    @Test
    public void pointInSquare() throws Exception {
        assertTrue(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(15, 15)));
        assertTrue(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(20, 20)));
        assertTrue(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(10, 10)));
        assertTrue(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(10, 20)));
        assertTrue(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(20, 10)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(5, 5)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(25, 25)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(15, 25)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(15, 5)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(25, 15)));
        assertFalse(Geometry.isPointInRectangle(new Point(10, 10), new Dimension(10, 10), new Point(5, 15)));
    }

    @Test
    public void pointEscapeSquare() throws Exception {
        assertEquals(new Point(25, 18), Geometry.pointEscapeRectangle(new Point(10, 10), new Dimension(10, 10), new Point(25, 18)));
        assertEquals(new Point(60, 30), Geometry.pointEscapeRectangle(new Point(10, 10), new Dimension(10, 10), new Point(20, 18)));
        assertEquals(new Point(30, 60), Geometry.pointEscapeRectangle(new Point(10, 10), new Dimension(10, 10), new Point(18, 20)));

    }

    @Test
    public void getSnappedPosition() throws Exception {
        assertEquals(new Point(30, 30), Geometry.getSnappedPosition(20, 20));
        assertEquals(new Point(30, 30), Geometry.getSnappedPosition(40, 20));
        assertEquals(new Point(30, 30), Geometry.getSnappedPosition(40, 40));
        assertEquals(new Point(60, 30), Geometry.getSnappedPosition(50, 40));
        assertEquals(new Point(60, 60), Geometry.getSnappedPosition(50, 50));
    }

    @Test
    public void clickedOnLine() throws Exception {
        assertFalse(Geometry.clickedOnLine(new Point(10, 10), new Point(100, 100), new Point(200, 200)));

        assertFalse(Geometry.clickedOnLine(new Point(150, 135), new Point(100, 100), new Point(200, 200)));
        assertTrue(Geometry.clickedOnLine(new Point(150, 150), new Point(100, 100), new Point(200, 200)));

        assertFalse(Geometry.clickedOnLine(new Point(150, 135), new Point(200, 200), new Point(100, 100)));
        assertTrue(Geometry.clickedOnLine(new Point(150, 150), new Point(200, 200), new Point(100, 100)));
    }

    @Test
    public void getReadableSize() throws Exception {
        assertEquals("10B", Geometry.getReadableSize(10));
        assertEquals("1000B", Geometry.getReadableSize(1000));
        assertEquals("1.2K", Geometry.getReadableSize(1229));
        assertEquals("977K", Geometry.getReadableSize(1000000));
        assertEquals("1.14M", Geometry.getReadableSize(1200000));
        assertEquals("1.12G", Geometry.getReadableSize(1200000000));
    }

    @Test
    public void fitPointToLimits() throws Exception {
        assertEquals(new Point(0, 0), Geometry.fitPointToLimits(new Point(-2, -5), new Point(0, 0), new Point(10, 10)));
        assertEquals(new Point(0, 5), Geometry.fitPointToLimits(new Point(-2, 5), new Point(0, 0), new Point(10, 10)));
        assertEquals(new Point(2, 5), Geometry.fitPointToLimits(new Point(2, 5), new Point(0, 0), new Point(10, 10)));
        assertEquals(new Point(2, 5), Geometry.fitPointToLimits(new Point(2, 5), new Point(0, 0), new Dimension(10, 10)));
        assertEquals(new Point(10, 5), Geometry.fitPointToLimits(new Point(20, 5), new Point(0, 0), new Point(10, 10)));
        assertEquals(new Point(10, 10), Geometry.fitPointToLimits(new Point(20, 50), new Point(0, 0), new Point(10, 10)));
    }

}