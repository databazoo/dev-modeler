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
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;

abstract class DataWindowOutputFavorites extends DataWindowOutputExplain {

	JScrollPane outputScrollFavorites;
	private JPanel outputFavorites;


	void prepareOutputFavorites(){
		outputFavorites = new VerticalContainer();
		outputFavorites.setBackground(Color.WHITE);
		outputFavorites.setBorder(new EmptyBorder(10, 10, 10, 10));
		outputScrollFavorites = new JScrollPane(outputFavorites);
		outputScrollFavorites.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);
	}

	public void updateFavoritesList(){
        Schedule.inEDT(() -> {
            final JPanel contentRQ = new JPanel(new MigLayout("wrap 3", "[grow,fill][150px!][150px!]"));
            contentRQ.setBackground(Color.WHITE);

            List<RecentQuery> queries = Project.getCurrent().favorites;
            if (queries.isEmpty()) {
                contentRQ.add(new JLabel("<html><h3>Favorites list is empty</h3></html>", JLabel.CENTER), "span");
                contentRQ.add(new JLabel("<html>Mark some recent query with a star to add it to favorites list.</html>", JLabel.CENTER), "span");
            } else {
                for (int i = queries.size() - 1; i >= 0; i--) {
                    RecentQuery.RecentQueryRow comp = queries.get(i).getComponent(getInstance());
                    comp.setFavorite(true);
                    contentRQ.add(comp.getQueryInput(), "width 100%-" + (UIConstants.isMac() ? "650" : "330") + "px");
                    contentRQ.add(comp.getResultLabel());
                    contentRQ.add(comp.getButtonPanel());
                    contentRQ.add(new Separator(), "span");
                }
            }

            outputFavorites.removeAll();
            outputFavorites.add(contentRQ, BorderLayout.NORTH);
            outputFavorites.add(new JLabel(), BorderLayout.CENTER);

            contentRQ.revalidate();
        });
	}
}
