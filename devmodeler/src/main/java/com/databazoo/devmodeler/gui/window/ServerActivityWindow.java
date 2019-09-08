package com.databazoo.devmodeler.gui.window;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import com.databazoo.components.GCFrame;
import com.databazoo.components.RotatedTabbedPane;
import com.databazoo.components.UIConstants;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.table.EditableTable;
import com.databazoo.components.table.LineNumberScrollPane;
import com.databazoo.components.table.PercentageCellRenderer;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Server status window.
 *
 * @author bobus
 */
public class ServerActivityWindow {
	private static List<ServerActivityWindow> instances = new CopyOnWriteArrayList<>();

	public static ServerActivityWindow get (IConnection newConn, String dbName) {
		ServerActivityWindow instance;
		for (ServerActivityWindow instance1 : instances) {
			instance = instance1;
			if(instance.getConnString().equals(newConn.getFullName()+"~"+dbName)){
				instance.frame.setVisible(true);
				//instance.isPaused = false;
				instance.loadServerStatus();
				instance.startReload();
				return instance;
			}
		}
		instance = new ServerActivityWindow(newConn, dbName);
		instances.add(instance);
		return instance;
	}

	GCFrame frame;
	JTable processTable, profilerTable;
	RotatedTabbedPane tabbedPane;

	private IConnection conn;
	private int selectedPID = 0;
	private boolean selectedRowFound = false;

	private JLabel labTotalProc, labActiveProc, labActiveLocks, labProfilerData;
	private boolean isPaused = false;
	private JButton menuBtnCancel, menuBtnTerminate, menuBtnClearProfiler;
	private JCheckBox profilerEnabledCB;
	private DB db;
	private Timer reloadTimer;

	private Map<String, ProfilerLine> profilerDataMap = new HashMap<>();
	private List<ProfilerLine> profilerData = new ArrayList<>();
	private int profilerDataSum = 0;

	private ServerActivityWindow(IConnection newConn, String dbName) {
		IConnection dedicatedConn = Project.getCurrent().getDedicatedConnection(dbName, newConn.getName());
		this.conn = dedicatedConn == null ? newConn : dedicatedConn;
		this.db = Project.getCurrent().getDatabaseByName(dbName);
		draw();
		initLoad();
	}

	private void initLoad(){
		Schedule.inWorker(() -> {
            try {
                conn.prepare(conn.getQueryConnect(), db).close();
            } catch (DBCommException e) {
                Dbg.notImportant("Server activity window should show some error here. Ignoring for now.", e);
            }

            loadServerStatus();
            startReload();
        });
	}

	private void draw(){
		drawFrame();
		drawTable();
		frame.getContentPane()
				.add(new VerticalContainer(
						new HorizontalContainer(
								drawLeftMenu(),
								null,
								drawRightMenu()
						),
						getTableComponents(),
						null
				));
	}

	private Component getTableComponents() {
		tabbedPane = new RotatedTabbedPane(RotatedTabbedPane.RIGHT);
		tabbedPane.addTab("Processes", new LineNumberScrollPane(processTable));
		tabbedPane.addTab("Profiler", new LineNumberScrollPane(profilerTable));
		return tabbedPane;
	}

	private void drawTable() {
		processTable = new ServerStatusTable(this);
		processTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		processTable.setCellSelectionEnabled(false);
		processTable.setColumnSelectionAllowed(false);
		processTable.setRowSelectionAllowed(true);
		processTable.getSelectionModel().addListSelectionListener(new ActivitySelectionListener());

		profilerTable = new JTable(new ProfilerTableModel());
		profilerTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		profilerTable.setCellSelectionEnabled(false);
		profilerTable.setColumnSelectionAllowed(false);
		profilerTable.setRowSelectionAllowed(true);

		TableColumn column0 = profilerTable.getColumnModel().getColumn(0);
		column0.setCellRenderer(new PercentageCellRenderer());
		column0.setMinWidth(100);
		column0.setMaxWidth(100);
	}

	void setProfilerEnabled(boolean enabled){
		profilerEnabledCB.setSelected(enabled);
	}

	private void drawFrame() {
		frame = new GCFrame("Activity on " + conn.getFullName());
		frame.setDefaultCloseOperation(GCFrame.HIDE_ON_CLOSE);
		frame.setIconImages(Theme.getAllSizes(Theme.ICO_STATUS));
		frame.pack();
		frame.setVisible(true);
		frame.setHalfScreenSize();
		if(Settings.getBool(Settings.L_MAXIMIZE_ACTIVITY)){
			frame.setExtendedState(GCFrame.MAXIMIZED_BOTH);
		}
		/*frame.setSize(scrSize.width, scrSize.height);
		frame.setExtendedState(GCFrame.MAXIMIZED_BOTH);*/
	}

	private synchronized void startReload(){
		if(reloadTimer == null){
			reloadTimer = new Timer("ServerActivityReloadDelay");
			reloadTimer.schedule(new TimerTask(){
				@Override public void run(){
					if(frame.isVisible() && !isPaused){
						loadServerStatus();
					}else{
						synchronized (this) {
							reloadTimer.cancel();
							reloadTimer = null;
						}
					}
				}
			}, Settings.getInt(Settings.L_SYNC_SERVER_ACTIVITY)*2000L, Settings.getInt(Settings.L_SYNC_SERVER_ACTIVITY)*1000L);
		}
	}

	void loadServerStatus(){
		Schedule.inWorker(() -> {
            int log = DesignGUI.getInfoPanel() != null ? DesignGUI.getInfoPanel().write("Loading server activity...") : -1;
            if(!selectedRowFound){
                selectedPID = 0;
                menuBtnCancel.setEnabled(false);
                menuBtnTerminate.setEnabled(false);
            }
            selectedRowFound = false;
            try {
                Result result = conn.getServerStatus();
                result.showNewLine(false);
                if(profilerEnabledCB.isSelected()) {
					((ProfilerTableModel) profilerTable.getModel()).updateRows(result);
					labProfilerData.setText(String.valueOf(profilerDataSum));
				}
				((ServerStatusTableModel) processTable.getModel()).mergeRows(processTable, result);

                if(conn.getQueryLocks().length() > 10){
                    Result r2 = conn.run(conn.getQueryLocks());
                    if(r2.getRowCount() > 0){
                        labTotalProc.setText((String)r2.getValueAt(0, 0));
                        labActiveLocks.setText((String)r2.getValueAt(0, 1));
                    }
                }else{
                    labTotalProc.setText(conn.getQueryLocks());
                }
                labActiveProc.setText(Integer.toString(result.getRowCount()));
                if(DesignGUI.getInfoPanel() != null) {
                    DesignGUI.getInfoPanel().writeOK(log);
                }
            } catch (DBCommException ex) {
            	Dbg.notImportant("Getting server status failed", ex);
                if(DesignGUI.getInfoPanel() != null) {
                    DesignGUI.getInfoPanel().writeFailed(log, ex.getMessage());
                }
            }
        });
	}

	void togglePause(){
		if(isPaused){
			isPaused = false;
			startReload();
		}else{
			isPaused = true;
		}
	}

	private void cancelSelected(boolean hard){
		int log = DesignGUI.getInfoPanel().write(hard ? "Terminating..." : "Cancelling...");
		try {
			conn.run(hard ? conn.getQueryTerminate(selectedPID) : conn.getQueryCancel(selectedPID));
			DesignGUI.getInfoPanel().writeOK(log);
		} catch (DBCommException ex) {
			Dbg.notImportant("Process termination failed", ex);
			DesignGUI.getInfoPanel().writeFailed(log, ex.getMessage());
		}
	}

	private Component drawLeftMenu(){
		JToggleButton menuBtnPause = new JToggleButton("Pause", Theme.getSmallIcon(Theme.ICO_PAUSE));
		menuBtnPause.addActionListener(e -> togglePause());

		menuBtnCancel = new JButton("Cancel query", Theme.getSmallIcon(Theme.ICO_STOP));
		menuBtnCancel.setEnabled(false);
		menuBtnCancel.addActionListener(e -> cancelSelected(false));

		menuBtnTerminate = new JButton("Terminate process", Theme.getSmallIcon(Theme.ICO_DELETE));
		menuBtnTerminate.setEnabled(false);
		menuBtnTerminate.addActionListener(e -> cancelSelected(true));

		profilerEnabledCB = new JCheckBox("Profiler");
		profilerEnabledCB.setToolTipText("Enable profiler");
		profilerEnabledCB.addActionListener(e -> updateProfilerUI());

		menuBtnClearProfiler = new JButton("Clear profiler data", Theme.getSmallIcon(Theme.ICO_CANCEL));
		menuBtnClearProfiler.setEnabled(false);
		menuBtnClearProfiler.addActionListener(e -> clearProfiler());

		JPanel panel = new JPanel();
		panel.add(menuBtnPause);
		panel.add(new JLabel("   "));
		panel.add(menuBtnCancel);
		panel.add(menuBtnTerminate);
		panel.add(new JLabel("   "));
		panel.add(profilerEnabledCB);
		panel.add(menuBtnClearProfiler);
		return panel;
	}

	void updateProfilerUI() {
		if(profilerEnabledCB.isSelected()){
			menuBtnClearProfiler.setEnabled(true);
			tabbedPane.setSelectedIndex(1);
		} else {
			menuBtnClearProfiler.setEnabled(false);
			tabbedPane.setSelectedIndex(0);
		}
	}

	void clearProfiler() {
		profilerData.clear();
		profilerDataMap.clear();
		profilerDataSum = 0;
	}

	private Component drawRightMenu(){
		JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		panel.add(createLabel("Total processes:", 5));
		labTotalProc = createLabel("0", 40);
		panel.add(labTotalProc);
		//panel.add(createLabel(""));
		panel.add(createLabel("Active processes:", 5));
		labActiveProc = createLabel("0", 40);
		panel.add(labActiveProc);
		//panel.add(createLabel("   "));
		panel.add(createLabel("Active locks:", 5));
		labActiveLocks = createLabel("0", 40);
		panel.add(labActiveLocks);
		panel.add(createLabel("Profiler data:", 5));
		labProfilerData = createLabel("0", 40);
		panel.add(labProfilerData);
		return panel;
	}

	private JLabel createLabel(String text, int whitespace){
		JLabel l = new JLabel(text);
		l.setPreferredSize(new Dimension(l.getPreferredSize().width+whitespace, 28));
		return l;
	}

	public IConnection getConnection(){
		return conn;
	}

	private String getConnString(){
		return conn.getFullName()+"~"+db.getName();
	}

	private static class ServerStatusTableModel extends AbstractTableModel {
		transient List<Row> data = new ArrayList<>();
		String[] cols = new String[]{};
		@Override
		public String getColumnName(int col) {
			return cols[col];
		}

		@Override
		public int getColumnCount(){
			return cols.length;
		}

		@Override
		public int getRowCount(){
			return data.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row).getColValue(col);
		}
		int getRowTTL(int row) {
			return data.get(row).ttl;
		}
		int getRowSeen(int row) {
			return data.get(row).querySeen;
		}

		private synchronized void mergeRows(final JTable table, Result result) {
			if(cols.length == 0 && result.getColumnCount() > 0){
				cols = new String[result.getColumnCount()];
				for(int i=0; i<cols.length; i++){
					cols[i] = result.getColumnName(i);
				}
				fireTableStructureChanged();
				for(int i=0; i<cols.length-1; i++){
					table.getColumnModel().getColumn(i).setMinWidth(result.getColumnMaxW(i));
					table.getColumnModel().getColumn(i).setMaxWidth(result.getColumnMaxW(i));
				}
				Schedule.reInvokeInEDT(Schedule.Named.SERVER_ACTIVITY_MERGE_ROWS, Schedule.TYPE_DELAY, () -> {
                    for(int i=0; i<cols.length-1; i++){
                        table.getColumnModel().getColumn(i).setMinWidth(20);
                        table.getColumnModel().getColumn(i).setMaxWidth(1000);
                    }
                });
			}

			for(Row row: data){
				row.found = false;
			}
			for(int i=0; i<result.getRowCount(); i++){
				String newPID = (String)result.getValueAt(i, 0);
				boolean found = false;
				for(Row row: data){
					String oldPID = row.getColValue(0);
					if(oldPID.equals(newPID)){
						found = true;
						row.found = true;
						row.ttl = Config.STATUS_ROW_TTL;
						if(
								(result.getColumnName(4).equals("start_time") && Objects.equals(row.getColValueNonFormatted(2), result.getValueAt(i, 2)) && Objects.equals(row.getColValueNonFormatted(4), result.getValueAt(i, 4))) ||	// Postgres >= 9.0
								(result.getColumnName(3).equals("start_time") && Objects.equals(row.getColValueNonFormatted(1), result.getValueAt(i, 1)) && Objects.equals(row.getColValueNonFormatted(3), result.getValueAt(i, 3))) ||	// Postgres < 9.0
								(result.getColumnName(3).equals("START_TIME") && Objects.equals(row.getColValueNonFormatted(1), result.getValueAt(i, 1)) && Objects.equals(row.getColValueNonFormatted(3), result.getValueAt(i, 3))) ||	// MySQL
								(result.getColumnName(5).equals("TEXT") && Objects.equals(row.getColValueNonFormatted(5), result.getValueAt(i, 5)))		// MSSQL
								){
							row.querySeen++;
						}else{
							row.querySeen = 0;
						}
						for(int k=0; k<cols.length; k++){
							row.setColValue(k, (String)result.getValueAt(i, k));
						}
						break;
					}
				}
				if(!found){
					String[] colValue = new String[cols.length];
					for(int j=0; j<cols.length; j++){
						colValue[j] = (String)result.getValueAt(i, j);
					}
					data.add(new Row(colValue));
				}
			}
			for(int j=0; j<data.size(); j++){
				Row row = data.get(j);
				if(!row.found){
					row.ttl--;
					if(row.ttl <= 0){
						data.remove(j);
						j--;
					}
				}
			}
			fireTableDataChanged();
		}

		private static class Row {
			private final String[] colValue;
			private int querySeen = 0;
			private boolean found = true;
			private int ttl = Config.STATUS_ROW_TTL;

			Row(String[] colValues) {
				this.colValue = colValues;
			}

			private void setColValue(int col, String value){
				this.colValue[col] = value;
			}

			private String getColValue(int col) {
				if((col==3 || col==2) && colValue[col] != null && (colValue[col].equals("t") || colValue[col].equals("f"))){
					return colValue[col].equals("t") ? "waiting" : "";
				}else{
					return colValue[col];
				}
			}

			private String getColValueNonFormatted(int col) {
				return colValue[col];
			}
		}
	}

	private static class ServerStatusRenderer extends JLabel implements TableCellRenderer {
		private final transient ServerActivityWindow window;

		private ServerStatusRenderer(ServerActivityWindow window){
			this.window = window;
			setOpaque(true);
			setBorder(BorderFactory.createMatteBorder(0,0,1,1, UIConstants.Colors.getTableBorders()));
		}

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Color fgLive, fgOld, fgVeryOld, fgSlow;

			ServerStatusTableModel model = (ServerStatusTableModel)table.getModel();
			if(isSelected){
				fgLive = Color.WHITE;
				fgOld = UIConstants.Colors.LIGHT_GRAY;
				fgVeryOld = UIConstants.Colors.GRAY;
				fgSlow = UIConstants.Colors.RED_SELECTED;
				setBackground(table.getSelectionBackground());
				window.selectedRowFound = true;
			}else{
				fgLive = Color.BLACK;
				fgOld = UIConstants.Colors.GRAY;
				fgVeryOld = UIConstants.Colors.LIGHT_GRAY;
				fgSlow = UIConstants.Colors.RED;
				setBackground(Color.WHITE);
			}
			setText((String)value);

			int ttl = model.getRowTTL(row);
			int seen = model.getRowSeen(row);
			if(ttl == Config.STATUS_ROW_TTL /*|| !Settings.getBool(Settings.L_ACTIVITY_FADE_ROWS)*/){
				if(seen >= Config.STATUS_SLOW_QUERY){
					setForeground(fgSlow);
				}else{
					setForeground(fgLive);
				}
			}else if(ttl * 1.0 > Config.STATUS_ROW_TTL2){
				setForeground(fgOld);
			}else{
				setForeground(fgVeryOld);
			}
			return this;
		}
	}

	private static class ServerStatusTable extends EditableTable {
		private final ServerStatusRenderer cellRenderer;

		private ServerStatusTable(ServerActivityWindow window) {
			super(new ServerStatusTableModel());
			cellRenderer = new ServerStatusRenderer(window);
		}

		@Override
        protected boolean isColEditable(int colIndex) { return false; }

		@Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            return cellRenderer;
        }
	}

	private class ActivitySelectionListener implements ListSelectionListener {
		@Override
        public void valueChanged(final ListSelectionEvent lse) {
			Schedule.inEDT(() -> {
                int[] selectedRows = processTable.getSelectedRows();
                if(selectedRows.length == 1){
                    selectedPID = Integer.parseInt((String) processTable.getModel().getValueAt(selectedRows[0], 0));
                    menuBtnCancel.setEnabled(true);
                    menuBtnTerminate.setEnabled(true);
                } else {
					menuBtnCancel.setEnabled(false);
					menuBtnTerminate.setEnabled(false);
				}
				processTable.repaint();
            });
        }
	}

	private class ProfilerTableModel extends AbstractTableModel {

		@Override public int getRowCount() {
			return profilerData.size();
		}

		@Override public int getColumnCount() {
			return 2;
		}

		@Override public String getColumnName(int columnIndex) {
			switch (columnIndex){
				case 0: return "Calls";
				case 1: return "SQL";
				default: return null;
			}
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex){
				case 0: return Math.round(profilerData.get(rowIndex).callCount * 100.0F / profilerDataSum);
				case 1: return profilerData.get(rowIndex).sql;
				default: return null;
			}
		}

		void updateRows(Result result) {
			for(int i=0; i<result.getRowCount(); i++) {
				String sql = result.getValueAt(i, result.getColumnCount()-1).toString();
				ProfilerLine line = profilerDataMap.get(sql);
				if(line == null){
					profilerDataMap.put(sql, new ProfilerLine(sql));
				} else {
					line.callCount++;
				}
			}
			profilerDataSum = profilerDataMap.values().stream().mapToInt(ProfilerLine::getCallCount).sum();
			profilerData = new ArrayList<>(profilerDataMap.values());
			Collections.sort(profilerData);
			fireTableDataChanged();
		}
	}

	private static class ProfilerLine implements Comparable<ProfilerLine> {

		int callCount = 1;
		final String sql;

		private ProfilerLine(String sql) {
			this.sql = sql;
		}

		int getCallCount() {
			return callCount;
		}

		String getSql() {
			return sql;
		}

		@Override public int compareTo(ProfilerLine obj) {
			return obj.callCount - callCount;
		}

		@Override public int hashCode() {
			return this.sql.hashCode();
		}

		@Override public boolean equals(Object obj) {
			return obj instanceof ProfilerLine && this.sql.equals(((ProfilerLine) obj).sql);
		}

		@Override public String toString() {
			return callCount + " runs of " + sql;
		}
	}
}
