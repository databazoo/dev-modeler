
package com.databazoo.devmodeler.project;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.DraggableComponentReference;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static com.databazoo.devmodeler.project.ProjectConstants.ACCESS_METHOD;
import static com.databazoo.devmodeler.project.ProjectConstants.ARGUMENTS;
import static com.databazoo.devmodeler.project.ProjectConstants.ATTR1;
import static com.databazoo.devmodeler.project.ProjectConstants.ATTR2;
import static com.databazoo.devmodeler.project.ProjectConstants.ATTRIBUTES;
import static com.databazoo.devmodeler.project.ProjectConstants.BODY;
import static com.databazoo.devmodeler.project.ProjectConstants.COLLATION;
import static com.databazoo.devmodeler.project.ProjectConstants.COLUMN;
import static com.databazoo.devmodeler.project.ProjectConstants.CONSTRAINT;
import static com.databazoo.devmodeler.project.ProjectConstants.COST;
import static com.databazoo.devmodeler.project.ProjectConstants.CURRENT_VALUE;
import static com.databazoo.devmodeler.project.ProjectConstants.DATABASE;
import static com.databazoo.devmodeler.project.ProjectConstants.DEFAULT;
import static com.databazoo.devmodeler.project.ProjectConstants.DEFINITION;
import static com.databazoo.devmodeler.project.ProjectConstants.DEPENDANCES;
import static com.databazoo.devmodeler.project.ProjectConstants.DESCRIPTION;
import static com.databazoo.devmodeler.project.ProjectConstants.EVENTS;
import static com.databazoo.devmodeler.project.ProjectConstants.FUNCTION;
import static com.databazoo.devmodeler.project.ProjectConstants.INCREMENT;
import static com.databazoo.devmodeler.project.ProjectConstants.INDEX;
import static com.databazoo.devmodeler.project.ProjectConstants.INHERITS;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_CONSTRAINT;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_CYCLED;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_DISABLED;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_NULLABLE;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_PRIMARY;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_SECURITY_DEFINER;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_STATEMENT;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_UNIQUE;
import static com.databazoo.devmodeler.project.ProjectConstants.LANGUAGE;
import static com.databazoo.devmodeler.project.ProjectConstants.LOCATION;
import static com.databazoo.devmodeler.project.ProjectConstants.MATERIALIZED;
import static com.databazoo.devmodeler.project.ProjectConstants.MAXIMUM;
import static com.databazoo.devmodeler.project.ProjectConstants.MINIMUM;
import static com.databazoo.devmodeler.project.ProjectConstants.NAME;
import static com.databazoo.devmodeler.project.ProjectConstants.OIDS;
import static com.databazoo.devmodeler.project.ProjectConstants.ON_DELETE;
import static com.databazoo.devmodeler.project.ProjectConstants.ON_UPDATE;
import static com.databazoo.devmodeler.project.ProjectConstants.OPTIONS;
import static com.databazoo.devmodeler.project.ProjectConstants.ORDINAL;
import static com.databazoo.devmodeler.project.ProjectConstants.PACKAGE;
import static com.databazoo.devmodeler.project.ProjectConstants.PK_COLUMNS;
import static com.databazoo.devmodeler.project.ProjectConstants.REL1;
import static com.databazoo.devmodeler.project.ProjectConstants.REL2;
import static com.databazoo.devmodeler.project.ProjectConstants.ROWS;
import static com.databazoo.devmodeler.project.ProjectConstants.SCHEMA;
import static com.databazoo.devmodeler.project.ProjectConstants.SEQUENCE;
import static com.databazoo.devmodeler.project.ProjectConstants.SIZE;
import static com.databazoo.devmodeler.project.ProjectConstants.SIZE_INDEX;
import static com.databazoo.devmodeler.project.ProjectConstants.SIZE_TOTAL;
import static com.databazoo.devmodeler.project.ProjectConstants.SOURCE;
import static com.databazoo.devmodeler.project.ProjectConstants.STORAGE;
import static com.databazoo.devmodeler.project.ProjectConstants.TABLE;
import static com.databazoo.devmodeler.project.ProjectConstants.TIMING;
import static com.databazoo.devmodeler.project.ProjectConstants.TRIGGER;
import static com.databazoo.devmodeler.project.ProjectConstants.TYPE;
import static com.databazoo.devmodeler.project.ProjectConstants.VALUE;
import static com.databazoo.devmodeler.project.ProjectConstants.VIEW;
import static com.databazoo.devmodeler.project.ProjectConstants.VOLATILITY;
import static com.databazoo.devmodeler.project.ProjectConstants.WHEN;
import static com.databazoo.devmodeler.project.ProjectConstants.WHERE;

/**
 * Disk input and output operations for projects.
 *
 * @author bobus
 */
abstract class ProjectIO {

	private static final String EXT_XML = ".xml";
	private static final String SKIPPED_ON_PROJECT_WRITE = " skipped on project write";

	public List<Workspace> workspaces = new CopyOnWriteArrayList<>();
	public List<DB> databases = new CopyOnWriteArrayList<>();
	public List<IConnection> connections = new CopyOnWriteArrayList<>();
	public final List<Revision> revisions = new ArrayList<>();
	public final List<RecentQuery> recentQueries = new ArrayList<>();
	public final List<RecentQuery> favorites = new ArrayList<>();

	String projectName;
	String revPath;
	String projectPath;
	String flywayPath;
	final Map<String,IConnection> dedicatedConnections = new HashMap<>();

	private boolean duplicityReloaded = false;
	private boolean writeWorkspaces = true;

	public String getProjectName() {
		return projectName;
	}

	public synchronized Map<String, IConnection> getDedicatedConnections() {
		return dedicatedConnections;
	}

	public String getRevPath() {
		return revPath != null ? revPath : "";
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public synchronized void setDedicatedConnections(Map<String, IConnection> connections) {
		dedicatedConnections.clear();
		dedicatedConnections.putAll(connections);
	}

	public void setRevPath(String revPath) {
		this.revPath = revPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	public DB getDatabaseByName(String dbName) {
		if(dbName != null){
			for(DB db: databases){
				if(dbName.equals(db.getName())){
					return db;
				}
			}
		}else{
			return databases.get(0);
		}
		return null;
	}

	public final String getProjectPath(){
		return projectPath != null ? projectPath : ProjectManager.getSettingsDirectory(projectName).toString();
	}

	final String getRevisionPath(){
		return revPath != null ? revPath : new File(ProjectManager.getSettingsDirectory(projectName), "revisions").toString();
	}

	private void reload(){
		Project p = ProjectManager.getInstance().getProjectByName(projectName);
		if(p != null) {
			p.unload();
			ProjectManager.getInstance().openProject(projectName);
		}
	}

	void saveToXML(){
		long startTime = System.currentTimeMillis();

		File projectDir = new File(getProjectPath());
		File revDir = new File(getRevisionPath());

		if(!projectDir.exists()){
			projectDir.mkdir();
		}
		if(!revDir.exists()){
			revDir.mkdir();
		}
		try {
			boolean duplicitiesOccured = false;

			for(int i=0; i<databases.size(); i++){
				DB db = databases.get(i);

				Document doc = XMLWriter.getNewDocument();
				Element elemDB = doc.createElement(DATABASE);
				doc.appendChild(elemDB);

				if(appendDbStructure(doc, elemDB, db)){
					duplicitiesOccured = true;
				}

				XMLWriter.out(doc, new File(getProjectPath(), DATABASE +(i+1)+ EXT_XML));
			}

			Document doc = XMLWriter.getNewDocument();
			Element root = doc.createElement("queries");
			doc.appendChild(root);
			for(RecentQuery r: recentQueries){
				Element elem = doc.createElement("query");
				root.appendChild(elem);
				XMLWriter.setAttribute(elem, TABLE, r.tableName);
				XMLWriter.setAttribute(elem, "result", r.result);
				XMLWriter.setAttribute(elem, ROWS, r.resultRows);
				elem.appendChild(doc.createTextNode(r.queryText.length() > Config.FORMATTER_OVERFLOW_LIMIT ? r.queryText.substring(0, Config.FORMATTER_OVERFLOW_LIMIT-4)+"..." : r.queryText));
			}
			for(RecentQuery r: favorites){
				Element elem = doc.createElement("favorite");
				root.appendChild(elem);
				XMLWriter.setAttribute(elem, TABLE, r.tableName);
				XMLWriter.setAttribute(elem, "result", r.result);
				XMLWriter.setAttribute(elem, ROWS, r.resultRows);
				elem.appendChild(doc.createTextNode(r.queryText));
			}
			XMLWriter.out(doc, new File(getProjectPath(), "queries.xml"));

			if(writeWorkspaces) {
				doc = XMLWriter.getNewDocument();
				root = doc.createElement("workspaces");
				doc.appendChild(root);
				for (Workspace ws : workspaces) {
					if(ws.getRelations().isEmpty() && ws.getFunctions().isEmpty() && ws.getViews().isEmpty() && ws.getSequences().isEmpty()){
						Dbg.info("Workspace "+ws+" is empty, not saving");
						continue;
					}
					Element elem = doc.createElement("workspace");
					root.appendChild(elem);

					XMLWriter.setAttribute(elem, NAME, ws.getName());
					XMLWriter.setAttribute(elem, DATABASE, ws.getDatabase().getName());

					for (DraggableComponentReference comp : ws.getRelations()) {
						Element elemRel = doc.createElement(TABLE);
						elem.appendChild(elemRel);
						XMLWriter.setAttribute(elemRel, NAME, comp.getElement().getFullName());
					}
					for (DraggableComponentReference comp : ws.getFunctions()) {
						Element elemFunc = doc.createElement(FUNCTION);
						elem.appendChild(elemFunc);
						XMLWriter.setAttribute(elemFunc, NAME, comp.getElement().getFullName());
					}
					for (DraggableComponentReference comp : ws.getViews()) {
						Element elemFunc = doc.createElement(VIEW);
						elem.appendChild(elemFunc);
						XMLWriter.setAttribute(elemFunc, NAME, comp.getElement().getFullName());
					}
					for (DraggableComponentReference comp : ws.getSequences()) {
						Element elemFunc = doc.createElement(SEQUENCE);
						elem.appendChild(elemFunc);
						XMLWriter.setAttribute(elemFunc, NAME, comp.getElement().getFullName());
					}
				}
				XMLWriter.out(doc, new File(getProjectPath(), "workspaces.xml"));
				Dbg.info("Project written to XML in " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
			}else{
				Dbg.info("Project written to XML in " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s, but skipped workspaces");
			}

			if(duplicitiesOccured && !duplicityReloaded){
				duplicityReloaded = true;
				Schedule.inWorker(Schedule.TYPE_DELAY, this::reload);
			}

		} catch (Exception e){
			Dbg.fixme("Project could not be saved to XML", e);
		}
	}

	boolean appendDbStructure(Document doc, Element elemDB, DB db) {
		boolean duplicitiesOccured = false;
		XMLWriter.setAttribute(elemDB, NAME, db.getFullName());

		Element elemLocations = doc.createElement("locations");
		elemDB.appendChild(elemLocations);
		Map<String,Point> locs = db.getLocations();
		if(locs != null){
			for(Map.Entry<String, Point> data: locs.entrySet()){
				Element elemLoc = doc.createElement(LOCATION);
				elemLocations.appendChild(elemLoc);

				XMLWriter.setAttribute(elemLoc, NAME, data.getKey());
				XMLWriter.setAttribute(elemLoc, VALUE, data.getValue());
			}
		}

		Element elemSchemata = doc.createElement("schemata");
		elemDB.appendChild(elemSchemata);

		String lastSchemaName = "";
		Collections.sort(db.getSchemas());
		for(Schema schema: db.getSchemas()){
			if(lastSchemaName.equals(schema.getFullName())) {
				Dbg.fixme("Duplicated schema " + lastSchemaName + SKIPPED_ON_PROJECT_WRITE);
				duplicitiesOccured = true;
				continue;
			} else {
				lastSchemaName = schema.getFullName();
			}

			Element elemSchema = doc.createElement(SCHEMA);
			elemSchemata.appendChild(elemSchema);

			XMLWriter.setAttribute(elemSchema, NAME, schema.getFullName());
			XMLWriter.setAttribute(elemSchema, LOCATION, Geometry.getUnZoomed(schema.getLocation()));
			XMLWriter.setAttribute(elemSchema, SIZE, Geometry.getUnZoomed(schema.getSize()));
			XMLWriter.setAttribute(elemSchema, DESCRIPTION, schema.getDescr());

			String lastRelName = "";
			Collections.sort(schema.getRelations());
			for(Relation rel: schema.getRelations()){
				if(lastRelName.equals(rel.getFullName())) {
					Dbg.fixme("Duplicated table " + lastRelName + SKIPPED_ON_PROJECT_WRITE);
					duplicitiesOccured = true;
					continue;
				} else {
					lastRelName = rel.getFullName();
				}

				Element elemRel = doc.createElement(TABLE);
				elemSchema.appendChild(elemRel);

				XMLWriter.setAttribute(elemRel, NAME, rel.getFullName());
				XMLWriter.setAttribute(elemRel, LOCATION, Geometry.getUnZoomed(rel.getLocation()));
				XMLWriter.setAttribute(elemRel, SIZE_TOTAL, rel.getSizeTotal());
				XMLWriter.setAttribute(elemRel, SIZE_INDEX, rel.getSizeIndexes());
				XMLWriter.setAttribute(elemRel, ROWS, rel.getCountRows());
				XMLWriter.setAttribute(elemRel, STORAGE, rel.getBehavior().getStorage());
				XMLWriter.setAttribute(elemRel, COLLATION, rel.getBehavior().getCollation());
				XMLWriter.setAttribute(elemRel, OIDS, rel.getBehavior().hasOIDs());
				XMLWriter.setAttribute(elemRel, DESCRIPTION, rel.getDescr());
				XMLWriter.setAttribute(elemRel, OPTIONS, rel.getBehavior().getOptions());
				XMLWriter.setAttribute(elemRel, PK_COLUMNS, rel.getPkCols());
				XMLWriter.setAttribute(elemRel, INHERITS, rel.getBehavior().getInheritParentName());

				String lastAttrName = "";
				//Collections.sort(rel.getAttributes());
				for(Attribute attr: rel.getAttributes()){
					if(lastAttrName.equals(attr.getFullName())) {
						Dbg.fixme("Duplicated column " + lastAttrName + SKIPPED_ON_PROJECT_WRITE);
						duplicitiesOccured = true;
						continue;
					} else {
						lastAttrName = attr.getFullName();
					}
					Element elemAttr = doc.createElement(COLUMN);
					elemRel.appendChild(elemAttr);

					XMLWriter.setAttribute(elemAttr, NAME, attr.getName());
					XMLWriter.setAttribute(elemAttr, TYPE, attr.getFullType());
					XMLWriter.setAttribute(elemAttr, IS_NULLABLE, attr.getBehavior().isAttNull());
					XMLWriter.setAttribute(elemAttr, ORDINAL, attr.getAttNum());
					XMLWriter.setAttribute(elemAttr, DEFAULT, attr.getBehavior().getDefaultValue());
					XMLWriter.setAttribute(elemAttr, STORAGE, attr.getBehavior().getStorageChar());
					XMLWriter.setAttribute(elemAttr, COLLATION, attr.getBehavior().getCollation());
					XMLWriter.setAttribute(elemAttr, DESCRIPTION, attr.getDescr());
				}

				Collections.sort(rel.getIndexes());
				for(Index index: rel.getIndexes()){
					if(lastAttrName.equals(index.getFullName())) {
						Dbg.fixme("Duplicated index " + lastAttrName + SKIPPED_ON_PROJECT_WRITE);
						duplicitiesOccured = true;
						continue;
					} else {
						lastAttrName = index.getFullName();
					}
					Element elemIndex = doc.createElement(INDEX);
					elemRel.appendChild(elemIndex);

					XMLWriter.setAttribute(elemIndex, NAME, index.getFullName());
					XMLWriter.setAttribute(elemIndex, DEFINITION, index.getBehavior().getDef());
					XMLWriter.setAttribute(elemIndex, WHERE, index.getBehavior().getWhere());
					XMLWriter.setAttribute(elemIndex, ACCESS_METHOD, index.getBehavior().getAccessMethod().equals("btree") ? "" : index.getBehavior().getAccessMethod());
					XMLWriter.setAttribute(elemIndex, IS_UNIQUE, index.getBehavior().isUnique());
					XMLWriter.setAttribute(elemIndex, IS_PRIMARY, index.getBehavior().isPrimary());
					XMLWriter.setAttribute(elemIndex, IS_CONSTRAINT, index.getBehavior().isConstraint());
					XMLWriter.setAttribute(elemIndex, DESCRIPTION, index.getDescr());
					XMLWriter.setAttribute(elemIndex, OPTIONS, index.getBehavior().getOptions());
				}
			}
			lastRelName = "";
			Collections.sort(schema.getPackages());
			for(Package pack: schema.getPackages()){
				if(lastRelName.equals(pack.getFullName())) {
					Dbg.fixme("Duplicated package " + lastRelName + SKIPPED_ON_PROJECT_WRITE);
					duplicitiesOccured = true;
					continue;
				} else {
					lastRelName = pack.getFullName();
				}
				Element elemPack = doc.createElement(PACKAGE);
				elemSchema.appendChild(elemPack);

				XMLWriter.setAttribute(elemPack, NAME, pack.getName());
				XMLWriter.setAttribute(elemPack, SOURCE, pack.getSrc());
				XMLWriter.setAttribute(elemPack, BODY, pack.getBody());
				XMLWriter.setAttribute(elemPack, DESCRIPTION, pack.getDescr());
				XMLWriter.setAttribute(elemPack, LOCATION, Geometry.getUnZoomed(pack.getLocation()));
			}
			lastRelName = "";
			Collections.sort(schema.getFunctions());
			for(Function function: schema.getFunctions()){
				if(lastRelName.equals(function.getFullName()+"("+function.getBehavior().getArgs()+")")) {
					Dbg.fixme("Duplicated function " + lastRelName + SKIPPED_ON_PROJECT_WRITE);
					duplicitiesOccured = true;
					continue;
				} else {
					lastRelName = function.getFullName()+"("+function.getBehavior().getArgs()+")";
				}
				Element elemFunc = doc.createElement(FUNCTION);
				elemSchema.appendChild(elemFunc);

				XMLWriter.setAttribute(elemFunc, NAME, function.getFullName());
				XMLWriter.setAttribute(elemFunc, TYPE, function.getRetType());
				XMLWriter.setAttribute(elemFunc, ARGUMENTS, function.getArgs());
				XMLWriter.setAttribute(elemFunc, SOURCE, function.getSrc());
				XMLWriter.setAttribute(elemFunc, LANGUAGE, function.getLang());
				XMLWriter.setAttribute(elemFunc, VOLATILITY, function.getVolatility());
				XMLWriter.setAttribute(elemFunc, IS_SECURITY_DEFINER, function.isSecurityDefiner());
				XMLWriter.setAttribute(elemFunc, COST, function.getCost());
				XMLWriter.setAttribute(elemFunc, ROWS, function.getRows());
				XMLWriter.setAttribute(elemFunc, DESCRIPTION, function.getDescr());
				XMLWriter.setAttribute(elemFunc, LOCATION, Geometry.getUnZoomed(function.getLocation()));
			}
			lastRelName = "";
			Collections.sort(schema.getViews());
			for(View view: schema.getViews()){
				if(lastRelName.equals(view.getFullName())) {
					Dbg.fixme("Duplicated view " + lastRelName + SKIPPED_ON_PROJECT_WRITE);
					duplicitiesOccured = true;
					continue;
				} else {
					lastRelName = view.getFullName();
				}
				Element elemFunc = doc.createElement(VIEW);
				elemSchema.appendChild(elemFunc);

				XMLWriter.setAttribute(elemFunc, NAME, view.getFullName());
				XMLWriter.setAttribute(elemFunc, MATERIALIZED, view.getMaterialized());
				XMLWriter.setAttribute(elemFunc, SOURCE, view.getSrc());
				XMLWriter.setAttribute(elemFunc, DESCRIPTION, view.getDescr());
				XMLWriter.setAttribute(elemFunc, LOCATION, Geometry.getUnZoomed(view.getLocation()));
			}
			lastRelName = "";
			Collections.sort(schema.getSequences());
			for(Sequence seq: schema.getSequences()){
				if(lastRelName.equals(seq.getFullName())) {
					Dbg.fixme("Duplicated sequence " + lastRelName + SKIPPED_ON_PROJECT_WRITE);
					duplicitiesOccured = true;
					continue;
				} else {
					lastRelName = seq.getFullName();
				}
				Element elemFunc = doc.createElement(SEQUENCE);
				elemSchema.appendChild(elemFunc);

				XMLWriter.setAttribute(elemFunc, NAME, seq.getFullName());
				XMLWriter.setAttribute(elemFunc, MINIMUM, seq.getBehavior().getMin());
				XMLWriter.setAttribute(elemFunc, MAXIMUM, seq.getBehavior().getMax());
				XMLWriter.setAttribute(elemFunc, INCREMENT, seq.getBehavior().getIncrement());
				XMLWriter.setAttribute(elemFunc, CURRENT_VALUE, seq.getBehavior().getCurrent());
				XMLWriter.setAttribute(elemFunc, IS_CYCLED, seq.getBehavior().isCycle());
				XMLWriter.setAttribute(elemFunc, DESCRIPTION, seq.getDescr());
				XMLWriter.setAttribute(elemFunc, DEPENDANCES, seq.getDependencies());
				XMLWriter.setAttribute(elemFunc, LOCATION, Geometry.getUnZoomed(seq.getLocation()));
			}
		}

		Element elemConstraints = doc.createElement("constraints");
		elemDB.appendChild(elemConstraints);

		Collections.sort(db.getConstraints());
		for(Constraint con: db.getConstraints()){
			if(lastSchemaName.equals(con.getFullName())) {
				Dbg.fixme("Duplicated constraint " + lastSchemaName + SKIPPED_ON_PROJECT_WRITE);
				duplicitiesOccured = true;
				continue;
			} else {
				lastSchemaName = con.getFullName();
			}

			Element elemCon = doc.createElement(CONSTRAINT);
			elemConstraints.appendChild(elemCon);

			XMLWriter.setAttribute(elemCon, NAME, con.getName());
			XMLWriter.setAttribute(elemCon, DESCRIPTION, con.getDescr());

			if(con.getRel1() != null && con.getRel2() != null && con.getAttr1() != null && con.getAttr2() != null){
				XMLWriter.setAttribute(elemCon, DEFINITION, "");
				XMLWriter.setAttribute(elemCon, REL1, con.getRel1().getFullName());
				XMLWriter.setAttribute(elemCon, REL2, con.getRel2().getFullName());
				XMLWriter.setAttribute(elemCon, ATTR1, con.getAttr1().getName());
				XMLWriter.setAttribute(elemCon, ATTR2, con.getAttr2().getName());
				XMLWriter.setAttribute(elemCon, ON_UPDATE, con.getBehavior().getOnUpdateChar());
				XMLWriter.setAttribute(elemCon, ON_DELETE, con.getBehavior().getOnDeleteChar());
			}else if(con instanceof CheckConstraint){
				XMLWriter.setAttribute(elemCon, DEFINITION, ((CheckConstraint) con).getDef());
				XMLWriter.setAttribute(elemCon, REL1, con.getRel1().getFullName());
			}else{
				elemConstraints.removeChild(elemCon);
			}
		}

		Element elemTriggers = doc.createElement("triggers");
		elemDB.appendChild(elemTriggers);

		Collections.sort(db.getTriggers());
		for(Trigger tr: db.getTriggers()){
			if(lastSchemaName.equals(tr.getFullName())) {
				Dbg.fixme("Duplicated trigger " + lastSchemaName + SKIPPED_ON_PROJECT_WRITE);
				duplicitiesOccured = true;
				continue;
			} else {
				lastSchemaName = tr.getFullName();
			}

			if(tr.getRel1() != null && (tr.getRel2() != null || tr.hasBody())){
				Element elemTrig = doc.createElement(TRIGGER);
				elemTriggers.appendChild(elemTrig);
				XMLWriter.setAttribute(elemTrig, NAME, tr.getName());
				XMLWriter.setAttribute(elemTrig, DEFINITION, tr.hasBody() ? tr.getBehavior().getDef() : tr.getBehavior().getProcname());
				XMLWriter.setAttribute(elemTrig, ATTRIBUTES, tr.getBehavior().getAttrs());
				XMLWriter.setAttribute(elemTrig, WHEN, tr.getBehavior().getWhen());
				XMLWriter.setAttribute(elemTrig, REL1, tr.getRel1().getFullName());
				XMLWriter.setAttribute(elemTrig, IS_DISABLED, !tr.getBehavior().isEnabled());
				XMLWriter.setAttribute(elemTrig, IS_STATEMENT, !tr.getBehavior().isRowType());
				XMLWriter.setAttribute(elemTrig, TIMING, tr.getBehavior().getTimingChar());
				XMLWriter.setAttribute(elemTrig, DESCRIPTION, tr.getDescr());
				XMLWriter.setAttribute(elemTrig, EVENTS, tr.getBehavior().getEvents());
			}
		}
		return duplicitiesOccured;
	}

	void checkNewRevisionsXML(){
		if(!UIConstants.DEBUG){
			//Dbg.info("Checking new revisions");
			try {
				File[] revFiles = new File(revPath).listFiles();
				if(revFiles != null) {
					boolean changed = false;
					RevisionSAXHandler revSaxHandler = new RevisionSAXHandler(this);
					for (File revFile : revFiles) {
						if (revFile.getName().endsWith(EXT_XML)) {
							String newUID = revFile.getName().replace(EXT_XML, "");
							boolean found = false;
							for (Revision revision : revisions) {
								if (revision.UID.equals(newUID)) {
									found = true;
									if(revision.getLastModified().before(new Date(revFile.lastModified()-5))){
										Dbg.info("Reloading changed revision from disk");
										revisions.remove(revision);
										found = false;
									}
									break;
								}
							}
							if (!found) {
								Dbg.info("Found new revision "+newUID);
								changed = true;
								try {
									SAXParserFactory.newInstance().newSAXParser().parse(revFile, revSaxHandler);
								} catch (Exception e) {
									Dbg.fixme("Failed to load revision from " + revFile.getName() + "\n" + e.getMessage(), e);
								}
							}
						}
					}
					if(changed){
						Collections.sort(revisions);
						DifferenceView.instance.updateRevisionTable();
					}
				}
			} catch (Exception e){
				Dbg.fixme("checking new revisions on disk failed", e);
			}
		}
	}

	void loadFromXML(File projectDir, final long startTime) {
		//Dbg.info("Start loading "+databases.size()+" DBs from XML in "+((System.currentTimeMillis() - startTime)/1000.0)+"s");

		for(int i=0; i<databases.size(); i++){
			DB db = databases.get(i);
			Dbg.info("Loading "+ db.getName()+" from XML at "+((System.currentTimeMillis() - startTime)/1000.0)+"s");
			loadDbFromXML(db, new File(projectDir, DATABASE +(i+1)+ EXT_XML));

			if(db.getConnection().isSupported(SupportedElement.RELATION_INHERIT)){
				try {
					checkInheritances(db);
				} catch (Exception e){
					Dbg.notImportant("Checking inheritances failed. Retrying.", e);
					checkInheritances(db);
				}
			}
		}
		Dbg.info("Databases loaded from XML at "+((System.currentTimeMillis() - startTime)/1000.0)+"s, loading workspaces");

		try {
			SAXParserFactory.newInstance().newSAXParser().parse(new File(getProjectPath(), "workspaces.xml"), new WorkspaceSAXHandler(this));
		} catch (FileNotFoundException e){
            Dbg.notImportantAtAll("File not found means this is the first run of the application", e);
        } catch (Exception e){
			StringBuilder stackTrace = new StringBuilder();
			for(StackTraceElement elem : e.getStackTrace()){
				stackTrace.append("\n").append(elem.toString());
			}
			Dbg.fixme("Failed to load workspaces:"+stackTrace.toString(), e);
			Dbg.backupError(true);
			writeWorkspaces = false;
		}

		Dbg.info("Workspaces loaded from XML at "+((System.currentTimeMillis() - startTime)/1000.0)+"s, loading revisions and recent queries");

		Schedule.inWorker(Schedule.TYPE_DELAY, () -> {
            final long localTime = System.currentTimeMillis();
            final CountDownLatch latch = new CountDownLatch(2);

			Schedule.inWorker(() -> {
                File[] revFiles = new File(revPath).listFiles();
                if(revFiles != null) {
                    RevisionSAXHandler revSaxHandler = new RevisionSAXHandler(this);
                    for (File revFile : revFiles) {
                        if (revFile.getName().endsWith(EXT_XML)) {
                            try {
                                SAXParserFactory.newInstance().newSAXParser().parse(revFile, revSaxHandler);
                            } catch (Exception e) {
                                Dbg.fixme("Failed to load revision from " + revFile.getName() + "\n" + e.getMessage(), e);
                            }
                        }
                    }
                    Collections.sort(revisions);
                }

                Dbg.info("Revisions loaded from XML in "+((System.currentTimeMillis() - localTime)/1000.0)+"s");
                latch.countDown();
            });

			Schedule.inWorker(() -> {
                try {
                    SAXParserFactory.newInstance().newSAXParser().parse(new File(getProjectPath(), "queries.xml"), new QueriesSAXHandler(this));
                } catch (FileNotFoundException e){
					Dbg.notImportantAtAll("File not found means this is the first run of the application", e);
				} catch (Exception e){
                    Dbg.fixme("Failed to load recent queries", e);
                }
                Dbg.info("Recent queries loaded from XML in "+((System.currentTimeMillis() - localTime)/1000.0)+"s");
                latch.countDown();
            });

            try{
				latch.await();
            } catch(Exception e){
                Dbg.notImportant("Nothing we can do", e);
			}
            ProjectManager.getInstance().removeProtectionAfterLoad();
            Dbg.info("Revisions and recent queries loaded from XML in "+((System.currentTimeMillis() - startTime)/1000.0)+"s, project is now fully available");
        });

		Dbg.info("Project loaded from XML in "+((System.currentTimeMillis() - startTime)/1000.0)+"s");
	}

	void checkInheritances() {
		for(DB db : databases){
			checkInheritances(db);
		}
	}

	void checkInheritances(DB db) {
		for (Schema schema : db.getSchemas()) {
            for(Relation rel: schema.getRelations()){
                rel.checkInheritance();
            }
        }
	}

	public void loadDbFromXML(DB db, File file){
		loadDbFromXML(db, file, false);
	}

	public void loadDbFromXML(DB db, File file, boolean mergeResults){
		ProjectSAX saxHandler = new ProjectSAX((Project)this, db, mergeResults);
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(file, saxHandler);
			Collections.sort(saxHandler.db.getSchemas());
			for(Schema schema: saxHandler.db.getSchemas()){
				Collections.sort(schema.getRelations());
				Collections.sort(schema.getFunctions());
				Collections.sort(schema.getViews());
				Collections.sort(schema.getPackages());
				for(Relation rel: schema.getRelations()){
					Collections.sort(rel.getTriggers());
					Collections.sort(rel.getConstraints());
					Collections.sort(rel.getIndexes());
				}
			}
		} catch (Exception e){
			Dbg.fixme(file.getName()+" could not be loaded, file broken", e);
		}
	}

}
