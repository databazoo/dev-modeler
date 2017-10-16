
package integration.postgres;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.project.Project;

/**
 * PostgreSQL 9.0 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class Pg90AlterTest extends PgCrudBase {

	public Pg90AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "db_tool_test_9.0";
	}

	@Test
	@Override
	public void testSequence() throws InterruptedException, DBCommException {
		System.out.println("CRUD: Sequence test is not supported on 9.0");
		assertEquals(0, Project.getCurrent().revisions.size());
	}
}
