package fr.nuage.souvenirs.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.PageViewBinding;
import fr.nuage.souvenirs.databinding.PaintElementViewBinding;
import fr.nuage.souvenirs.databinding.TextElementViewBinding;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.view.helpers.ViewGenerator;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;
import fr.nuage.souvenirs.viewmodel.VideoElementViewModel;

public class PageView extends ConstraintLayout {

    public static final int SWING_DIRECTION_UP = 1;
    public static final int SWING_DIRECTION_DOWN = 2;

    private PageViewModel pageViewModel;
    private AlbumViewModel albumViewModel;
    private boolean editMode;

    public PageView(@NonNull Context context) {
        super(context);
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PageView,
                0, 0);
        try {
            editMode = a.getBoolean(R.styleable.PageView_editMode, false);
        } finally {
            a.recycle();
        }
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setViewModels(PageViewModel pageViewModel, AlbumViewModel albumViewModel) {
        this.pageViewModel = pageViewModel;
        this.albumViewModel = albumViewModel;
        initView();
    }

    private void initView() {
        removeAllViews();
        if ((pageViewModel != null) && (albumViewModel != null)) {
            PageViewBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),R.layout.page_view,this,true);
            binding.setPage(pageViewModel);

            setTransitionName(pageViewModel.getId().toString());

            ConstraintLayout pageLayout = binding.pageLayout;
            //listen to elements changes
            LifecycleOwner lifecycleOwner = (LifecycleOwner) Div.unwrap(getContext());
            pageViewModel.getLdElements().observe(lifecycleOwner, elementViewModels -> {
                //remove all
                pageLayout.removeAllViews();
                //rebuild layout
                if (elementViewModels != null) {
                    LayoutInflater inflater = LayoutInflater.from(pageLayout.getContext());
                    for (ElementViewModel e : elementViewModels) {
                        if (e.getClass() == TextElementViewModel.class) {
                            TextElementViewModel et = (TextElementViewModel) e;
                            //load xml layout and bind data
                            TextElementViewBinding elBinding = DataBindingUtil.inflate(inflater, R.layout.text_element_view, pageLayout,false);
                            elBinding.setLifecycleOwner(lifecycleOwner);
                            elBinding.setElement(et);
                            elBinding.setAlbum(albumViewModel);
                            elBinding.executePendingBindings();
                            elBinding.textElement.setPageViewModel(pageViewModel);
                            elBinding.textElement.setTextElementViewModel(et);
                            elBinding.textElement.setEditMode(editMode);
                            pageLayout.addView(elBinding.getRoot());
                        } else if ((e.getClass() == ImageElementViewModel.class) || (e.getClass() == VideoElementViewModel.class)) {
                            ImageElementViewModel ei = (ImageElementViewModel)e;
                            //load xml layout and bind data
                            ImageElementViewBinding elBinding = DataBindingUtil.inflate(inflater, R.layout.image_element_view, pageLayout,false);
                            elBinding.setLifecycleOwner(lifecycleOwner);
                            elBinding.setElement(ei);
                            elBinding.setAlbum(albumViewModel);
                            elBinding.executePendingBindings();
                            elBinding.imageImageview.setPageViewModel(pageViewModel);
                            elBinding.imageImageview.setImageElementViewModel(ei);
                            elBinding.imageImageview.setEditMode(editMode);
                            pageLayout.addView(elBinding.getRoot());
                        } else if (e.getClass() == PaintElementViewModel.class) {
                            PaintElementViewModel ep = (PaintElementViewModel)e;
                            //load xml layout and bind data
                            PaintElementViewBinding elBinding = DataBindingUtil.inflate(inflater, R.layout.paint_element_view, pageLayout,false);
                            elBinding.setLifecycleOwner(lifecycleOwner);
                            elBinding.setElement(ep);
                            elBinding.executePendingBindings();
                            elBinding.paintImageview.setViewModels(pageViewModel, ep);
                            pageLayout.addView(elBinding.getRoot());
                        } else if (e.getClass() == AudioElementViewModel.class) {
                            continue;
                        } else {
                                //unknown element : display default view
                                inflater.inflate(R.layout.unknown_element_view, pageLayout, true);
                        }
                    }
                }
            });
        }
    }


}
