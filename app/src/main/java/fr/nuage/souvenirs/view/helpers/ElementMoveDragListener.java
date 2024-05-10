package fr.nuage.souvenirs.view.helpers;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.EditTextElementDialogFragment;
import fr.nuage.souvenirs.view.ImageElementView;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class ElementMoveDragListener implements View.OnDragListener, View.OnLongClickListener, View.OnClickListener, View.OnTouchListener {

    public final static String MOVE_DRAG = "SWITCH_DRAG";
    public final static String RESIZE_DRAG_RIGHT_BOTTOM = "RESIZE_DRAG_RIGHT_BOTTOM";
    public final static String RESIZE_DRAG_LEFT_TOP = "RESIZE_DRAG_LEFT_TOP";

    private final PageViewModel pageVM;
    private final ElementViewModel elVM;
    private static int activateMoveViewId = 0;
    private float initialX, initialY;
    private View view;
    private ScaleGestureDetector scaleGestureDetector;
    private Matrix imageMatrix;
    private boolean isZooming = false;
    private boolean isPaning = false;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    public static int RESIZE_DRAG_AMPLIFIER_FACTOR = 2;

    public ElementMoveDragListener(PageViewModel page, ElementViewModel el, AppCompatActivity activity) {
        pageVM = page;
        elVM = el;
        if (elVM instanceof ImageElementViewModel)  {
            ((ImageElementViewModel)elVM).getTransformType().observe(activity, scaleType -> {
                if (scaleType.equals(ImageElement.ZOOM_OFFSET)) {
                    scaleGestureDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                        @Override
                        public boolean onScaleBegin (ScaleGestureDetector detector) {
                            isZooming = true;
                            return true;
                        }

                        @Override
                        public boolean onScale(ScaleGestureDetector scaleGestureDetector)
                        {
                            final float scale = scaleGestureDetector.getScaleFactor();
                            if (scale > 0.01) {
                                imageMatrix.postScale(scale,scale,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY());
                                ((ImageElementView)view).setImageMatrix(imageMatrix);
                                view.invalidate();
                                return true;
                            } else {
                                return false;
                            }
                        }

                        @Override
                        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                            updateZoomOffset((ImageElementView)view,(ImageElementViewModel)elVM);
                            isZooming = false;
                        }
                    });
                }
            });
        }


    }

    private void updateZoomOffset(ImageElementView imageElementView, ImageElementViewModel imageElementViewModel) {
        final int viewWidth = imageElementView.getWidth();
        final int viewHeight = imageElementView.getHeight();
        final int drawableWidth = imageElementView.getDrawable().getIntrinsicWidth();
        final int drawableHeight = imageElementView.getDrawable().getIntrinsicHeight();
        final float widthScale = (float)viewWidth / drawableWidth;
        final float heightScale = (float)viewHeight / drawableHeight;
        final float scale = Math.max(widthScale, heightScale);
        final int baseOffsetX = Math.round((viewWidth - drawableWidth * scale) / 2F);
        final int baseOffsetY = Math.round((viewHeight - drawableHeight * scale) / 2F);

        float[] v = new float[9];
        imageElementView.getImageMatrix().getValues(v);
        final float secondScale = v[Matrix.MSCALE_X]/scale;
        final int newOffsetX = (int)((v[Matrix.MTRANS_X]/secondScale-baseOffsetX*scale)/viewWidth*100);
        final int newOffsetY = (int)((v[Matrix.MTRANS_Y]/secondScale-baseOffsetY*scale)/viewHeight*100);

        imageElementViewModel.setZoom((int)(secondScale*100));
        imageElementViewModel.setOffset(newOffsetX,newOffsetY);
    }

    private void updateOffsetOnDrag(View view, DragEvent dragEvent) {
        String dragType = (String)dragEvent.getLocalState();
        if (dragType.equals(RESIZE_DRAG_RIGHT_BOTTOM)) {
            //change height/width of image
            float x = dragEvent.getX();
            float y = dragEvent.getY();
            int height = ((View)(view.getParent().getParent())).getHeight();
            int width = ((View)(view.getParent().getParent())).getWidth();
            elVM.setPosition(
                    elVM.getTop().getValue(),
                    elVM.getLeft().getValue(),
                    elVM.getBottom().getValue() + (int)((y - initialY) * 100 * RESIZE_DRAG_AMPLIFIER_FACTOR)/height,
                    elVM.getRight().getValue() + (int)((x - initialX) * 100 * RESIZE_DRAG_AMPLIFIER_FACTOR)/width
            );
            initialX = x;
            initialY = y;
        }
        if (dragType.equals(RESIZE_DRAG_LEFT_TOP)) {
            //change height/width of image
            float x = dragEvent.getX();
            float y = dragEvent.getY();
            int height = ((View)(view.getParent().getParent())).getHeight();
            int width = ((View)(view.getParent()).getParent()).getWidth();
            elVM.setPosition(
                    elVM.getTop().getValue() + (int)((y - initialY) * 100 * RESIZE_DRAG_AMPLIFIER_FACTOR)/height,
                    elVM.getLeft().getValue() + (int)((x - initialX) * 100 * RESIZE_DRAG_AMPLIFIER_FACTOR)/width,
                    elVM.getBottom().getValue(),
                    elVM.getRight().getValue()
            );
            //initialX = x;
            //initialY = y;
        }
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {

        String dragType = (String)dragEvent.getLocalState();
        if (dragType.equals(MOVE_DRAG)) {
            //handle switch elements drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                case DragEvent.ACTION_DRAG_EXITED:
                    view.setAlpha((float)0.5);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_ENDED:
                    view.setAlpha(1);
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                case DragEvent.ACTION_DROP:
                    // Gets the page id to swap
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);
                    UUID oriElementUUID = UUID.fromString((String)item.getText());
                    UUID destElementUUID = elVM.getId().getValue();
                    pageVM.swapElements(oriElementUUID,destElementUUID);
                    return true;
                default:
                    break;
            }
        }

        if (dragType.startsWith("RESIZE_DRAG")) {
            //handle move element drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    //capture all
                    initialX = dragEvent.getX();
                    initialY = dragEvent.getY();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if ((activateMoveViewId == 0) || (activePointerId == view.getId())) {
                        activateMoveViewId = view.getId();
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (activateMoveViewId == view.getId()) {
                        updateOffsetOnDrag(view, dragEvent);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (activateMoveViewId == view.getId()) {
                        activateMoveViewId = 0;
                    }
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        if (!pageVM.getPaintMode()) {
            if (!view.isSelected()) {
                ClipData dragData = ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, view.getTag().toString());
                view.startDragAndDrop(dragData, new View.DragShadowBuilder(view), MOVE_DRAG, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (!pageVM.getPaintMode()) {
            if (!view.isSelected()) {
                if (elVM instanceof ImageElementViewModel) {
                    elVM.setSelected(true);
                }
                if (elVM.getClass().equals(TextElementViewModel.class)) {
                    elVM.setSelected(true);
                    EditTextElementDialogFragment.newInstance((TextElementViewModel) elVM).show(((AppCompatActivity)view.getContext()).getSupportFragmentManager(), "");
                }
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!pageVM.getPaintMode()) {
            //get view context for local instance
            if (this.view == null){
                this.view = view;
                if (view instanceof ImageElementView) {
                    imageMatrix = ((ImageElementView)view).getImageMatrix();
                }
            }

            if (view.isSelected()) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //check if resize
                        int resize_radius = (int) (view.getResources().getDimension(R.dimen.selected_circle_ctl));
                        String dragAction = null;
                        if (motionEvent.getX() < resize_radius) {
                            if (motionEvent.getY() < resize_radius) {
                                dragAction = RESIZE_DRAG_LEFT_TOP;
                            }
                        } else if ((view.getWidth() - motionEvent.getX()) < resize_radius) {
                            if ((view.getHeight() - motionEvent.getY()) < resize_radius) {
                                dragAction = RESIZE_DRAG_RIGHT_BOTTOM;
                            }
                        }
                        if (dragAction != null) {
                            //do drag
                            //move view to front before drag
                            ((View)view.getParent()).bringToFront();
                            //start drag
                            view.startDragAndDrop(null, new View.DragShadowBuilder() {
                                @Override
                                public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
                                    outShadowSize.set(1, 1);
                                    outShadowTouchPoint.set(0, 0);
                                }
                            }, dragAction, 0);
                            return true;
                        } else {
                            final int pointerIndex = motionEvent.getActionIndex();
                            initialX = motionEvent.getX(pointerIndex);
                            initialY = motionEvent.getY(pointerIndex);
                            activePointerId = motionEvent.getPointerId(0);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if ((elVM instanceof ImageElementViewModel) && (((ImageElementViewModel) elVM).getTransformType().getValue() == ImageElement.ZOOM_OFFSET)) {
                            isPaning = true;
                            final int pointerIndex = motionEvent.findPointerIndex(activePointerId);
                            final float x = motionEvent.getX(pointerIndex);
                            final float y = motionEvent.getY(pointerIndex);
                            imageMatrix.postTranslate(x- initialX,y- initialY);
                            ((ImageElementView)view).setImageMatrix(imageMatrix);
                            view.invalidate();
                            initialX = x;
                            initialY = y;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if ((elVM instanceof ImageElementViewModel) && (((ImageElementViewModel) elVM).getTransformType().getValue() == ImageElement.ZOOM_OFFSET)) {
                            activePointerId = MotionEvent.INVALID_POINTER_ID;
                            if (!isZooming && isPaning) {
                                updateZoomOffset((ImageElementView)view,(ImageElementViewModel)elVM);
                            }
                            isPaning = false;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        if ((elVM instanceof ImageElementViewModel) && (((ImageElementViewModel) elVM).getTransformType().getValue() == ImageElement.ZOOM_OFFSET)) {
                            final int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                            if (pointerId == activePointerId) {
                                // This was our active pointer going up. Choose a new
                                // active pointer and adjust accordingly.
                                final int newPointerIndex = activePointerId == 0 ? 1 : 0;
                                initialX = motionEvent.getX(newPointerIndex);
                                initialY = motionEvent.getY(newPointerIndex);
                                activePointerId = motionEvent.getPointerId(newPointerIndex);
                            }
                        }
                        break;
                }

                if (scaleGestureDetector != null) {
                    scaleGestureDetector.onTouchEvent(motionEvent);
                    return scaleGestureDetector.isInProgress();
                }

            }
        }
        return false;
    }


}
