package fr.nuage.souvenirs.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.nc.APIProvider;

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
        try {
            String share_url = APIProvider.getApi().createShare(albumViewModel.getId().toString()).execute().body();
            if (share_url != null) {
                //create intent to share url
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.nextcloud_notification_share_url)+" "+albumViewModel.getName().getValue());
                i.putExtra(Intent.EXTRA_TEXT, share_url);
                context.startActivity(i);
            } else {
                throw new IOException("Null token");
            }
        } catch (IOException e) {
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
