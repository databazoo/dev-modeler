
package integration.postgres;

import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class Pg93AlterTest extends PgCrudBase {

	public Pg93AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "db_tool_test_9.3";
	}

	@Test
	@Override
	public void testView () throws Exception {
		super.testView();
	}


}
