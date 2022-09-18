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
import fr.nuage.souvenirs.databinding.PageViewBinding;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.view.helpers.ViewGenerator;
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

    public void setPageViewModel(PageViewModel pageViewModel) {
        this.pageViewModel = pageViewModel;
        initView();
    }

    private void initView() {
        if (pageViewModel != null) {
            PageViewBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),R.layout.page_view,this,true);
            binding.setPage(pageViewModel);

            setTransitionName(pageViewModel.getId().toString());


            ConstraintLayout pageLayout = binding.pageLayout;
            //listen to elements changes
            LifecycleOwner lifecycleOwner = (LifecycleOwner) Div.unwrap(getContext());
            pageViewModel.getLdElements().observe(lifecycleOwner, elementViewModels -> {
                //remove all
                pageLayout.removeAllViewsInLayout();
                //rebuild layout
                if (elementViewModels != null) {
                    LayoutInflater inflater1 = LayoutInflater.from(pageLayout.getContext());
                    for (ElementViewModel e : elementViewModels) {
                        if (e.getClass() == TextElementViewModel.class) {
                            TextElementViewModel et = (TextElementViewModel) e;
                            ViewGenerator.generateView(pageViewModel, et, pageLayout, lifecycleOwner,editMode);
                        } else if (e.getClass() == ImageElementViewModel.class) {
                            ImageElementViewModel ei = (ImageElementViewModel) e;
                            ViewGenerator.generateView(pageViewModel, ei, pageLayout, lifecycleOwner, editMode);
                        } else if (e.getClass() == PaintElementViewModel.class) {
                            PaintElementViewModel ep = (PaintElementViewModel) e;
                            ViewGenerator.generateView(pageViewModel, ep, pageLayout, lifecycleOwner, editMode);
                        } else if (e.getClass() == VideoElementViewModel.class) {
                            VideoElementViewModel ep = (VideoElementViewModel) e;
                            ViewGenerator.generateView(pageViewModel, ep, pageLayout, lifecycleOwner, editMode);
                        } else if (e.getClass() == AudioElementViewModel.class) {
                            continue;
                        } else {
                                //unknown element : display default view
                                inflater1.inflate(R.layout.unknown_element_view, pageLayout, true);
                        }
                    }
                }
            });
        } else {
            removeAllViewsInLayout();
        }

    }


}
