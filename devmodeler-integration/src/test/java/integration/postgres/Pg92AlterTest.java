
package integration.postgres;

import org.junit.Ignore;

/**
 * PostgreSQL 9.2 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class Pg92AlterTest extends PgCrudBase {

	public Pg92AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "db_tool_test_9.2";
	}
}
