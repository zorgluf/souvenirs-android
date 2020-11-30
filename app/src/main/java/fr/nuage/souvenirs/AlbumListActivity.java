package fr.nuage.souvenirs;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;

import java.io.File;

import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class AlbumListActivity extends AppCompatActivity  {

    public static final String CHANNEL_ID = "1";

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        //init utils
        NCUtils.init(getApplicationContext());

        //set default prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String albumPathPref = prefs.getString(SettingsActivity.ALBUMS_PATH, null);
        if (albumPathPref == null || albumPathPref == "") {
            File path = new File(getExternalFilesDir(null),"albums");
            path.mkdirs();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SettingsActivity.ALBUMS_PATH, path.getPath());
            editor.commit();
        }

        setContentView(R.layout.activity_album_list);

        NavController navController = Navigation.findNavController(this,R.id.main_navhost);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationUI.setupWithNavController(toolbar,navController);

    }



    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void transparentAppbar(boolean activate) {
        if (activate) {
            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
            appBarLayout.setBackgroundColor(Color.TRANSPARENT);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setBackgroundColor(Color.TRANSPARENT);
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_navhost);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) navHostFragment.getView().getLayoutParams();
            params.setBehavior(null);
        } else {
            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
            appBarLayout.setBackgroundColor(getResources().getColor(R.color.primaryColor));
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setBackgroundColor(getResources().getColor(R.color.primaryColor));
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_navhost);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) navHostFragment.getView().getLayoutParams();
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior(this, null));
        }
    }


}
