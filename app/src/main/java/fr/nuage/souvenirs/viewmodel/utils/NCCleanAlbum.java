package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

import fr.nuage.souvenirs.model.nc.AlbumNC;

import static fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList.API_ALBUM_URL;

public class NCCleanAlbum extends RemoteOperation {

    private static final String TAG = NCCleanAlbum.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;

    public NCCleanAlbum(AlbumNC album) {
        this.album = album;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetMethod getMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            getMethod = new GetMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString() + "/cleanassets");
            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(getMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = getMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result")) {
                    result = new RemoteOperationResult(true, getMethod);
                } else {
                    result = new RemoteOperationResult(false, getMethod);
                }
            } else {
                result = new RemoteOperationResult(false, getMethod);

            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Get asset probe failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (getMethod != null)
                getMethod.releaseConnection();
        }
        return result;
    }
}
