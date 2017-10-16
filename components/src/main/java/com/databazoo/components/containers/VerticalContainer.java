
package com.databazoo.components.containers;

import java.awt.*;

import javax.swing.*;


/**
 * Vertical panel. Helps defining placements directly from constructor.
 *
 * @author bobus
 */
public class VerticalContainer extends JPanel {

	/**
	 * Empty constructor
	 */
	public VerticalContainer(){
		setLayout(new BorderLayout(0,0));
	}

	/**
	 * Constructor with components
	 *
	 * @param topComponent upper component
	 * @param centerComponent center component
	 * @param bottomComponent lower component
	 */
	public VerticalContainer (Component topComponent, Component centerComponent, Component bottomComponent) {
		setLayout(new BorderLayout(0,0));
		if(topComponent != null){
			add(topComponent, BorderLayout.NORTH);
		}
		if(centerComponent != null){
			add(centerComponent, BorderLayout.CENTER);
		}
		if(bottomComponent != null){
			add(bottomComponent, BorderLayout.SOUTH);
		}
	}

	public static class Builder {
		Dimension minSize;
		Dimension maxSize;
		Component topComponent;
		Component centerComponent;
		Component bottomComponent;

		public Builder min(Dimension size){
			minSize = size;
			return this;
		}

		public Builder max(Dimension size){
			maxSize = size;
			return this;
		}

		public Builder top(Component component){
			topComponent = component;
			return this;
		}

		public Builder center(Component component){
			centerComponent = component;
			return this;
		}

		public Builder bottom(Component component){
			bottomComponent = component;
			return this;
		}

		public VerticalContainer build(){
			VerticalContainer instance = new VerticalContainer();
			if(topComponent != null){
				instance.add(topComponent, BorderLayout.NORTH);
			}
			if(centerComponent != null){
				instance.add(centerComponent, BorderLayout.CENTER);
			}
			if(bottomComponent != null){
				instance.add(bottomComponent, BorderLayout.SOUTH);
			}
			instance.setMinimumSize(minSize);
			instance.setMaximumSize(maxSize);
			return instance;
		}
	}
}
