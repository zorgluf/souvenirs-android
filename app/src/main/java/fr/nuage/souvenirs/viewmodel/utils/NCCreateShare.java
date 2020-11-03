package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.util.ArrayList;



public class NCCreateShare extends RemoteOperation {

    private static final String TAG = NCCreateShare.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String CREATE_SHARE_URL = "/index.php/apps/souvenir/api/share";

    private String albumId;

    /**
     * Constructor
     */
    public NCCreateShare(String albumId) {
        this.albumId = albumId;
    }

    /**
     * @param client Client object
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PostMethod postMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            postMethod = new PostMethod(client.getBaseUri() + CREATE_SHARE_URL);
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            postMethod.setParameter("albumId", albumId);

            int status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = postMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                String actionResult = respJSON.getString("action");
                if (actionResult.equals("success")) {
                    result = new RemoteOperationResult(true, postMethod);
                    String url = respJSON.getString("shareUrl");
                    ArrayList<Object> keys = new ArrayList<>();
                    keys.add(url);
                    result.setData(keys);
                } else {
                    result = new RemoteOperationResult(false, postMethod);
                    client.exhaustResponse(postMethod.getResponseBodyAsStream());
                }
            } else {
                result = new RemoteOperationResult(false, postMethod);
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Creation of album share failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();
        }
        return result;
    }

}