package com.databazoo.tools;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

import us.monoid.web.JSONResource;

public class RestClientTest {

    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        Config.getPassword();

        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().setLoaded();
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void getJSON() throws Exception {
        JSONResource json = RestClient.getInstance().getJSON("version",
                "generation", "1.0",
                "username", UIConstants.getUsernameWithOS(),
                "version", UIConstants.getVersionWithEnvironment());
        String available = json.get("available").toString();
        assertEquals("1.0.6", available);
    }

    @Test
    public void getJSONMap() throws Exception {
        LinkedHashMap<String, String> args = new LinkedHashMap<>();
        args.put("generation", "1.0");
        args.put("username", UIConstants.getUsernameWithOS());
        args.put("version", UIConstants.getVersionWithEnvironment());
        JSONResource json = RestClient.getInstance().getJSON("version", args);
        String available = json.get("available").toString();
        assertEquals("1.0.6", available);
    }

    @Test
    public void postJSON() throws Exception {
        JSONResource json = RestClient.getInstance().postJSON("version",
                "generation", "1.0",
                "username", UIConstants.getUsernameWithOS(),
                "version", UIConstants.getVersionWithEnvironment());
        String available = json.get("available").toString();
        assertEquals("1.0.6", available);
    }

    @Test
    public void postJSONMap() throws Exception {
        LinkedHashMap<String, String> args = new LinkedHashMap<>();
        args.put("generation", "1.0");
        args.put("username", UIConstants.getUsernameWithOS());
        args.put("version", UIConstants.getVersionWithEnvironment());
        JSONResource json = RestClient.getInstance().postJSON("version", args);
        String available = json.get("available").toString();
        assertEquals("1.0.6", available);
    }

}