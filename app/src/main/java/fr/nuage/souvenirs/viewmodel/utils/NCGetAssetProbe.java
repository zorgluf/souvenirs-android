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

public class NCGetAssetProbe extends RemoteOperation {

    private static final String TAG = NCGetAssetProbe.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;
    private String assetPath;

    public NCGetAssetProbe(AlbumNC album, String assetPath) {
        this.album = album;
        this.assetPath = assetPath;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetMethod getMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            getMethod = new GetMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString() + "/assetprobe/" + assetPath);
            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(getMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = getMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result")) {
                    JSONObject resultJSON = respJSON.getJSONObject("result");
                    if (resultJSON.has("status")) {
                        String assetStatus = resultJSON.getString("status");
                        if (assetStatus.equals("ok")) {
                            result = new RemoteOperationResult(true, getMethod);
                            if (resultJSON.has("fullpath")) {
                                result.setSingleData(resultJSON.getString("fullpath"));
                            } else {
                                result.setSingleData("");
                            }
                        } else if (assetStatus.equals("missing")) {
                            result = new RemoteOperationResult(false, getMethod);
                            if (resultJSON.has("suggested_path")) {
                                result.setSingleData(resultJSON.getString("suggested_path"));
                            } else {
                                result.setSingleData("");
                            }
                        } else {
                            result = new RemoteOperationResult(false, getMethod);
                        }
                    } else {
                        result = new RemoteOperationResult(false, getMethod);
                    }
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
