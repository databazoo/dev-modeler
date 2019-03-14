package com.databazoo.devmodeler.gui.window.datawindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import com.databazoo.components.Separator;
import com.databazoo.components.UIConstants;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.devmodeler.project.RecentQuery;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;

abstract class DataWindowOutputRecentQueries extends DataWindowOutputFavorites {

	JScrollPane outputScrollRecentQueries;
	private JPanel outputRecentQueries;

	void prepareOutputRecentQueries(){
		outputRecentQueries = new VerticalContainer();
		outputRecentQueries.setBackground(Color.WHITE);
		outputRecentQueries.setBorder(new EmptyBorder(10, 10, 10, 10));
		outputScrollRecentQueries = new JScrollPane(outputRecentQueries);
		outputScrollRecentQueries.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);
	}

	void updateRecentQueriesList(){
        Schedule.inEDT(() -> {
            final JPanel contentRQ = new JPanel(new MigLayout("wrap 3", "[fill][150px!][150px!]"));
            contentRQ.setBackground(Color.WHITE);

            List<RecentQuery> queries = RecentQuery.getQueriesOnTable(rel == null ? "" : rel.getFullName());
            for (int i = queries.size() - 1; i >= 0; i--) {
                RecentQuery.RecentQueryRow comp = queries.get(i).getComponent(getInstance());
                contentRQ.add(comp.getQueryInput(), "width 100%-" + (UIConstants.isMac() ? "650" : "330") + "px");
                contentRQ.add(comp.getResultLabel());
                contentRQ.add(comp.getButtonPanel());
                contentRQ.add(new Separator(), "span");
            }

            queries = RecentQuery.getQueriesOnOtherTables(rel == null ? "" : rel.getFullName());
            if (queries.size() > 0) {
                contentRQ.add(new JLabel("<html><h3>Recent queries on other tables</h3></html>", JLabel.CENTER), "span");
                contentRQ.add(new Separator(), "span");
            }
            for (int i = queries.size() - 1; i >= 0; i--) {
                RecentQuery.RecentQueryRow comp = queries.get(i).getComponent(getInstance());
                contentRQ.add(comp.getQueryInput(), "width 100%-" + (UIConstants.isMac() ? "650" : "330") + "px");
                contentRQ.add(comp.getResultLabel());
                contentRQ.add(comp.getButtonPanel());
                contentRQ.add(new Separator(), "span");
            }

            outputRecentQueries.removeAll();
            outputRecentQueries.add(contentRQ, BorderLayout.NORTH);
            outputRecentQueries.add(new JLabel(), BorderLayout.CENTER);
        });
	}
}
