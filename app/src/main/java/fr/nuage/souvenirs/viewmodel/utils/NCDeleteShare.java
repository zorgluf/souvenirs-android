package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.json.JSONObject;

import java.util.ArrayList;


public class NCDeleteShare extends RemoteOperation {

    private static final String TAG = NCDeleteShare.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String CREATE_SHARE_URL = "/index.php/apps/souvenir/api/share";

    private String token;

    /**
     * Constructor
     */
    public NCDeleteShare(String token) {
        this.token = token;
    }

    /**
     * @param client Client object
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        DeleteMethod deleteMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            deleteMethod = new DeleteMethod(client.getBaseUri() + CREATE_SHARE_URL + "/" + token);
            deleteMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(deleteMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = deleteMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                String actionResult = respJSON.getString("action");
                if (actionResult.equals("success")) {
                    result = new RemoteOperationResult(true, deleteMethod);
                } else {
                    result = new RemoteOperationResult(false, deleteMethod);
                    if (respJSON.has("error")) {
                        result.setSingleData(respJSON.getString("error"));
                    }
                    client.exhaustResponse(deleteMethod.getResponseBodyAsStream());
                }
            } else {
                result = new RemoteOperationResult(false, deleteMethod);
                client.exhaustResponse(deleteMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Deletion of album share failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (deleteMethod != null)
                deleteMethod.releaseConnection();
        }
        return result;
    }

}