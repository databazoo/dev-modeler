
package plugins;

import java.awt.*;

import javax.swing.*;

import plugins.api.IDataWindow;

/**
 * Data window plugins must implement this interface
 * @author bobus
 */
public interface IDataWindowPlugin {

	public void init(IDataWindow instance);
	public IDataWindowPlugin clone();

	public String getPluginName();
	public String getPluginDescription();
	public ImageIcon getPluginIcon();

	public Component getWindowComponent();
	public Component getTabComponent();

	public void onQueryChange();
	public void onQueryRun();
	public void onQueryExplain();
	public void onQueryResult();
	public void onQueryFail();
	public void onWindowClose();

	public boolean hasError();
}
