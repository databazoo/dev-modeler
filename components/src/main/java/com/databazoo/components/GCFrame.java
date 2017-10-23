
package com.databazoo.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.databazoo.tools.Dbg;
import com.databazoo.tools.GC;

/**
 * JFrame with garbage collector invocation on window close. Also closes child windows, notifies observers and tracks active window.
 *
 * @author bobus
 */
public class GCFrame extends JFrame {
	public static boolean SHOW_GUI = true;
	private static final List<GCFrame> topWindows = new ArrayList<>();

	public static final int DEFAULT_WIDTH = 1080;
	public static final int DEFAULT_HEIGHT = 660;

	/**
	 * Get last active window
	 *
	 * @return GCFrameWithObservers
	 */
	public static GCFrame getActiveWindow(){
		if(!SHOW_GUI){
			return topWindows.get(topWindows.size()-1);
		}
		for(GCFrame frame: topWindows){
			if(frame.isActive()){
				return frame;
			}
		}
		return null;
	}

	/**
	 * Add child window for the top-most parent of the given component.
	 * Useful for subordinating dialog windows to some window by it's content pane (or any lower component).
	 *
	 * @param comp component
	 * @param child child window
	 */
	public static void addChildWindowByComponent(Container comp, Frame child){
		Container c = comp;
		while(c.getParent() != null){
			c = c.getParent();
		}
		if (c instanceof GCFrame){
			((GCFrame)c).addChildWindow(child);
		}
	}

	/**
	 * Terminate all windows. Can be called on program exit to remove the UI if the program will be still processing
	 * something for a few moments.
	 */
	public static void disposeAll() {
		new ArrayList<>(topWindows)
				.forEach(GCFrame::dispose);
	}

	private final List<Frame> childWindows = new ArrayList<>();

	/**
	 * Constructor
	 *
	 * @param fullName title text
	 */
	public GCFrame(String fullName){
		super(fullName);
		Dbg.toFile("Window "+getTitle()+" is created");
		addToList();
	}

	public void setDefaultSize(){
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public void setHalfScreenSize(){
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(scrSize.width, scrSize.height / 2);
	}

	/**
	 * Close subordinated windows, notify observers and invoke garbage collector
	 */
	@Override
	public void dispose(){
		super.dispose();
		for(Frame win: childWindows){
			win.dispose();
		}
		Dbg.toFile("Window "+getTitle()+" is being closed. Window had "+childWindows.size()+" child windows.");
		removeFromList();
		GC.invoke();
	}

	/**
	 * Add child window.
	 *
	 * @param child child window
	 */
	private void addChildWindow(Frame child){
		childWindows.add(child);
	}

	/**
	 * Add to top window list
	 */
	private void addToList(){
		if(!topWindows.contains(this)) {
			topWindows.add(this);
		}
	}

	/**
	 * Remove from top window list
	 */
	private void removeFromList(){
		topWindows.remove(this);
	}

	@Override
	public void setVisible(boolean visible){
		if (GCFrame.SHOW_GUI) {
			super.setVisible(visible);
		}
	}
}
