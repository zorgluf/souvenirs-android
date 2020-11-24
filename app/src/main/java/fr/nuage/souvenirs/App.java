package fr.nuage.souvenirs;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.request.target.ViewTarget;


public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ViewTarget.setTagId(R.id.glide_tag);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
