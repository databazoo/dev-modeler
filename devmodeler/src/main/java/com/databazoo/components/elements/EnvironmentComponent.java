package com.databazoo.components.elements;

import java.util.HashMap;
import java.util.Map;

import com.databazoo.devmodeler.conn.IConnection;

/**
 * Operate environment-dependent components.
 * Example: same table might not be available on all servers - this is implemented with an environment availability.
 *
 * @author bobus
 */
public abstract class EnvironmentComponent extends ClickableComponent{

	private Map<String,Boolean> availability;

	/**
	 * Check availability in selected environment
	 *
	 * @param env Connection/Sever
	 * @return is available?
	 */
	public boolean isInEnvironment(IConnection env){
		if(availability == null){
			availability = new HashMap<>();
		}else{
			Boolean isAvailable = availability.get(env.getName());
			if(isAvailable != null){
				return isAvailable;
			}
		}
		return true;
	}

	/**
	 * Set availability in selected environment
	 *
	 * @param env Connection/Sever
	 * @param isAvailable is available?
	 */
	public void setInEnvironment(IConnection env, boolean isAvailable){

	}
}
