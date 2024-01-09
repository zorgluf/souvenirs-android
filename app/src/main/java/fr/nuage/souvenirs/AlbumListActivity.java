package fr.nuage.souvenirs;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.chooser.ChooserTarget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class AlbumListActivity extends AppCompatActivity  {

    public static final String CHANNEL_ID = "1";
    public static final int DIRECT_SHARE_MAX_DAYS = 30;
    private static final int MAX_SHORTCUTS = 4;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        updateShorcuts();

        setContentView(R.layout.activity_album_list);

        NavController navController = Navigation.findNavController(this,R.id.main_navhost);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationUI.setupWithNavController(toolbar,navController);

    }

    private void updateShorcuts() {
        //create shortcuts for direct share targets
        //first remove all shortcuts
        ShortcutManagerCompat.removeAllDynamicShortcuts(getApplicationContext());
        //create shortcut based on last edited albums
        File albumsPath = new File(getApplication().getApplicationContext().getExternalFilesDir(null),"albums");
        Albums albums = Albums.getInstance(albumsPath.getPath());
        ArrayList<ShortcutInfoCompat> shorcutsList = new ArrayList<>();
        for (Album a : albums.getAlbumList().subList(0,MAX_SHORTCUTS-1)) {
            //we exclude albums edited more than 30 days ago
            long deltaDays = TimeUnit.DAYS.convert((new Date()).getTime() - a.getPagesLastEditDate().getTime(), TimeUnit.MILLISECONDS);
            if (deltaDays > DIRECT_SHARE_MAX_DAYS) {
                continue;
            }
            ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(getApplicationContext(),a.getId().toString())
                    .setShortLabel(a.getName())
                    .setRank((int)deltaDays+1)
                    .setLongLived(true)
                    .setIcon(IconCompat.createWithResource(getApplicationContext(),R.drawable.ic_launcher_foreground))
                    .setIntent(new Intent(Intent.ACTION_MAIN))
                    .setCategories(new HashSet<>(Arrays.asList(AddImageToAlbumActivity.IMAGE_SHARE)))
                    .setPerson(new Person.Builder()
                            .setName(a.getName())
                            .build())
                    .build();
            shorcutsList.add(shortcutInfo);
        }
        ShortcutManagerCompat.addDynamicShortcuts(getApplicationContext(),shorcutsList);
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
