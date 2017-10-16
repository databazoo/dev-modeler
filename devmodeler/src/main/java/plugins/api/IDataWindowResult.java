
package plugins.api;

import javax.swing.table.TableModel;

/**
 *
 * @author bobus
 */
public interface IDataWindowResult extends TableModel {
	public boolean isEmpty();
	public double getTime();
	public int getAffectedRows();
}
