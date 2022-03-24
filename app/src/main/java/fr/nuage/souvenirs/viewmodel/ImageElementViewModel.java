package fr.nuage.souvenirs.viewmodel;

import android.provider.ContactsContract;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.io.InputStream;

import fr.nuage.souvenirs.model.ImageElement;

public class ImageElementViewModel extends ElementViewModel {

    private final LiveData<String> imagePath;
    private final LiveData<Integer> transformType;
    private final LiveData<Integer> offsetX;
    private final LiveData<Integer> offsetY;
    private final LiveData<Integer> zoom;
    private final LiveData<Boolean> isPano;

    public ImageElementViewModel(ImageElement e) {
        super(e);
        imagePath = Transformations.map(e.getLiveDataImagePath(), imagePath -> imagePath);
        transformType = Transformations.map(e.getLiveDataTransformType(), transformType -> transformType);
        offsetX = Transformations.map(e.getLdOffsetX(), offsetX -> offsetX);
        offsetY = Transformations.map(e.getLdOffsetY(), offsetY -> offsetY);
        zoom = Transformations.map(e.getLdZoom(), zoom -> zoom);
        isPano = Transformations.map(e.getLdIsPano(), isPano -> isPano);
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

    public LiveData<Boolean> getIsPano() {
        return isPano;
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
                final float viewRatio = viewWidth / (float)viewHeight;
                final float drawableRatio = drawableWidth / (float)drawableHeight;
                final float scale = Math.min(drawableRatio/viewRatio,viewRatio/drawableRatio);
                e.setZoom((int)(scale*100));
                e.setOffsetX((int)(50*(1/scale-1)));
                e.setOffsetY((int)(50*(1/scale-1)));
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

    public void zoomIn(double zoomFactor) {
        ImageElement imageElement = (ImageElement) element;
        imageElement.setZoom((int)(imageElement.getZoom()*(1+zoomFactor)));
        imageElement.setOffsetX((int)(imageElement.getOffsetX()-zoomFactor*(imageElement.getOffsetX()+50)));
        imageElement.setOffsetY((int)(imageElement.getOffsetY()-zoomFactor*(imageElement.getOffsetY()+50)));
    }

    public void zoomOut(double zoomFactor) {
        ImageElement imageElement = (ImageElement) element;
        imageElement.setZoom((int)(imageElement.getZoom()*(1-zoomFactor)));
        imageElement.setOffsetX((int)(imageElement.getOffsetX()+zoomFactor*(50+imageElement.getOffsetX())));
        imageElement.setOffsetY((int)(imageElement.getOffsetY()+zoomFactor*(50+imageElement.getOffsetY())));
    }
}
