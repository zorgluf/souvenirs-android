package fr.nuage.souvenirs.viewmodel;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.Utils;
import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.PageNC;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class SyncToNextcloudAsyncTask extends AsyncTask<Void, Integer, Integer> {

    public static final int RESULT_NC_ERR = 1;
    public static final int RESULT_OK = 4;
    public static final int RESULT_CANCEL = 5;

    private Context context;
    private AlbumViewModel albumVM;
    private NotificationCompat.Builder nBuilder;
    private int notificationId;
    private String notificationMsg;

    public SyncToNextcloudAsyncTask(Context context, AlbumViewModel album) {
        notificationId = album.getId().hashCode();
        this.context = context;
        this.albumVM = album;
        //cancel if an other task running on same album
        if (album.getSyncInProgress()) {
            cancel(false);
        } else {
            album.setSyncInProgress(true);
        }
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        if (isCancelled()) {
            return RESULT_CANCEL;
        }
        Album album = albumVM.getAlbum();
        AlbumNC albumNC = albumVM.getAlbumNC();
        Boolean localNewerThanNC = null;

        //create local album if do not exist
        if (album == null) {
            notificationMsg = context.getString(R.string.sync_create_local_album);
            Log.d("SYNC",notificationMsg);
            publishProgress(0);

            album = Albums.getInstance().createAlbum(albumNC.getId());
            //albumVM.setAlbum(album);
            localNewerThanNC = false;
        }

        //create remote album if do not exist
        if (albumNC == null) {
            notificationMsg = context.getString(R.string.sync_create_remote_album);
            Log.d("SYNC",notificationMsg);
            publishProgress(0);
            //create album
            albumNC = AlbumNC.create(albumVM.getId());
            if (albumNC == null) {
                return RESULT_NC_ERR;
            }
            albumVM.setAlbumNC(albumNC);
            localNewerThanNC = true;
        }

        //fetch remote album
        notificationMsg = context.getString(R.string.sync_fetch_remote_album);
        Log.d("SYNC",notificationMsg);
        publishProgress(0);
        if (!albumNC.load(true)) {
            return RESULT_NC_ERR;
        }

        if (localNewerThanNC == null) {
            if (albumNC.getLastEditDate() == null) {
                localNewerThanNC = true;
            } else {
                if (!album.getLastEditDate().equals(albumNC.getLastEditDate())) {
                    localNewerThanNC = album.getLastEditDate().after(albumNC.getLastEditDate());
                }
            }
        }

        //sync album infos
        if (localNewerThanNC != null) {
            if (localNewerThanNC) {
                notificationMsg = context.getString(R.string.sync_album_info_to_nc);
                Log.d("SYNC",notificationMsg);
                publishProgress(0);
                if (album.getName()!=null) { albumNC.setName(album.getName()); }
                if (album.getDate()!=null) { albumNC.setDate(album.getDate()); }
                if (album.getAlbumImage()!=null) {
                    //asset path
                    String assetPath = Utils.getRelativePath(album.getAlbumPath(),album.getAlbumImage());
                    //push image asset
                    if (!albumNC.pushAsset(album.getAlbumPath(),assetPath,"",0)) {
                        return RESULT_NC_ERR;
                    }
                    albumNC.setAlbumImage(assetPath);
                }
                albumNC.setLastEditDate(album.getLastEditDate());
                if (!albumNC.save()) {
                    return RESULT_NC_ERR;
                }
            } else {
                notificationMsg = context.getString(R.string.sync_album_info_to_local);
                Log.d("SYNC",notificationMsg);
                publishProgress(0);
                if (albumNC.getName()!=null) { album.setName(albumNC.getName()); }
                if (albumNC.getDate()!=null) { album.setDate(albumNC.getDate()); }
                if (albumNC.getAlbumImage()!=null) {
                    //pull image asset
                    if (!albumNC.pullAsset(album.getAlbumPath(),albumNC.getAlbumImage())) {
                        return RESULT_NC_ERR;
                    }
                    album.setAlbumImage(new File(album.getAlbumPath(),albumNC.getAlbumImage()).getPath());
                }
                if (albumNC.getLastEditDate()!=null) { album.setLastEditDate(albumNC.getLastEditDate()); }
                //implicite save on local album
            }
        }


        //sync pages
        if (!album.getPagesLastEditDate().equals(albumNC.getPagesLastEditDate())) {
            int nbPage = album.getPages().size();
            //delete local pages if needed
            if ((albumNC.getPagesLastEditDate() != null) && (album.getPagesLastSyncDate()!= null)) {
                for (Page page : album.getPages()) {
                    if ((page.getLastEditDate().before(album.getPagesLastSyncDate())) && (album.getPagesLastSyncDate().before(albumNC.getPagesLastEditDate()))) {
                        if (!albumNC.hasPage(page.getId())) {
                            notificationMsg = context.getString(R.string.sync_album_del_local_page, album.getIndex(page) +"/"+ nbPage);
                            Log.d("SYNC", notificationMsg);
                            publishProgress(nbPage,album.getIndex(page));
                            album.delPage(page);
                        } else {
                            //check local order according to remote
                            PageNC remotePage = albumNC.getPage(page.getId());
                            int remotePos = albumNC.getIndex(remotePage);
                            if (album.getIndex(page) != remotePos) {
                                notificationMsg = context.getString(R.string.sync_album_change_local_page_pos, page.getId().toString());
                                Log.d("SYNC", notificationMsg);
                                publishProgress(nbPage,album.getIndex(page));
                                album.movePage(page,remotePos);
                            }
                        }
                    }
                }
            }

            //push local page modification to remote (or pull if newer on remote)
            for (Page page : album.getPages()) {
                PageNC remotePage = albumNC.getPage(page.getId());
                if (remotePage != null) {
                    // if remote page exists, push mods
                    if ((remotePage.getLastEditDate()==null) ||
                            (page.getLastEditDate().after(remotePage.getLastEditDate()))) {
                        //local page version newer, push mod
                        notificationMsg = context.getString(R.string.sync_album_remote_update_page,albumNC.getIndex(remotePage)+"/"+nbPage);
                        Log.d("SYNC",notificationMsg);
                        publishProgress(nbPage,albumNC.getIndex(remotePage));
                        remotePage.update(page);
                        if (!albumNC.pushPage(remotePage,album.getAlbumPath())) {
                            return RESULT_NC_ERR;
                        }
                    }
                } else { //if not push page
                    int index = album.getIndex(page);
                    PageNC pageNC = new PageNC();
                    pageNC.update(page);
                    notificationMsg = context.getString(R.string.sync_album_create_remote_page,(index+1)+"/"+nbPage);
                    Log.d("SYNC",notificationMsg);
                    publishProgress(nbPage,index);
                    if(!albumNC.createPage(pageNC,index,album.getAlbumPath())) {
                        return RESULT_NC_ERR;
                    }
                }
            }

            if ((albumNC.getPagesLastEditDate() != null) && (album.getPagesLastSyncDate()!= null)) {
                //delete remote pages if needed
                for (PageNC pageNC : new ArrayList<>(albumNC.getPages())) {
                    if ((pageNC.getLastEditDate()==null) ||
                            ((pageNC.getLastEditDate().before(album.getPagesLastSyncDate()))  //no mod after last sync
                                    && (album.getPagesLastSyncDate().before(album.getPagesLastEditDate())))) { //local pages mod after last sync
                        if (!album.hasPage(pageNC.getId())) {
                            notificationMsg = context.getString(R.string.sync_album_del_remote_page,albumNC.getIndex(pageNC)+"/"+nbPage);
                            Log.d("SYNC",notificationMsg);
                            publishProgress(nbPage,albumNC.getIndex(pageNC));
                            if (!albumNC.delPage(pageNC)) {
                                return RESULT_NC_ERR;
                            }
                        }
                    }
                }
                //check page order (at this stage, same page should be present on both side, but not necessary in the right order)
                //local to remote
                for (Page page : album.getPages()) {
                    //check remote order according to local
                    PageNC remotePage = albumNC.getPage(page.getId());
                    int localPos = album.getIndex(page);
                    int remotePos = albumNC.getIndex(remotePage);
                    if (localPos != remotePos) {
                        notificationMsg = context.getString(R.string.sync_album_change_remote_page_pos, remotePos + "/" + nbPage);
                        Log.d("SYNC", notificationMsg);
                        publishProgress(nbPage, remotePos);
                        //move remote page to localpos
                        if (!albumNC.movePage(remotePage, localPos)) {
                            return RESULT_NC_ERR;
                        }
                    }
                }
            }

            //clean remote album (remove assets)
            if (!albumNC.clean()) {
                notificationMsg = context.getString(R.string.sync_album_clean);
                Log.d("SYNC",notificationMsg);
                publishProgress(0);
                return RESULT_NC_ERR;
            }

            //pull remote mod to local
            for (PageNC pageNC : albumNC.getPages()) {
                Page localPage = album.getPage(pageNC.getId());
                if (localPage != null) { // if local page exists, pull mods
                    if ((pageNC.getLastEditDate()==null) ||
                            (pageNC.getLastEditDate().after(localPage.getLastEditDate()))) {
                        //remote page version newer, pull mod
                        notificationMsg = context.getString(R.string.sync_album_local_update_page,album.getIndex(localPage)+"/"+nbPage);
                        Log.d("SYNC",notificationMsg);
                        publishProgress(nbPage,album.getIndex(localPage));
                        if(!pageNC.pullAssets(album.getAlbumPath(),albumNC)) {
                            return RESULT_NC_ERR;
                        }
                        if (!localPage.update(pageNC)) {
                            return RESULT_NC_ERR;
                        }
                    }
                } else { //if not pull page
                    int index = albumNC.getIndex(pageNC);
                    notificationMsg = context.getString(R.string.sync_album_create_local_page,index+"/"+nbPage);
                    Log.d("SYNC",notificationMsg);
                    publishProgress(nbPage,index);
                    if(!pageNC.pullAssets(album.getAlbumPath(),albumNC)) {
                        return RESULT_NC_ERR;
                    }
                    Page page = album.createPage(index,false);
                    page.update(pageNC);
                }
            }

            //set edit date on album page
            if ((albumNC.getPagesLastEditDate() != null) && (albumNC.getPagesLastEditDate().after(album.getPagesLastEditDate()))) {
                //NC newer ref
                album.setPagesLastEditDate(albumNC.getPagesLastEditDate());
            } else if ((albumNC.getPagesLastEditDate() == null) || (albumNC.getPagesLastEditDate().before(album.getPagesLastEditDate()))) {
                //local newer ref
                albumNC.setPagesLastEditDate(album.getPagesLastEditDate());
                if (!albumNC.save()) {
                    return RESULT_NC_ERR;
                }
            }
        }

        //set last sync date page on album
        album.setPagesLastSyncDate(new Date());

        return RESULT_OK;
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
        nBuilder.setContentText(notificationMsg);
        if (progress[0] == 0) {
            nBuilder.setProgress(1, 0, true);
        } else {
            nBuilder.setProgress(progress[0], progress[1], false);
        }
        NotificationManagerCompat.from(context).notify(notificationId,nBuilder.build());
    }

    @Override
    protected void onPreExecute() {
        //show progress bar in notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        nBuilder = new NotificationCompat.Builder(context, AlbumListActivity.CHANNEL_ID);
        nBuilder.setContentTitle(context.getString(R.string.sync_to_nextcloud,albumVM.getName().getValue()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_sync_black_24dp);
        nBuilder.setProgress(1, 0, true);
        notificationManager.notify(notificationId, nBuilder.build());
        Log.i(getClass().getName(),context.getString(R.string.sync_to_nextcloud,albumVM.getName().getValue()));
    }

    @Override
    protected void onCancelled(Integer result) {
        Log.i(getClass().getName(),"Sync cancelled.");
    }

    @Override
    protected void onPostExecute(Integer result) {
        int notIcon;
        switch (result) {
            case RESULT_NC_ERR:
                notificationMsg = context.getString(R.string.nextcloud_notification_err);
                notIcon = R.drawable.ic_error_black_24dp;
                break;
            case RESULT_OK:
                notificationMsg = context.getString(R.string.nextcloud_notification_finish_ok);
                notIcon = R.drawable.ic_check_black_24dp;
                //close notification on success after a few seconds
                new Handler().postDelayed(() -> NotificationManagerCompat.from(context).cancel(notificationId),5000);
                break;
            default:
                notificationMsg = "";
                notIcon = R.drawable.ic_error_black_24dp;
                break;
        }
        Log.d("SYNC",notificationMsg);
        nBuilder.setContentText(notificationMsg)
                .setProgress(0,0,false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setSmallIcon(notIcon);
        NotificationManagerCompat.from(context).notify(notificationId,nBuilder.build());
        //set progress flag off
        albumVM.setSyncInProgress(false);
    }

}
