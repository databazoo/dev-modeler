
package integration.mysql;

import org.junit.Ignore;
import org.junit.Test;

/**
 * MySQL 5.6 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class My56AlterTest extends MyCrudBase {

	public My56AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "mysql_test_5.6";
	}

	@Override
	@Test
	public void testFK () throws Exception {
		super.testFK();
	}

}
