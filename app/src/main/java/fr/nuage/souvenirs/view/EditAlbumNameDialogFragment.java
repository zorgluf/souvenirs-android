package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.Calendar;
import java.util.Date;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class EditAlbumNameDialogFragment extends DialogFragment {

    private final AlbumViewModel albumViewModel;
    private Date selectedDate;
    private int selectedMargin;

    public EditAlbumNameDialogFragment(AlbumViewModel albumViewModel) {
        super();
        this.albumViewModel = albumViewModel;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity(),R.style.AppTheme_MaterialDialog_Alert);
        //load view and set data
        View createLayout = getLayoutInflater().inflate(R.layout.edit_album_name_dialog,null);
        EditText albumEditText = createLayout.findViewById(R.id.editTextAlbumName);
        albumEditText.setText(albumViewModel.getAlbum().getName());
        CalendarView calendarView = createLayout.findViewById(R.id.calendarViewAlbum);
        calendarView.setDate(albumViewModel.getAlbum().getDate().getTime());
        selectedDate = albumViewModel.getAlbum().getDate();
        calendarView.setOnDateChangeListener((calendarView1, year, month, day) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year,month,day);
            selectedDate = new Date(calendar.getTimeInMillis());
        });
        Slider slider = createLayout.findViewById(R.id.marginSliderViewAlbum);
        slider.setValue(albumViewModel.getAlbum().getElementMargin());
        selectedMargin = albumViewModel.getAlbum().getElementMargin();
        slider.addOnChangeListener((s, value, fromUser) -> {
            selectedMargin = (int)value;
        });
        //build dialog with accept button
        builder.setTitle(R.string.dialog_edit_album_msg)
                .setView(createLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String albumName = albumEditText.getText().toString();
                    albumViewModel.getAlbum().setName(albumName);
                    albumViewModel.getAlbum().setDate(selectedDate);
                    albumViewModel.getAlbum().setElementMargin(selectedMargin);
                    dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        return builder.create();
    }
}
