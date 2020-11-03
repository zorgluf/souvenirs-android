package fr.nuage.souvenirs.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;

public class Utils {

    public static String getRelativePath(String albumPath, String absolutePath) {
        return new File(albumPath).toURI().relativize(new File(absolutePath).toURI()).getPath();
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static class NextcloudFile {

        private String filePath;
        private boolean isDir;
        private long modDate;

        public NextcloudFile(String filePath, boolean isDir, long modDate) {
            this.filePath = filePath;
            this.isDir = isDir;
            this.modDate = modDate;
        }

        public String getFilePath() {
            return filePath;
        }

        public boolean isDir() {
            return isDir;
        }

        public long getModDate() {
            return modDate;
        }
    }
}
