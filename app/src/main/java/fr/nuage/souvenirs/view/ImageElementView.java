package fr.nuage.souvenirs.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.view.helpers.ElementMoveDragListener;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;



public class ImageElementView extends AppCompatImageView implements View.OnLayoutChangeListener {

    private Paint contourPaint;
    private final Rect rect = new Rect();
    private ImageElementViewModel imageElementViewModel;
    private PageViewModel pageViewModel;
    private ElementMoveDragListener elementMoveDragListener;
    private boolean isEditMode = false;
    private int offsetX = 0;
    private int offsetY = 0;
    private int zoom = 100;

    public ImageElementView(Context context) {
        super(context);
        initView();
    }

    public ImageElementView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }
    public ImageElementView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initView();
    }

    private void initView() {
        contourPaint = new Paint();
        contourPaint.setAntiAlias(true);
        contourPaint.setColor(ContextCompat.getColor(getContext(),R.color.primaryDarkColor));
        contourPaint.setStrokeWidth(getResources().getDimension(R.dimen.selected_strokewidth));
        contourPaint.setStyle(Paint.Style.STROKE);

        addOnLayoutChangeListener(this);
    }

    public void setPageViewModel(@NonNull PageViewModel pageViewModel) {
        this.pageViewModel = pageViewModel;
        if (this.imageElementViewModel != null) {
            AppCompatActivity appCompatActivity = Div.unwrap(getContext());
            elementMoveDragListener = new ElementMoveDragListener(pageViewModel, imageElementViewModel, appCompatActivity);
            pageViewModel.getLdPaintMode().observe(appCompatActivity, (b) -> {
                setListeners();
            });
        }
    }

    public void setImageElementViewModel(@NonNull ImageElementViewModel imageElementViewModel) {
        this.imageElementViewModel = imageElementViewModel;
        if (this.pageViewModel != null) {
            AppCompatActivity appCompatActivity = Div.unwrap(getContext());
            elementMoveDragListener = new ElementMoveDragListener(pageViewModel, imageElementViewModel, appCompatActivity);
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
        assert pageViewModel != null;
        assert imageElementViewModel != null;

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

    public void updateMatrix() {
        if (getDrawable() == null) {
            return;
        }
        final float viewWidth = getWidth();
        final float viewHeight = getHeight();
        final int drawableWidth = getDrawable().getIntrinsicWidth();
        final int drawableHeight = getDrawable().getIntrinsicHeight();
        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;
        final float scale = Math.max(widthScale, heightScale);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                (viewHeight - drawableHeight * scale) / 2F);
        matrix.postTranslate(offsetX*viewWidth/100f,offsetY*viewHeight/100f);
        matrix.postScale(zoom/100f,zoom/100f);
        setImageMatrix(matrix);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        // update matrix if style Zoom_offset
        updateMatrix();
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
            oldRight, int oldBottom) {
        // update matrix if style Zoom_offset
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateMatrix();
        }
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }
}
