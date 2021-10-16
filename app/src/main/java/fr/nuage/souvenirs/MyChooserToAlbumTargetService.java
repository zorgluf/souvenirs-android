package fr.nuage.souvenirs;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MyChooserToAlbumTargetService extends ChooserTargetService {
    //FIXME

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {

        ArrayList<ChooserTarget> targets = new ArrayList<>();

        //load albums
        //get albums path
        File albumsPath = new File(getApplication().getApplicationContext().getExternalFilesDir(null),"albums");
        Albums albums = Albums.getInstance(albumsPath.getPath());
        for (Album a : albums.getAlbumList()) {
            //we exclude albums edited more than 30 days ago
            long deltaDays = TimeUnit.DAYS.convert((new Date()).getTime() - a.getLastEditDate().getTime(), TimeUnit.MILLISECONDS);
            if (deltaDays > 30) {
                continue;
            }
            float score = 1 - (deltaDays / 30);
            Bundle extras = new Bundle();
            extras.putString(AddImageToAlbumActivity.EXTRA_ALBUM,a.getAlbumPath());
            ChooserTarget ct = new ChooserTarget(a.getName(),Icon.createWithResource(getApplicationContext(),R.drawable.ic_launcher_foreground),score,new ComponentName("fr.nuage.souvenirs","fr.nuage.souvenirs.AddImageToAlbumActivity"),extras);
            targets.add(ct);
        }
        return targets;
    }


}
