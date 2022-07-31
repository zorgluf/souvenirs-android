package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;

public class CreateAlbumDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity(),R.style.AppTheme_MaterialDialog_Alert);
        View createLayout = getLayoutInflater().inflate(R.layout.create_album_dialog,null);
        EditText albumEditText = createLayout.findViewById(R.id.editTextAlbumName);
        builder.setTitle(R.string.dialog_create_album_msg)
                .setView(createLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String albumName = albumEditText.getText().toString();
                    Album newAlbum = Albums.getInstance().createAlbum();
                    newAlbum.setName(albumName);
                    //open album in edit mode
                    AlbumListFragmentDirections.ActionNavAlbumListToNavAlbumEdit action = AlbumListFragmentDirections.actionNavAlbumListToNavAlbumEdit(newAlbum.getAlbumPath(),null);
                    Navigation.findNavController(getActivity(),R.id.main_navhost).navigate(action);
                    dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        return builder.create();
    }
}
