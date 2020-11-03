package fr.nuage.souvenirs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import java.util.UUID;

import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.SyncToNextcloudAsyncTask;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class SyncService extends IntentService {

    // IntentService can perform
    private static final String ACTION_SYNC = "fr.nuage.souvenirs.action.SYNC";

    private static final String EXTRA_PARAM_ALBUMID = "fr.nuage.souvenirs.extra.PARAM_ALBUMID";

    public SyncService() {
        super("SyncService");
    }

    /**
     * Starts this service to perform action sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startSync(Context context, AlbumViewModel albumViewModel) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_SYNC);
        intent.putExtra(EXTRA_PARAM_ALBUMID, albumViewModel.getId().toString());
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)) {
                final String albumId = intent.getStringExtra(EXTRA_PARAM_ALBUMID);
                handleActionSync(albumId);
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSync(String albumId) {
        UUID id = UUID.fromString(albumId);
        if (id != null) {
            AlbumViewModel albumViewModel = AlbumListViewModelFactory.getAlbumListViewModel().getAlbum(id);
            //start sync to nextcloud task
            SyncToNextcloudAsyncTask task = new SyncToNextcloudAsyncTask(getApplication().getApplicationContext(),albumViewModel);
            task.execute();
        }
    }

}
