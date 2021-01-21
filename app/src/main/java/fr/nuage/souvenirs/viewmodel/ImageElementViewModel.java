package fr.nuage.souvenirs.viewmodel;

import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.ImageElementView;

public class ImageElementViewModel extends ElementViewModel {

    private LiveData<String> imagePath;
    private LiveData<Integer> transformType;
    private LiveData<Integer> offsetX;
    private LiveData<Integer> offsetY;
    private LiveData<Integer> zoom;

    public ImageElementViewModel(ImageElement e) {
        super(e);
        imagePath = Transformations.map(e.getLiveDataImagePath(), imagePath -> {
            return imagePath;
        });
        transformType = Transformations.map(e.getLiveDataTransformType(), transformType -> {
            return transformType;
        });
        offsetX = Transformations.map(e.getLdOffsetX(), offsetX -> {
            return offsetX;
        });
        offsetY = Transformations.map(e.getLdOffsetY(), offsetY -> {
            return offsetY;
        });
        zoom = Transformations.map(e.getLdZoom(), zoom -> {
            return zoom;
        });
    }

    public LiveData<Integer> getOffsetX() {
        return offsetX;
    }

    public LiveData<Integer> getOffsetY() {
        return offsetY;
    }

    public LiveData<Integer> getZoom() {
        return zoom;
    }

    public LiveData<String> getImagePath() {
        return imagePath;
    }

    public LiveData<Integer> getTransformType() {
        return transformType;
    }

    public void clearImage() {
        ((ImageElement)element).setImage("",null);
    }

    public void setImage(InputStream input, String mimeType) {
        ((ImageElement)element).setImage(input,mimeType);
    }

    public void setImage(String localImagePath, String mimeType) {
        ((ImageElement)element).setImage(localImagePath,mimeType);
    }

    public String getMimeType() {
        return ((ImageElement)element).getMimeType();
    }

    public void switchTransformType() {
        ImageElement e = (ImageElement)element;
        if (e.getTransformType() == ImageElement.FIT) {
            e.setTransformType(ImageElement.CENTERCROP);
        } else if (e.getTransformType() == ImageElement.CENTERCROP) {
            e.setTransformType(ImageElement.FIT);
        }
    }

    public void setAsAlbumImage() {
        ((ImageElement)element).setAsAlbumImage();
    }

    public void setOffset(int offsetX, int offsetY) {
        ((ImageElement)element).setOffsetX(offsetX);
        ((ImageElement)element).setOffsetY(offsetY);
    }

    public void setZoom(int zoom) {
        ((ImageElement)element).setZoom(zoom);
    }
}
