
package integration.postgres;

import org.junit.Ignore;

/**
 * PostgreSQL 9.1 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class Pg91AlterTest extends PgCrudBase {

	public Pg91AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "db_tool_test_9.1";
	}
}
