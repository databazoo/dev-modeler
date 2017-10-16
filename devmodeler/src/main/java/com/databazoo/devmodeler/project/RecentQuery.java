
package com.databazoo.devmodeler.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Query history
 *
 * @author bobus
 */
public class RecentQuery {
	public static final int RESULT_DATA = 1;
	public static final int RESULT_EMPTY = 2;
	public static final int RESULT_FAILED = 3;
	private static final Map<DataWindow,Map<RecentQuery,RecentQueryRow>> CACHE = new HashMap<>();
	private static final Map<RecentQuery,RecentQueryRow> DEFAULT_CACHE = new HashMap<>();

	/**
	 * Get queries on given table.
	 *
	 * @param tableName table name
	 * @return query list
	 */
	public static List<RecentQuery> getQueriesOnTable(String tableName){
		List<RecentQuery> ret = new ArrayList<>();
		for(RecentQuery q: Project.getCurrent().recentQueries){
			if(q.tableName.equals(tableName)){
				ret.add(q);
			}
		}
		return ret;
	}

	/**
	 * Get queries on other than given table.
	 *
	 * @param tableName table name
	 * @return query list
	 */
	public static List<RecentQuery> getQueriesOnOtherTables(String tableName){
		return Project.getCurrent().recentQueries.stream()
				.filter(q -> !q.tableName.equals(tableName))
				.collect(Collectors.toList());
	}

	/**
	 * Add query to history.
	 *
	 * @param newQuery RecentQuery
	 */
	public static synchronized void add(RecentQuery newQuery){
		List<RecentQuery> queries = Project.getCurrent().recentQueries;
		removeEqualQueries(newQuery, queries);
		while(queries.size() >= Settings.getInt(Settings.L_DATA_HISTORY_LIMIT)){
			queries.remove(0);
		}
		queries.add(newQuery);
		checkQueryListSize(queries);
		Project.getCurrent().save();
	}

	private static void checkQueryListSize(List<RecentQuery> queries) {
		long currentSize = queries.stream().mapToLong(recentQuery -> recentQuery.queryText.length()).sum();
		while (currentSize > Settings.getInt(Settings.L_DATA_HISTORY_SIZE)*1024){
			currentSize -= queries.get(0).queryText.length();
			queries.remove(0);
		}
	}

	private static void removeEqualQueries(RecentQuery newQuery, List<RecentQuery> queries) {
		for(int i=0; i<queries.size(); i++){
			RecentQuery storedQuery = queries.get(i);
			if(storedQuery.tableName.equals(newQuery.tableName) && storedQuery.queryText.equals(newQuery.queryText)){
				queries.remove(i);
				i--;
			}
		}
	}

	/**
	 * Window is closed - drop cache.
	 *
	 * @param instance window
	 */
	public static void destroyCache(DataWindow instance){
		CACHE.remove(instance);
		//Dbg.info("Cache destroyed for window, now "+CACHE.size()+" windows are cached");
	}

	public final String tableName;
	public String queryText;
	public final int result;
	public final int resultRows;

	/**
	 * Constructor
	 *
	 * @param tableName table name
	 * @param queryText query text
	 * @param result result status
	 * @param resultRows returned rows
	 */
	public RecentQuery(String tableName, String queryText, int result, int resultRows) {
		this.tableName = tableName;
		this.queryText = queryText;
		this.result = result;
		this.resultRows = resultRows;

		prepareRow();
	}

	/**
	 * Cache history row component.
	 */
	private void prepareRow(){
		DEFAULT_CACHE.put(this, new RecentQueryRow(null));
	}

	/**
	 * Get history row component.
	 *
	 * @param instance window
	 * @return history row component
	 */
	public RecentQueryRow getComponent(final DataWindow instance){
		RecentQueryRow ret = null;
		Map<RecentQuery,RecentQueryRow> cachedRows = CACHE.get(instance);
		if(cachedRows == null){
			cachedRows = new HashMap<>();
			CACHE.put(instance, cachedRows);
			//Dbg.info("New cache created for window, now "+CACHE.size()+" windows are cached");
		}else{
			ret = cachedRows.get(this);
		}
		if(ret == null){
			//Dbg.info("No cache for current window, trying default cache");
			ret = DEFAULT_CACHE.get(this);
			if(ret == null){
				//Dbg.info("Filling default cache");
				ret = new RecentQueryRow(instance);
				//ret.setPreferredSize(new Dimension(300, ret.getPreferredSize().height+20));
				DEFAULT_CACHE.put(this, ret);
			}else{
				//Dbg.info("Resetting listeners");
				ret.setListeners(instance);
				Schedule.inWorker(() -> DEFAULT_CACHE.put(RecentQuery.this, new RecentQueryRow(instance)));
			}
			cachedRows.put(this, ret);
			//Dbg.info("Cache now contains "+cachedRows.size()+"rows");
		}
		return ret;
	}

	/**
	 * History row component
	 */
	public class RecentQueryRow{
		private JButton btnRun, btnApp, btnFav;
		private boolean isFavorite;
		private UndoableTextField queryInput;
		private JLabel resultLabel;
		private JPanel buttonPanel;

		/**
		 * Constructor
		 *
		 * @param instance window
		 */
		public RecentQueryRow(final DataWindow instance) {
			try {
				SwingUtilities.invokeAndWait(() -> {
                    if(queryText.length() > Config.FORMATTER_OVERFLOW_LIMIT){
                        queryInput = new UndoableTextField(queryText);
                    }else{
                        queryInput = new FormattedClickableTextField(Project.getCurrent(), queryText);
                        queryInput.setEditable(false);
                    }

                    resultLabel = new JLabel("<html>"+(result==RESULT_DATA ? "Returned "+resultRows+" rows" : (result==RESULT_EMPTY ? "Empty result" : "<font color=red>Query failed</font>"))+"</html>");

                    buttonPanel = new JPanel();
                    buttonPanel.add(btnRun = new JButton(Theme.getSmallIcon(Theme.ICO_RUN)));
                    buttonPanel.add(btnApp = new JButton(Theme.getSmallIcon(Theme.ICO_SQL_WINDOW)));
                    buttonPanel.add(btnFav = new JButton(Theme.getSmallIcon(Theme.ICO_FAVORITE_ADD)));
                    //buttonPanel.add(btnRev = new JButton(Theme.getSmallIcon(Theme.ICO_DIFFERENCE)));
                    buttonPanel.setBackground(Color.WHITE);

                    if(instance != null){
                        setListeners(instance);
                    }
                });
			} catch (InterruptedException | InvocationTargetException ex) {
				Dbg.info(ex);
			}
		}

		/**
		 * Get SQL input.
		 *
		 * @return input
		 */
		public JComponent getQueryInput(){
			return queryInput;
		}

		/**
		 * Get result label.
		 *
		 * @return JLabel
		 */
		public JComponent getResultLabel(){
			return resultLabel;
		}

		/**
		 * Get buttons.
		 *
		 * @return JPanel
		 */
		public JComponent getButtonPanel(){
			return buttonPanel;
		}

		/**
		 * Add button listeners.
		 *
		 * @param instance window
		 */
		private void setListeners(final DataWindow instance){
			if(btnRun.getActionListeners().length > 0){
				for(ActionListener l : btnRun.getActionListeners()){
					btnRun.removeActionListener(l);
				}
				for(ActionListener l : btnApp.getActionListeners()){
					btnApp.removeActionListener(l);
				}
				for(ActionListener l : btnFav.getActionListeners()){
					btnFav.removeActionListener(l);
				}
			}
			btnRun.setToolTipText("Run query");
			btnRun.addActionListener(ae -> {
                instance.setQuery(queryText);
                instance.runQuery();
                if(isFavorite) {
                    instance.setCurrentlyEdited(RecentQuery.this);
                }
            });
			btnApp.setToolTipText("Append query");
			btnApp.addActionListener(ae -> {
                instance.appendQuery(queryText);
                if(isFavorite) {
                    instance.setCurrentlyEdited(RecentQuery.this);
                }
            });
			btnFav.setToolTipText("Add query to favorites");
			btnFav.addActionListener(ae -> Schedule.inWorker(() -> {
				Project p = instance.getDB().getProject();
				if(!isFavorite){
					p.favorites.add(RecentQuery.this);
				}else{
					p.favorites.remove(RecentQuery.this);
				}
				instance.updateFavoritesList();
				p.save();
				if(!isFavorite){
					Schedule.inWorker(Schedule.CLICK_DELAY, () -> instance.selectTab(DataWindow.Tab.FAVORITES));
				}
			}));
		}

		public void setFavorite(boolean b) {
			isFavorite = b;
			if(isFavorite){
				btnFav.setToolTipText("Remove query from favorites");
				btnFav.setIcon(Theme.getSmallIcon(Theme.ICO_FAVORITE_REM));
			}
		}
	}

}
