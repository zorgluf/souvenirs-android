package fr.nuage.souvenirs.view;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;

public class CreateAlbumDialogFragment extends DialogFragment {

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity(),R.style.AppTheme_MaterialDialog_Alert);
        final EditText albumEditText = new EditText(getActivity());
        albumEditText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        albumEditText.setSingleLine();
        builder.setTitle(R.string.dialog_create_album_msg)
                .setView(albumEditText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String albumName = albumEditText.getText().toString();
                        Album newAlbum = Albums.getInstance().createAlbum();
                        newAlbum.setName(albumName);
                        //open album in edit mode
                        AlbumListFragmentDirections.ActionNavAlbumListToNavAlbumEdit action = AlbumListFragmentDirections.actionNavAlbumListToNavAlbumEdit(newAlbum.getAlbumPath(),null);
                        Navigation.findNavController(getActivity(),R.id.main_navhost).navigate(action);
                        dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }
}
