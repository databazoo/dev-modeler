package com.databazoo.devmodeler.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

import static com.databazoo.devmodeler.gui.UsageElement.SEARCH_CLEAR;
import static com.databazoo.devmodeler.gui.UsageElement.SEARCH_FULLTEXT;
import static com.databazoo.devmodeler.gui.UsageElement.SEARCH_HISTORY;
import static com.databazoo.devmodeler.gui.UsageElement.SEARCH_INVERT;
import static com.databazoo.devmodeler.gui.UsageElement.SEARCH_USED;

/**
 * Search input with history
 */
public class SearchPanel extends JPanel {

    public static final SearchPanel instance = new SearchPanel();

    private final List<String> searchHistory = new ArrayList<>();
    final UndoableTextField searchText;
    private final JButton historyButton;
    private final JButton searchButton;
    private final JCheckBox searchFulltext, searchNotMatching;

    private SearchPanel() {
        searchFulltext = new JCheckBox("fulltext");
        searchFulltext.setFocusable(false);
        searchFulltext.setPreferredSize(new Dimension(70, 14));
        searchFulltext.setToolTipText("Search inside functions, indexes, comments, etc.");
        searchFulltext.addActionListener(ae -> {
            Usage.log(SEARCH_FULLTEXT);
            triggerSearch();
        });

        searchNotMatching = new JCheckBox("invert");
        searchNotMatching.setFocusable(false);
        searchNotMatching.setPreferredSize(new Dimension(70, 14));
        searchNotMatching.setToolTipText("Invert the search. Only find elements not matching the pattern.");
        searchNotMatching.addActionListener(ae -> {
            Usage.log(SEARCH_INVERT);
            triggerSearch();
        });

        searchText = new UndoableTextField();
        searchText.disableFinder();
        searchText.setBordered(true);
        searchText.addKeyListener(new KeyAdapter(){
            @Override public void keyTyped(KeyEvent ke) {
                Usage.log(SEARCH_USED);
                triggerSearch();
            }
            @Override public void keyPressed(KeyEvent ke) {
                if(ke.getKeyCode()==KeyEvent.VK_ENTER){
                    ke.consume();
                }
            }
        });

        historyButton = new JButton(Theme.getSmallIcon(Theme.ICO_HISTORY));
        historyButton.setEnabled(false);
        historyButton.setFocusable(false);
        historyButton.addActionListener(e -> {
            Usage.log(SEARCH_HISTORY);
            RightClickMenu.setLocationTo(searchText, new Point(2, searchText.getHeight()-2));
            RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
                searchText.setText(searchHistory.get(type));
                triggerSearch();
            });
            for(int i=searchHistory.size()-1; i>=0; i--){
                menu.addItem(searchHistory.get(i), i);
            }
        });
        historyButton.setPreferredSize(UIConstants.MENU_BUTTON_SIZE);

        searchButton = new JButton(Theme.getSmallIcon(Theme.ICO_SEARCH));
        searchButton.setFocusable(false);
        searchButton.addActionListener(e -> {
            Usage.log(SEARCH_CLEAR);
            searchText.setText("");
            searchFulltext.setSelected(false);
            searchNotMatching.setSelected(false);
            triggerSearch();
        });
        searchButton.setPreferredSize(UIConstants.MENU_BUTTON_SIZE);

        JPanel buttonPanel = new JPanel(new GridLayout(1,0,0,0));
        buttonPanel.add(searchButton);
        buttonPanel.add(historyButton);

        JPanel searchOptionsPanel = new JPanel(new GridLayout(0,1,0,0));
        searchOptionsPanel.add(searchFulltext);
        searchOptionsPanel.add(searchNotMatching);

        setLayout(new BorderLayout(0,0));
        add(searchOptionsPanel, BorderLayout.WEST);
        add(searchText, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
        setMinimumSize(new Dimension(0, 0));
    }

    public void updateDbTree(){
        if(searchText.getText().isEmpty()){
            triggerSearch();
        }else{
            Schedule.inWorker(() -> DBTree.instance.checkDB(false));
        }
    }

    void triggerSearch(){
        Schedule.reInvokeInWorker(Schedule.Named.SEARCH_PANEL_TRIGGER_SEARCH, UIConstants.TYPE_TIMEOUT, () -> {
            final String text = searchText.getText();
            if(text.isEmpty()){
                searchButton.setIcon(Theme.getSmallIcon(Theme.ICO_SEARCH));
            }else{
                searchButton.setIcon(Theme.getSmallIcon(Theme.ICO_CANCEL));
                addSearchHistory(text);
            }
            DBTree.instance.checkDB(text, searchFulltext.isSelected(), searchNotMatching.isSelected());
        });
    }

    private void addSearchHistory(String text){
        for(int i=0; i<searchHistory.size(); i++){
            String val = searchHistory.get(i);
            if(val.equals(text)){
                searchHistory.remove(val);
            }
        }
        searchHistory.add(text);
        historyButton.setEnabled(true);
    }

    public void clearSearch(){
        Schedule.stopScheduled(Schedule.Named.SEARCH_PANEL_TRIGGER_SEARCH);
        if(!searchText.getText().isEmpty()){
            searchText.setText("");
            triggerSearch();
        }
    }

    public void focus(){
        searchText.requestFocusInWindow();
    }
}
