package fr.nuage.souvenirs.view.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.EditTextElementDialogFragment;
import fr.nuage.souvenirs.view.ImageActionModeCallback;
import fr.nuage.souvenirs.view.ImageElementView;
import fr.nuage.souvenirs.view.TextActionModeCallback;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class ElementMoveDragListener implements View.OnDragListener, View.OnLongClickListener, View.OnClickListener, View.OnTouchListener {

    private final static String SWITCH_DRAG = "SWITCH_DRAG";
    private final static String MOVE_DRAG = "MOVE_DRAG";
    private final static String RESIZE_DRAG_RIGHT_BOTTOM = "RESIZE_DRAG_RIGHT_BOTTOM";
    private final static String RESIZE_DRAG_LEFT_TOP = "RESIZE_DRAG_LEFT_TOP";

    private final PageViewModel pageVM;
    private final ElementViewModel elVM;
    private final GestureDetectorCompat gestureDetector;
    private static int activateMoveViewId = 0;
    private float initialX, initialY;
    private View view;
    private ScaleGestureDetector scaleGestureDetector;

    public ElementMoveDragListener(PageViewModel page, ElementViewModel el, AppCompatActivity activity) {
        pageVM = page;
        elVM = el;
        gestureDetector = new GestureDetectorCompat(activity, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                if ((elVM instanceof ImageElementViewModel) && (((ImageElementViewModel)elVM).getTransformType().getValue() == ImageElement.ZOOM_OFFSET)) {
                    ImageElementViewModel ei = (ImageElementViewModel)elVM;
                    ei.setOffset((int)(-distanceX/(float)view.getWidth()*100)+ei.getOffsetX().getValue(),(int)(-distanceY/(float)view.getHeight()*100)+ei.getOffsetY().getValue());
                    return true;
                }
                return false;
            }

        });
        if (elVM instanceof ImageElementViewModel)  {
            ((ImageElementViewModel)elVM).getTransformType().observe(activity, scaleType -> {
                if (scaleType.equals(ImageElement.ZOOM_OFFSET)) {
                    scaleGestureDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        private float sumScale;

                        @Override
                        public boolean onScaleBegin (ScaleGestureDetector detector) {
                            sumScale = 1;
                            return true;
                        }

                        @Override
                        public boolean onScale(ScaleGestureDetector scaleGestureDetector)
                        {
                            final float scale = scaleGestureDetector.getScaleFactor();
                            sumScale *= scale;
                            Matrix matrix = ((ImageElementView)view).getImageMatrix();
                            matrix.postScale(scale,scale,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY());
                            view.invalidate();
                            return true;
                        }

                        @Override
                        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                            ImageElementViewModel imageElementViewModel = (ImageElementViewModel)elVM;
                            final int viewWidth = view.getWidth();
                            final int viewHeight = view.getHeight();
                            final int drawableWidth = ((ImageView)view).getDrawable().getIntrinsicWidth();
                            final int drawableHeight = ((ImageView)view).getDrawable().getIntrinsicHeight();
                            final float widthScale = viewWidth / drawableWidth;
                            final float heightScale = viewHeight / drawableHeight;
                            final float scale = Math.max(widthScale, heightScale);
                            final int baseOffsetX = Math.round((viewWidth - drawableWidth * scale) / 2F);
                            final int baseOffsetY = Math.round((viewHeight - drawableHeight * scale) / 2F);
                            final float finalScale = (float)imageElementViewModel.getZoom().getValue()/100*sumScale;

                            float[] v = new float[9];
                            ((ImageElementView)view).getImageMatrix().getValues(v);
                            final int newOffsetX = (int)((v[Matrix.MTRANS_X]/finalScale-baseOffsetX)/viewWidth*100);
                            final int newOffsetY = (int)((v[Matrix.MTRANS_Y]/finalScale-baseOffsetY)/viewHeight*100);;

                            imageElementViewModel.setZoom((int)(finalScale*100));
                            imageElementViewModel.setOffset(newOffsetX,newOffsetY);
                        }
                    });
                }
            });
        }


    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {

        String dragType = (String)dragEvent.getLocalState();
        if (dragType.equals(SWITCH_DRAG)) {
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

        if (dragType.equals(MOVE_DRAG) || (dragType.startsWith("RESIZE_DRAG"))) {
            //handle move element drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    //capture all
                    initialX = dragEvent.getX();
                    initialY = dragEvent.getY();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (activateMoveViewId == 0) {
                        activateMoveViewId = view.getId();
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (activateMoveViewId == view.getId()) {
                        if (dragType.equals(MOVE_DRAG)) {
                            //move image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setX(view.getX()+x- initialX);
                            view.setY(view.getY()+y- initialY);
                        }
                        if (dragType.equals(RESIZE_DRAG_RIGHT_BOTTOM)) {
                            //change height/width of image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setRight(Math.round(view.getRight()+x- initialX));
                            view.setBottom(Math.round(view.getBottom()+y- initialY));
                            initialX = x;
                            initialY = y;
                        }
                        if (dragType.equals(RESIZE_DRAG_LEFT_TOP)) {
                            //change height/width of image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setLeft(Math.round(view.getLeft()+x- initialX));
                            view.setTop(Math.round(view.getTop()+y- initialY));
                            initialX = x;
                            initialY = y;
                        }
                    }
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (activateMoveViewId == view.getId()) {
                        activateMoveViewId = 0;
                        //set new element coordinate
                        int parentX = ((View)view.getParent()).getWidth();
                        int parentY = ((View)view.getParent()).getHeight();
                        int top = Math.round(view.getY()/parentY*100);
                        int left = Math.round(view.getX()/parentX*100);
                        int bottom = Math.round((view.getY()+view.getHeight())/parentY*100);
                        int right = Math.round((view.getX()+view.getWidth())/parentX*100);
                        elVM.setPosition(top,left,bottom,right);
                        elVM.bringToFront();
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
            if (view.isSelected()) {
            } else {
                ClipData dragData = ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, view.getTag().toString());
                view.startDrag(dragData, new View.DragShadowBuilder(view), SWITCH_DRAG, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (!pageVM.getPaintMode()) {
            if (view.isSelected()) {

            } else {
                if (elVM.getClass().equals(ImageElementViewModel.class)) {
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
            this.view = view;
            if (view.isSelected()) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!(elVM instanceof ImageElementViewModel) || (((ImageElementViewModel) elVM).getTransformType().getValue() != ImageElement.ZOOM_OFFSET)) {
                        //check if resize
                        int resize_radius = (int) (view.getResources().getDimension(R.dimen.selected_circle_ctl));
                        String dragAction = MOVE_DRAG;
                        if (motionEvent.getX() < resize_radius) {
                            if (motionEvent.getY() < resize_radius) {
                                dragAction = RESIZE_DRAG_LEFT_TOP;
                            }
                        } else if ((view.getWidth() - motionEvent.getX()) < resize_radius) {
                            if ((view.getHeight() - motionEvent.getY()) < resize_radius) {
                                dragAction = RESIZE_DRAG_RIGHT_BOTTOM;
                            }
                        }
                        //do drag
                        //move view to front before drag
                        view.bringToFront();
                        //start drag
                        view.startDrag(null, new View.DragShadowBuilder() {
                            @Override
                            public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
                                outShadowSize.set(1, 1);
                                outShadowTouchPoint.set(0, 0);
                            }
                        }, dragAction, 0);
                        return true;
                    }
                }
                if (gestureDetector != null) {
                    if (gestureDetector.onTouchEvent(motionEvent)) {
                        return true;
                    }
                }
                if (scaleGestureDetector != null) {
                    scaleGestureDetector.onTouchEvent(motionEvent);
                    if (scaleGestureDetector.isInProgress()) {
                        return true;
                    }
                }

            }
        }
        return false;
    }


}
