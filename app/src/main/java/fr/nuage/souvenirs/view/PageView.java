package fr.nuage.souvenirs.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.PageViewBinding;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;
import fr.nuage.souvenirs.view.helpers.ViewGenerator;

public class PageView extends ConstraintLayout {

    private PageViewModel pageViewModel;

    public PageView(@NonNull Context context) {
        super(context);
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPageViewModel(PageViewModel pageViewModel) {
        this.pageViewModel = pageViewModel;
        if (pageViewModel != null) {
            initView();
        }
    }

    private void initView() {
        PageViewBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),R.layout.page_view,this,true);
        binding.setPage(pageViewModel);

        setTransitionName(pageViewModel.getId().toString());

        ConstraintLayout pageLayout = binding.pageLayout;
        //listen to elements changes
        pageViewModel.getElements().observe((AppCompatActivity)getContext(), elementViewModels -> {
            //remove all
            pageLayout.removeAllViewsInLayout();
            //rebuild layout
            if (elementViewModels != null) {
                LayoutInflater inflater1 = LayoutInflater.from(pageLayout.getContext());
                for (ElementViewModel e : elementViewModels) {
                    if (e.getClass() == TextElementViewModel.class) {
                        TextElementViewModel et = (TextElementViewModel) e;
                        ViewGenerator.generateView(pageViewModel, et, pageLayout, (AppCompatActivity)getContext());
                    } else if (e.getClass() == ImageElementViewModel.class) {
                        ImageElementViewModel ei = (ImageElementViewModel) e;
                        ViewGenerator.generateView(pageViewModel, ei, pageLayout, (AppCompatActivity)getContext());
                    } else if (e.getClass() == PaintElementViewModel.class) {
                        PaintElementViewModel ep = (PaintElementViewModel) e;
                        ViewGenerator.generateView(pageViewModel, ep, pageLayout, (AppCompatActivity)getContext());
                    } else {
                        //unknown element : display default view
                        inflater1.inflate(R.layout.unknown_element_view, pageLayout, true);
                    }
                }
            }
        });
    }


}
