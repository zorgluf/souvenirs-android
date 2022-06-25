package fr.nuage.souvenirs.view.helpers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.ImageElementView;
import fr.nuage.souvenirs.view.PaintElementView;
import fr.nuage.souvenirs.view.TextElementView;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;
import fr.nuage.souvenirs.viewmodel.VideoElementViewModel;

public class ViewGenerator {

    public static int convertDpToPixel(int dp, Context context){
        return (int)(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private  static View applyDefaultElementView(ConstraintLayout parentView, View view, ElementViewModel elementViewModel,
                                                 LifecycleOwner lifecycleOwner) {
        view.setId(View.generateViewId());
        //add to parent
        parentView.addView(view);
        //set constraints
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        ConstraintSet set = new ConstraintSet();
        set.clone(parentView);
        set.connect(view.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0);
        set.connect(view.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0);
        set.connect(view.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0);
        set.connect(view.getId(),ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0);
        set.applyTo(parentView);
        //observe change on position inside page
        Observer<Integer> observer = integer -> {
            if ((elementViewModel.getRight().getValue() != null) && (elementViewModel.getLeft().getValue() != null)
                    && (elementViewModel.getTop().getValue() != null) && (elementViewModel.getBottom().getValue() != null)) {
                ConstraintSet set1 = new ConstraintSet();
                set1.clone(parentView);
                set1.constrainPercentWidth(view.getId(), ((float) elementViewModel.getRight().getValue() - elementViewModel.getLeft().getValue()) / 100);
                set1.setHorizontalBias(view.getId(), ((float) elementViewModel.getLeft().getValue()) / (100 - elementViewModel.getRight().getValue() + elementViewModel.getLeft().getValue()));
                set1.constrainPercentHeight(view.getId(), ((float) elementViewModel.getBottom().getValue() - elementViewModel.getTop().getValue()) / 100);
                set1.setVerticalBias(view.getId(), ((float) elementViewModel.getTop().getValue()) / (100 - elementViewModel.getBottom().getValue() + elementViewModel.getTop().getValue()));
                set1.applyTo(parentView);
            }
        };
        elementViewModel.getLeft().observe(lifecycleOwner, observer);
        elementViewModel.getRight().observe(lifecycleOwner, observer);
        elementViewModel.getTop().observe(lifecycleOwner, observer);
        elementViewModel.getBottom().observe(lifecycleOwner, observer);
        elementViewModel.getId().observe(lifecycleOwner, view::setTag);
        elementViewModel.getIsSelected().observe(lifecycleOwner, view::setSelected);
        return view;
    }

    /*
generate view based on paintElementViewModel
*/
    public static PaintElementView generateView(PageViewModel pageViewModel, PaintElementViewModel paintElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen paintview
        PaintElementView paintElementView = new PaintElementView(parentViewGroup.getContext(),pageViewModel,paintElementViewModel);
        //apply default params
        applyDefaultElementView(parentViewGroup,paintElementView,paintElementViewModel,lifecycleOwner);
        //define observables for data binding
        paintElementViewModel.getImagePath().observe(lifecycleOwner, s -> {
            if (s != null) {
                if (!s.equals("")) {
                    Glide.with(paintElementView.getContext())
                            .asBitmap()
                            .load(new File(s))
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                    paintElementView.setFirstBitmap(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {   }
                            });
                }
            }
        });
        return paintElementView;
    }

    public static View generateView(PageViewModel pageViewModel, VideoElementViewModel videoElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        View imageView = generateView(pageViewModel,(ImageElementViewModel) videoElementViewModel,parentViewGroup,lifecycleOwner);
        //gen video icon
        AppCompatImageView imageViewIcon = new AppCompatImageView(parentViewGroup.getContext());
        imageViewIcon.setId(View.generateViewId());
        imageViewIcon.setImageResource(R.drawable.ic_baseline_videocam_24);
        parentViewGroup.addView(imageViewIcon);
        ImageViewCompat.setImageTintList(imageViewIcon, ColorStateList.valueOf(parentViewGroup.getResources().getColor(R.color.secondaryColor)));
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentViewGroup);
        constraintSet.constrainWidth(imageViewIcon.getId(),convertDpToPixel(48,parentViewGroup.getContext()));
        constraintSet.constrainHeight(imageViewIcon.getId(),convertDpToPixel(48,parentViewGroup.getContext()));
        constraintSet.connect(imageViewIcon.getId(),ConstraintSet.TOP,imageView.getId(),ConstraintSet.TOP,convertDpToPixel(8,parentViewGroup.getContext()));
        constraintSet.connect(imageViewIcon.getId(),ConstraintSet.LEFT,imageView.getId(),ConstraintSet.LEFT,convertDpToPixel(8,parentViewGroup.getContext()));
        constraintSet.applyTo(parentViewGroup);

        return imageView;
    }

    /*
    generate view based on imageelementviewmodel
    */
    public static View generateView(PageViewModel pageViewModel, ImageElementViewModel imageElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen imageview
        ImageElementView imageView = new ImageElementView(parentViewGroup.getContext(),pageViewModel,imageElementViewModel);
        imageView.setScrollContainer(true);
        //apply default params
        applyDefaultElementView(parentViewGroup,imageView,imageElementViewModel,lifecycleOwner);
        //define observables for data binding
        imageElementViewModel.getTransformType().observe(lifecycleOwner, (Observer<Integer>) i -> {
            if (i != null) {
                if (i == ImageElement.CENTERCROP) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else if (i == ImageElement.FIT) {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else if (i == ImageElement.ZOOM_OFFSET) {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                }
                //force re-glide because of bug on glide when ScaleType.CENTER_CROP on image creation (no fit_center possible)
                ((ImageElement)imageElementViewModel.getElement()).setImagePath(((ImageElement)imageElementViewModel.getElement()).getImagePath());
            }
        });
        imageElementViewModel.getImagePath().observe(lifecycleOwner, s -> {
            if (s != null) {
                if (s.equals("")) {
                    imageView.setImageDrawable(ResourcesCompat.getDrawable(imageView.getResources(),R.drawable.ic_image_black_24dp,null));
                } else {
                    Glide.with(imageView.getContext()).load(new File(s)).into(imageView);
                }
            }
        });
        Observer<Integer> zoomOffsetObserver = integer -> {
            if ((imageElementViewModel.getOffsetX().getValue() != null) && (imageElementViewModel.getOffsetY().getValue() != null) && (imageElementViewModel.getZoom().getValue() != null)) {
                imageView.updateMatrix();
            }
        };
        imageElementViewModel.getOffsetX().observe(lifecycleOwner, zoomOffsetObserver);
        imageElementViewModel.getOffsetY().observe(lifecycleOwner, zoomOffsetObserver);
        imageElementViewModel.getZoom().observe(lifecycleOwner, zoomOffsetObserver);
        if (imageElementViewModel.getIsPano().getValue()) {
            //gen pano icon
            AppCompatImageView imageViewIcon = new AppCompatImageView(parentViewGroup.getContext());
            imageViewIcon.setId(View.generateViewId());
            imageViewIcon.setImageResource(R.drawable.ic_baseline_panorama_horizontal_24);
            parentViewGroup.addView(imageViewIcon);
            ImageViewCompat.setImageTintList(imageViewIcon, ColorStateList.valueOf(parentViewGroup.getResources().getColor(R.color.secondaryColor)));
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentViewGroup);
            constraintSet.constrainWidth(imageViewIcon.getId(),convertDpToPixel(48,parentViewGroup.getContext()));
            constraintSet.constrainHeight(imageViewIcon.getId(),convertDpToPixel(48,parentViewGroup.getContext()));
            constraintSet.connect(imageViewIcon.getId(),ConstraintSet.TOP,imageView.getId(),ConstraintSet.TOP,convertDpToPixel(8,parentViewGroup.getContext()));
            constraintSet.connect(imageViewIcon.getId(),ConstraintSet.LEFT,imageView.getId(),ConstraintSet.LEFT,convertDpToPixel(8,parentViewGroup.getContext()));
            constraintSet.applyTo(parentViewGroup);
        }
        return imageView;
    }

    /*
    generate view based on viewmodel
    */
    public static View generateView(PageViewModel pageViewModel, TextElementViewModel textElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen textview
        TextView textView = new TextElementView(parentViewGroup.getContext(),pageViewModel,textElementViewModel);
        textView.setClickable(true);
        textView.setHint(R.string.text_element_hint);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //apply default params
        applyDefaultElementView(parentViewGroup,textView,textElementViewModel,lifecycleOwner);
        //define observables for data binding
        textElementViewModel.getText().observe(lifecycleOwner, s -> {
            textView.setText(s);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if ( (s==null) || (s.equals("")) ) {
                    textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                } else {
                    textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                }
            }
        });
        return textView;
    }


}
