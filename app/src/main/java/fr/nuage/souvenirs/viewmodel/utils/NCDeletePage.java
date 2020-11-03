package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;

import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.PageNC;

import static fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList.API_ALBUM_URL;

public class NCDeletePage extends RemoteOperation {

    private static final String TAG = NCDeletePage.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;
    private PageNC page;

    public NCDeletePage(AlbumNC album, PageNC page) {
        this.album = album;
        this.page = page;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        DeleteMethod deleteMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            deleteMethod = new DeleteMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString() + "/page/" + page.getId().toString());
            deleteMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(deleteMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = deleteMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result")) {
                    result = new RemoteOperationResult(true, deleteMethod);
                } else {
                    result = new RemoteOperationResult(false, deleteMethod);
                    client.exhaustResponse(deleteMethod.getResponseBodyAsStream());
                }
            } else {
                result = new RemoteOperationResult(false, deleteMethod);
                client.exhaustResponse(deleteMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Delete page failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (deleteMethod != null)
                deleteMethod.releaseConnection();
        }
        return result;
    }
}
