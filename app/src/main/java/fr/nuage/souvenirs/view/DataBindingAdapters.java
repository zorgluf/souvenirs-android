package fr.nuage.souvenirs.view;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.helpers.ZoomOffsetTransformation;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;

public class DataBindingAdapters {

    @BindingAdapter("layout_constraintGuide_percent")
    public static void setLayoutConstraintGuidePercent(Guideline guideline, Integer percent) {
        if (percent != null) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            params.guidePercent = percent.floatValue()/100;
            guideline.setLayoutParams(params);
        }
    }


    @BindingAdapter(value = { "srcCompat", "android:scrollX", "android:scrollY" }, requireAll=false)
    public static void setSrcCompatZoomOffset(ImageView view, String imagePath, int offsetX, int offsetY) {
        if (imagePath != null) {
            if (imagePath.equals("")) {
                view.setImageDrawable(view.getResources().getDrawable(R.drawable.ic_image_black_24dp));
            } else {
                if (view.getScaleType() == ImageView.ScaleType.MATRIX) {
                    Glide.with(view.getContext()).load(new File(imagePath)).dontTransform().transform(new ZoomOffsetTransformation(offsetX, offsetY)).into(view);
                } else {
                    Glide.with(view.getContext()).load(new File(imagePath)).into(view);
                }
            }
        }
    }

    @BindingAdapter("srcCompat")
    public static void setSrcCompat(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable)drawable).start();
        }
    }

    @BindingAdapter("android:scaleType")
    public static void setScaleType(ImageView view, Integer transformType) {
        if (transformType != null) {
            if (transformType == ImageElement.FIT) {
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else if (transformType == ImageElement.CENTERCROP){
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                view.setScaleType(ImageView.ScaleType.MATRIX);
            }
        }
    }

    @BindingAdapter("android:elevation")
    public static void setElevation(View v,int elevation) {
        v.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,elevation,v.getResources().getDisplayMetrics()));
    }

    @BindingAdapter("android:padding")
    public static void setPadding(View v,int padding) {
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,padding,v.getResources().getDisplayMetrics()));
        v.setPadding(px,px,px,px);
    }

    @BindingAdapter("android:tint")
    public static void setTint(ImageView v, int color) {
        v.setColorFilter(color);
    }

}
