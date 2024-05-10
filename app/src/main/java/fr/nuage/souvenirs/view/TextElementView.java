package fr.nuage.souvenirs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.view.helpers.ElementMoveDragListener;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class TextElementView extends androidx.appcompat.widget.AppCompatTextView {

    private Paint contourPaint;
    private final Rect rect = new Rect();
    private PageViewModel pageViewModel;
    private TextElementViewModel textElementViewModel;
    private ElementMoveDragListener elementMoveDragListener;
    private boolean isEditMode = false;

    public TextElementView(Context context) {
        super(context);
        initView();
    }

    public TextElementView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }
    public TextElementView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initView();
    }

    private void initView() {
        contourPaint = new Paint();
        contourPaint.setAntiAlias(true);
        contourPaint.setColor(ContextCompat.getColor(getContext(),R.color.primaryDarkColor));
        contourPaint.setStrokeWidth(getResources().getDimension(R.dimen.selected_strokewidth));
        contourPaint.setStyle(Paint.Style.STROKE);

    }

    public void setPageViewModel(@NonNull PageViewModel pageViewModel) {
        this.pageViewModel = pageViewModel;
        if (this.textElementViewModel != null) {
            AppCompatActivity appCompatActivity = Div.unwrap(getContext());
            elementMoveDragListener = new ElementMoveDragListener(pageViewModel, textElementViewModel, appCompatActivity);
            pageViewModel.getLdPaintMode().observe(appCompatActivity, (b) -> {
                setListeners();
            });
        }
    }

    public void setTextElementViewModel(@NonNull TextElementViewModel textElementViewModel) {
        this.textElementViewModel = textElementViewModel;
        if (this.pageViewModel != null) {
            AppCompatActivity appCompatActivity = Div.unwrap(getContext());
            elementMoveDragListener = new ElementMoveDragListener(pageViewModel, textElementViewModel, appCompatActivity);
            pageViewModel.getLdPaintMode().observe(appCompatActivity, (b) -> {
                setListeners();
            });
        }
    }

    private void setListeners() {
        if (pageViewModel.getLdPaintMode().getValue() != null) {
            if (this.isEditMode) {
                if (!pageViewModel.getLdPaintMode().getValue()) {
                    setOnClickListener(elementMoveDragListener);
                    setOnTouchListener(elementMoveDragListener);
                    setOnLongClickListener(elementMoveDragListener);
                    setOnDragListener(elementMoveDragListener);
                    setClickable(true);
                    return;
                }
            }
        }
        setOnClickListener(null);
        setOnTouchListener(null);
        setOnLongClickListener(null);
        setOnDragListener(null);
        setClickable(false);
        setLongClickable(false);
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        setListeners();
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSelected()) {
            getDrawingRect(rect);
            //draw circles for image resize UI
            canvas.drawCircle(rect.right,rect.bottom,getResources().getDimension(R.dimen.selected_circle_ctl),contourPaint);
            canvas.drawCircle(rect.left,rect.top,getResources().getDimension(R.dimen.selected_circle_ctl),contourPaint);
            //draw box around image area
            int offset = (int) getResources().getDimension(R.dimen.selected_strokewidth)/2;
            rect.left += offset;
            rect.right -= offset;
            rect.top += offset;
            rect.bottom -= offset;
            canvas.drawRect(rect,contourPaint);
        }
    }
}
