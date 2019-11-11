
package com.databazoo.components;

import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author bobus
 */
public interface FileChooser {

	Map<File, File> CACHED_PATHS = new HashMap<>();

	static File show(String title, String buttonText, File initialFile, FileFilter fileFilter){
		return show(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, fileFilter);
	}

	static File show(String title, String buttonText, File initialFile, FileFilter[] fileFilters){
		return show(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, fileFilters);
	}

	static File show(String title, String buttonText, File initialFile, int selectionMode, boolean allowAllFilesFilter, FileFilter fileFilter){
		return show(title, buttonText, initialFile, selectionMode, allowAllFilesFilter, new FileFilter[]{ fileFilter });
	}

	static File show(String title, String buttonText, File initialFile, int selectionMode, boolean allowAllFilesFilter, FileFilter[] fileFilters){
		final AtomicReference<File> result = new AtomicReference<>();
		try {
			Schedule.waitInEDT(()-> {
				// Get chooser
				JFileChooser chooser = getFileChooser(title, buttonText);
				setPath(chooser, initialFile);
				setFilters(chooser, selectionMode, allowAllFilesFilter, fileFilters);

				// Show the dialog
				if (chooser.showOpenDialog(GCFrame.getActiveWindow()) == JFileChooser.APPROVE_OPTION) {
					result.set(chooser.getSelectedFile());
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Dbg.notImportant("File chooser interrupted", e);
		}
		return result.get();
	}

	static File showWithOverwrite(String title, String buttonText, File initialFile, FileFilter fileFilter){
		return showWithOverwrite(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, new FileFilter[]{ fileFilter }, true);
	}

	static File showWithOverwrite(String title, String buttonText, File initialFile, FileFilter[] fileFilters){
		return showWithOverwrite(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, fileFilters, true);
	}

	static File showWithOverwriteNoCache(String title, String buttonText, File initialFile, FileFilter fileFilter){
		return showWithOverwrite(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, new FileFilter[]{ fileFilter }, false);
	}

	static File showWithOverwriteNoCache(String title, String buttonText, File initialFile, FileFilter[] fileFilters){
		return showWithOverwrite(title, buttonText, initialFile, JFileChooser.FILES_ONLY, false, fileFilters, false);
	}

	static File showWithOverwrite(
			String title,
			String buttonText,
			File initialFile,
			int selectionMode,
			boolean allowAllFilesFilter,
			FileFilter[] fileFilters,
			boolean useCache
	){
		final AtomicReference<File> result = new AtomicReference<>();
		try {
			Schedule.waitInEDT(()-> {
				while (true) {
					// Get chooser
					JFileChooser chooser = getFileChooser(title, buttonText);
					setPath(chooser, getCachedPath(initialFile, useCache));
					setFilters(chooser, selectionMode, allowAllFilesFilter, fileFilters);

					// Show the dialog
					if (chooser.showOpenDialog(GCFrame.getActiveWindow()) == JFileChooser.APPROVE_OPTION) {
						File selectedFile = chooser.getSelectedFile();
						if (!selectedFile.exists() || confirmOverwrite()) {
							setCachedPath(initialFile, selectedFile, useCache);
							result.set(selectedFile);
							break;
						}
					} else {
						break;
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Dbg.notImportant("File chooser interrupted", e);
		}
		return result.get();
	}

	/*private*/ static void setFilters(JFileChooser chooser, int selectionMode, boolean allowAllFilesFilter, FileFilter[] fileFilters) {
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(selectionMode);
		chooser.setAcceptAllFileFilterUsed(allowAllFilesFilter);
		for(FileFilter fileFilter : fileFilters) {
            chooser.addChoosableFileFilter(fileFilter);
        }
	}

	/*private*/ static void setPath(JFileChooser chooser, File initialFile) {
		if(initialFile.isDirectory()){
            chooser.setCurrentDirectory(initialFile);
        }else{
            chooser.setCurrentDirectory(new File(initialFile.getParent()));
            chooser.setSelectedFile(initialFile);
        }
	}

	/*private*/ static boolean confirmOverwrite() {
		return JOptionPane.showOptionDialog(
                GCFrame.getActiveWindow(),
                "File already exists. Overwrite it?",
                "Overwrite file",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Overwrite", "Cancel"},
                "Overwrite"
        ) == 0;
	}

	/*private*/ static JFileChooser getFileChooser(String title, String buttonText) {
		JFileChooser chooser = new JFileChooser();
		chooser.setPreferredSize(new Dimension(chooser.getPreferredSize().width, 400));
		chooser.setDialogTitle(title);
		chooser.setApproveButtonText(buttonText);
		return chooser;
	}

	/*private*/ static File getCachedPath (File initialFile, boolean useCache) {
		if(useCache) {
			File cachedFile = CACHED_PATHS.get(getBaseDir(initialFile));
			return cachedFile != null ? cachedFile : initialFile;
		} else {
			return initialFile;
		}
	}

	/*private*/ static void setCachedPath (File initialFile, File selectedFile, boolean useCache) {
		if(useCache) {
			CACHED_PATHS.put(getBaseDir(initialFile), selectedFile);
		}
	}

	/*private*/ static File getBaseDir(File initialFile){
		if(initialFile.isDirectory()){
			return initialFile;
		}else{
			return new File(initialFile.getParent());
		}
	}
}
