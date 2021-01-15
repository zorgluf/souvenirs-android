package fr.nuage.souvenirs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.ElementMoveDragListener;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;


public class ImageElementView extends AppCompatImageView implements View.OnLayoutChangeListener {

    private Paint contourPaint;
    private Rect rect = new Rect();
    private ImageElementViewModel imageElementViewModel;

    public ImageElementView(Context context, PageViewModel pageViewModel, ImageElementViewModel imageElementViewModel) {
        super(context);

        contourPaint = new Paint();
        contourPaint.setAntiAlias(true);
        contourPaint.setColor(getResources().getColor(R.color.primaryDarkColor));
        contourPaint.setStrokeWidth(getResources().getDimension(R.dimen.selected_strokewidth));
        contourPaint.setStyle(Paint.Style.STROKE);

        ElementMoveDragListener elementMoveDragListener = new ElementMoveDragListener(pageViewModel, imageElementViewModel, (AppCompatActivity)context);
        pageViewModel.getLdPaintMode().observe((AppCompatActivity)context, paintMode -> {
            if (paintMode) {
                setOnClickListener(null);
                setOnTouchListener(null);
                setOnLongClickListener(null);
                setOnDragListener(null);
            } else {
                setOnClickListener(elementMoveDragListener);
                setOnTouchListener(elementMoveDragListener);
                setOnLongClickListener(elementMoveDragListener);
                setOnDragListener(elementMoveDragListener);
            }
        });

        addOnLayoutChangeListener(this);

        this.imageElementViewModel = imageElementViewModel;

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
        matrix.preTranslate(-imageElementViewModel.getOffsetX().getValue()*viewWidth/100f,-(float)imageElementViewModel.getOffsetY().getValue()*viewHeight/100f);
        setImageMatrix(matrix);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        // update matrix
        updateMatrix();
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
            oldRight, int oldBottom) {
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateMatrix();
        }
    }
}
