package fr.nuage.souvenirs;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import java.util.UUID;

import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.SyncToNextcloudAsyncTask;

public class SyncService extends Service {

    // IntentService can perform
    private static final String ACTION_SYNC = "fr.nuage.souvenirs.action.SYNC";

    private static final String EXTRA_PARAM_ALBUMID = "fr.nuage.souvenirs.extra.PARAM_ALBUMID";


    public static void startSync(Context context, AlbumViewModel albumViewModel) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_SYNC);
        intent.putExtra(EXTRA_PARAM_ALBUMID, albumViewModel.getId().toString());
        context.startService(intent);
    }

    @Override
    public int onStartCommand (Intent intent,
                               int flags,
                               int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)) {
                //check id
                UUID id = UUID.fromString(intent.getStringExtra(EXTRA_PARAM_ALBUMID));
                if (id != null) {
                    AlbumViewModel albumViewModel = AlbumListViewModelFactory.getAlbumListViewModel().getAlbum(id);
                    SyncToNextcloudAsyncTask task = new SyncToNextcloudAsyncTask(getApplication().getApplicationContext(),albumViewModel);
                    //make forground
                    int type = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                    }
                    ServiceCompat.startForeground(
                            this,
                            100,
                            task.getNotification(),
                            type
                    );
                    //start sync to nextcloud task
                    task.execute();
                }
            }
        }
        return START_NOT_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
