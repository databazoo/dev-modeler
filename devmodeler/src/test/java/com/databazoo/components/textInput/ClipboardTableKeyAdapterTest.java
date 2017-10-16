package com.databazoo.components.textInput;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static org.junit.Assert.assertEquals;

public class ClipboardTableKeyAdapterTest {
    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void pasteFromClipboard() throws Exception {
        String text;
        FormattedClickableTextField textField = new FormattedClickableTextField(Project.getCurrent());

        text = "SELECT count(*) FROM myTable";
        textField.setText(text);
        textField.setCaretPosition(text.length());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        new ClipboardTableKeyAdapter(textField).pasteFromClipboard();
        assertEquals(text, textField.getText());

        text = "\n\nSELECT count(*)\nFROM myTable\nORDER BY id;";
        textField.setText(text);
        textField.setCaretPosition(text.length());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        new ClipboardTableKeyAdapter(textField).pasteFromClipboard();
        assertEquals(text, textField.getText());

        text = "AA\tBBB\tCCC\nDDD\tEE\tFF\n";
        textField.setText(text);
        textField.setCaretPosition(text.length());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        new ClipboardTableKeyAdapter(textField).pasteFromClipboard();
        assertEquals("('AA','BBB','CCC'),\n('DDD','EE','FF')", textField.getText());

        text = "AA\t12.5\nBBB\t16\n";
        textField.setText(text);
        textField.setCaretPosition(text.length());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        new ClipboardTableKeyAdapter(textField).pasteFromClipboard();
        assertEquals("('AA',12.5),\n('BBB',16)", textField.getText());

        text = "\tSELECT lg.lottery_game_dows, lg.lottery_game_times\n" +
                "\tINTO _dows, _times\n" +
                "\tFROM lotteries.lst_lottery_games lg\n" +
                "\tWHERE lg.id_lottery_game = NEW.id_lottery_game;\n";
        textField.setText(text);
        textField.setCaretPosition(text.length());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        new ClipboardTableKeyAdapter(textField).pasteFromClipboard();
        assertEquals("\tSELECT lg.lottery_game_dows, lg.lottery_game_times\n" +
                "\tINTO _dows, _times\n" +
                "\tFROM lotteries.lst_lottery_games lg\n" +
                "\tWHERE lg.id_lottery_game = NEW.id_lottery_game;\n", textField.getText());
    }

}