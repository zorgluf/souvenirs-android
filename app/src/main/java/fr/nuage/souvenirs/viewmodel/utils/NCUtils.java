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

import androidx.lifecycle.LiveData;

import fr.nuage.souvenirs.SettingsActivity;

public class NCUtils {

    private static NCEnabledLiveData isNCEnable;

    public static void init(Context context) {
        isNCEnable = new NCEnabledLiveData(context);
    }

    public static LiveData<Boolean> getIsNCEnable() {
        return isNCEnable;
    }

    public static void updateNCState() {
        isNCEnable.updateState();
    }


    private static class NCEnabledLiveData extends LiveData<Boolean> {

        private final Context context;
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

        private void updateState() {
            //get nextcloud settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (!prefs.getBoolean(SettingsActivity.NEXTCLOUD_ENABLED,false)) {
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

        private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateState();
            }
        };
    }

}
