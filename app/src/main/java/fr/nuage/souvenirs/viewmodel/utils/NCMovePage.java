package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;

import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.PageNC;

import static fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList.API_ALBUM_URL;

public class NCMovePage extends RemoteOperation {

    private static final String TAG = NCMovePage.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;
    private PageNC page;
    private int pos;

    public NCMovePage(AlbumNC album, PageNC page, int pos) {
        this.album = album;
        this.page = page;
        this.pos = pos;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PostMethod postMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            postMethod = new PostMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString() + "/page/" + page.getId().toString() + "/pos/" + String.valueOf(pos));
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = postMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result") && (respJSON.getString("result").equals("success"))) {
                    result = new RemoteOperationResult(true, postMethod);
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
            Log_OC.e(TAG, "Move page failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();
        }
        return result;
    }
}
