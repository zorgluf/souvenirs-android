package fr.nuage.souvenirs.view;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class SelectAlbumStyleDialogFragment extends DialogFragment {

    private AlbumViewModel albumViewModel;

    public SelectAlbumStyleDialogFragment(AlbumViewModel albumViewModel) {
        super();
        this.albumViewModel = albumViewModel;
    }
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.AppTheme_MaterialDialog_Alert);
        View selectLayout = getLayoutInflater().inflate(R.layout.select_album_style,null);
        RadioGroup styleRadioGroup = selectLayout.findViewById(R.id.createAlbumRadioGroupStyle);
        //set style
        RadioButton styleRadioButton = selectLayout.findViewWithTag(albumViewModel.getDefaultStyle());
        styleRadioButton.setChecked(true);

        builder.setView(selectLayout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RadioButton selectedStyle = selectLayout.findViewById(styleRadioGroup.getCheckedRadioButtonId());
                        albumViewModel.setDefaultStyle((String)selectedStyle.getTag());
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
