package fr.nuage.souvenirs;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.util.UUID;

import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.SyncToNextcloudThread;

public class SyncService extends Service {

    // IntentService can perform
    private static final String ACTION_SYNC = "fr.nuage.souvenirs.action.SYNC";

    private static final String EXTRA_PARAM_ALBUMID = "fr.nuage.souvenirs.extra.PARAM_ALBUMID";


    public static void startSync(Context context, AlbumViewModel albumViewModel) {
        if (! albumViewModel.getSyncInProgress()) {
            Intent intent = new Intent(context, SyncService.class);
            intent.setAction(ACTION_SYNC);
            intent.putExtra(EXTRA_PARAM_ALBUMID, albumViewModel.getId().toString());
            context.startService(intent);
        }
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
                    //create notification
                    int notificationId = albumViewModel.getId().hashCode();
                    NotificationCompat.Builder nBuilder = createNotification(getApplicationContext(),albumViewModel);
                    //show progress bar in notification
                    nBuilder.setProgress(1, 0, true);

                    //start foreground service
                    int type = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                    }
                    ServiceCompat.startForeground(
                            this,
                            notificationId,
                            nBuilder.build(),
                            type
                    );
                    //start sync
                    SyncToNextcloudThread task = new SyncToNextcloudThread(getApplication().getApplicationContext(),albumViewModel, nBuilder);
                    task.start();
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

    private NotificationCompat.Builder createNotification(Context context, AlbumViewModel albumVM) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlbumListActivity.CHANNEL_ID);
        builder.setContentTitle(context.getString(R.string.sync_to_nextcloud,albumVM.getName().getValue()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_sync_black_24dp);
        return builder;
    }
}
