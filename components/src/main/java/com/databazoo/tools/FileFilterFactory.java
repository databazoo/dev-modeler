
package com.databazoo.tools;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.databazoo.components.UIConstants;

/**
 * Returns filters for JFileChooser
 * @author bobus
 */
public final class FileFilterFactory {

	private static final PngFilter PNG_FILTER = new PngFilter();
	private static final JpegFilter JPEG_FILTER = new JpegFilter();
	private static final XmlFilter XML_FILTER = new XmlFilter();
	private static final SqlFilter SQL_FILTER = new SqlFilter();
	private static final DProjFilter D_PROJ_FILTER = new DProjFilter();
	private static final ExecutableFilter EXECUTABLE_FILTER = new ExecutableFilter();
	private static final CsvFilter CSV_FILTER = new CsvFilter();
	private static final XlsFilter XLS_FILTER = new XlsFilter();

	private FileFilterFactory() {}

	/**
	 * Filter for supported image files.
	 *
	 * @param withAllOption include "All supported files" option?
	 * @return FileFilter array
	 */
	public static FileFilter[] getImagesFilter(boolean withAllOption){
		if(withAllOption) {
			return new FileFilter[]{new AllFilter("All image files (.png, .jpg, .jpeg)", PNG_FILTER, JPEG_FILTER), PNG_FILTER, JPEG_FILTER};
		} else {
			return new FileFilter[]{PNG_FILTER, JPEG_FILTER};
		}
	}

	/**
	 * Filter for XML files.
	 *
	 * @return FileFilter
	 */
	public static FileFilter getXmlFilter(){
		return XML_FILTER;
	}

	/**
	 * Filter for SQL files.
	 *
	 * @return FileFilter
	 */
	public static FileFilter getSqlFilter(){
		return SQL_FILTER;
	}

	/**
	 * Filter for supported project files.
	 *
	 * @param withAllOption include "All supported files" option?
	 * @return FileFilter array
	 */
	public static FileFilter[] getImportFilter(boolean withAllOption){
		if(withAllOption) {
			return new FileFilter[]{new AllFilter("All project files (.dproj, .xml)", D_PROJ_FILTER, XML_FILTER), D_PROJ_FILTER, XML_FILTER};
		} else {
			return new FileFilter[]{D_PROJ_FILTER, XML_FILTER};
		}
	}

	/**
	 * Filter for supported executable files.
	 *
	 * @return FileFilter
	 */
	public static FileFilter getExecutableFilter(){
		return EXECUTABLE_FILTER;
	}

	/**
	 * Filter for CSV files.
	 *
	 * @return FileFilter
	 */
	public static FileFilter getCsvFilter(){
		return CSV_FILTER;
	}

	/**
	 * Filter for supported data files.
	 *
	 * @param withAllOption include "All supported files" option?
	 * @return FileFilter array
	 */
	public static FileFilter[] getXlsCsvFilter(boolean withAllOption){
		if(withAllOption) {
			return new FileFilter[]{new AllFilter("All data files (.xls, .csv)", XLS_FILTER, CSV_FILTER), XLS_FILTER, CSV_FILTER};
		} else {
			return new FileFilter[]{XLS_FILTER, CSV_FILTER};
		}
	}

	private static class JpegFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().matches(".*\\.jpe?g"); }
		@Override public String getDescription(){ return "Joint Photographic Experts Group (.jpg, .jpeg)"; }
	}

	private static class PngFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".png"); }
		@Override public String getDescription(){ return "Portable Network Graphics (.png)"; }
	}

	private static class XmlFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".xml"); }
		@Override public String getDescription(){ return "Extensible Markup Language (.xml)"; }
	}

	private static class SqlFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".sql"); }
		@Override public String getDescription(){ return "Structured Query Language (.sql)"; }
	}

	private static class DProjFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".dproj"); }
		@Override public String getDescription(){ return "Encrypted project files (.dproj)"; }
	}

	private static class ExecutableFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || (UIConstants.isWindows() && f.getName().matches(".*\\.(exe|bat)")) || !UIConstants.isWindows(); }
		@Override public String getDescription(){ return "Executable files"; }
	}

	private static class CsvFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".csv"); }
		@Override public String getDescription(){ return "Comma Separated Values (.csv)"; }
	}

	private static class XlsFilter extends FileFilter {
		@Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".xls"); }
		@Override public String getDescription(){ return "Spreadsheet files (.xls)"; }
	}

	/**
	 * Takes a description plus given filters and creates an Uber-filter.
	 */
	private static class AllFilter extends FileFilter {
		private final String description;
		private final FileFilter[] filters;

		private AllFilter(String description, FileFilter... filters) {
			this.description = description;
			this.filters = filters;
		}

		@Override
		public boolean accept(File f) {
			for(FileFilter fileFilter : filters){
				if(fileFilter.accept(f)){
					return true;
				}
			}
			return false;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}
}
