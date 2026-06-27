package fr.nuage.souvenirs;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;

import java.util.UUID;

import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.SyncToNextcloudThread;

public class SyncService extends Service {

    private static final String ACTION_SYNC = "fr.nuage.souvenirs.action.SYNC";
    //fired by the conflict notification's action buttons to resolve the conflict in one direction
    private static final String ACTION_RESOLVE_KEEP_LOCAL = "fr.nuage.souvenirs.action.RESOLVE_KEEP_LOCAL";
    private static final String ACTION_RESOLVE_KEEP_REMOTE = "fr.nuage.souvenirs.action.RESOLVE_KEEP_REMOTE";

    private static final String EXTRA_PARAM_ALBUMID = "fr.nuage.souvenirs.extra.PARAM_ALBUMID";


    public static void startSync(Context context, AlbumViewModel albumViewModel) {
        if (! albumViewModel.getSyncInProgress()) {
            Intent intent = new Intent(context, SyncService.class);
            intent.setAction(ACTION_SYNC);
            intent.putExtra(EXTRA_PARAM_ALBUMID, albumViewModel.getId().toString());
            context.startService(intent);
        }
    }

    /**
     * Build a PendingIntent that relaunches the sync for {@code albumId} resolving conflicts in the
     * given direction ({@link SyncToNextcloudThread#RESOLVE_KEEP_LOCAL} or
     * {@link SyncToNextcloudThread#RESOLVE_KEEP_REMOTE}). Used by the conflict notification actions.
     */
    public static PendingIntent getResolvePendingIntent(Context context, UUID albumId, int resolveMode) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(resolveMode == SyncToNextcloudThread.RESOLVE_KEEP_LOCAL
                ? ACTION_RESOLVE_KEEP_LOCAL : ACTION_RESOLVE_KEEP_REMOTE);
        intent.putExtra(EXTRA_PARAM_ALBUMID, albumId.toString());
        //distinct request code per album + direction so the two actions don't collide
        int requestCode = albumId.hashCode() + resolveMode;
        return PendingIntent.getService(context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            int resolveMode = SyncToNextcloudThread.RESOLVE_NONE;
            if (ACTION_RESOLVE_KEEP_LOCAL.equals(action)) {
                resolveMode = SyncToNextcloudThread.RESOLVE_KEEP_LOCAL;
            } else if (ACTION_RESOLVE_KEEP_REMOTE.equals(action)) {
                resolveMode = SyncToNextcloudThread.RESOLVE_KEEP_REMOTE;
            }
            if (ACTION_SYNC.equals(action) || resolveMode != SyncToNextcloudThread.RESOLVE_NONE) {
                String idStr = intent.getStringExtra(EXTRA_PARAM_ALBUMID);
                if (idStr != null) {
                    launchSync(UUID.fromString(idStr), resolveMode, startId);
                    return START_NOT_STICKY;
                }
            }
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void launchSync(UUID id, int resolveMode, int startId) {
        AlbumListViewModel listVM = AlbumListViewModelFactory.getAlbumListViewModel();
        if (listVM == null) {
            //process may have been recreated by the notification action: rebuild the list view model
            listVM = new AlbumListViewModelFactory(getApplication()).create(AlbumListViewModel.class);
        }
        AlbumViewModel albumViewModel = listVM.getAlbum(id);
        if (albumViewModel == null || albumViewModel.getSyncInProgress()) {
            //unknown album, or a sync is already running for it: nothing to do
            stopSelf(startId);
            return;
        }
        albumViewModel.setSyncInProgress(true);

        //a resolution choice was made: dismiss the conflict notification
        if (resolveMode != SyncToNextcloudThread.RESOLVE_NONE) {
            NotificationManagerCompat.from(this).cancel(SyncToNextcloudThread.conflictNotificationId(id));
        }

        //create progress notification
        int notificationId = albumViewModel.getId().hashCode();
        NotificationCompat.Builder nBuilder = createNotification(getApplicationContext(), albumViewModel);
        nBuilder.setProgress(1, 0, true);

        //start foreground service
        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }
        ServiceCompat.startForeground(this, notificationId, nBuilder.build(), type);

        //release the foreground state once the sync finishes
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        SyncToNextcloudThread.OnSyncComplete onComplete = result -> mainHandler.post(() -> {
            //DETACH keeps the final ok/error notification posted while leaving the foreground state
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
            if (result == SyncToNextcloudThread.RESULT_CONFLICT) {
                //the actionable conflict notification replaces the progress one
                NotificationManagerCompat.from(this).cancel(notificationId);
            }
            stopSelf(startId);
        });

        //start sync
        SyncToNextcloudThread task = new SyncToNextcloudThread(getApplication().getApplicationContext(),
                albumViewModel, nBuilder, resolveMode, onComplete);
        task.start();
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
