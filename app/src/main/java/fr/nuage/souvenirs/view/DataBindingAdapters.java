package fr.nuage.souvenirs.view;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;

public class DataBindingAdapters {

    @BindingAdapter("layout_constraintGuide_percent")
    public static void setLayoutConstraintGuidePercent(Guideline guideline, Integer percent) {
        if (percent != null) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            params.guidePercent = percent.floatValue()/100;
            guideline.setLayoutParams(params);
        }
    }


    @BindingAdapter(value = { "srcCompat", "android:scrollX", "android:scrollY", "android:scaleX"}, requireAll=false)
    public static void setSrcCompatZoomOffset(ImageElementView view, String imagePath, int offsetX, int offsetY, int scaleX) {
        if (imagePath != null) {
            if (imagePath.equals("")) {
                view.setImageDrawable(ContextCompat.getDrawable(view.getContext(),R.drawable.ic_image_black_24dp));
            } else {
                view.setOffsetX(offsetX);
                view.setOffsetY(offsetY);
                view.setZoom(scaleX);
                Glide.with(view.getContext()).load(new File(imagePath)).into(view);
            }
        } else {
            view.setImageDrawable(null);
        }
    }

    @BindingAdapter(value = { "srcCompat"}, requireAll=false)
    public static void setSrcCompatZoomOffset(ImageView view, String imagePath) {
        if (imagePath != null) {
            if (imagePath.equals("")) {
                view.setImageDrawable(ContextCompat.getDrawable(view.getContext(),R.drawable.ic_image_black_24dp));
            } else {
                Glide.with(view.getContext()).load(new File(imagePath)).into(view);
            }
        } else {
            view.setImageDrawable(null);
        }
    }

    @BindingAdapter(value = { "srcCompat" }, requireAll=false)
    public static void setSrcCompatZoomOffset(PaintElementView view, String imagePath) {
        if (imagePath != null) {
            if (!imagePath.equals("")) {
                Glide.with(view.getContext())
                        .asBitmap()
                        .load(new File(imagePath))
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                view.setFirstBitmap(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {   }
                        });
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

    @BindingAdapter(value = { "android:layout_marginStart", "android:layout_marginTop", "android:layout_marginEnd", "android:layout_marginBottom"}, requireAll=false)
    public static void setLayoutMarginLeft(View v, int marginLeft, int marginTop, int marginRight, int marginBottom) {
        if ((v.getParent() != null) && !(v instanceof PaintElementView)) {
            int height = ((View)(v.getParent())).getHeight();
            int width = ((View)(v.getParent())).getWidth();
            if ((height > 0) && (width > 0)) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
                params.setMargins((int)width * marginLeft / 100,
                        (int)height * marginTop / 100,
                        (int)(100 - marginRight) * width / 100,
                        (int)(100 - marginBottom) * height / 100);
                v.setLayoutParams(params);
            }
            ImageElementView imageElementView = v.findViewById(R.id.image_imageview);
            if (imageElementView != null) {
                imageElementView.updateMatrix();
            }
        }
    }

    public static void onLayoutChange(View view, ElementViewModel element, AlbumViewModel album) {
        if ((element != null) && (album != null)) {
            if ((element.getLeft().getValue() != null) && (element.getRight().getValue() != null) && (element.getTop().getValue() != null) && (element.getBottom().getValue() != null)) {
                if (album.getLdElementMargin().getValue() != null) {
                    setLayoutMarginLeft(view, element.getLeft().getValue() + album.getLdElementMargin().getValue(),
                            element.getTop().getValue() + album.getLdElementMargin().getValue(),
                            element.getRight().getValue() - album.getLdElementMargin().getValue(),
                            element.getBottom().getValue() - album.getLdElementMargin().getValue());
                }
            }
        }
    }

    @BindingAdapter("is_selected")
    public static void setSelected(View view, boolean selected) {
        view.setSelected(selected);
    }

}
