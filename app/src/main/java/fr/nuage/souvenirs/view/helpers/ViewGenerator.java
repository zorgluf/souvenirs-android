package fr.nuage.souvenirs.view.helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.ImageElementView;
import fr.nuage.souvenirs.view.PaintElementView;
import fr.nuage.souvenirs.view.TextElementView;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class ViewGenerator {

    /*
generate view based on paintElementViewModel
*/
    public static PaintElementView generateView(PageViewModel pageViewModel, PaintElementViewModel paintElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen paintview
        PaintElementView paintElementView = new PaintElementView(parentViewGroup.getContext(),pageViewModel,paintElementViewModel);
        paintElementView.setId(View.generateViewId());
        //add to parent
        parentViewGroup.addView(paintElementView);
        //set constraints
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) paintElementView.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        ConstraintSet set = new ConstraintSet();
        set.clone(parentViewGroup);
        set.connect(paintElementView.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0);
        set.connect(paintElementView.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0);
        set.connect(paintElementView.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0);
        set.connect(paintElementView.getId(),ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0);
        set.applyTo(parentViewGroup);
        //define observables for data binding
        paintElementViewModel.getImagePath().observe(lifecycleOwner, s -> {
            if (s != null) {
                if (!s.equals("")) {
                    Glide.with(paintElementView.getContext())
                            .asBitmap()
                            .load(new File(s))
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    paintElementView.setFirstBitmap(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {   }
                            });
                }
            }
        });
        ConstraintSet set1 = new ConstraintSet();
        set1.clone(parentViewGroup);
        set1.constrainPercentWidth(paintElementView.getId(), 1);
        set1.constrainPercentHeight(paintElementView.getId(), 1);
        set1.applyTo(parentViewGroup);

        return paintElementView;
    }

    /*
    generate view based on imageelementviewmodel
    */
    public static View generateView(PageViewModel pageViewModel, ImageElementViewModel imageElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen imageview
        ImageElementView imageView = new ImageElementView(parentViewGroup.getContext(),pageViewModel,imageElementViewModel);
        imageView.setId(View.generateViewId());
        imageView.setScrollContainer(true);
        //add to parent
        parentViewGroup.addView(imageView);
        //set constraints
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        ConstraintSet set = new ConstraintSet();
        set.clone(parentViewGroup);
        set.connect(imageView.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0);
        set.connect(imageView.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0);
        set.connect(imageView.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0);
        set.connect(imageView.getId(),ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0);
        set.applyTo(parentViewGroup);
        //define observables for data binding
        imageElementViewModel.getTransformType().observe(lifecycleOwner, (Observer<Integer>) i -> {
            if (i != null) {
                if (i.intValue() == ImageElement.CENTERCROP) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else if (i.intValue() == ImageElement.FIT) {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else if (i.intValue() == ImageElement.ZOOM_OFFSET) {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                }
                //force re-glide because of bug on glide when ScaleType.CENTER_CROP on image creation (no fit_center possible)
                ((ImageElement)imageElementViewModel.getElement()).setImagePath(((ImageElement)imageElementViewModel.getElement()).getImagePath());
            }
        });
        imageElementViewModel.getImagePath().observe(lifecycleOwner, s -> {
            if (s != null) {
                if (s.equals("")) {
                    imageView.setImageDrawable(imageView.getResources().getDrawable(R.drawable.ic_image_black_24dp));
                } else {
                    Glide.with(imageView.getContext()).load(new File(s)).into(imageView);
                }
            }
        });
        Observer<Integer> observer = integer -> {
            if ((imageElementViewModel.getRight().getValue() != null) && (imageElementViewModel.getLeft().getValue() != null)
                    && (imageElementViewModel.getTop().getValue() != null) && (imageElementViewModel.getBottom().getValue() != null)) {
                ConstraintSet set1 = new ConstraintSet();
                set1.clone(parentViewGroup);
                set1.constrainPercentWidth(imageView.getId(), ((float) imageElementViewModel.getRight().getValue() - imageElementViewModel.getLeft().getValue()) / 100);
                set1.setHorizontalBias(imageView.getId(), ((float) imageElementViewModel.getLeft().getValue()) / (100 - imageElementViewModel.getRight().getValue() + imageElementViewModel.getLeft().getValue()));
                set1.constrainPercentHeight(imageView.getId(), ((float) imageElementViewModel.getBottom().getValue() - imageElementViewModel.getTop().getValue()) / 100);
                set1.setVerticalBias(imageView.getId(), ((float) imageElementViewModel.getTop().getValue()) / (100 - imageElementViewModel.getBottom().getValue() + imageElementViewModel.getTop().getValue()));
                set1.applyTo(parentViewGroup);
            }
        };
        imageElementViewModel.getLeft().observe(lifecycleOwner, observer);
        imageElementViewModel.getRight().observe(lifecycleOwner, observer);
        imageElementViewModel.getTop().observe(lifecycleOwner, observer);
        imageElementViewModel.getBottom().observe(lifecycleOwner, observer);
        imageElementViewModel.getId().observe(lifecycleOwner, new Observer<UUID>() {
            @Override
            public void onChanged(UUID id) {
                imageView.setTag(id);
            }
        });
        imageElementViewModel.getIsSelected().observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                imageView.setSelected(aBoolean);
            }
        });
        Observer<Integer> offsetObserver = integer -> {
            if ((imageElementViewModel.getOffsetX().getValue() != null) && (imageElementViewModel.getOffsetY().getValue() != null)) {
                imageView.updateMatrix();
            }
        };
        imageElementViewModel.getOffsetX().observe(lifecycleOwner, offsetObserver);
        imageElementViewModel.getOffsetY().observe(lifecycleOwner, offsetObserver);
        return imageView;
    }

    /*
    generate view based on viewmodel
    */
    public static View generateView(PageViewModel pageViewModel, TextElementViewModel textElementViewModel, ConstraintLayout parentViewGroup, LifecycleOwner lifecycleOwner) {
        //gen textview
        TextView textView = new TextElementView(parentViewGroup.getContext(),pageViewModel,textElementViewModel);
        textView.setId(View.generateViewId());
        textView.setClickable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        }
        textView.setHint(R.string.text_element_hint);
        //add to parent
        parentViewGroup.addView(textView);
        //set constraints
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) textView.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        ConstraintSet set = new ConstraintSet();
        set.clone(parentViewGroup);
        set.connect(textView.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0);
        set.connect(textView.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0);
        set.connect(textView.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0);
        set.connect(textView.getId(),ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0);
        set.applyTo(parentViewGroup);
        //define observables for data binding
        textElementViewModel.getText().observe(lifecycleOwner, s -> textView.setText(s));
        Observer<Integer> observer = integer -> {
            if ((textElementViewModel.getRight().getValue() != null) && (textElementViewModel.getLeft().getValue() != null)
                    && (textElementViewModel.getTop().getValue() != null) && (textElementViewModel.getBottom().getValue() != null)) {
                ConstraintSet set1 = new ConstraintSet();
                set1.clone(parentViewGroup);
                set1.constrainPercentWidth(textView.getId(), ((float) textElementViewModel.getRight().getValue() - textElementViewModel.getLeft().getValue()) / 100);
                set1.setHorizontalBias(textView.getId(), ((float) textElementViewModel.getLeft().getValue()) / (100 - textElementViewModel.getRight().getValue() + textElementViewModel.getLeft().getValue()));
                set1.constrainPercentHeight(textView.getId(), ((float) textElementViewModel.getBottom().getValue() - textElementViewModel.getTop().getValue()) / 100);
                set1.setVerticalBias(textView.getId(), ((float) textElementViewModel.getTop().getValue()) / (100 - textElementViewModel.getBottom().getValue() + textElementViewModel.getTop().getValue()));
                set1.applyTo(parentViewGroup);
            }
        };
        textElementViewModel.getLeft().observe(lifecycleOwner, observer);
        textElementViewModel.getRight().observe(lifecycleOwner, observer);
        textElementViewModel.getTop().observe(lifecycleOwner, observer);
        textElementViewModel.getBottom().observe(lifecycleOwner, observer);
        textElementViewModel.getId().observe(lifecycleOwner, id -> textView.setTag(id));
        textElementViewModel.getIsSelected().observe(lifecycleOwner, aBoolean -> textView.setSelected(aBoolean));
        return textView;
    }


}
