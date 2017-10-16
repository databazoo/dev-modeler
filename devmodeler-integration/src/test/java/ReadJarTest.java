import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ReadJarTest {

    private static final String NAME = "/home/boris/.ddm/launch4j.jar";

    @Test
    public void readJar() throws Exception {
        Enumeration<? extends ZipEntry> entries = new ZipFile(NAME).entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if(entry.isDirectory()){
                // TODO
            } else {
                //entry.get
            }
        }
    }

    @Test
    public void readJar2() throws Exception {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(NAME));
        for (ZipEntry entry; (entry = zipInputStream.getNextEntry()) != null;) {
            if (!entry.isDirectory()) {
                if(entry.getName().endsWith(".class")) {
                    System.out.println(entry.getName());
                    /*Scanner sc = new Scanner(zipInputStream);
                    while (sc.hasNextLine()) {
                        System.out.println(sc.nextLine());
                    }*/
                }
            }
        }
        zipInputStream.close();
    }
}
