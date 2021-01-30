package fr.nuage.souvenirs.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class DeleteShareDialogFragment extends DialogFragment {
    private AlbumViewModel albumViewModel;

    public DeleteShareDialogFragment(AlbumViewModel albumViewModel) {
        super();
        this.albumViewModel = albumViewModel;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.AppTheme_MaterialDialog_Alert);
        builder.setTitle(getResources().getString(R.string.dialog_delete_album_share_confirm, albumViewModel.getName().getValue()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    new DeleteShareTask(getContext()).execute(albumViewModel.getAlbumNC());
                    dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        return builder.create();
    }

    @SuppressLint("StaticFieldLeak")
    private class DeleteShareTask extends AsyncTask<AlbumNC, Void, Boolean> {
        private Context context;
        private AlertDialog dialog;

        public DeleteShareTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(AlbumNC... albumNC) {
            return albumNC[0].deleteShare();
        }

        @Override
        protected void onPreExecute() {
            //launch progress dialog
            dialog = new AlertDialog.Builder(context).setCancelable(false)
                    .setView(new ProgressBar(context,null,android.R.attr.progressBarStyleLarge)).create();
            dialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            if (!result) {
                Toast.makeText(context,R.string.delete_share_failure,Toast.LENGTH_LONG).show();
            }
        }
    }
}
