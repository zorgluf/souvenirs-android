package fr.nuage.souvenirs.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;

public class PaintElementView extends AppCompatImageView implements View.OnTouchListener {

    private PaintElementViewModel paintElementViewModel;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint penPaint;
    private Paint eraserPaint;
    private float mX, mY;
    private Bitmap mBitmapToAdd;
    private static final float TOUCH_TOLERANCE = 4;

    public PaintElementView(Context context) {
        this(context,null,null);
    }

    public PaintElementView(Context context, PageViewModel pageViewModel, PaintElementViewModel paintElementViewModel) {
        super(context);

        this.paintElementViewModel = paintElementViewModel;

        //initialize pen paint
        penPaint = new Paint();
        penPaint.setAntiAlias(true);
        penPaint.setDither(true);
        penPaint.setColor(0xFF000000);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeWidth(12);

        //initialize eraser paint
        eraserPaint = new Paint();
        eraserPaint.setStrokeWidth(40);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        AppCompatActivity activity = Div.unwrap(getContext());
        //listen to color change
        paintElementViewModel.getLdColor().observe(activity, color ->  { setColor(color); });
        //listen to paint mode
        pageViewModel.getLdPaintMode().observe(activity, isPaintMode -> {
            //activate/deactivate draw on page
            setPaintMode(isPaintMode);
        });
    }

    public void setColor(int color) {
        penPaint.setColor(color);
    }

    public void setPaintMode(boolean ispaintMode) {
        if (ispaintMode) {
            setOnTouchListener(this);
        } else {
            setOnTouchListener(null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if ((w > 0) && (h > 0)) {
            if (mBitmap != null) {
                mBitmapToAdd = mBitmap;
            }
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            if (mBitmapToAdd != null) {
                mCanvas.drawBitmap(mBitmapToAdd,new Rect(0,0,mBitmapToAdd.getWidth(),mBitmapToAdd.getHeight()),new Rect(0,0,mCanvas.getWidth(),mCanvas.getHeight()),mBitmapPaint);
                mBitmapToAdd = null;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(0x00FFFFFF);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        canvas.drawPath(mPath, penPaint);
    }

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, penPaint);
        //commit to model
        paintElementViewModel.setImage(mBitmap);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (paintElementViewModel.getToolSelected()==PaintElementViewModel.TOOL_ERASER) {
                    mCanvas.drawCircle(x,y,40,eraserPaint);
                } else {
                    touch_start(x, y);
                }
                v.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (paintElementViewModel.getToolSelected()==PaintElementViewModel.TOOL_ERASER) {
                    mCanvas.drawCircle(x,y,40,eraserPaint);
                } else {
                    touch_move(x, y);
                }
                v.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (paintElementViewModel.getToolSelected()==PaintElementViewModel.TOOL_ERASER) {
                    //commit changes to model
                    paintElementViewModel.setImage(mBitmap);
                } else {
                    touch_up();
                }
                v.invalidate();
                break;
        }
        return true;
    }

    public void setFirstBitmap(Bitmap resource) {
        if (mCanvas != null) {
            mCanvas.drawBitmap(resource,new Rect(0,0,resource.getWidth(),resource.getHeight()),new Rect(0,0,mCanvas.getWidth(),mCanvas.getHeight()),mBitmapPaint);
        } else {
            mBitmapToAdd = resource;
        }
    }
}
