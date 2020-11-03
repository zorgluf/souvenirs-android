package fr.nuage.souvenirs.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.utils.NCCreateShare;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class ShareAlbumAsyncTask extends AsyncTask<Void, Integer, Integer> {

    private Context context;
    private AlertDialog dialog;
    private AlbumViewModel albumViewModel;
    private static final int RESULT_OK = 0;
    private static final int RESULT_ERR = 1;

    public ShareAlbumAsyncTask(Context context, AlbumViewModel albumViewModel) {
        this.context = context;
        this.albumViewModel = albumViewModel;
    }

    @Override
    protected Integer doInBackground(Void... voids) {

        //call to share album
        RemoteOperationResult result = new NCCreateShare(albumViewModel.getId().toString()).execute(NCUtils.getNCClient(context));
        if (result.isSuccess()) {
            String share_url = (String)(result.getData().get(0));
            //create intent to share url
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.nextcloud_notification_share_url)+" "+albumViewModel.getName().getValue());
            i.putExtra(Intent.EXTRA_TEXT, share_url);
            context.startActivity(i);
        } else {
            return RESULT_ERR;
        }
        return RESULT_OK;
    }

    @Override
    protected void onPreExecute() {
        //launch progress dialog
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        dialog = alertBuilder.setCancelable(false)
                .setView(new ProgressBar(context,null,android.R.attr.progressBarStyleLarge)).create();
        dialog.show();
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == RESULT_OK) {
            dialog.dismiss();
        } else {
            Log.w(getClass().getName(),"Error on album share creation.");
            Toast.makeText(context,"Error on album share creation.",Toast.LENGTH_LONG);
        }

    }

}
