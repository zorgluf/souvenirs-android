package fr.nuage.souvenirs.model.nc;

import android.net.Uri;
import android.util.Log;


import com.nextcloud.android.sso.aidl.NextcloudRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class Utils {

    public static boolean downloadFile(String remotePath, String localFolderPath, String localFileName) {
        //if local folder do not exist, create it
        if (!new File(localFolderPath).exists()) {
            new File(localFolderPath).mkdirs();
        }

        //make webdav call
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("GET")
                .setUrl(Uri.encode("/remote.php/webdav/"+remotePath,"/"))
                .build();

        //write response to file
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                inputStream = APIProvider.getNextcloudApi().performNetworkRequest(nextcloudRequest);
                outputStream = new FileOutputStream(new File(localFolderPath,localFileName));
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }
                outputStream.flush();
                return true;
            } catch (Exception e) {
                Log.w("Download file",e);
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            Log.w("Download file",e);
            return false;
        }
    }

    public static boolean uploadFile(String remoteFile, String localPath) {
        //read file
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(localPath);
            //make webdav call
            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod("PUT")
                    .setUrl(Uri.encode("/remote.php/webdav/"+remoteFile,"/"))
                    .setRequestBodyAsStream(inputStream)
                    .build();
            APIProvider.getNextcloudApi().performNetworkRequest(nextcloudRequest);
            return true;
        } catch (Exception e) {
            Log.w("Download file",e);
            return false;
        }
    }
}
