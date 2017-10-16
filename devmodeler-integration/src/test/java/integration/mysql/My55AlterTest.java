
package integration.mysql;

import org.junit.Ignore;
import org.junit.Test;

/**
 * MySQL 5.5 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class My55AlterTest extends MyCrudBase {

	public My55AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "mysql_test_5.5";
	}

	@Override
	@Test
	public void testFK () throws Exception {
		super.testFK();
	}

}
