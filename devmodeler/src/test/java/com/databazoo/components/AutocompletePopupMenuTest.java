
package com.databazoo.components;

import org.junit.*;

import javax.swing.*;

import static org.junit.Assert.*;

/**
 *
 * @author bobus
 */
public class AutocompletePopupMenuTest {

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	public AutocompletePopupMenuTest() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
		AutocompletePopupMenu.get().dispose();
	}

	@Test
	public void testGet() {
		assertNotNull(AutocompletePopupMenu.get());
	}

	@Test
	public void testIsShown() throws Exception {
		assertEquals(false, AutocompletePopupMenu.isShown());
		AutocompletePopupMenu.get().draw(0, 0);
		Thread.sleep(200);
		assertEquals(true, AutocompletePopupMenu.isShown());
		AutocompletePopupMenu.get().dispose();
	}

	@Test
	public void testIsShownAndSelected() throws Exception {
		assertEquals(false, AutocompletePopupMenu.isShownAndSelected());
		AutocompletePopupMenu.get().clear();
		AutocompletePopupMenu.get().add(new JMenuItem());
		AutocompletePopupMenu.get().draw(0, 0);
		Thread.sleep(100);
		assertEquals(false, AutocompletePopupMenu.isShownAndSelected());
		AutocompletePopupMenu.get().processKeyDown();
		assertEquals(true, AutocompletePopupMenu.isShownAndSelected());
		AutocompletePopupMenu.get().dispose();
	}

	@Test
	public void testDispose() {
		AutocompletePopupMenu m1 = AutocompletePopupMenu.get();
		m1.dispose();
		assertNotSame(m1, AutocompletePopupMenu.get());
	}

	@Test
	public void testDraw() throws Exception {
		int x = 102;
		int y = 105;
		AutocompletePopupMenu.get().draw(x, y);
		Thread.sleep(100);
		assertEquals(x, AutocompletePopupMenu.get().getLocationOnScreen().x);
		assertEquals(y, AutocompletePopupMenu.get().getLocationOnScreen().y);
		AutocompletePopupMenu.get().dispose();

		AutocompletePopupMenu.get().drawAboveCenter(x, y);
		assertTrue(AutocompletePopupMenu.get().getLocationOnScreen().x < x);
		assertTrue(AutocompletePopupMenu.get().getLocationOnScreen().y < y);
		AutocompletePopupMenu.get().dispose();
	}

	@Test
	public void testProcessKeyUp() throws Exception {
		JMenuItem m1 = new JMenuItem();
		JMenuItem m2 = new JMenuItem();

		AutocompletePopupMenu.get().clear();
		AutocompletePopupMenu.get().add(m1);
		AutocompletePopupMenu.get().add(m2);
		AutocompletePopupMenu.get().draw(0, 0);
		Thread.sleep(100);
		AutocompletePopupMenu.get().processKeyUp();
		assertSame(m2, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().processKeyUp();
		assertSame(m1, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().processKeyUp();
		assertSame(m2, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().dispose();
	}

	@Test
	public void testProcessKeyDown() throws Exception {
		JMenuItem m1 = new JMenuItem();
		JMenuItem m2 = new JMenuItem();

		AutocompletePopupMenu.get().clear();
		AutocompletePopupMenu.get().add(m1);
		AutocompletePopupMenu.get().add(m2);
		AutocompletePopupMenu.get().draw(0, 0);
		Thread.sleep(100);
		AutocompletePopupMenu.get().processKeyDown();
		assertSame(m1, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().processKeyDown();
		assertSame(m2, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().processKeyDown();
		assertSame(m1, AutocompletePopupMenu.get().selectedItem);
		AutocompletePopupMenu.get().dispose();
	}
}
