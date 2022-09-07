package fr.nuage.souvenirs.view.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class BlurTransformation extends BitmapTransformation {

    private static final float BITMAP_SCALE = 0.4f;
    private float blurRadius;
    private Context context;

    public BlurTransformation(float blurRadius, Context context) {
        super();
        this.blurRadius = blurRadius;
        this.context = context;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {

        int width = Math.round(outWidth * BITMAP_SCALE);
        int height = Math.round(outHeight * BITMAP_SCALE);
        Bitmap inputBitmap = Bitmap.createScaledBitmap(toTransform, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(blurRadius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BlurTransformation) {
            BlurTransformation other = (BlurTransformation) o;
            return (blurRadius == other.blurRadius);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Util.hashCode("blur transformation".hashCode());
        hash = Util.hashCode(blurRadius,hash);
        return hash;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update("blar transformation".getBytes());
        byte[] offsetXData = ByteBuffer.allocate(4).putFloat(blurRadius).array();
        messageDigest.update(offsetXData);
    }
}
