package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.AlbumsNC;

import static fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList.API_ALBUM_URL;

public class NCCreateAlbum extends RemoteOperation {

    private static final String TAG = NCCreateShare.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;

    public NCCreateAlbum(AlbumNC album) {
        this.album = album;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PutMethod putMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            putMethod = new PutMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString());
            putMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(putMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = putMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result")) {
                    result = new RemoteOperationResult(true, putMethod);
                    JSONObject albumJSON = respJSON.getJSONObject("result");
                    album.loadFromJson(albumJSON);
                    result.setSingleData(album);
                } else {
                    result = new RemoteOperationResult(false, putMethod);
                    client.exhaustResponse(putMethod.getResponseBodyAsStream());
                }
            } else {
                result = new RemoteOperationResult(false, putMethod);
                client.exhaustResponse(putMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Get album failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (putMethod != null)
                putMethod.releaseConnection();
        }
        return result;
    }
}
