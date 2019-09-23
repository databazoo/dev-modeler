
package com.databazoo.devmodeler.plugincontrol;

import com.databazoo.tools.Dbg;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 *
 * @author bobus
 */
class PluginClassLoader extends ClassLoader {
    private final File directory;
	private ZipInputStream zipInputStream;
	private long zipEntrySize;

	/** The constructor. Just initialize the directory */
    PluginClassLoader (File dir) {
		directory = dir;
	}

    /** A convenience method that calls the 2-argument form of this method */
	@Override
    public Class loadClass (String name) throws ClassNotFoundException {
		return loadClass(name, true);
    }

    /**
     * This is one abstract method of ClassLoader that all subclasses must
     * define. Its job is to load an array of bytes from somewhere and to
     * pass them to defineClass(). If the resolve argument is true, it must
     * also call resolveClass(), which will do things like verify the presence
     * of the superclass. Because of this second step, this method may be called to
     * load superclasses that are system classes, and it must take this into account.
     */
	@Override
    public Class<?> loadClass (String classname, boolean resolve) throws ClassNotFoundException {
		try {
			// Our ClassLoader superclass has a built-in cache of classes it has
			// already loaded. So, first check the cache.
			Class<?> c = findLoadedClass(classname);

			// After this method loads a class, it will be called again to
			// load the superclasses. Since these may be system classes, we've
			// got to be able to load those too. So try to load the class as
			// a system class (i.e. from the CLASSPATH) and ignore any errors
			if (c == null) {
				try {
					c = findSystemClass(classname);
				} catch (Exception e) {
					Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
				}
			}

			// If the class wasn't found by either of the above attempts, then
			// try to load it from a file in (or beneath) the directory
			// specified when this ClassLoader object was created. Form the
			// filename by replacing all dots in the class name with
			// (platform-independent) file separators and by adding the ".class" extension.
			if (c == null) {
				c = getaClassFromFile(classname, new File(directory, classname.replace('.', File.separatorChar) + ".class"));
			}

			// If the resolve argument is true, call the inherited resolveClass method.
			if (resolve) {
				resolveClass(c);
			}

			return c;

		} catch (Exception ex) {
			throw new ClassNotFoundException(ex.toString(), ex);
		}
	}

	private Class getaClassFromFile(String classname, File file) throws IOException {
		try (InputStream inputStream = zipInputStream == null ? new FileInputStream(file) : zipInputStream) {
			int length = zipInputStream == null ? (int) file.length() : (int) zipEntrySize;

            byte[] classbytes = new byte[length];
            try (DataInputStream in = new DataInputStream(inputStream)) {
                in.readFully(classbytes);
            }

            // Now call an inherited method to convert those bytes into a Class
            return defineClass(classname, classbytes, 0, length);
        }
	}

	void setZipInputStream(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
	}

	void setZipEntrySize(long zipEntrySize) {
		this.zipEntrySize = zipEntrySize;
	}
}
