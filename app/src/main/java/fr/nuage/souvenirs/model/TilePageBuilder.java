package fr.nuage.souvenirs.model;

import static fr.nuage.souvenirs.view.helpers.Div.getNameAndSizeFromUri;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class TilePageBuilder {

    private final Object[][][] PAGE_STYLE_MAP_TILE = {
            {
                // 1
                    { 0, 0, 100, 100 }
            },
            {
                    // 2 V (1 small) -> title style
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

    public Object[][][] getPageStyleMap() {
        return PAGE_STYLE_MAP_TILE;
    }

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

    public static InputStream getInputStreamFromUri(ContentResolver cr, Uri uri) {
        InputStream input;
        try {
            input = cr.openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.w(TilePageBuilder.class.getName(),e);
            return null;
        }
        return input;
    }

    public void create(AlbumViewModel albumVM, int position, ArrayList<Uri> images, ArrayList<String> texts, ArrayList<Uri> videos) {
        if (images != null) {
            for (Uri uri: images) {
                Page p = albumVM.createPage(position);
                ImageElement imageElement = p.createImageElement();
                InputStream input = getInputStreamFromUri(albumVM.getApplication().getContentResolver(), uri);
                String mime = albumVM.getApplication().getContentResolver().getType(uri);
                imageElement.setImage(input, mime);
                Div.NameSize nameSize = getNameAndSizeFromUri(uri,albumVM.getApplication().getContentResolver());
                imageElement.setName(nameSize.name);
                imageElement.setSize(nameSize.size);
                imageElement.setTransformType(ImageElement.ZOOM_OFFSET);
                applyDefaultStyle(p);
            }
        }
        if (videos != null) {
            for (Uri uri: videos) {
                Page p = albumVM.createPage(position);
                VideoElement videoElement = p.createVideoElement();
                InputStream input = getInputStreamFromUri(albumVM.getApplication().getContentResolver(), uri);
                String mime = albumVM.getApplication().getContentResolver().getType(uri);
                videoElement.setVideo(input, mime);
                Div.NameSize nameSize = getNameAndSizeFromUri(uri,albumVM.getApplication().getContentResolver());
                videoElement.setName(nameSize.name);
                videoElement.setSize(nameSize.size);
                videoElement.setTransformType(ImageElement.ZOOM_OFFSET);
                applyDefaultStyle(p);
            }
        }
        if (texts != null) {
            for (String text: texts) {
                Page p = albumVM.createPage(position);
                TextElement textElement = p.createTextElement();
                textElement.setText(text);
                applyDefaultStyle(p);
            }
        }
    }

    public void applyDefaultStyle(Page page) {
        //select default style
        int defStyle = getDefaultStyle(page);
        if (defStyle == -1) {
            //if no default, grid style
            int columns=(int)Math.ceil(Math.sqrt((double)page.getElements().size()));
            int rows=(int)Math.ceil((double)page.getElements().size()/(double)columns);
            for (int i=0;i<page.getElements().size();i++) {
                Element el = page.getElements().get(i);
                el.setTop((int)((double)(i/columns)*100/rows));
                el.setBottom((int)((((double)(i/columns))+1)*100/rows));
                el.setLeft((int)((double)(i%columns)*(100/columns)));
                el.setRight((int)((double)((i%columns)+1)*(100/columns)));
            }
        } else {
            applyStyle(defStyle,page);
        }
    }

    /* create as much as wanted pages with style and text/images provided
     */
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
    public void applyStyle(int style, Page page) {
        //seperate txt and im elements
        ArrayList<Element> imageElementArrayList = new ArrayList<>();
        ArrayList<TextElement> textElementArrayList = new ArrayList<>();
        for (Element e : page.getElements()) {
            if (e.getClass().equals(TextElement.class)) {
                textElementArrayList.add((TextElement)e);
            }
            if ((e instanceof ImageElement) && !(e instanceof PaintElement)) {
                imageElementArrayList.add(e);
            }
        }
        //read style template and apply
        int imCursor = 0;
        int txtCursor = 0;
        while ( (imCursor < imageElementArrayList.size()) || (txtCursor < textElementArrayList.size()) ) {
            for (Object[] elDef: getPageStyleMap()[style]) {
                if (imCursor < imageElementArrayList.size()) {
                    ImageElement e_img = (ImageElement) imageElementArrayList.get(imCursor);
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

    public int getDefaultStyle(Page page) {
        int visibleElements = 0;
        boolean hasTextElement = false;
        boolean areAllPortrait = true;
        for (Element element: page.getElements()) {
            if (element instanceof PaintElement) {
                continue;
            }
            if ((element instanceof ImageElement) || (element instanceof TextElement) || (element instanceof VideoElement)) {
                visibleElements += 1;
            }
            if (element instanceof TextElement) {
                hasTextElement = true;
            }
            if (element instanceof ImageElement) {
                if (!Utils.isImagePortrait((ImageElement)element)) {
                    areAllPortrait = false;
                }
            }
        }
        //for 2 elements
        if (visibleElements == 2) {
            //if text, apply title style
            if (hasTextElement) {
                return 1;
            }
            //if images, test if portrait or landscape
            if (areAllPortrait) {
                return 3;
            } else {
                return 2;
            }
        }
        //generic : choose first matching style in list
        int defStyle=-1;
        for (int i=0;i<getPageStyleMap().length;i++) {
            if (getPageStyleMap()[i].length == visibleElements) {
                defStyle = i;
                break;
            }
        }
        return defStyle;
    }

    public boolean isStyleFitted(int style, int imageNb, int textNb) {
        if ((imageNb == -1) || (textNb == -1)) {
            return true;
        }
        return getPageStyleMap()[style].length == (imageNb + textNb);
    }

    public void switchStyle(int style, AlbumViewModel albumVM, Page page) {
        //extract texts and images
        ArrayList<String> texts = new ArrayList<>();
        ArrayList<Uri> images = new ArrayList<>();
        //parse page and fill text and image arrays
        for (Element e : page.getElements()) {
            if (e.getClass().equals(TextElement.class)) {
                texts.add(((TextElement)e).getText());
            }
            if (e instanceof ImageElement) {
                //FIXME : will not work with video
                images.add(Uri.fromFile(new File(((ImageElement)e).getImagePath())));
            }
        }
        int position = albumVM.getPosition(page.getId());
        create(style,albumVM,position,images,texts);
    }

}
