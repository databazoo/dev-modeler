package com.databazoo.devmodeler.gui.window.datawindow;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.components.RotatedTabbedPane;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.icons.PlainColorIcon;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.LineNumberRowHeader;
import com.databazoo.components.textInput.QueryErrorPositionObserver;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.*;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.plugincontrol.DataWindowPluginManager;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.RecentQuery;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.devmodeler.wizards.DiffWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import plugins.api.IDataWindowResult;
import plugins.api.IModelTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data window.
 *
 * @author bobus
 */
public class DataWindow extends DataWindowOutputMessages {

	private static final String NEWLINE_NN = "\n\n";
	private static final String NEWLINE_NN_COMMENTED_OUT = NEWLINE_NN + "-- ";

	public enum Tab {
        HISTORY(0, "History"),
        FAVORITES(1, "Favorites"),
        DATA(2, "Data"),
        EXPLAIN(3, "Explain"),
        MESSAGES(4, "Messages");

        private final int index;
        private final String label;

        Tab(int index, String label){
            this.index = index;
            this.label = label;
        }

        public int getIndex() {
            return index;
        }
        public String getLabel() {
            return label;
        }
    }

	private static final String L_FAVORITE_NEW_TITLE = "Favourite edited";
	private static final String L_FAVORITE_NEW_MESSAGE = "Overwrite currently open favorite query?";

	private static final String L_FAVORITE_EDITED_TITLE = "Favourite edited";
	private static final String L_FAVORITE_EDITED_MESSAGE = "Favourite query was edited.\n\nDo you wish to save it?";

	public static synchronized DataWindow get(){
		return new DataWindow();
	}

	private final FormattedClickableTextField queryInput = new FormattedClickableTextField(Project.getCurrent(), new FormatterSQL());
	private JButton btnRun;
	private JButton btnStop;
	private JButton btnExplain;

	private IConnectionQuery running;
	private JLabel rowCounter, timeCounter, connectionInfo;
	private Timer queryTimer, warningTimer;
	private RotatedTabbedPane outputTabPane;
	private String oldMessages;
	private JSplitPane querySplitPane, pluginSplitPane;

	private RecentQuery editedRecentQuery;

	private JCheckBox reloadAfterSaveCB, addToRevisionCB;
	private IconableComboBox databaseCombo;
	private IconableComboBox connectionCombo;
	private String windowBaseName;
	private JPanel pluginPane;
	private String where;
	private int limit = Settings.getInt(Settings.L_DATA_DEFAULT_LIMIT);
	private boolean isQuerySizeManual = false;
	private boolean isQuerySizeCalculated = false;

	@Override
	protected DataWindow getInstance(){
		return this;
	}

	/**
	 * Open an empty query window
	 */
	public void drawQueryWindow(){
		draw("SQL query on %database_name% - %connection_name%");
	}

	/**
	 * Open a query window with given query.
	 *
	 * @param sql given query
	 * @param db current database
	 */
	public void drawQueryWindow(String sql, DB db) {
		database = db;
		connection = ConnectionUtils.getCurrent(database.getName());
		draw("SQL query on %database_name% - %connection_name%");
		queryInput.setQuery(sql);
	}

	/**
	 * Open a query window with given query.
	 *
	 * @param sql given query
	 * @param db current database
	 * @param conn current connection
	 */
	public void drawQueryWindow(String sql, DB db, IConnection conn) {
		connection = conn;
		drawQueryWindow(sql, db);
	}

	/**
	 * Open a query window with a SELECT-JOIN on given constraint.
	 * Query is automatically executed on window open.
	 *
	 * @param con selected constraint
	 */
	public void drawConstraintData(Constraint con) {
		this.rel = (Relation)con.getRel1();
		database = rel.getDB();
		connection = ConnectionUtils.getCurrent(database.getName());

		prepareRelationData(rel, false);

		queryInput.setQuery(connection.getQuerySelect(con));
		runQuery();
		cacheFKsForColumns();
	}

	/**
	 * Open a query window with a SELECT on given view.
	 * Query is automatically executed on window open.
	 *
	 * @param view selected view
	 */
	public void drawViewData(View view) {
		drawQueryWindow(connection.getQuerySelect(view, where, "", Settings.getInt(Settings.L_DATA_DEFAULT_LIMIT)), view.getDB());
		runQuery();

	}

	/**
	 * Open a query window with a SELECT on given table.
	 * Query is automatically executed on window open.
	 *
	 * @param rel selected table
	 * @param desc ORDER BY ... DESC?
	 */
	public void drawRelationData(Relation rel, boolean desc) {
		this.rel = rel;
		database = rel.getDB();
		connection = ConnectionUtils.getCurrent(database.getName());

		prepareRelationData(rel, desc);

		runQuery();
		cacheFKsForColumns();
		Dbg.info("drawRelationData complete");
	}

	void drawRelationData(IConnection conn, Relation rel, boolean desc) {
		this.rel = rel;
		database = rel.getDB();
		connection = conn;

		prepareRelationData(rel, desc);

		runQuery();
		cacheFKsForColumns();
		Dbg.info("drawRelationData(conn) complete");
	}

	private void prepareRelationData(Relation rel, boolean desc) {
		String relName = rel.getFullName();
		draw(relName+" in %database_name% - %connection_name%");

		String order = rel.getPKey();
		if(!order.isEmpty() && desc){
			order = order.replaceAll(", ", " DESC, ") + " DESC";
		}
		queryInput.setQuery(connection.getQuerySelect(rel, where, order, limit));
		Dbg.info("prepareRelationData complete");
	}

	public DataWindow setWhere(String where) {
		this.where = where;
		return this;
	}

	/**
	 * Drawing data window, already in EDT.
	 */
	private void draw(String fullName) {
		windowBaseName = fullName;
		drawFrame();

		prepareButtons();

		prepareOutputData();
		prepareOutputExplain();
		prepareOutputMessages();
		prepareOutputRecentQueries();
		prepareOutputFavorites();
		prepareOutputTabs();

		frame.getContentPane().add(drawPluginSplitPane());

		updateWindowTitle();
		prepareQueryInputSizeListeners();
		preparePlugins();
	}

	private Component drawPluginSplitPane() {
		pluginSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawQuerySplitPane(), drawPluginPane());
		pluginSplitPane.setResizeWeight(1.0);
		pluginSplitPane.setDividerSize(3);
		return pluginSplitPane;
	}

	private Component drawPluginPane() {
		pluginPane = new JPanel(new GridLayout(0,1,0,0));
		return pluginPane;
	}

	private Component drawQuerySplitPane() {
		querySplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, prepareMenuPanel(), outputTabPane);
		querySplitPane.setDividerSize(3);
		querySplitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "none");
		return querySplitPane;
	}

	private void drawFrame() {
		frame = new GCFrameWithObservers(windowBaseName);
		frame.setDefaultCloseOperation(GCFrameWithObservers.DISPOSE_ON_CLOSE);
		frame.setIconImages(Theme.getAllSizes(rel == null ? Theme.ICO_SQL_WINDOW : Theme.ICO_DATA));
		frame.pack();
		frame.setVisible(true);
		frame.setHalfScreenSize();
		if(Settings.getBool(Settings.L_MAXIMIZE_DATA)){
			frame.setExtendedState(GCFrameWithObservers.MAXIMIZED_BOTH);
		}
		final DataWindow instance = this;
		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent we) {
				RecentQuery.destroyCache(instance);
				String qInput = queryInput.getText();
				if(editedRecentQuery != null && !editedRecentQuery.queryText.equals(qInput)){
					int ret = JOptionPane.showConfirmDialog(frame, L_FAVORITE_EDITED_MESSAGE, L_FAVORITE_EDITED_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if(ret == 0){
						editedRecentQuery.queryText = qInput;
						database.getProject().save();
					}
				}
			}
		});
	}

	private void prepareOutputTabs() {
		outputTabPane = new RotatedTabbedPane(JTabbedPane.RIGHT, JTabbedPane.SCROLL_TAB_LAYOUT);
		outputTabPane.addTab(Tab.HISTORY.getLabel(), outputScrollRecentQueries);
		outputTabPane.addTab(Tab.FAVORITES.getLabel(), outputScrollFavorites);
		outputTabPane.addTab(Tab.DATA.getLabel(), outputScrollData);
		outputTabPane.addTab(Tab.EXPLAIN.getLabel(), outputScrollExplain);
		outputTabPane.addTab(Tab.MESSAGES.getLabel(), outputScrollMessages);
		outputTabPane.addChangeListener(ce -> {
            if(outputTabPane.getSelectedIndex() == Tab.HISTORY.getIndex()) {
                updateRecentQueriesList();
            }else if(outputTabPane.getSelectedIndex() == Tab.FAVORITES.getIndex()) {
                updateFavoritesList();
            }
        });
		outputTabPane.setSelectedIndex(Tab.DATA.getIndex());
	}

	private JPanel prepareMenuPanel() {
		JPanel menuPane = new HorizontalContainer(new LeftMenu(), drawQueryInput(), new RightMenu());
		menuPane.getActionMap().put("saveNewRow", new AbstractAction(){ @Override public void actionPerformed(ActionEvent evt) {
			if(btnSave.isEnabled()){
				int editRow = editingRow;
				if(outputData.isEditing()){
					outputData.editingStopped(null);
				}
				if(editRow == outputData.getModel().getRowCount()-1){
					setEditing(editRow);
					saveNewRow();
				}
			}
		} });
		menuPane.getActionMap().put("runQuery", new AbstractAction(){ @Override public void actionPerformed(ActionEvent evt) { runQuery(); } });
		menuPane.getActionMap().put("runFocusedQuery", new AbstractAction(){ @Override public void actionPerformed(ActionEvent evt) { runFocusedQuery(); } });
		menuPane.getActionMap().put("runExplain", new AbstractAction(){ @Override public void actionPerformed(ActionEvent evt) { explainQuery(); } });
		menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "runExplain");
		menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "runQuery");
		menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "runFocusedQuery");
		menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "saveNewRow");
		return menuPane;
	}

	private void preparePlugins(){
		boolean showPluginPane = false;
		DataWindowPluginManager.init(this);
		for(int i=0; i<DataWindowPluginManager.getPluginCount(); i++){
			String pluginName = DataWindowPluginManager.getPluginName(this, i);
			ImageIcon pluginIcon = DataWindowPluginManager.getPluginIcon(this, i);
			Component pluginComp = DataWindowPluginManager.getWindowComponent(this, i);
			if(pluginComp != null){
				JLabel lab = pluginIcon == null ? new JLabel(pluginName, JLabel.CENTER) : new JLabel(pluginName, pluginIcon, JLabel.CENTER);
				lab.setBorder(new EmptyBorder(0,10,0,10));

				pluginPane.add(new VerticalContainer(lab, pluginComp, null));
				showPluginPane = true;
			}
			pluginComp = DataWindowPluginManager.getTabComponent(this, i);
			if(pluginComp != null){
				outputTabPane.addTab(DataWindowPluginManager.getPluginName(this, i), pluginComp);
			}
		}
		if(showPluginPane){
			pluginPane.setMinimumSize(new Dimension(150,0));
		}else{
			pluginPane.setPreferredSize(new Dimension(0,0));
			pluginPane.setMinimumSize(new Dimension(0,0));
			pluginSplitPane.setDividerLocation(1.0);
			pluginSplitPane.setDividerSize(0);
		}
		final DataWindow instance = this;
		frame.addWindowListener(new WindowAdapter(){
			@Override public void windowClosed(WindowEvent we) {
				DataWindowPluginManager.onWindowClose(instance);
			}
		});
	}

	private void updateWindowTitle(){
		frame.setTitle(windowBaseName.replace("%database_name%", database.getFullName()).replace("%connection_name%", connection.getFullName()));
		connectionInfo.setText(connection.getName());
		Color color = IColoredConnection.getColor(connection);
		if (color != null) {
			connectionInfo.setIcon(new PlainColorIcon(color));
		}
	}

	private void checkQueryInputSize(){
		if(!isQuerySizeManual){
			isQuerySizeCalculated = true;
			try {
				JViewport headerView = ((JScrollPane)queryInput.getParent().getParent()).getColumnHeader();
				int headerViewHeight = headerView != null && headerView.getView() != null ? headerView.getView().getHeight() : 0;
				int preferredHeight = queryInput.getPreferredSize().height;

				if(preferredHeight + headerViewHeight > Config.MAX_QUERY_INPUT_SIZE){
					querySplitPane.setDividerLocation(Config.MAX_QUERY_INPUT_SIZE);

				}else if(preferredHeight < Config.MIN_QUERY_INPUT_SIZE){
					querySplitPane.setDividerLocation(Config.MIN_QUERY_INPUT_SIZE + headerViewHeight + 8);

				}else{
					querySplitPane.setDividerLocation(preferredHeight + headerViewHeight + 8);
				}
			} catch (Exception e){
				Dbg.notImportant("This functionality is just a helper. Any failures are not important.", e);
			}
		}
	}

	@Override
	protected boolean getReloadAfterSave(){
		return reloadAfterSaveCB.isSelected();
	}

	@Override
	protected boolean getAddToRevision(){
		return addToRevisionCB.isSelected();
	}

	public void selectTab(final Tab tab){
		Schedule.inEDT(() -> outputTabPane.setSelectedIndex(tab.getIndex()));
	}

	@Override
	public void runFocusedQuery(){
		if(queryInput.getSelectedText() == null){
			selectFocusedQuery();
		}

		runQuery();
	}

	@Override
	public void runQuery(){
		String sql;
		if (queryInput.getSelectedText() != null) {
			sql = queryInput.getSelectedText();
			QueryErrorPositionObserver.get(frame).setCorrection(queryInput.getSelectionStart());
		} else {
			sql = queryInput.getText();
			QueryErrorPositionObserver.get(frame).setCorrection(0);
		}
		lastSQL = sql;
		runQuery(sql);
	}

	@Override
	public void runQuery(final String sql){
		setStopButton();
		selectTab(Tab.MESSAGES);
		DataWindowPluginManager.onQueryRun(getInstance());
		Schedule.inWorker(() -> {
            result = queryExec(sql);
            if(result != null){
                result.showNewLine(true);
                updateModel();
                DataWindowPluginManager.onQueryResult(getInstance());
            }else{
                DataWindowPluginManager.onQueryFail(getInstance());
            }
            if(result == null || result.isEmpty()){
                selectTab(Tab.MESSAGES);
            }else{
                selectTab(Tab.DATA);
            }
            int res, rows;
            if(result == null){
                res = RecentQuery.RESULT_FAILED;
                rows = 0;
            }else if(result.isEmpty()){
                res = RecentQuery.RESULT_EMPTY;
                rows = 0;
            }else{
                res = RecentQuery.RESULT_DATA;
                rows = result.getRowCount()-1;
            }
            RecentQuery.add(new RecentQuery(rel == null ? "" : rel.getFullName(), sql, res, rows));
        });
	}

	private void stopRunningQuery(){
		if(running != null){
			running.cancel();
		}
		if(queryTimer != null){
			queryTimer.cancel();
		}
	}

	@Override
	public IDataWindowResult getResult(){
		return result;
	}

	@Override
	public void explainQuery(){
		setStopButton();
		final DataWindow instance = this;
		DataWindowPluginManager.onQueryExplain(instance);
		Schedule.inWorker(() -> {
            outputExplain.removeAll();
            String sql;
            if (queryInput.getSelectedText() != null) {
                sql = connection.getQueryExplain(queryInput.getSelectedText());
                QueryErrorPositionObserver.get(frame).setCorrection(queryInput.getSelectionStart());
            } else {
                sql = connection.getQueryExplain(queryInput.getText());
                QueryErrorPositionObserver.get(frame).setCorrection(0);
            }
            result = queryExec(sql);
            if(result != null){
                result.showNewLine(false);
                updateModel();
                drawExplain();
                DataWindowPluginManager.onQueryResult(instance);
            }else{
                DataWindowPluginManager.onQueryFail(instance);
            }
            if(result == null || result.isEmpty()){
                selectTab(Tab.MESSAGES);
            }else{
                selectTab(connection.isSupported(SupportedElement.GRAPHICAL_EXPLAIN) ? Tab.EXPLAIN : Tab.DATA);
            }
        });
	}

	private Result queryExec(String sql) {
		int log = DesignGUI.getInfoPanel().write(sql);
		IConnectionQuery q;
		try {
			q = connection.prepare(sql, database);
			running = q;
			startTimeCount(q);
			Result r = q.run().fetchResult();
			stopTimeCount();
			DesignGUI.getInfoPanel().writeOK(log);
			timeCounter.setText(q.getTimeString());
			if(r.isEmpty()){
				rowCounter.setText(String.valueOf(r.getAffectedRows()));
				setMessages(getShortenedWarnings(q.getWarnings()) +
					"\n\nQuery was successful. " + (r.getAffectedRows() > 0 ? r.getAffectedRows()+" rows affected" : "Empty result returned") +
					" in "+q.getTimeString()
				);
			}else{
				rowCounter.setText(String.valueOf(r.getRowCount()));
				setMessages(getShortenedWarnings(q.getWarnings()) +
					"\n\nQuery returned "+r.getRowCount()+" rows" +
					" in "+q.getTimeString()
				);
			}
			running = null;
			resetRunButton();
			return r;
		} catch (DBCommException ex) {
			stopTimeCount();
			Dbg.notImportant("Getting data failed", ex);
			DesignGUI.getInfoPanel().writeFailed(log, ex.getMessage());
			timeCounter.setText(running != null ? running.getTimeString() : "");
			rowCounter.setText("");
			setMessages((running != null ? getShortenedWarnings(running.getWarnings()) + NEWLINE_NN : "")+connection.getCleanError(ex.getMessage()));
			running = null;
			resetRunButton();
			Point error = connection.getErrorPosition(sql, ex.getMessage());
			if(error != null){
				QueryErrorPositionObserver.get(frame).errorAt(error);
			}
			return null;
		}
	}

	private void setStopButton(){
		btnExplain.setEnabled(false);
		btnRun.setEnabled(false);
		btnStop.setEnabled(true);
	}

	private void resetRunButton(){
		btnExplain.setEnabled(true);
		btnRun.setEnabled(true);
		btnStop.setEnabled(false);
	}

	private JComponent drawQueryInput(){
		QueryErrorPositionObserver.get(frame).registerObserver(queryInput);
		queryInput.setAutocomplete(frame, connection);
		queryInput.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent me) {
				outputData.clearLastEditedCell();
			}
		});
		final DataWindow instance = this;
		if(DataWindowPluginManager.getPluginCount() > 0) {
			queryInput.addKeyListener(new KeyAdapter(){
				@Override public void keyTyped(KeyEvent ke) { DataWindowPluginManager.onQueryChange(instance); }
			});
		}
		JScrollPane scroll = new JScrollPane(queryInput);
		scroll.setRowHeaderView(new LineNumberRowHeader(queryInput, scroll));
		return scroll;
	}

	private void updateConnectionToSelected(){
		connection = database.getProject().getConnectionByName(connectionCombo.getSelectedItem().toString());
		IConnection dConn = database.getProject().getDedicatedConnection(database.getName(), connection.getName());
		if(dConn != null) {
			connection = dConn;
		}
		updateWindowTitle();
	}

	@Override
	protected void setEditing(int row){
		super.setEditing(row);
		btnSave.setEnabled(row >= 0);
	}

	private void startTimeCount(final IConnectionQuery running) {
		if(warningTimer != null) {
			warningTimer.cancel();
		}
		if(queryTimer != null) {
			queryTimer.cancel();
		}
		oldMessages = "";
		warningTimer = new Timer("DataWindowCollectWarnings");
		warningTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				running.checkWarnings();
				if(!oldMessages.equals(running.getWarnings())) {
					oldMessages = running.getWarnings();
					setMessages(getShortenedWarnings(oldMessages) + NEWLINE_NN);
				}
			}
		}, 500, 500);
		queryTimer = new Timer("DataWindowRunningQueryTime");
		queryTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				timeCounter.setText(running.getRunningTimeString());
			}
		}, 83, 83);
	}

	private void stopTimeCount(){
		if(warningTimer != null) {
			warningTimer.cancel();
		}
		if(queryTimer != null) {
			queryTimer.cancel();
		}
	}

	private void prepareQueryInputSizeListeners(){
		queryInput.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent ce) {
				JViewport headerView = ((JScrollPane)queryInput.getParent().getParent()).getColumnHeader();
				int headerViewHeight = headerView != null && headerView.getView() != null ? headerView.getView().getHeight() : 0;
				int preferredHeight = queryInput.getPreferredSize().height;
				if(querySplitPane.getDividerLocation() < preferredHeight + headerViewHeight + 8){
					checkQueryInputSize();
				}
			}
		});
		queryInput.addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInEDT(Schedule.Named.DATA_WINDOW_QUERY_SIZE, Schedule.TYPE_DELAY, DataWindow.this::checkQueryInputSize);
			}
		});

		querySplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, pce -> {
            if(!isQuerySizeCalculated){
                isQuerySizeManual = querySplitPane.getDividerLocation() > Config.MIN_QUERY_INPUT_SIZE + 20;
                if(!isQuerySizeManual){
					Schedule.inEDT(this::checkQueryInputSize);
                }
            }else{
                isQuerySizeCalculated = false;
            }
        });
	}

	@Override
	public void appendQuery(String queryText) {
		setQuery(queryInput.getText()+ NEWLINE_NN +queryText);
	}

	@Override
	public void setQuery(String query) {
		queryInput.setQuery(query);
	}

	@Override
	public String getQuery(){
		return queryInput.getText();
	}

	@Override
	public IModelTable getTable(){
		return rel;
	}

	@Override
	public void setQuerySelect(String columns) {
		final String text = queryInput.getText();
		if (StringUtils.countMatches(text, "SELECT") == 1) {
			queryInput.setText(connection.getQuerySelect(text, columns));
			queryInput.format();
			runQuery();
		} else {
			queryInput.setText(queryInput.getText() + connection.getQuerySelect("", columns).replace(NEWLINE_NN, NEWLINE_NN_COMMENTED_OUT));
			queryInput.format();
		}
	}

	@Override
	public void setQueryJoin(Relation rel2, String attr2, String attr1) {
		String rel2Alias = ConnectionUtils.getTableAlias(rel2.getName());
		final String text = queryInput.getText();
		String sqlJoin = "JOIN " + connection.escapeFullName(rel2.getFullName()) + " " + rel2Alias + " ON " + rel2Alias + "." + connection.escape(attr2) + " = " + connection.escape(attr1) + "\n";
		if (StringUtils.countMatches(text, "SELECT") == 1) {
			int location = -1;
			if ((location = text.indexOf("WHERE")) > 0) {
				if (StringUtils.countMatches(text, "WHERE") > 1) {
					location = -1;
				}
			} else if ((location = text.indexOf("ORDER BY")) > 0) {
				if (StringUtils.countMatches(text, "ORDER BY") > 1) {
					location = -1;
				}
			} else if ((location = text.indexOf("OFFSET")) > 0) {
				if (StringUtils.countMatches(text, "OFFSET") > 1) {
					location = -1;
				}
			} else if ((location = text.indexOf("LIMIT")) > 0) {
				if (StringUtils.countMatches(text, "LIMIT") > 1) {
					location = -1;
				}
			}
			if (location > 0) {
				queryInput.setText(text.substring(0, location) + sqlJoin + text.substring(location));
				queryInput.format();
				runQuery();
				return;
			}
		}
		queryInput.setText(queryInput.getText() + NEWLINE_NN_COMMENTED_OUT + sqlJoin);
		queryInput.format();
	}

	@Override
	public void setQueryOrder(String orderBy) {
		final String text = queryInput.getText();
		if (StringUtils.countMatches(text, "ORDER BY") <= 1 && StringUtils.countMatches(text, "SELECT") == 1) {
			queryInput.setText(connection.getQueryOrder(text, orderBy));
			queryInput.format();
			runQuery();
		} else {
			queryInput.setText(queryInput.getText() + connection.getQueryOrder("\n", orderBy).replace(NEWLINE_NN, NEWLINE_NN_COMMENTED_OUT));
			queryInput.format();
		}
	}

	@Override
	public void setQueryWhere(String where) {
		final String text = queryInput.getText();
		if (StringUtils.countMatches(text, "WHERE") <= 1 &&
				StringUtils.countMatches(text, "SELECT") + StringUtils.countMatches(text, "UPDATE") + StringUtils.countMatches(text, "DELETE") == 1) {
			queryInput.setText(connection.getQueryWhere(text, where));
			queryInput.format();
			if (!text.contains("DELETE") && !text.contains("UPDATE")) {
				runQuery();
			}
		} else {
			queryInput.setText(queryInput.getText() + connection.getQueryWhere(NEWLINE_NN, where).replace(NEWLINE_NN, NEWLINE_NN_COMMENTED_OUT));
			queryInput.format();
		}
	}

	@Override
	public int getConnectionType(){
		return database.getProject().getType();
	}

	public DataWindow setLimit (int newLimit) {
		limit = newLimit;
		return this;
	}

	private void selectFocusedQuery(){
		int caretPosition = queryInput.getCaretPosition();
		String text = queryInput.getText();

		String startText = text.substring(0, caretPosition);
		String endText = text.substring(caretPosition);

		Pattern pattern = Pattern.compile("SELECT[^;]+$");
		Matcher matcher = pattern.matcher(startText);
		if(matcher.find()){
			int matchedStart = matcher.start();
			int matchedEnd = endText.indexOf(';');

			queryInput.setSelectionStart(matchedStart);
			queryInput.setSelectionEnd(matchedEnd>=0 ? matchedEnd+startText.length() : text.length());
		}
	}

	public void setCurrentlyEdited(RecentQuery recentQuery) {
		editedRecentQuery = recentQuery;
	}

	private class LeftMenu extends JPanel {

		private LeftMenu() {
			JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			buttonPane.add(drawQueryButton());
			//buttonPane.add(btnSave);
			buttonPane.add(new JLabel("<html><font color='#BBBBBB'>&nbsp;|&nbsp;</font></html>"));
			buttonPane.add(drawAddToRevisionButton());
			buttonPane.add(drawAddToFavoritesButton());
			buttonPane.add(new JLabel("<html><font color='#BBBBBB'>&nbsp;|&nbsp;</font></html>"));
			buttonPane.add(drawExplainButton());
			buttonPane.add(drawRunButton());
			buttonPane.add(drawStopButton());

			JPanel statusPane = new JPanel(new MigLayout(
					"insets 1 20 1 20, wrap 4",
					"[5px::][10::50%,grow,fill]4px[5px::][10::50%,grow,fill]",
					"[]1[]"
			));
			statusPane.add(new JLabel("Connection:"), "span 2, align left center");
			statusPane.add(connectionInfo = new JLabel(), "span 2, height 50px:100%-20px:, align left center");
			statusPane.add(new JLabel(" Rows:"));
			statusPane.add(rowCounter = new JLabel("0"));
			statusPane.add(new JLabel(" Time:"));
			statusPane.add(timeCounter = new JLabel("0s"));

			JPanel topComp = new JPanel();
			topComp.setLayout(new BorderLayout());
			topComp.add(buttonPane, BorderLayout.NORTH);
			topComp.add(statusPane, BorderLayout.CENTER);

			setLayout(new BorderLayout(0,0));
			add(topComp, BorderLayout.NORTH);
			add(new JLabel(), BorderLayout.CENTER);
		}

		private Component drawQueryButton() {
			JButton btnQuery = new JButton(Theme.getSmallIcon(Theme.ICO_SQL_WINDOW));
			btnQuery.setToolTipText("Open new query window");
			btnQuery.setPreferredSize(BUTTON_SIZE);
			btnQuery.setFocusable(false);
			btnQuery.addActionListener(e -> {
				DataWindow win = DataWindow.get();
				if(rel != null){
					win.database = database;
					win.connection = connection;
					win.prepareRelationData(rel, false);
					win.queryInput.setText(queryInput.getText());
					win.runQuery();
				}else{
					win.drawQueryWindow(queryInput.getText(), database, connection);
				}
			});
			return btnQuery;
		}

		private Component drawAddToRevisionButton() {
			JButton btnRev = new JButton(Theme.getSmallIcon(Theme.ICO_REVISION));
			btnRev.setToolTipText("Add current query to some revision");
			btnRev.setPreferredSize(BUTTON_SIZE);
			btnRev.setFocusable(false);
			btnRev.addActionListener(ev -> DiffWizard.get().drawAddChange(queryInput.getText(), database, connection));
			return btnRev;
		}

		private Component drawAddToFavoritesButton() {
			JButton btnFav = new JButton(Theme.getSmallIcon(Theme.ICO_FAVORITE_ADD));
			btnFav.setToolTipText("Add current query to favorites");
			btnFav.setPreferredSize(BUTTON_SIZE);
			btnFav.setFocusable(false);
			btnFav.addActionListener(ev -> Schedule.inWorker(() -> {
                String[] opts = new String[]{"Overwrite current", "Create new favorite"};
                if(editedRecentQuery != null &&
                        JOptionPane.showOptionDialog(frame,
                                L_FAVORITE_NEW_MESSAGE,
                                L_FAVORITE_NEW_TITLE,
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                opts,
                                opts[0]
                        ) == 0)
                {
                    editedRecentQuery.queryText = queryInput.getText();

                }else{
                    RecentQuery rq = new RecentQuery(rel == null ? "" : rel.getFullName(), queryInput.getText(), RecentQuery.RESULT_EMPTY, 0);
                    database.getProject().favorites.add(rq);
                    setCurrentlyEdited(rq);

                    if(outputTabPane.getSelectedIndex() == Tab.FAVORITES.getIndex()) {
                        updateFavoritesList();
                    }else{
                        selectTab(Tab.FAVORITES);
                    }
                }
                database.getProject().save();
            }));
			return btnFav;
		}

		private Component drawExplainButton() {
			btnExplain = new JButton(Theme.getSmallIcon(Theme.ICO_EXPLAIN));
			btnExplain.setToolTipText("Explain query");
			btnExplain.setPreferredSize(BUTTON_SIZE);
			btnExplain.setFocusable(false);
			btnExplain.addActionListener(ev -> explainQuery());
			return btnExplain;
		}

		private Component drawRunButton() {
			btnRun = new JButton(Theme.getSmallIcon(Theme.ICO_RUN));
			btnRun.setToolTipText("Run query");
			btnRun.setPreferredSize(BUTTON_SIZE);
			btnRun.setFocusable(false);
			btnRun.addActionListener(ev -> runQuery());
			return btnRun;
		}

		private Component drawStopButton() {
			btnStop = new JButton(Theme.getSmallIcon(Theme.ICO_STOP));
			btnStop.setToolTipText("Stop running query");
			btnStop.setPreferredSize(BUTTON_SIZE);
			btnStop.setFocusable(false);
			btnStop.setEnabled(false);
			btnStop.addActionListener(ev -> {
				stopRunningQuery();
				resetRunButton();
			});
			return btnStop;
		}
	}

	private class RightMenu extends JPanel {
		RightMenu() {
			JPanel buttonPane = new JPanel(new GridLayout(0,1,0,0));
			buttonPane.add(databaseCombo = new IconableComboBox(database.getProject().getDatabaseNames()));
			buttonPane.add(connectionCombo = new IconableComboBox(database.getProject().getConnectionNames()));
			buttonPane.add(reloadAfterSaveCB = new JCheckBox("Reload data after save", Settings.getBool(Settings.L_DATA_RELOAD_AFTER_SAVE)));
			buttonPane.add(addToRevisionCB = new JCheckBox("Add changes to revision", Settings.getBool(Settings.L_REVISION_ADD_DATA)));

			databaseCombo.setPreferredSize(new Dimension(150, databaseCombo.getPreferredSize().height));
			databaseCombo.setSelectedItem(database.getName());
			connectionCombo.setPreferredSize(new Dimension(150, connectionCombo.getPreferredSize().height));

			connectionCombo.setSelectedByNamePart(connection.getName()+" <.*");

			reloadAfterSaveCB.setBorder(new EmptyBorder(0, 5, 0, 7));
			addToRevisionCB.setBorder(new EmptyBorder(0, 5, 0, 7));

			databaseCombo.addActionListener(ae -> {
                database = database.getProject().getDatabaseByName(databaseCombo.getSelectedItem().toString());
                updateConnectionToSelected();
            });
			connectionCombo.addActionListener(ae -> updateConnectionToSelected());

			setLayout(new BorderLayout(0,0));
			add(buttonPane, BorderLayout.NORTH);
			add(new JLabel(), BorderLayout.CENTER);
		}
	}
}
