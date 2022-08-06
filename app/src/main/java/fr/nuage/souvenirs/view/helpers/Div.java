package fr.nuage.souvenirs.view.helpers;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.appcompat.app.AppCompatActivity;

public class Div {

    public static NameSize getNameAndSizeFromUri(Uri uri, ContentResolver contentResolver) {
        NameSize NameSizeResult = new NameSize();
        try (Cursor cursor = contentResolver
                .query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                NameSizeResult.name = cursor.getString(nameIndex);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                NameSizeResult.size = 0;
                if (!cursor.isNull(sizeIndex)) {
                    NameSizeResult.size = cursor.getInt(sizeIndex);
                }
            }
        }
        return NameSizeResult;
    }

    public static class NameSize {
        public String name;
        public int size;
    }

    public static AppCompatActivity unwrap(Context context) {
        while (!(context instanceof AppCompatActivity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        return (AppCompatActivity) context;
    }
}
