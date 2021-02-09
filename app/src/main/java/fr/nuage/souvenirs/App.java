package fr.nuage.souvenirs;

import android.app.Application;
import android.content.Context;

import fr.nuage.souvenirs.viewmodel.utils.NCUtils;


public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        //init Utils
        NCUtils.init(base);
    }
}
