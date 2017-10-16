package com.databazoo.devmodeler.project;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.conn.IColoredConnection.ConnectionColor;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.databazoo.devmodeler.project.ProjectConstants.*;

/**
 * Project IO SAX loader
 */
class ProjectSAX extends DefaultHandler {

	protected Project project;
	protected DB db;
	protected Schema schema;
	protected Relation rel;
	protected Attribute attr;
	protected Index index;
	protected Function func;
	protected Package pack;
	protected Sequence seq;
	protected View view;

	private boolean mergeResults;
	private Map<String, Relation> rels;

	ProjectSAX(){}

	ProjectSAX(Project project, DB db, boolean mergeResults) {
		this.project = project;
		this.db = db;
		this.mergeResults = mergeResults;
	}

	@Override
	public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
		if(tagName.equalsIgnoreCase("project")) {
			if(project != null) {
				project.checkInheritances();
			}
			readProject(attributes);

		}else if(project != null) {

			if(tagName.equalsIgnoreCase("server")) {
				readServer(attributes);

			}else if(tagName.equalsIgnoreCase(DATABASE)) {
				readDatabase(attributes);

			}else if(tagName.equalsIgnoreCase("connection")) {
				readConnection(attributes);

			}else if (tagName.equalsIgnoreCase(SCHEMA)) {
				readSchema(attributes);

			} else if (tagName.equalsIgnoreCase(TABLE)) {
				readTable(attributes);

			} else if (tagName.equalsIgnoreCase(COLUMN)) {
				readColumn(attributes);

			} else if (tagName.equalsIgnoreCase(INDEX)) {
				readIndex(attributes);

			} else if (tagName.equalsIgnoreCase(VIEW)) {
				readView(attributes);

			} else if (tagName.equalsIgnoreCase(SEQUENCE)) {
				readSequence(attributes);

			} else if (tagName.equalsIgnoreCase(PACKAGE)) {
				readPackage(attributes);

			} else if (tagName.equalsIgnoreCase(FUNCTION)) {
				readFunction(attributes);

			} else if (tagName.equalsIgnoreCase(CONSTRAINT)) {
				readConstraint(attributes);

			} else if (tagName.equalsIgnoreCase(TRIGGER)) {
				readTrigger(attributes);

			} else if (tagName.equalsIgnoreCase(LOCATION)) {
				readLocation(attributes);
			}
		}
	}

	private void readProject (Attributes attributes) throws HeadlessException {
		String name = attributes.getValue(NAME);
		project = ProjectManager.getInstance().getProjectByName(name);
		if(project != null){
			Object[] options = {"Save as new project", "Merge with existing configuration"};
			int n = JOptionPane.showOptionDialog(
				GCFrame.getActiveWindow(),
				"Project " + name + " is being imported, but project with this name already exists.\n\n" +
					"Do you want to create a new project from imported data?",
				"Import",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
			);
			if(n == 0){
				// TODO: check new project can be used
				name = ProjectManager.getInstance().checkName(name);
				if(name != null){
					Dbg.info("Creating new project");
					project = ProjectManager.getInstance().createNew(name, XMLWriter.getInt(attributes.getValue(TYPE)));
				}else{
					project = null;
				}
			}else{
				Dbg.info("Using existing project");
			}
		}else{
			project = ProjectManager.getInstance().createNew(name, XMLWriter.getInt(attributes.getValue(TYPE)));
			project.setLoaded();
		}
	}

	private void readServer (Attributes attributes) {
		String name = XMLWriter.getString(attributes.getValue(NAME));
		IConnection conn = project.getConnectionByName(name);
		ConnectionColor color = ConnectionColor.fromString(XMLWriter.getString(attributes.getValue("color")));
		if(conn == null){
			project.addConnection(
				XMLWriter.getString(attributes.getValue(NAME)),
				XMLWriter.getString(attributes.getValue("host")),
				XMLWriter.getString(attributes.getValue("user")),
				XMLWriter.getString(attributes.getValue("pass")),
				XMLWriter.getInt(attributes.getValue(TYPE)),
				color
			);
		}else{
			conn.setHost(XMLWriter.getString(attributes.getValue("host")));
			conn.setUser(XMLWriter.getString(attributes.getValue("user")));
			//conn.setPass(XMLWriter.getString(attributes.getValue("pass")));
			if (color != ConnectionColor.DEFAULT) {
				conn.setColor(color);
			}
		}
	}

	private void readDatabase (Attributes attributes) {
		String name = XMLWriter.getString(attributes.getValue(NAME));
		db = project.getDatabaseByName(name);
		if(db == null){
			db = new DB(project, name);
			project.databases.add(db);
		}
		rels = null;
	}

	private void readConnection (Attributes attributes) {
		String name = XMLWriter.getString(attributes.getValue(NAME));
		IConnection conn = project.getDedicatedConnection(db.getName(), name);
		if(conn == null){
			project.addDedicatedConnection(db.getName(), name,
				XMLWriter.getString(attributes.getValue("host")),
				XMLWriter.getString(attributes.getValue("user")),
				XMLWriter.getString(attributes.getValue("pass")),
				XMLWriter.getInt(attributes.getValue(TYPE)),
				XMLWriter.getString(attributes.getValue("dbAlias")),
				XMLWriter.getString(attributes.getValue("defaultSchema"))
			);
		}else{
			conn.setHost(XMLWriter.getString(attributes.getValue("host")));
			conn.setUser(XMLWriter.getString(attributes.getValue("user")));
			//conn.setPass(XMLWriter.getString(attributes.getValue("pass")));
			conn.setDbAlias(XMLWriter.getString(attributes.getValue("dbAlias")));
			conn.setDefaultSchema(XMLWriter.getString(attributes.getValue("defaultSchema")));
		}
	}

	private void readSchema (Attributes attributes) {
		if (db.getConnection().isSupported(SupportedElement.SCHEMA) || db.getSchemas().isEmpty()) {

			// TRY EXISTING SCHEMA
			if(mergeResults) {
				schema = db.getSchemaByFullName(XMLWriter.getString(attributes.getValue(NAME)));
			}else{
				schema = null;
			}

			// ASSING NEW
			if(schema == null){
				schema = new Schema(db, XMLWriter.getString(attributes.getValue(NAME)), XMLWriter.getString(attributes.getValue(DESCRIPTION)));
				schema.assignToDB(db);
			}

			// SET PARAMS
			if(attributes.getValue(LOCATION) != null) {
				schema.setLocationWoChecks(XMLWriter.getPoint(attributes.getValue(LOCATION)));
			}
			if(attributes.getValue(SIZE) != null) {
				schema.setSizeWoChecks(XMLWriter.getDim(attributes.getValue(SIZE)));
			}

		} else {
			schema = db.getSchemas().get(0);
			schema.getFunctions().clear();
			schema.getRelations().clear();
			schema.removeAll();
			db.unload();
			schema.assignToDB(db);
		}
		Dbg.info("Schema " + db.getProject().projectName + "." + db.getName() + "." + schema.getFullName() + " loaded from XML");
	}

	private void readTable (Attributes attributes) {

		// TRY EXISTING TABLE
		if(mergeResults) {
			rel = db.getRelationByFullName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			rel = null;
		}

		// ASSIGN NEW
		if(rel == null) {
			rel = new Relation(schema, XMLWriter.getString(attributes.getValue(NAME)));
			rel.assignToSchema();
		}

		// SET PARAMS
		if(attributes.getValue(LOCATION) != null) {
			rel.setLocation(XMLWriter.getPoint(attributes.getValue(LOCATION)));
		}
		if(attributes.getValue(SIZE_TOTAL) != null) {
			rel.setSizeTotal(XMLWriter.getLong(attributes.getValue(SIZE_TOTAL)));
		}
		if(attributes.getValue(SIZE_INDEX) != null) {
			rel.setSizeIndexes(XMLWriter.getLong(attributes.getValue(SIZE_INDEX)));
		}
		if(attributes.getValue(ROWS) != null) {
			rel.setCountRows(XMLWriter.getInt(attributes.getValue(ROWS)));
		}
		if(attributes.getValue(STORAGE) != null) {
			rel.getBehavior().setStorage(XMLWriter.getString(attributes.getValue(STORAGE)));
		}
		rel.getBehavior().setCollation(attributes.getValue(COLLATION) == null ? null : XMLWriter.getString(attributes.getValue(COLLATION)));
		if(attributes.getValue(OIDS) != null) {
			rel.getBehavior().setHasOIDs(XMLWriter.getBool(attributes.getValue(OIDS)));
		}
		if(attributes.getValue(DESCRIPTION) != null) {
			rel.getBehavior().setDescr(XMLWriter.getString(attributes.getValue(DESCRIPTION)));
		}
		if(attributes.getValue(OPTIONS) != null) {
			rel.getBehavior().setOptions(XMLWriter.getStringArray(attributes.getValue(OPTIONS)));
		}
		if(attributes.getValue(PK_COLUMNS) != null) {
			rel.setPkCols(XMLWriter.getIntArray(attributes.getValue(PK_COLUMNS)));
		}
		if(attributes.getValue(INHERITS) != null) {
			rel.getBehavior().setInheritParentName(XMLWriter.getString(attributes.getValue(INHERITS)));
		}
	}

	private void readColumn (Attributes attributes) {

		// TRY EXISTING COLUMN
		if(mergeResults) {
			attr = rel.getAttributeByName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			attr = null;
		}

		// ASSIGN NEW
		if(attr == null) {
			attr = new Attribute(rel,
				XMLWriter.getString(attributes.getValue(NAME)),
				XMLWriter.getString(attributes.getValue(TYPE)),
				XMLWriter.getBool(attributes.getValue(IS_NULLABLE)),
				XMLWriter.getInt(attributes.getValue(ORDINAL)),
				XMLWriter.getString(attributes.getValue(DEFAULT)),
				XMLWriter.getString(attributes.getValue(STORAGE)),
				XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			attr.setRel(rel);
			attr.assignToRels();
		}

		// SET PARAMS
		attr.getBehavior().setCollation(attributes.getValue(COLLATION) == null ? null : XMLWriter.getString(attributes.getValue(COLLATION)));

		// TODO: constructor params?
	}

	private void readIndex (Attributes attributes) {

		// TRY EXISTING INDEX
		if(mergeResults) {
			index = rel.getIndexByName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			index = null;
		}

		// ASSIGN NEW
		if(index == null) {
			index = new Index(rel,
				XMLWriter.getString(attributes.getValue(NAME)),
				XMLWriter.getString(attributes.getValue(DEFINITION)),
				XMLWriter.getString(attributes.getValue(WHERE)),
				XMLWriter.getString(attributes.getValue(ACCESS_METHOD)),
				XMLWriter.getStringArray(attributes.getValue(OPTIONS)),
				XMLWriter.getBool(attributes.getValue(IS_UNIQUE)),
				XMLWriter.getBool(attributes.getValue(IS_PRIMARY)),
				XMLWriter.getBool(attributes.getValue(IS_CONSTRAINT)),
				XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			index.assignToRelation(rel);
		}

		// TODO: constructor params?
	}

	private void readView (Attributes attributes) {

		// TRY EXISTING VIEW
		if(mergeResults) {
			view = db.getViewByFullName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			view = null;
		}

		// ASSIGN NEW
		if(view == null) {
			view = new View(schema,
				XMLWriter.getString(attributes.getValue(NAME)),
				XMLWriter.getBool(attributes.getValue(MATERIALIZED)),
				XMLWriter.getString(attributes.getValue(SOURCE)),
				XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			view.assignToSchema();
		}

		// SET PARAMS
		if(attributes.getValue(LOCATION) != null) {
			view.setLocation(XMLWriter.getPoint(attributes.getValue(LOCATION)));
		}

		// TODO: constructor params?
	}

	private void readSequence (Attributes attributes) {

		// TRY EXISTING SEQUENCE
		if(mergeResults) {
			seq = db.getSequenceByFullName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			seq = null;
		}

		// ASSIGN NEW
		if(seq == null) {
			String[] deps = new String[0];
			String depsString = XMLWriter.getString(attributes.getValue(DEPENDANCES));
			if (!depsString.isEmpty()) {
				deps = depsString.split(",");
			}
			seq = new Sequence(schema,
				XMLWriter.getString(attributes.getValue(NAME)),
				deps,
				XMLWriter.getString(attributes.getValue(INCREMENT)),
				XMLWriter.getString(attributes.getValue(MINIMUM)),
				XMLWriter.getString(attributes.getValue(MAXIMUM)),
				XMLWriter.getString(attributes.getValue(CURRENT_VALUE)),
				XMLWriter.getBool(attributes.getValue(IS_CYCLED)),
				XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			seq.assignToSchemaAndAttributes();
		}

		// SET PARAMS
		if(attributes.getValue(LOCATION) != null) {
			seq.setLocation(XMLWriter.getPoint(attributes.getValue(LOCATION)));
		}

		// TODO: constructor params?
	}

	private void readFunction (Attributes attributes) {

		// TRY EXISTING FUNCTION
		if(mergeResults) {
			func = db.getFunctionByFullName(XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			func = null;
		}

		// ASSIGN NEW
		if(func == null) {
			func = new Function(schema,
					XMLWriter.getString(attributes.getValue(NAME)),
					XMLWriter.getString(attributes.getValue(TYPE)),
					XMLWriter.getString(attributes.getValue(ARGUMENTS)),
					XMLWriter.getString(attributes.getValue(SOURCE)),
					XMLWriter.getString(attributes.getValue(LANGUAGE)),
					XMLWriter.getString(attributes.getValue(VOLATILITY)),
					XMLWriter.getBool(attributes.getValue(IS_SECURITY_DEFINER)),
					XMLWriter.getInt(attributes.getValue(COST)),
					XMLWriter.getInt(attributes.getValue(ROWS)),
					XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			func.assignToSchema();
		}

		// SET PARAMS
		if(attributes.getValue(LOCATION) != null) {
			func.setLocation(XMLWriter.getPoint(attributes.getValue(LOCATION)));
		}

		// TODO: constructor params?
	}

	private void readPackage (Attributes attributes) {

		// TRY EXISTING PACKAGE
		if(mergeResults) {
			pack = db.getPackageByFullName(schema.getName() + "." + XMLWriter.getString(attributes.getValue(NAME)));
		}else{
			pack = null;
		}

		// ASSIGN NEW
		if(pack == null) {
			pack = new Package(schema,
					XMLWriter.getString(attributes.getValue(NAME)),
					XMLWriter.getString(attributes.getValue(SOURCE)),
					XMLWriter.getString(attributes.getValue(BODY)),
					XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			pack.assignToSchema();
		}

		// SET PARAMS
		if(attributes.getValue(LOCATION) != null) {
			pack.setLocation(XMLWriter.getPoint(attributes.getValue(LOCATION)));
		}
	}

	private void readConstraint (Attributes attributes) {

		if(rels == null){
			rels = db.getRelationMap();
		}

		String[] parts = XMLWriter.getString(attributes.getValue(NAME)).split("\\.");
		String conName = parts.length > 1 ? parts[1] : parts[0];
		Relation rel1 = db.getRelationByFullName(XMLWriter.getString(attributes.getValue(REL1)));
		if (rel1 != null && rel1.getConstraintByName(conName) == null) {
			Constraint con;
			String def = XMLWriter.getString(attributes.getValue(DEFINITION));
			if (def.startsWith("CHECK")) {
				CheckConstraint c = new CheckConstraint(db,
					XMLWriter.getString(attributes.getValue(NAME)),
					def,
					XMLWriter.getString(attributes.getValue(DESCRIPTION)));
				c.setRelByName(db, XMLWriter.getString(attributes.getValue(REL1)), false);
			} else {
				con = new Constraint(db,
					XMLWriter.getString(attributes.getValue(NAME)),
					XMLWriter.getString(attributes.getValue(ON_UPDATE)),
					XMLWriter.getString(attributes.getValue(ON_DELETE)),
					XMLWriter.getString(attributes.getValue(DESCRIPTION)));
				con.setRelsAttrsByName(
					rels.get(XMLWriter.getString(attributes.getValue(REL1))),
					rels.get(XMLWriter.getString(attributes.getValue(REL2))),
					XMLWriter.getString(attributes.getValue(ATTR1)),
					XMLWriter.getString(attributes.getValue(ATTR2)),
					false);
			}
		}
	}

	private void readTrigger (Attributes attributes) {
		String[] parts = XMLWriter.getString(attributes.getValue(NAME)).split("\\.");
		String trigName = parts.length > 1 ? parts[1] : parts[0];
		final String relFullName = XMLWriter.getString(attributes.getValue(attributes.getValue("rel") != null ? "rel" : REL1));
		Relation rel1 = db.getRelationByFullName(relFullName);
		if (rel1 != null && rel1.getTriggerByName(trigName) == null) {
			Trigger trigger = new Trigger(db,
					XMLWriter.getString(attributes.getValue(NAME)),
					XMLWriter.getString(attributes.getValue(DEFINITION)),
					XMLWriter.getString(attributes.getValue(ATTRIBUTES)),
					XMLWriter.getString(attributes.getValue(WHEN)),
					!XMLWriter.getBool(attributes.getValue(IS_DISABLED)),
					XMLWriter.getString(attributes.getValue(DESCRIPTION)));
			trigger.setRelFuncByNameDef(db,
					relFullName,
					XMLWriter.getString(attributes.getValue(DEFINITION)),
					false);
			trigger.getBehavior().setRowType(!XMLWriter.getBool(attributes.getValue(IS_STATEMENT)));
			trigger.getBehavior().setTiming(XMLWriter.getChar(attributes.getValue(TIMING)));
			trigger.getBehavior().setEvents(XMLWriter.getCharArray(attributes.getValue(EVENTS)));
		}
	}

	private void readLocation (Attributes attributes) {
		String name = XMLWriter.getString(attributes.getValue(NAME));
		Point value = XMLWriter.getPoint(attributes.getValue(VALUE));
		if (name != null && value != null) {
			db.saveKnownLocation(name, value);
		}
	}
}
