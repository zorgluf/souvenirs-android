package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONObject;

import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.PageNC;

import static fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList.API_ALBUM_URL;

public class NCCreatePage extends RemoteOperation {

    private static final String TAG = NCCreatePage.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private AlbumNC album;
    private PageNC page;
    private int position;

    public NCCreatePage(AlbumNC album, PageNC page, int position) {
        this.album = album;
        this.page = page;
        this.position = position;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PutMethod putMethod = null;
        RemoteOperationResult result;

        try {
            // remote request
            putMethod = new PutMethod(client.getBaseUri() + API_ALBUM_URL + "/" + album.getId().toString() + "/page/" + position);
            putMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            //send album infos values
            putMethod.setRequestHeader("Content-Type","application/json");
            JSONObject reqJson = new JSONObject();
            reqJson.put("infos",page.toJSON());
            StringRequestEntity entity = new StringRequestEntity(reqJson.toString());
            putMethod.setRequestEntity(entity);

            int status = client.executeMethod(putMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = putMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                if (respJSON.has("result")) {
                    result = new RemoteOperationResult(true, putMethod);
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
            Log_OC.e(TAG, "Create page failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (putMethod != null)
                putMethod.releaseConnection();
        }
        return result;
    }
}
