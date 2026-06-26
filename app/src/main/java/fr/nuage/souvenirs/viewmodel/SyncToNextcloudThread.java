package fr.nuage.souvenirs.viewmodel;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.SyncService;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.Utils;
import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.PageNC;

/**
 * Synchronises a local {@link Album} with its Nextcloud counterpart {@link AlbumNC}.
 *
 * The three edit timestamps (album-level {@code lastEditDate}, album-level {@code pagesLastEditDate}
 * and per-page {@code lastEditDate}) are now opaque tokens set <b>exclusively by the server</b> and
 * returned in the body of every modification request. The app no longer generates them; it stores the
 * last token received and tracks local edits with dirty flags ({@code Album.isEdited},
 * {@code Album.isPagesEdited}, {@code Page.isEdited}).
 *
 * For each independent domain (album info, page array, each page's content) the direction is decided by
 * comparing the local token with the remote token and combining it with the dirty flag:
 * <ul>
 *   <li>tokens equal + local dirty  -> push local to server (store returned token, clear dirty)</li>
 *   <li>tokens differ + local clean -> pull server to local (store remote token)</li>
 *   <li>tokens differ + local dirty  -> conflict (left untouched, reported; resolution GUI is future work)</li>
 *   <li>tokens equal + clean -> nothing</li>
 * </ul>
 */
public class SyncToNextcloudThread extends Thread {

    public static final int RESULT_NC_ERR = 1;
    public static final int RESULT_OK = 4;
    public static final int RESULT_CANCEL = 5;
    public static final int RESULT_CONFLICT = 6;

    //conflict resolution mode: how to resolve a domain that is in conflict (local edit + remote change)
    public static final int RESOLVE_NONE = 0;        //report the conflict, leave both sides untouched
    public static final int RESOLVE_KEEP_LOCAL = 1;  //keep the local version, overwrite Nextcloud
    public static final int RESOLVE_KEEP_REMOTE = 2;  //keep the Nextcloud version, overwrite local

    //sync direction for a single domain
    private static final int SYNC_NONE = 0;
    private static final int SYNC_PUSH = 1;
    private static final int SYNC_PULL = 2;
    private static final int SYNC_CONFLICT = 3;

    /** Notified (on a background thread) when the sync finishes, so the service can release the foreground. */
    public interface OnSyncComplete {
        void onComplete(int result);
    }

    private final Context context;
    private final AlbumViewModel albumVM;
    private final NotificationCompat.Builder nBuilder;
    private final int notificationId;
    private final int resolveMode;
    private final OnSyncComplete onSyncComplete;
    private String notificationMsg;
    private boolean conflictDetected = false;

    public SyncToNextcloudThread(Context context, AlbumViewModel album, NotificationCompat.Builder nBuilder,
                                 int resolveMode, OnSyncComplete onSyncComplete) {
        notificationId = album.getId().hashCode();
        this.context = context;
        this.albumVM = album;
        this.nBuilder = nBuilder;
        this.resolveMode = resolveMode;
        this.onSyncComplete = onSyncComplete;
    }

    /** Stable id of the heads-up conflict notification for an album (distinct from the progress one). */
    public static int conflictNotificationId(UUID albumId) {
        return (albumId.toString() + "-conflict").hashCode();
    }

    public void run() {
        Album album = albumVM.getAlbum();
        AlbumNC albumNC = albumVM.getAlbumNC();
        boolean forcePushAll = false;
        boolean forcePullAll = false;

        Log.i(getClass().getName(), context.getString(R.string.sync_to_nextcloud, albumVM.getName().getValue()));

        //create local album if it does not exist yet -> pull everything from remote
        if (album == null) {
            notificationMsg = context.getString(R.string.sync_create_local_album);
            Log.d("SYNC", notificationMsg);
            publishProgress(0);
            album = Albums.getInstance().createAlbum(albumNC.getId());
            forcePullAll = true;
        }

        //create remote album if it does not exist yet -> push everything to remote
        if (albumNC == null) {
            notificationMsg = context.getString(R.string.sync_create_remote_album);
            Log.d("SYNC", notificationMsg);
            publishProgress(0);
            albumNC = AlbumNC.create(albumVM.getId());
            if (albumNC == null) {
                endTask(RESULT_NC_ERR);
                return;
            }
            albumVM.setAlbumNC(albumNC);
            forcePushAll = true;
        }

        //fetch full remote album
        notificationMsg = context.getString(R.string.sync_fetch_remote_album);
        Log.d("SYNC", notificationMsg);
        publishProgress(0);
        if (!albumNC.load(true)) {
            endTask(RESULT_NC_ERR);
            return;
        }

        //----- domain 1: album info (name, date, albumImage, elementMargin) -----
        switch (decideDirection(album.getLastEditDate(), albumNC.getLastEditDate(), album.isEdited(), forcePushAll, forcePullAll)) {
            case SYNC_PUSH:
                if (!pushAlbumInfo(album, albumNC)) { endTask(RESULT_NC_ERR); return; }
                break;
            case SYNC_PULL:
                if (!pullAlbumInfo(album, albumNC)) { endTask(RESULT_NC_ERR); return; }
                break;
            case SYNC_CONFLICT:
                Log.d("SYNC", "Album info conflict: left untouched.");
                conflictDetected = true;
                break;
        }

        //----- domain 2: page array (which pages exist + order) -----
        switch (decideDirection(album.getPagesLastEditDate(), albumNC.getPagesLastEditDate(), album.isPagesEdited(), forcePushAll, forcePullAll)) {
            case SYNC_PUSH:
                if (!pushPageArray(album, albumNC)) { endTask(RESULT_NC_ERR); return; }
                break;
            case SYNC_PULL:
                if (!pullPageArray(album, albumNC)) { endTask(RESULT_NC_ERR); return; }
                break;
            case SYNC_CONFLICT:
                Log.d("SYNC", "Page array conflict: structure left untouched.");
                conflictDetected = true;
                break;
        }

        //----- domain 3: content of pages present on both sides -----
        //token-only comparison: pages just created/pulled above already have matching tokens (-> NONE)
        if (!syncPageContents(album, albumNC)) { endTask(RESULT_NC_ERR); return; }

        //persist the stored tokens and cleared dirty flags immediately (do not rely on debounced save)
        album.save();

        endTask(conflictDetected ? RESULT_CONFLICT : RESULT_OK);
    }

    /**
     * Decide the sync direction for one domain from its local/remote tokens and local dirty flag.
     * forcePush/forcePull short-circuit the decision when one side was just created in this sync.
     */
    private int decideDirection(Date localToken, Date remoteToken, boolean dirty, boolean forcePush, boolean forcePull) {
        if (forcePush) return SYNC_PUSH;
        if (forcePull) return SYNC_PULL;
        if (tokenEquals(localToken, remoteToken)) {
            return dirty ? SYNC_PUSH : SYNC_NONE;
        }
        //tokens differ
        if (!dirty) {
            return SYNC_PULL;
        }
        //local edit + remote change = conflict: resolve per the user's choice, else report it
        switch (resolveMode) {
            case RESOLVE_KEEP_LOCAL:
                return SYNC_PUSH;
            case RESOLVE_KEEP_REMOTE:
                return SYNC_PULL;
            default:
                return SYNC_CONFLICT;
        }
    }

    private static boolean tokenEquals(Date a, Date b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    //----- album info -----

    private boolean pushAlbumInfo(Album album, AlbumNC albumNC) {
        notificationMsg = context.getString(R.string.sync_album_info_to_nc);
        Log.d("SYNC", notificationMsg);
        publishProgress(0);
        if (album.getName() != null) { albumNC.setName(album.getName()); }
        if (album.getDate() != null) { albumNC.setDate(album.getDate()); }
        if (album.getAlbumImage() != null) {
            String assetPath = Utils.getRelativePath(album.getAlbumPath(), album.getAlbumImage());
            if (!albumNC.pushAsset(album.getAlbumPath(), assetPath, "", 0)) {
                return false;
            }
            albumNC.setAlbumImage(assetPath);
        }
        albumNC.setElementMargin(album.getElementMargin());
        //save() pushes infos and stores the server-returned lastEditDate onto albumNC
        if (!albumNC.save()) {
            return false;
        }
        album.setLastEditDate(albumNC.getLastEditDate());
        album.setEdited(false);
        return true;
    }

    private boolean pullAlbumInfo(Album album, AlbumNC albumNC) {
        notificationMsg = context.getString(R.string.sync_album_info_to_local);
        Log.d("SYNC", notificationMsg);
        publishProgress(0);
        if (albumNC.getName() != null) { album.setName(albumNC.getName()); }
        if (albumNC.getDate() != null) { album.setDate(albumNC.getDate()); }
        if (albumNC.getAlbumImage() != null) {
            if (!albumNC.pullAsset(album.getAlbumPath(), albumNC.getAlbumImage())) {
                return false;
            }
            album.setAlbumImage(new File(album.getAlbumPath(), albumNC.getAlbumImage()).getPath());
        }
        album.setElementMargin(albumNC.getElementMargin());
        //store remote token and clear the dirty flag set by the setters above
        album.setLastEditDate(albumNC.getLastEditDate());
        album.setEdited(false);
        return true;
    }

    //----- page array (structural) -----

    private boolean pushPageArray(Album album, AlbumNC albumNC) {
        int nbPage = album.getPages().size();
        //create local-only pages on the server (full content goes with the create)
        for (Page page : new ArrayList<>(album.getPages())) {
            if (albumNC.getPage(page.getId()) == null) {
                int index = album.getIndex(page);
                notificationMsg = context.getString(R.string.sync_album_create_remote_page, (index + 1) + "/" + nbPage);
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, index);
                PageNC pageNC = new PageNC();
                pageNC.update(page);
                if (!albumNC.createPage(pageNC, index, album.getAlbumPath())) {
                    return false;
                }
                //createPage stored the server tokens onto pageNC and albumNC
                page.setLastEditDate(pageNC.getLastEditDate());
                page.setEdited(false);
            }
        }
        //delete remote pages that no longer exist locally
        for (PageNC pageNC : new ArrayList<>(albumNC.getPages())) {
            if (!album.hasPage(pageNC.getId())) {
                notificationMsg = context.getString(R.string.sync_album_del_remote_page, albumNC.getIndex(pageNC) + "/" + nbPage);
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, albumNC.getIndex(pageNC));
                if (!albumNC.delPage(pageNC)) {
                    return false;
                }
            }
        }
        //reorder remote pages to match the local order
        for (Page page : album.getPages()) {
            PageNC remotePage = albumNC.getPage(page.getId());
            int localPos = album.getIndex(page);
            int remotePos = albumNC.getIndex(remotePage);
            if (localPos != remotePos) {
                notificationMsg = context.getString(R.string.sync_album_change_remote_page_pos, remotePos + "/" + nbPage);
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, remotePos);
                if (!albumNC.movePage(remotePage, localPos)) {
                    return false;
                }
            }
        }
        //remove orphaned assets on the server
        notificationMsg = context.getString(R.string.sync_album_clean);
        Log.d("SYNC", notificationMsg);
        publishProgress(0);
        if (!albumNC.clean()) {
            return false;
        }
        album.setPagesLastEditDate(albumNC.getPagesLastEditDate());
        album.setPagesEdited(false);
        return true;
    }

    private boolean pullPageArray(Album album, AlbumNC albumNC) {
        int nbPage = albumNC.getPages().size();
        //create remote-only pages locally (full content + assets go with the create)
        for (PageNC pageNC : albumNC.getPages()) {
            if (album.getPage(pageNC.getId()) == null) {
                int index = albumNC.getIndex(pageNC);
                notificationMsg = context.getString(R.string.sync_album_create_local_page, index + "/" + nbPage);
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, index);
                if (!pageNC.pullAssets(album.getAlbumPath(), albumNC)) {
                    return false;
                }
                Page page = album.createPage(index, false);
                //update copies content and stores the page token; the pulled page is clean
                page.update(pageNC);
            }
        }
        //delete local pages that no longer exist remotely
        for (Page page : new ArrayList<>(album.getPages())) {
            if (!albumNC.hasPage(page.getId())) {
                notificationMsg = context.getString(R.string.sync_album_del_local_page, album.getIndex(page) + "/" + nbPage);
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, album.getIndex(page));
                album.delPage(page);
            }
        }
        //reorder local pages to match the remote order
        for (PageNC pageNC : albumNC.getPages()) {
            Page localPage = album.getPage(pageNC.getId());
            int remotePos = albumNC.getIndex(pageNC);
            int localPos = album.getIndex(localPage);
            if (localPos != remotePos) {
                notificationMsg = context.getString(R.string.sync_album_change_local_page_pos, pageNC.getId().toString());
                Log.d("SYNC", notificationMsg);
                publishProgress(nbPage, localPos);
                album.movePage(localPage, remotePos);
            }
        }
        album.setPagesLastEditDate(albumNC.getPagesLastEditDate());
        album.setPagesEdited(false);
        return true;
    }

    //----- per-page content -----

    /** @return false on a Nextcloud error (caller aborts), true otherwise (conflicts flag the run). */
    private boolean syncPageContents(Album album, AlbumNC albumNC) {
        int nbPage = album.getPages().size();
        for (Page page : new ArrayList<>(album.getPages())) {
            PageNC pageNC = albumNC.getPage(page.getId());
            if (pageNC == null) {
                //not present on both sides (e.g. structural conflict was skipped) -> nothing to do
                continue;
            }
            //per-page content uses token comparison only: a freshly created/pulled page already matches
            switch (decideDirection(page.getLastEditDate(), pageNC.getLastEditDate(), page.isEdited(), false, false)) {
                case SYNC_PUSH:
                    notificationMsg = context.getString(R.string.sync_album_remote_update_page, albumNC.getIndex(pageNC) + "/" + nbPage);
                    Log.d("SYNC", notificationMsg);
                    publishProgress(nbPage, albumNC.getIndex(pageNC));
                    pageNC.update(page);
                    if (!albumNC.pushPage(pageNC, album.getAlbumPath())) {
                        return false;
                    }
                    page.setLastEditDate(pageNC.getLastEditDate());
                    page.setEdited(false);
                    break;
                case SYNC_PULL:
                    notificationMsg = context.getString(R.string.sync_album_local_update_page, album.getIndex(page) + "/" + nbPage);
                    Log.d("SYNC", notificationMsg);
                    publishProgress(nbPage, album.getIndex(page));
                    if (!pageNC.pullAssets(album.getAlbumPath(), albumNC)) {
                        return false;
                    }
                    if (!page.update(pageNC)) {
                        return false;
                    }
                    break;
                case SYNC_CONFLICT:
                    Log.d("SYNC", "Page content conflict on " + page.getId() + ": left untouched.");
                    conflictDetected = true;
                    break;
            }
        }
        return true;
    }

    private void publishProgress(Integer... progress) {
        nBuilder.setContentText(notificationMsg);
        if (progress[0] == 0) {
            nBuilder.setProgress(1, 0, true);
        } else {
            nBuilder.setProgress(progress[0], progress[1], false);
        }
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, nBuilder.build());
        }
    }

    private void endTask(Integer result) {
        if (result == RESULT_CONFLICT) {
            //post a separate high-importance, actionable conflict notification (heads-up + vibration);
            //the progress notification is retired by the service completion callback below.
            postConflictNotification();
        } else {
            int notIcon;
            switch (result) {
                case RESULT_NC_ERR:
                    notificationMsg = context.getString(R.string.nextcloud_notification_err);
                    notIcon = R.drawable.ic_error_black_24dp;
                    break;
                case RESULT_OK:
                    notificationMsg = context.getString(R.string.nextcloud_notification_finish_ok);
                    notIcon = R.drawable.ic_check_black_24dp;
                    break;
                default:
                    notificationMsg = "";
                    notIcon = R.drawable.ic_error_black_24dp;
                    break;
            }
            Log.d("SYNC", notificationMsg);
            nBuilder.setContentText(notificationMsg)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSmallIcon(notIcon);
            if (result == RESULT_OK) { //auto close on success
                nBuilder.setTimeoutAfter(5000);
            }
            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, nBuilder.build());
            }
        }
        //set progress flag off
        albumVM.setSyncInProgress(false);
        //let the service release the foreground state (and clear the progress notification on conflict)
        if (onSyncComplete != null) {
            onSyncComplete.onComplete(result);
        }
    }

    /**
     * Build and post the actionable conflict notification on the high-importance alert channel.
     * It offers the user the two resolution choices (keep this device / keep Nextcloud), each of which
     * relaunches the sync through {@link SyncService} with the matching resolve mode.
     */
    private void postConflictNotification() {
        UUID albumId = albumVM.getId();
        String albumName = albumVM.getName().getValue();
        notificationMsg = context.getString(R.string.nextcloud_notification_conflict);
        Log.d("SYNC", "Conflict on album " + albumId + ": prompting user for resolution.");

        PendingIntent keepLocal = SyncService.getResolvePendingIntent(context, albumId, RESOLVE_KEEP_LOCAL);
        PendingIntent keepRemote = SyncService.getResolvePendingIntent(context, albumId, RESOLVE_KEEP_REMOTE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlbumListActivity.CHANNEL_ID_ALERT)
                .setContentTitle(context.getString(R.string.nextcloud_conflict_title, albumName))
                .setContentText(notificationMsg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.nextcloud_conflict_bigtext, albumName)))
                .setSmallIcon(R.drawable.ic_baseline_warning_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check_black_24dp, context.getString(R.string.nextcloud_conflict_keep_local), keepLocal)
                .addAction(R.drawable.ic_nextcloud_logo, context.getString(R.string.nextcloud_conflict_keep_remote), keepRemote);

        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(conflictNotificationId(albumId), builder.build());
        }
    }

}
