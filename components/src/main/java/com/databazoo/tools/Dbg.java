
package com.databazoo.tools;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.databazoo.components.UIConstants;

/**
 * Reporting tool that prints information strings into console and sends error reports to server.
 * @author bobus
 */
public final class Dbg {
	public static final String THIS_SHOULD_NEVER_HAPPEN = "This should never happen.";
	public static final String MANIPULATING_TEXT_OUTSIDE = "Manipulating text outside the document.";
	public static final String PLUGIN_MAY_NOT_AFFECT_APP = "Whatever happens in plugins can not affect the main app.";

	private static final File SETTINGS_DIR = new File(System.getProperty("user.home"), ".devmodeler");
	private static final Logger LOGGER = Logger.getLogger(Dbg.class.getName());

	static final File ERROR_DIR = new File(SETTINGS_DIR, "err");
	static final File LOG_FILE = new File(SETTINGS_DIR, "run.log");
	private static final int ERROR_DIR_SIZE = 30;

	private static boolean FIRST_FILE_WRITE = true;
	public static boolean sendCrashReports = true;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %5$s%6$s%n");
	}

	/**
	 * Report an error. Invokes the error reporting process.
	 *
	 * @param msg situation description
	 */
	public static void fixme(Object msg){
		Schedule.inWorker(() -> {
			String message = msg.toString();
			LOGGER.log(Level.SEVERE, message);
			toFile("FIX ME: " + message);
			if(!message.contains("Exception in thread AWT-EventQueue-0: GlyphView: Stale view: javax.swing.text.BadLocationException: Length must be positive")) {
				backupError(false);
			}
		});
	}

	/**
	 * Report an error. Invokes the error reporting process.
	 *
	 * @param msgObj situation description
	 * @param e reported exception
	 */
	public static void fixme(Object msgObj, Exception e){
		Schedule.inWorker(() -> {
			String message = msgObj.toString();
			ByteArrayOutputStream stack = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(stack));

			LOGGER.log(Level.SEVERE, message, e);
			toFile("FIX ME: " + message + "\n" + e.getMessage() + "\n" + stack.toString());
			backupError(false);
		});
	}

	/**
	 * Write to file or to std out in debug mode.
	 *
	 * @param msg message
	 */
	public static void info(Object msg){
		Schedule.inWorker(() -> {
			if(UIConstants.DEBUG) {
				LOGGER.log(Level.INFO, msg.toString());
			}
			toFile(msg);
		});
	}

	/**
	 * Write to file.
	 *
	 * @param msg message
	 */
	public static void toFile(Object msg) {
		outToFile(msg);
	}

	/**
	 * Write the last caller to file.
	 */
	public static void toFile(){
		outToFile(null);
	}

	private static void outToFile(final Object msg){
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElem = stackTrace[3].getClassName().equals(Dbg.class.getName()) ? stackTrace[4] : stackTrace[3];
		final String traceString = stackTraceElem.getClassName()+"."+stackTraceElem.getMethodName()+"("+stackTraceElem.getLineNumber()+")";

		Schedule.inWorker(() -> {
			if (SETTINGS_DIR.exists()) {
				if(FIRST_FILE_WRITE){
					FIRST_FILE_WRITE = false;
					if(LOG_FILE.exists()) {
						//noinspection ResultOfMethodCallIgnored
						LOG_FILE.delete();
					}
				}
				try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
					writer.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date())).append(traceString).append("\n");
					if(msg != null && !msg.equals("")){
						writer.append("\t").append(msg.toString()).append("\n");
					}
				} catch (Exception e) {
					Dbg.notImportant("Dbg could not write to file. No recovery possible, so just ignoring.", e);
				}
			}
		});
	}

	/**
	 * Copies the current run log into {@code ERROR_DIR} and reports to the server.
	 *
	 * Sending skipped in debug mode.
	 */
	public static void backupError(boolean forced) {
		if (!sendCrashReports) {
			return;
		}
		if(!UIConstants.DEBUG || forced){
			Schedule.reInvokeInWorker(Schedule.Named.DBG_BACKUP_ERROR_LOG, Schedule.TYPE_DELAY, Dbg::doBackupErrorLog);
		}
	}

	static void doBackupErrorLog() {
		FileChannel source = null;
		FileChannel destination = null;
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
            if(!ERROR_DIR.exists()) {
                //noinspection ResultOfMethodCallIgnored
                ERROR_DIR.mkdirs();
            }
            File destFile = new File(ERROR_DIR, "err"+new Date().getTime()+".log");
            if(!destFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFile.createNewFile();
            }

			inputStream = new FileInputStream(LOG_FILE);
			source = inputStream.getChannel();

			outputStream = new FileOutputStream(destFile);
			destination = outputStream.getChannel();

			destination.transferFrom(source, 0, source.size());

			RestClient.getInstance().postJSON("error",
					"content", new String(Files.readAllBytes(Paths.get(destFile.getAbsolutePath()))),
					"username", UIConstants.getUsernameWithOS(),
					"version", UIConstants.getVersionWithEnvironment()
			);

			//noinspection ResultOfMethodCallIgnored
            destFile.delete();

        } catch (Exception e){
            Dbg.notImportant("Dbg error backup failed", e);
		} finally {
        	try {
				if (source != null) {
					source.close();
				}
			} catch (IOException ex){
				Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, ex);
			}
			try {
				if (destination != null) {
					destination.close();
				}
			} catch (IOException ex){
				Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, ex);
			}
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ex){
				Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, ex);
			}
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException ex){
				Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, ex);
			}
        }

		cleanUpErrorDir();
	}

	public static void cleanUpErrorDir() {
		final File[] files = ERROR_DIR.listFiles();
		if(files != null && files.length > ERROR_DIR_SIZE){
			Arrays.stream(files)
					.sorted(Comparator.comparing(File::getName))
					.limit(files.length - ERROR_DIR_SIZE)
					.forEach(File::delete);
		}
	}

	/**
	 * Only write out in debug mode.
	 *
	 * @param message situation description
	 * @param e	the reported exception
	 */
	public static void notImportant(String message, Exception e) {
		if(UIConstants.DEBUG){
			LOGGER.log(Level.WARNING, "Not Important, but should be checked: " + message, e);
		}
	}

	/**
	 * Completely ignore such output. Important for Sonar to show we understand why this error happens.
	 *
	 * @param message situation description
	 * @param e	the reported exception
	 */
	public static void notImportantAtAll(String message, Exception e) {
		// > /dev/null
	}

	public static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			StringBuilder out = new StringBuilder();
			out.append("Exception in thread ").append(thread.getName()).append(": ").append(ex.getMessage());
			for(StackTraceElement elem : ex.getStackTrace()){
				out.append("\n").append(elem.toString());
			}
			Dbg.fixme(out.toString());
		}
	}
}
