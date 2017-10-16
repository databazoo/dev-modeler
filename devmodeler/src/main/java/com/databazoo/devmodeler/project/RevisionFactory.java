package com.databazoo.devmodeler.project;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.tools.Dbg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.databazoo.tools.Dbg.THIS_SHOULD_NEVER_HAPPEN;

public interface RevisionFactory {
    String REVISION_NUMBER_REGEX = ".*#([0-9]+).*";

    static Revision getCurrent(IConnection conn) throws OperationCancelException {
        return getCurrent(conn, false, null);
    }

    static Revision getCurrent(IConnection conn, String suggestedName) throws OperationCancelException {
        return getCurrent(conn, false, suggestedName);
    }

    static Revision getCurrentIncoming(IConnection conn) {
        try {
            return getCurrent(conn, true, null);
        } catch (OperationCancelException e) {
            Dbg.notImportantAtAll(THIS_SHOULD_NEVER_HAPPEN + " Incoming revisions are never cancelled.", e);
            return new Revision("cancelled", true);
        }
    }

    static Revision getCurrent(IConnection conn, boolean isIncoming, String suggestedName) throws OperationCancelException {
        Dbg.toFile("Looking for " + (isIncoming ? "incoming " : "") + "revision" + (conn != null ? " on " + conn.getName() : ""));
        List<Revision> revs = Project.getCurrent().revisions;
        Collections.reverse(revs);
        for (int i = 0; i < revs.size(); i++) {
            Revision rev = revs.get(i);
            if (rev.isArchived || rev.isClosed) {
                continue;
            }
            if (!rev.author.equals(Settings.getStr(Settings.L_REVISION_AUTHOR)) && !isIncoming) {
                continue;
            }
            if (rev.isIncoming != isIncoming) {
                continue;
            }
            if (!rev.getChangeDate().equals(Config.DATE_FORMAT.format(new Date()))) {
                continue;
            }
            if ((conn == null) != rev.appliedIn.isEmpty()) {
                continue;
            }
            if (conn != null && (rev.appliedIn.size() != 1 || !conn.getName().equals(rev.appliedIn.get(0).getName()))) {
                continue;
            }
            Dbg.toFile("Revision " + rev.getName() + " matched, appending");
            Collections.sort(revs);
            return rev;
        }
        Revision rev = new Revision("#" + getNextID(revs) + (suggestedName != null ? " - " + suggestedName : ""), isIncoming);
        if (!isIncoming && Settings.getBool(Settings.L_REVISION_NEW_REV_NAME)) {
            rev.askForName();
        }
        Dbg.toFile("Revision " + rev.getName() + " created");
        revs.add(rev);

        if (conn != null) {
            rev.appliedIn.add(conn);
        }
        DifferenceView.instance.updateRevisionTable();
        return rev;
    }

    static int getNextID(List<Revision> revs) {
        int highestRev = 0;
        for (Revision rev : new ArrayList<>(revs)) {
            int revNum = 1;
            if (rev.getName().matches(REVISION_NUMBER_REGEX)) {
                revNum = Integer.parseInt(rev.getName().replaceAll(REVISION_NUMBER_REGEX, "$1"));
            }
            if (revNum > highestRev) {
                highestRev = revNum;
            }
        }
        return highestRev + 1;
    }
}
