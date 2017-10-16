
package com.databazoo.components.containers;

import java.awt.*;

import javax.swing.*;


/**
 * Horizontal panel. Helps defining placements directly from constructor.
 *
 * @author bobus
 */
public class HorizontalContainer extends JPanel {

	/**
	 * Empty constructor
	 */
	public HorizontalContainer(){
		setLayout(new BorderLayout(0,0));
	}

	/**
	 * Constructor with components
	 *
	 * @param leftComp left component
	 * @param centerComp center component
	 * @param rightComp right component
	 */
	public HorizontalContainer (Component leftComp, Component centerComp, Component rightComp) {
		setLayout(new BorderLayout(0,0));
		if(leftComp != null){
			add(leftComp, BorderLayout.WEST);
		}
		if(centerComp != null){
			add(centerComp, BorderLayout.CENTER);
		}
		if(rightComp != null){
			add(rightComp, BorderLayout.EAST);
		}
	}

	public static class Builder {
		Dimension minSize;
		Dimension maxSize;
		Component leftComponent;
		Component centerComponent;
		Component rightComponent;

		public Builder min(Dimension size){
			minSize = size;
			return this;
		}

		public Builder max(Dimension size){
			maxSize = size;
			return this;
		}

		public Builder left(Component component){
			leftComponent = component;
			return this;
		}

		public Builder center(Component component){
			centerComponent = component;
			return this;
		}

		public Builder right(Component component){
			rightComponent = component;
			return this;
		}

		public HorizontalContainer build(){
			HorizontalContainer instance = new HorizontalContainer();
			if(leftComponent != null){
				instance.add(leftComponent, BorderLayout.WEST);
			}
			if(centerComponent != null){
				instance.add(centerComponent, BorderLayout.CENTER);
			}
			if(rightComponent != null){
				instance.add(rightComponent, BorderLayout.EAST);
			}
			instance.setMinimumSize(minSize);
			instance.setMaximumSize(maxSize);
			return instance;
		}
	}
}
