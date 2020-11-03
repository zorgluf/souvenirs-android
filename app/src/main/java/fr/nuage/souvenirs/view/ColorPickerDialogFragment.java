package fr.nuage.souvenirs.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.ColorPickerBinding;

public class ColorPickerDialogFragment extends DialogFragment {

    private ColorPickerListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //inflate layout
        ColorPickerBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.color_picker, null, false);
        binding.setCallback(this);
        //build alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.color_picker_title)
                .setView(binding.getRoot());
        return builder.create();
    }

    public void setListener(ColorPickerListener listener) {
        this.listener = listener;
    }

    public void onColorSelected(int c) {
        if (listener != null) {
            //call listener
            listener.onColorPicked(c);
        }
        dismiss();
    }

    public interface ColorPickerListener {
        void onColorPicked(int color);
    }
}
