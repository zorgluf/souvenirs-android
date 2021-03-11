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
        imagePath = Transformations.map(e.getLiveDataImagePath(), imagePath -> imagePath);
        transformType = Transformations.map(e.getLiveDataTransformType(), transformType -> transformType);
        offsetX = Transformations.map(e.getLdOffsetX(), offsetX -> offsetX);
        offsetY = Transformations.map(e.getLdOffsetY(), offsetY -> offsetY);
        zoom = Transformations.map(e.getLdZoom(), zoom -> zoom);
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
        } else if (e.getTransformType() == ImageElement.ZOOM_OFFSET) {
            if ((e.getZoom() != 100) || (e.getOffsetX() != 0) || (e.getOffsetY() != 0)) {
                //if custom zoom/offset, reset
                e.setZoom(100);
                e.setOffsetX(0);
                e.setOffsetY(0);
            } else {
                //if not, zoom out like a fit
                final int viewWidth = e.getRight()-e.getLeft();
                final int viewHeight = e.getBottom()-e.getTop();
                final int drawableWidth = e.getImageWidth();
                final int drawableHeight = e.getImageHeight();
                final float viewRatio = (float)viewWidth / viewHeight;
                final float drawableRatio = (float)drawableWidth / drawableHeight;
                final float scale = (drawableRatio/viewRatio);
                e.setZoom((int)(scale*100));
                e.setOffsetX((int)(50*(1-scale)/scale));
                e.setOffsetY((int)(50*(1-scale)/scale));
            }

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
