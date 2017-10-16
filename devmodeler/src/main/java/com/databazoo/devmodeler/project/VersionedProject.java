
package com.databazoo.devmodeler.project;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Implementation of automated versioning in projects
 * @author bobus
 */
abstract class VersionedProject extends ProjectIO {

	public static final String VERSIONING_MANUAL		= "manual";
	public static final String VERSIONING_GIT			= "GIT";
	public static final String VERSIONING_SVN			= "SVN";

	public static final String COMMIT_ON_CLOSE_ASK		= "on project close, ask for commit message";
	public static final String COMMIT_ON_CLOSE_SILENT	= "on project close, silently";
	public static final String COMMIT_AFTER_CHANGE		= "after each change, silently";

	public boolean showChangeNote = true;
	boolean changeHappened = false;
	Revision changedRevision;

	String verType = VERSIONING_MANUAL;
	String verURL;
	String verPath;
	String verUser;
	String verPass;
	String verCommitMode;
	String verExecutableGIT = "git";
	String verExecutableSVN = "svn";

	private String outputStd, outputErr;

	public String getVerType(){
		return verType;
	}
	public void setVerType(String value){
		if(value!=null && !value.isEmpty() && (VERSIONING_GIT+VERSIONING_SVN+VERSIONING_MANUAL).contains(value)){
			verType = value;
		}
	}
	public boolean isVerURLNull(){
		return verURL == null;
	}
	public String getVerURL(){
		return verURL == null ? "https://127.0.0.1/"+ projectName +"/trunk" : verURL;
	}
	public void setVerURL(String value){
		verURL = value==null || value.isEmpty() ? null : value;
	}
	public String getVerUser(){
		return verUser == null ? System.getProperty("user.name") : verUser;
	}
	public void setVerUser(String value){
		verUser = value==null || value.isEmpty() ? null : value;
	}
	public String getVerPass(){
		return verPass;
	}
	public void setVerPass(String value){
		verPass = value==null || value.isEmpty() ? null : value;
	}
	public String getVerPath(){
		return verPath==null ? revPath : verPath;
	}
	public void setVerPath(String value){
		verPath = value==null || value.isEmpty() ? null : value;
	}
	public String getVerCommitMode(){
		return verCommitMode==null ? COMMIT_AFTER_CHANGE : verCommitMode;
	}
	public void setVerCommitMode(String value){
		if(value!=null && !value.isEmpty() && (COMMIT_AFTER_CHANGE+COMMIT_ON_CLOSE_ASK+COMMIT_ON_CLOSE_SILENT).contains(value)){
			verCommitMode = value;
		}
	}
	public String getVerExecutableGIT(){
		return verExecutableGIT;
	}
	public void setVerExecutableGIT(String value){
		if(value!=null && !value.isEmpty()){
			verExecutableGIT = value;
		}
	}
	public String getVerExecutableSVN(){
		return verExecutableSVN;
	}
	public void setVerExecutableSVN(String value){
		if(value!=null && !value.isEmpty()){
			verExecutableSVN = value;
		}
	}

	private List<String> getBaseArgs(){
		List<String> args = new ArrayList<>();
		if(getVerPass()!=null){
			try {
				String absPath = new File(VersionedProject.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
				String libPath = UIConstants.DEBUG ? "/home/bobus/dist/lib/" : new File(absPath, "lib").getPath();
				File sshpass = new File(libPath, "sshpass");
				//Dbg.info(sshpass.getPath()+" found: "+sshpass.exists());
				if(!UIConstants.isWindows() && sshpass.exists()){
					args.add(sshpass.getPath());
					args.add("-p");
					args.add(getVerPass());
				}else{
					// TODO: check Windows, MacOS
				}
			} catch (Exception ex) {
				Dbg.fixme("could not load sshpass", ex);
			}
		}
		return args;
	}

	public synchronized void runCheckout(){
		Dbg.info("Checkout");
		List<String> args = getBaseArgs();

		switch(verType){
			case VERSIONING_SVN:
				args.add("svn");
				args.add("--username");
				args.add(getVerUser());
				if(getVerPass()!=null){
					args.add("--password");
					args.add(getVerPass());
				}
				args.add("--non-interactive");
				args.add("checkout");
				args.add(getVerURL());
				args.add(getVerPath());
				break;
			case VERSIONING_GIT:
				args.add("git");
				args.add("clone");
				args.add(getVerURL());
				args.add(getVerPath());
				break;

			default: return;
		}
		exec(args.toArray());
	}

	synchronized void runPull(){
		//Dbg.info("Pull");
		List<String> args = getBaseArgs();

		switch(verType){
			case VERSIONING_SVN:
				args.add("svn");
				args.add("--username");
				args.add(getVerUser());
				if(getVerPass()!=null){
					args.add("--password");
					args.add(getVerPass());
				}
				args.add("--non-interactive");
				args.add("update");
				break;
			case VERSIONING_GIT:
				args.add("git");
				args.add("pull");
				break;

			default: return;
		}
		exec(args.toArray());
	}

	synchronized void runPush(){
		//Dbg.info("Push");
		if(changeHappened){
			changeHappened = false;

			List<String> args = getBaseArgs();

			switch(verType){
				case VERSIONING_SVN:
					if(UIConstants.isWindows()){
						// TODO: check Windows
						exec(new String[]{"cmd", "/c", "for /f \"usebackq tokens=2*\" %%i in (`svn status ^| findstr /r \"^\\?\"`) do svn add \"%%i %%j\""});
						exec(new String[]{"cmd", "/c", "for /f \"usebackq tokens=2*\" %%i in (`svn status ^| findstr /r \"^\\!\"`) do svn rm \"%%i %%j\""});
					}else{
						exec(new String[]{"/bin/sh", "-c", "svn status | grep '^\\?' | sed 's/? *//' | xargs -I% svn add %"});
						exec(new String[]{"/bin/sh", "-c", "svn status | grep '^\\!' | sed 's/! *//' | xargs -I% svn rm %"});
					}

					args.add("svn");
					args.add("--username");
					args.add(getVerUser());
					if(getVerPass()!=null){
						args.add("--password");
						args.add(getVerPass());
					}
					args.add("--non-interactive");
					args.add("commit");
					break;

				case VERSIONING_GIT:
					args.add("git");
					args.add("commit");
					args.add("-a");
					break;

				default: return;
			}

			switch(getVerCommitMode()){
				case COMMIT_AFTER_CHANGE:
				case COMMIT_ON_CLOSE_SILENT:
					args.add("--message");
					args.add(changedRevision.getName());
					break;

				default:
					String commitMessage = JOptionPane.showInputDialog(DesignGUI.instance.frame, "Committing changes on project "+ projectName +"\n\nYour commit message:", changedRevision.getName());
					if(commitMessage == null || commitMessage.isEmpty()){
						return;
					}else{
						args.add("--message");
						args.add(commitMessage);
					}
			}

			int ret = exec(args.toArray());

			if(verType.equals(VERSIONING_GIT)){
				if(ret == 128 && (outputStd+outputErr).contains("git config --global user.email")){
					List<String> configArgs = getBaseArgs();
					configArgs.add("git");
					configArgs.add("config");
					configArgs.add("user.email");
					configArgs.add(Settings.getStr(Settings.L_REVISION_AUTHOR));
					exec(configArgs.toArray());

					configArgs = getBaseArgs();
					configArgs.add("git");
					configArgs.add("config");
					configArgs.add("user.name");
					configArgs.add(Settings.getStr(Settings.L_REVISION_AUTHOR));
					exec(configArgs.toArray());

					exec(args.toArray());
				}
				List<String> pushArgs = getBaseArgs();
				pushArgs.add("git");
				pushArgs.add("push");
				exec(pushArgs.toArray());
			}
		}
	}

	private int exec(Object[] args){
		try {
			final Process p = Runtime.getRuntime().exec(Arrays.copyOf(args, args.length, String[].class), new String[0], new File(revPath));

			Schedule.inWorker(() -> {
                String s;
                StringBuilder outputSB = new StringBuilder();
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    while ((s = input.readLine()) != null) {
                        Dbg.info(s);
						outputSB.append(s);
                    }
                } catch (IOException ex) {
                    Dbg.fixme("std input failed", ex);
                }
				outputStd += outputSB.toString();
            });
			Schedule.inWorker(() -> {
                String s;
				StringBuilder outputSB = new StringBuilder();
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                try {
                    while ((s = input.readLine()) != null) {
                        Dbg.info(s);
						outputSB.append(s);
                    }
                } catch (IOException ex) {
                    Dbg.fixme("error input failed", ex);
                }
				outputErr += outputSB.toString();
            });

			int result = p.waitFor();
			Dbg.info("Returned: "+result);
			return result;
		}
		catch (Exception ex) {
			Dbg.fixme("Call "+Arrays.toString(args)+" failed", ex);
			return -1;
		}
	}

	public File getFlywayFolder() {
		return new File(flywayPath != null && !flywayPath.isEmpty() ? flywayPath : revPath);
	}

	public String getFlywayPath() {
		return flywayPath != null ? flywayPath : "";
	}

	public void setFlywayPath(String flywayPath) {
		this.flywayPath = flywayPath;
	}
}
