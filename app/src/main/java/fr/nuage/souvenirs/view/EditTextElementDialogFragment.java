package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class EditTextElementDialogFragment extends BottomSheetDialogFragment {

    private TextElementViewModel eText;

    public static EditTextElementDialogFragment newInstance(TextElementViewModel eText) {
        EditTextElementDialogFragment dialog = new EditTextElementDialogFragment();
        dialog.eText = eText;
        return dialog;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_edit_text,container);
        Button bOK = dialogView.findViewById(R.id.button_validate);
        bOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = dialogView.findViewById(R.id.editText_textElement);
                eText.setText(editText.getText().toString());
                eText.setSelected(false);
                dismiss();
            }
        });
        Button bDel = dialogView.findViewById(R.id.button_del);
        bDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                eText.del();
                dismiss();
            }
        });
        EditText editText = dialogView.findViewById(R.id.editText_textElement);
        editText.setText(eText.getText().getValue());
        editText.requestFocus();
        //show keyboard
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialogView;
    }

}
