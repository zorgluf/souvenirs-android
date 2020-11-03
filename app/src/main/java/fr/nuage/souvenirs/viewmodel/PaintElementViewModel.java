package fr.nuage.souvenirs.viewmodel;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.model.PaintElement;
import fr.nuage.souvenirs.view.ImageElementView;

public class PaintElementViewModel extends ImageElementViewModel {

    public final static int TOOL_PEN = 0;
    public final static int TOOL_ERASER = 1;

    private int toolSelected;
    private MutableLiveData<Integer> color = new MutableLiveData<>();

    public PaintElementViewModel(PaintElement e) {
        super(e);
        color.postValue(Color.BLACK);
    }

    public void setImage(Bitmap mBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(outputStream.toByteArray());
        setImage(byteArrayInputStream,"image/png");
    }

    public int getToolSelected() {
        return toolSelected;
    }

    public void setToolSelected(int toolSelected) {
        this.toolSelected = toolSelected;
    }

    public LiveData<Integer> getLdColor() {
        return color;
    }

    public void setColor(int color) {
        this.color.postValue(color);
    }
}
