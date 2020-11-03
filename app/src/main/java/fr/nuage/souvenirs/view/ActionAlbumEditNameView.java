package fr.nuage.souvenirs.view;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ActionAlbumEditNameView extends LinearLayout {

    private EditText albumNameEdit;

    public ActionAlbumEditNameView(Context context) {
        super(context);
        albumNameEdit = new EditText(context);
        albumNameEdit.setSingleLine();
        addView(albumNameEdit);
    }


    public void setName(String value) {
        albumNameEdit.setText(value);
    }

    public String getName() {
        return albumNameEdit.getText().toString();
    }
}
