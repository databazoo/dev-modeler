
package integration.postgres;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.project.Project;

/**
 * PostgreSQL 8.4 alter all elements test.
 *
 * @author bobus
 */
@Ignore
public class Pg84AlterTest extends PgCrudBase {

	public Pg84AlterTest() throws Exception {
		super();
	}

	@Override
	protected String getProjectName(){
		return "db_tool_test_8.4";
	}

	@Test
	@Override
	public void testSequence() throws InterruptedException, DBCommException {
		System.out.println("CRUD: Sequence test is not supported on 8.4");
		assertEquals(0, Project.getCurrent().revisions.size());
	}
}
