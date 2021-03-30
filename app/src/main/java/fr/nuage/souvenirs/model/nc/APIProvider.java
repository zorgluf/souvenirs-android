package fr.nuage.souvenirs.model.nc;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import retrofit2.NextcloudRetrofitApiBuilder;

public class APIProvider {

    private NCAPI mApi = null;
    private NextcloudAPI nextcloudAPI = null;
    private static APIProvider apiProvider = null;

    private APIProvider(Context context) {
        SingleSignOnAccount ssoAccount = null;
        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(context);
        } catch (NextcloudFilesAppAccountNotFoundException e) {
            Log.w("NCAPI","Selected Nextcloud account not found.",e);
            return;
        } catch (NoCurrentAccountSelectedException e) {
            Log.w("NCAPI","No Nextcloud account selected.",e);
            return;
        }

        NextcloudAPI.ApiConnectedListener callback = new NextcloudAPI.ApiConnectedListener() {
            @Override
            public void onConnected() {
                // ignore
            }
            @Override
            public void onError(Exception ex) {
                //on error, just log (FIXME)
                Log.w("NCAPI","Nextcloud API connection error.");
            }
        };

        nextcloudAPI = new NextcloudAPI(context, ssoAccount, getGson(), callback);
        mApi = new NextcloudRetrofitApiBuilder(nextcloudAPI, NCAPI.mApiEndpoint).create(NCAPI.class);

    }

    public static void init(Context context) {
        apiProvider = new APIProvider(context);
    }

    public static NCAPI getApi() {
        return apiProvider.mApi;
    }

    public static NextcloudAPI getNextcloudApi() { return  apiProvider.nextcloudAPI; }

    public Gson getGson() {

        return new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyyMMddHHmmss")
                .create();
    }

    //some response POJO classes
    public static class AssetProbeResult {
        String status;
        String path;
        int size;
    }

    public static class AlbumResp {
        UUID id;
        String name;
        Date date;
        Date lastEditDate;
        Date pagesLastEditDate;
        String albumImage;
        boolean isShared;
        String shareToken;
        String defaultStyle;
        List<PageResp> pages;
    }

    public static class PageResp {
        List<ElementResp> elements;
        UUID id;
        Date lastEditDate;
    }

    public static class ElementResp {
        Integer left;
        Integer right;
        Integer top;
        Integer bottom;
        UUID id;
        @SerializedName("class")
        String className;
        //for imageelement
        @SerializedName("image")
        String imagePath;
        @SerializedName("mime")
        String mimeType;
        int transformType;
        int zoom;
        int offsetX;
        int offsetY;
        //for textelement
        String text;
    }

}
