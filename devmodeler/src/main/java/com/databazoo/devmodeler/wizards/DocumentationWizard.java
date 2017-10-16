package com.databazoo.devmodeler.wizards;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import com.databazoo.components.WizardTree;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Use-cases described in HTML
 */
public class DocumentationWizard extends MigWizard {

    private static final String L_USECASES = "How do I...";
    //private static final String L_TUTORIAL = "UI guide";
    private static final int MORE_INFO = 31;
    public static final String L_MORE_INFO = "More info";

    public static synchronized DocumentationWizard getInstance() {
        return new DocumentationWizard();
    }

    enum UseCase {
        UC_01("1. connect to an existing database", "/doc/usecase01.html"),
        UC_02("2. create an empty database", "/doc/usecase02.html"),
        UC_03("3. set up a complete stack", "/doc/usecase03.html"),
        UC_04("4. share my configuration", "/doc/usecase04.html"),
        UC_05("5. create a table", "/doc/usecase05.html"),
        UC_06("6. see, export and edit data", "/doc/usecase06.html"),
        UC_07("7. navigate through a database", "/doc/usecase07.html"),
        UC_08("8. optimize SQL queries", "/doc/usecase08.html"),
        UC_09("9. optimize the model", "/doc/usecase09.html"),
        UC_10("10. publish changes", "/doc/usecase10.html"),
        UC_11("11. compare 2 databases", "/doc/usecase11.html"),
        UC_12("12. see running processes", "/doc/usecase12.html"),
        UC_13("13. migrate to different DB type", "/doc/usecase13.html"),
        UC_14("14. program in PL/SQL", "/doc/usecase14.html");

        private final String ucName;
        private final URL contentURL;

        UseCase(String ucName, String contentResourceName) {
            this.ucName = ucName;
            this.contentURL = getClass().getResource(contentResourceName);
        }

        public URL getContentURL() {
            return contentURL;
        }

        @Override
        public String toString() {
            return ucName;
        }
    }

    WizardTree tree;

    /**
     * Draw the Documentation wizard.
     *
     * Must be started in EDT.
     */
    public void draw(){
        drawWindow(Menu.L_HELP, createTree(), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), false);
    }

    private JComponent createTree(){
        DefaultMutableTreeNode useCasesNode = new DefaultMutableTreeNode(L_USECASES);
        for(UseCase useCase : UseCase.values()) {
            useCasesNode.add(new DefaultMutableTreeNode(useCase));
        }

        DefaultMutableTreeNode tutorialNode = new DefaultMutableTreeNode(IntroWizard.L_TUTORIAL);
        tutorialNode.add(new DefaultMutableTreeNode(IntroWizard.L_TUTORIAL_1));
        tutorialNode.add(new DefaultMutableTreeNode(IntroWizard.L_TUTORIAL_2));
        tutorialNode.add(new DefaultMutableTreeNode(IntroWizard.L_TUTORIAL_3));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(useCasesNode);
        root.add(tutorialNode);

        tree = new WizardTree(root, 1, new ModelIconRenderer(), this);
        tree.setRootVisible(false);
        return tree;
    }

    @Override
    public void valueChanged(final TreeSelectionEvent tse) {
        Schedule.inEDT(() -> {
            if (tse.getNewLeadSelectionPath() != null) {
                switch (tse.getNewLeadSelectionPath().getLastPathComponent().toString()) {
                case L_USECASES:
                    loadWelcomePage();
                    break;
                case IntroWizard.L_TUTORIAL:
                    tree.setSelectionRow(tree.getLeadSelectionRow() + 1);
                    break;
                case IntroWizard.L_TUTORIAL_1:
                    loadTutorialPage1();
                    break;
                case IntroWizard.L_TUTORIAL_2:
                    loadTutorialPage2();
                    break;
                case IntroWizard.L_TUTORIAL_3:
                    loadTutorialPage3();
                    break;
                default:
                    loadUseCasePage(((DefaultMutableTreeNode) tse.getNewLeadSelectionPath().getLastPathComponent()).getUserObject());
                    break;
                }
            } else {
                loadWelcomePage();
            }
        });
    }

    private void loadWelcomePage() {
        resetContent();
        addText("<h1>How do I...</h1>", "span, align center");

        JPanel content = new JPanel(new GridLayout(0, 2, 20, 20));
        for(final UseCase useCase : UseCase.values()){
            JButton button = new JButton(useCase.toString());
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.selectRow(useCase.toString());
                }
            });
            content.add(button);
        }
        addPanel(content, "span, align center, width 450px, height 520px");

        setNextButton(L_MORE_INFO, true, MORE_INFO);
    }

    private void loadUseCasePage(Object lastPathComponent) {
        resetContent();
        if(lastPathComponent instanceof UseCase){
            UseCase useCase = (UseCase) lastPathComponent;

            try {
                UndoableTextField contentText = new UndoableTextField();
                contentText.setEditable(false);
                contentText.setPage(useCase.getContentURL());
                addPanel(new JScrollPane(contentText), "height 100%, width 100%-6px!, span");
            } catch (IOException e) {
                Dbg.fixme("Loading content of UC failed", e);
            }
            setNextButton(L_MORE_INFO, true, MORE_INFO);

        }else{
            throw new IllegalArgumentException("object is not a use-case instance");
        }
    }

    private void loadTutorialPage1(){
        resetContent();
        addPanel(new JLabel(new ImageIcon(getClass().getResource("/gfx/tutorial1.png"))), SPAN_CENTER);

        setNextButton(L_MORE_INFO, true, MORE_INFO);
    }

    private void loadTutorialPage2(){
        resetContent();
        addPanel(new JLabel(new ImageIcon(getClass().getResource("/gfx/tutorial2.png"))), SPAN_CENTER);

        setNextButton(L_MORE_INFO, true, MORE_INFO);
    }

    private void loadTutorialPage3(){
        resetContent();
        addPanel(new JLabel(new ImageIcon(getClass().getResource("/gfx/tutorial3.png"))), SPAN_CENTER);

        setNextButton(L_MORE_INFO, true, MORE_INFO);
    }


    @Override
    protected void executeAction(int type){
        if(type == CLOSE_WINDOW){
            frame.dispose();

        }else if(type == MORE_INFO) {
            DesignGUI.get().openURL(Config.APP_DEFAULT_URL);
        }
    }
    @Override public void notifyChange(String elementName, String value) {}
    @Override public void notifyChange(String elementName, boolean value) {}
    @Override public void notifyChange(String elementName, boolean[] values) {}
}
