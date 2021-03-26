package fr.nuage.souvenirs.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.helpers.ElementMoveDragListener;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;


@SuppressLint("ViewConstructor")
public class ImageElementView extends AppCompatImageView implements View.OnLayoutChangeListener {

    private final Paint contourPaint;
    private final Rect rect = new Rect();
    private final ImageElementViewModel imageElementViewModel;

    public ImageElementView(Context context, PageViewModel pageViewModel, ImageElementViewModel imageElementViewModel) {
        super(context);

        contourPaint = new Paint();
        contourPaint.setAntiAlias(true);
        contourPaint.setColor(ContextCompat.getColor(context,R.color.primaryDarkColor));
        contourPaint.setStrokeWidth(getResources().getDimension(R.dimen.selected_strokewidth));
        contourPaint.setStyle(Paint.Style.STROKE);

        ElementMoveDragListener elementMoveDragListener = new ElementMoveDragListener(pageViewModel, imageElementViewModel, (AppCompatActivity)context);
        Observer<Boolean> activateListenersObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if ((pageViewModel.getLdEditMode().getValue() != null) && (pageViewModel.getLdPaintMode().getValue() != null)) {
                    if (pageViewModel.getLdEditMode().getValue()) {
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
        };
        pageViewModel.getLdPaintMode().observe((AppCompatActivity)context, activateListenersObserver);
        pageViewModel.getLdEditMode().observe((AppCompatActivity)context, activateListenersObserver);

        addOnLayoutChangeListener(this);

        this.imageElementViewModel = imageElementViewModel;

    }



    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSelected()) {
            getDrawingRect(rect);
            if (ImageElement.ZOOM_OFFSET != imageElementViewModel.getTransformType().getValue()) {
                //draw circles for image resize UI
                canvas.drawCircle(rect.right,rect.bottom,getResources().getDimension(R.dimen.selected_circle_ctl),contourPaint);
                canvas.drawCircle(rect.left,rect.top,getResources().getDimension(R.dimen.selected_circle_ctl),contourPaint);
            }
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
        if ((imageElementViewModel.getOffsetX().getValue() == null) || (imageElementViewModel.getOffsetY().getValue() == null) || (imageElementViewModel.getZoom().getValue() == null)) {
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
        matrix.postTranslate(imageElementViewModel.getOffsetX().getValue()*viewWidth/100f,imageElementViewModel.getOffsetY().getValue()*viewHeight/100f);
        matrix.postScale(imageElementViewModel.getZoom().getValue()/100f,imageElementViewModel.getZoom().getValue()/100f);
        setImageMatrix(matrix);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        // update matrix if style Zoom_offset
        if ((imageElementViewModel.getTransformType().getValue() != null) && (imageElementViewModel.getTransformType().getValue().equals(ImageElement.ZOOM_OFFSET))) {
            updateMatrix();
        }
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
            oldRight, int oldBottom) {
        // update matrix if style Zoom_offset
        if ((imageElementViewModel.getTransformType().getValue() != null) && (imageElementViewModel.getTransformType().getValue().equals(ImageElement.ZOOM_OFFSET))) {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                updateMatrix();
            }
        }
    }
}
