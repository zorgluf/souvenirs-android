package fr.nuage.souvenirs.model;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class TilePageBuilder extends PageBuilder {

    private final Object[][][] PAGE_STYLE_MAP_TILE = {
            {
                // 1
                    { 0, 0, 100, 100 }
            },
            {
                    // 2 V (1 small)
                    { 0, 0, 100, 80 },
                    { 0, 80, 100, 100 }
            },
            {
                    // 2 V
                    { 0, 0, 100, 50 },
                    { 0, 50, 100, 100 }
            },
            {
                    // 2 H
                    { 0, 0, 50, 100 },
                    { 50, 0, 100, 100 }
            },
            {
                    // 3 1H + 2H
                    { 0, 0, 100, 50 },
                    { 0, 50, 50, 100 },
                    { 50, 50, 100, 100 }
            },
            {
                    // 3 2H + 1H
                    { 0, 0, 50, 50 },
                    { 50, 0, 100, 50 },
                    { 0, 50, 100, 100 }
            },
            {
                    // 3 1V + 2V
                    { 0, 0, 50, 100 },
                    { 50, 0, 100, 50 },
                    { 50, 50, 100, 100 }
            },
            {
                    // 3 2V + 1V
                    { 0, 0, 50, 50 },
                    { 0, 50, 50, 100 },
                    { 50, 0, 100, 100 }
            },
            {
                    // 4
                    { 0, 0, 50, 50 },
                    { 50, 0, 100, 50 },
                    { 0, 50, 50, 100 },
                    { 50, 50, 100, 100 }
            },
    };

    @Override
    public Object[][][] getPageStyleMap() {
        return PAGE_STYLE_MAP_TILE;
    }

    @Override
    public View genPreview(int style, ViewGroup parentView, LayoutInflater inflater) {
        ConstraintLayout pageView = (ConstraintLayout) inflater.inflate(R.layout.page_preview, parentView,false);
        ConstraintSet cs = new ConstraintSet();
        cs.clone(pageView);
        for (Object[] elDef: getPageStyleMap()[style]) {
            ConstraintLayout elView = (ConstraintLayout) inflater.inflate(R.layout.element_preview,pageView,false);
            ImageView im = (ImageView) elView.findViewById(R.id.preview_el_image);
            im.setImageDrawable(ContextCompat.getDrawable(parentView.getContext(),R.drawable.ic_image_black_24dp));
            Guideline left = (Guideline) elView.findViewById(R.id.guideline_left);
            left.setGuidelinePercent(((Integer)elDef[0]).floatValue()/100);
            Guideline top = (Guideline) elView.findViewById(R.id.guideline_top);
            top.setGuidelinePercent(((Integer)elDef[1]).floatValue()/100);
            Guideline right = (Guideline) elView.findViewById(R.id.guideline_right);
            right.setGuidelinePercent(((Integer)elDef[2]).floatValue()/100);
            Guideline bottom = (Guideline) elView.findViewById(R.id.guideline_bottom);
            bottom.setGuidelinePercent(((Integer)elDef[3]).floatValue()/100);
            elView.setId(View.generateViewId());
            pageView.addView(elView);
            cs.constrainWidth(elView.getId(),ConstraintSet.MATCH_CONSTRAINT);
            cs.constrainHeight(elView.getId(),ConstraintSet.MATCH_CONSTRAINT);
            cs.connect(elView.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT);
            cs.connect(elView.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT);
            cs.connect(elView.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP);
            cs.connect(elView.getId(),ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM);
        }
        cs.applyTo(pageView);
        return pageView;
    }



    /* create as much as wanted pages with style and text/images provided
     */
    @Override
    public void create(int style, AlbumViewModel albumVM, int position, ArrayList<Uri> images, ArrayList<String> texts) {
        if (texts == null) {
            texts = new ArrayList<>();
        }
        if (images == null) {
            images = new ArrayList<>();
        }
        int imCursor = 0;
        int txtCursor = 0;
        while ( (imCursor < images.size())  || (txtCursor < texts.size()) ) {
            Page p = albumVM.createPage(position);
            position = albumVM.getPosition(p.getId()) + 1;
            for (Object[] elDef: getPageStyleMap()[style]) {
                if (imCursor < images.size()) {
                    ImageElement e_img = new ImageElement((int)elDef[0],(int)elDef[1],(int)elDef[2],(int)elDef[3]);
                    e_img.setTransformType(ImageElement.ZOOM_OFFSET);
                    //reset offset
                    e_img.setOffsetX(0);
                    e_img.setOffsetY(0);
                    //reset zoom to center crop
                    e_img.setZoom(100);
                    //add to page
                    p.addElement(e_img);
                    InputStream input = getInputStreamFromUri(albumVM.getApplication().getContentResolver(), images.get(imCursor));
                    String mime = albumVM.getApplication().getContentResolver().getType(images.get(imCursor));
                    imCursor += 1;
                    if (input != null) {
                        e_img.setImage(input, mime);
                    }
                    continue;
                }
                if (txtCursor < texts.size()) {
                    String txt = texts.get(txtCursor);
                    txtCursor += 1;
                    Element e_txt = new TextElement(txt,(int)elDef[0],(int)elDef[1],(int)elDef[2],(int)elDef[3]);
                    p.addElement(e_txt);
                }
            }
        }
    }


    /*
    Apply style in place in page
     */
    @Override
    public void applyStyle(int style, Page page) {
        //seperate txt and im elements
        ArrayList<ImageElement> imageElementArrayList = new ArrayList<>();
        ArrayList<TextElement> textElementArrayList = new ArrayList<>();
        for (Element e : page.getElements()) {
            if (e.getClass().equals(TextElement.class)) {
                textElementArrayList.add((TextElement)e);
            }
            if (e.getClass().equals(ImageElement.class)) {
                imageElementArrayList.add((ImageElement)e);
            }
        }
        //read style template and apply
        int imCursor = 0;
        int txtCursor = 0;
        while ( (imCursor < imageElementArrayList.size()) || (txtCursor < textElementArrayList.size()) ) {
            for (Object[] elDef: getPageStyleMap()[style]) {
                if (imCursor < imageElementArrayList.size()) {
                    ImageElement e_img = imageElementArrayList.get(imCursor);
                    if (e_img != null) {
                        e_img.setTop((int)elDef[1]);
                        e_img.setBottom((int)elDef[3]);
                        e_img.setLeft((int)elDef[0]);
                        e_img.setRight((int)elDef[2]);
                        e_img.setTransformType(ImageElement.ZOOM_OFFSET);
                        //reset offset
                        e_img.setOffsetX(0);
                        e_img.setOffsetY(0);
                        //reset zoom to center crop
                        e_img.setZoom(100);

                        imCursor += 1;
                    }
                    continue;
                }
                if (txtCursor < textElementArrayList.size()) {
                    TextElement e_txt = textElementArrayList.get(txtCursor);
                    if (e_txt != null) {
                        e_txt.setTop((int)elDef[1]);
                        e_txt.setBottom((int)elDef[3]);
                        e_txt.setLeft((int)elDef[0]);
                        e_txt.setRight((int)elDef[2]);
                        txtCursor += 1;
                    }
                }
            }
        }
    }

    @Override
    public int getDefaultStyle(Page page) {
        int defStyle=-1;
        for (int i=0;i<getPageStyleMap().length;i++) {
            if (getPageStyleMap()[i].length == page.getElements().size()) {
                defStyle = i;
                break;
            }
        }
        return defStyle;
    }

    @Override
    public boolean isStyleFitted(int style, int imageNb, int textNb) {
        if ((imageNb == -1) || (textNb == -1)) {
            return true;
        }
        return getPageStyleMap()[style].length == (imageNb + textNb);
    }

}
