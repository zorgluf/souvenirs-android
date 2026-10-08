package fr.nuage.souvenirs;

import android.app.Application;
import android.content.Context;

import fr.nuage.souvenirs.view.PanoView;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;


public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        //no PanoView exists yet, so every pano temp copy is a leftover
        PanoView.clearTempPanoFiles(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        //init Utils
        NCUtils.init(base);
    }
}
