package fr.nuage.souvenirs.viewmodel.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;

import java.io.File;
import java.util.UUID;

import fr.nuage.souvenirs.SettingsActivity;

public class NCUtils {

    public static NCEnabledLiveData isNCEnable;

    public static void init(Context context) {
        isNCEnable = new NCEnabledLiveData(context);
    }

    public static LiveData<Boolean> getIsNCEnable() {
        return isNCEnable;
    }

    public static boolean isNCSets(Context context) {
        return (getNCClient(context) != null);
    }

    public static OwnCloudClient getNCClient(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String nextcloudUrl = prefs.getString(SettingsActivity.NEXTCLOUD_URL,"");
        if (!nextcloudUrl.startsWith("http")) {
            nextcloudUrl = "https://" + nextcloudUrl;
        }
        String nextcloudLogin = prefs.getString(SettingsActivity.NEXTCLOUD_LOGIN,"");
        String nextcloudPasswd = prefs.getString(SettingsActivity.NEXTCLOUD_PASSWD,"");
        if ((nextcloudUrl.equals("")) || (nextcloudLogin.equals("")) || (nextcloudPasswd.equals(""))) {
            return null;
        }
        OwnCloudClient nClient = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(nextcloudUrl),context,true);
        nClient.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(nextcloudLogin,nextcloudPasswd));
        return nClient;
    }

    private static class NCEnabledLiveData extends LiveData<Boolean> {

        private Context context;
        private boolean NCEnabled = false;

        public NCEnabledLiveData(Context context) {
            this.context = context;
            postValue(NCEnabled);
        }

        @Override
        protected void onActive() {
            super.onActive();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            context.unregisterReceiver(networkReceiver);
        }

        private void setState(boolean state) {
            if (state != NCEnabled) {
                NCEnabled = state;
                postValue(state);
            }
        }

        private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //get nextcloud settings
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean(SettingsActivity.NEXTCLOUD_ENABLED,false)) {
                    if (prefs.getString(SettingsActivity.NEXTCLOUD_URL,"").equals("")) {
                        setState(false);
                        return;
                    }
                } else {
                    setState(false);
                    return;
                }
                if (prefs.getBoolean(SettingsActivity.NEXTCLOUD_WIFIONLY,true)) {
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = cm.getActiveNetworkInfo();
                    setState(info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
                } else {
                    setState(true);
                }
            }
        };
    }

}
