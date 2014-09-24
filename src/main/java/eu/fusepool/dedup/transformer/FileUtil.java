package eu.fusepool.dedup.transformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class FileUtil {
	
	public static File inputStreamToFile(InputStream in) throws IOException {
        OutputStream out = null;
        try {
            File temp = File.createTempFile("temp-", ".txt");
            out = new FileOutputStream(temp);
            IOUtils.copy(in, out);
            return temp;
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            out.close();
        }
    }

}
