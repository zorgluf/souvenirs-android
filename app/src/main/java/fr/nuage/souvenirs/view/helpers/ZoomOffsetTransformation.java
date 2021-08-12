package fr.nuage.souvenirs.view.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;

public class ZoomOffsetTransformation extends BitmapTransformation {

    private final int offsetX;
    private final int offsetY;
    private final int zoom;

    public ZoomOffsetTransformation(int offsetX, int offsetY, int zoom) {
        super();
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        final int drawableWidth = toTransform.getWidth();
        final int drawableHeight = toTransform.getHeight();
        final float widthScale = (float) outWidth / drawableWidth;
        final float heightScale = (float) outHeight / drawableHeight;
        final float scale = Math.max(widthScale, heightScale);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(((float) outWidth - drawableWidth * scale) / 2F,
                ((float) outHeight - drawableHeight * scale) / 2F);
        matrix.postTranslate(offsetX* (float) outWidth /100f,offsetY* (float) outHeight /100f);
        matrix.postScale(zoom/100f,zoom/100f);

        Bitmap newBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(toTransform, matrix, null);


        return newBitmap;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ZoomOffsetTransformation) {
            ZoomOffsetTransformation other = (ZoomOffsetTransformation) o;
            return ((offsetX == other.offsetX) && (offsetY == other.offsetY));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode("zoom offset transformation".hashCode(),
                Util.hashCode(offsetX+1000*offsetY));
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update("zoom offset transformation".getBytes());
        byte[] data = ByteBuffer.allocate(4).putInt(offsetX+1000*offsetY).array();
        messageDigest.update(data);
    }
}
