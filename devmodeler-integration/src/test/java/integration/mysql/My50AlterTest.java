
package integration.mysql;

import org.junit.Ignore;
import org.junit.Test;

/**
 * MySQL 5.0 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class My50AlterTest extends MyCrudBase {

	public My50AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "mysql_test_5.0";
	}

	@Test
	@Override
	public void testFK() throws Exception {
		// Constraints suck really badly in 5.0, so not testing
	}
}
