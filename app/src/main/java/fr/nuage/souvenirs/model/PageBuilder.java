package fr.nuage.souvenirs.model;

import static fr.nuage.souvenirs.view.helpers.Div.getNameAndSizeFromUri;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class PageBuilder {

    private final Object[][][] PAGE_STYLE_MAP = {
            {
                //PAGE_STYLE_TXT
                    { "txt", 5, 5, 95, 95 }
            },
            {
                    //PAGE_STYLE_IMG_full
                    { "img", 2, 2, 98, 98 }
            },
            {
                    //PAGE_STYLE_IMG
                    { "img", 5, 5, 95, 95 }
            },
            {
                    //PAGE_STYLE_IMG_LEGEND_V
                    { "img", 2, 2, 98, 80 },
                    { "txt", 2, 80, 98, 98 }
            },
            {
                    //PAGE_STYLE_IMG_LEGEND_V_1/2
                    { "img", 2, 2, 98, 49 },
                    { "txt", 2, 51, 98, 98 }
            },
            {
                //PAGE_STYLE_IMG_LEGEND_H
                    { "img", 2, 2, 70, 98},
                    { "txt", 70, 2, 98, 98 }
            },
            {
                    //PAGE_STYLE_IMG_2_H
                    { "img", 2, 2, 98, 49 },
                    { "img", 2, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_2_V
                    { "img", 2, 2, 49, 98 },
                    { "img", 51, 2, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_2 overlap
                    { "img", 55, 2, 98, 45 },
                    { "img", 2, 20, 80, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 49 },
                    { "img", 51, 2, 98, 49 },
                    { "img", 2, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 49 },
                    { "txt", 51, 2, 98, 49 },
                    { "img", 2, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 98, 49 },
                    { "img", 2, 51, 49, 98 },
                    { "img", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 98, 49 },
                    { "img", 2, 51, 49, 98 },
                    { "txt", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 98 },
                    { "img", 51, 2, 98, 49 },
                    { "img", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 98 },
                    { "img", 51, 2, 98, 49 },
                    { "txt", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 49 },
                    { "img", 2, 51, 49, 98 },
                    { "img", 51, 2, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 49 },
                    { "txt", 2, 51, 49, 98 },
                    { "img", 51, 2, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_3
                    { "img", 2, 2, 49, 79 },
                    { "img", 51, 2, 98, 79 },
                    { "txt", 2, 81, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_4
                    { "img", 2, 2, 49, 49 },
                    { "img", 51, 2, 98, 49 },
                    { "img", 2, 51, 49, 98 },
                    { "img", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_LEGEND_4
                    { "img", 2, 2, 49, 49 },
                    { "img", 51, 2, 98, 49 },
                    { "img", 2, 51, 49, 98 },
                    { "txt", 51, 51, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_4
                    { "img", 2, 2, 98, 39 },
                    { "img", 2, 41, 32, 98 },
                    { "img", 34, 41, 65, 98 },
                    { "img", 67, 41, 98, 98 },
            },
            {
                    //PAGE_STYLE_IMG_LEGEND_4
                    { "img", 2, 2, 98, 39 },
                    { "img", 2, 41, 32, 98 },
                    { "img", 34, 41, 65, 98 },
                    { "txt", 67, 41, 98, 98 },
            },

    };

    public Object[][][] getPageStyleMap() {
        return PAGE_STYLE_MAP;
    }

    private int getNbImages(int style) {
        int nb = 0;
        for (int i=0;i<PAGE_STYLE_MAP[style].length;i++) {
            if (PAGE_STYLE_MAP[style][i][0].equals("img")) {
                nb += 1;
            }
        }
        return nb;
    }

    private int getNbTxt(int style) {
        int nb = 0;
        for (int i=0;i<PAGE_STYLE_MAP[style].length;i++) {
            if (PAGE_STYLE_MAP[style][i][0].equals("txt")) {
                nb += 1;
            }
        }
        return nb;
    }

    private boolean isConsumingImage(int styleIndex) {
        return (getNbImages(styleIndex) > 0);
    }

    private boolean isConsumingTxt(int styleIndex) {
        return (getNbTxt(styleIndex) > 0);
    }

    public View genPreview(int style, ViewGroup parentView, LayoutInflater inflater) {
        ConstraintLayout pageView = (ConstraintLayout) inflater.inflate(R.layout.page_preview,null);
        ConstraintSet cs = new ConstraintSet();
        cs.clone(pageView);
        for (Object[] elDef: PAGE_STYLE_MAP[style]) {
            ConstraintLayout elView = (ConstraintLayout) inflater.inflate(R.layout.element_preview,null);
            ImageView im = (ImageView) elView.findViewById(R.id.preview_el_image);
            switch ((String)elDef[0]) {
                case "txt":
                    im.setImageDrawable(parentView.getResources().getDrawable(R.drawable.ic_text_fields_black_24dp));
                    break;
                case "img":
                    im.setImageDrawable(parentView.getResources().getDrawable(R.drawable.ic_image_black_24dp));
                    break;
            }
            Guideline left = (Guideline) elView.findViewById(R.id.guideline_left);
            left.setGuidelinePercent(((Integer)elDef[1]).floatValue()/100);
            Guideline top = (Guideline) elView.findViewById(R.id.guideline_top);
            top.setGuidelinePercent(((Integer)elDef[2]).floatValue()/100);
            Guideline right = (Guideline) elView.findViewById(R.id.guideline_right);
            right.setGuidelinePercent(((Integer)elDef[3]).floatValue()/100);
            Guideline bottom = (Guideline) elView.findViewById(R.id.guideline_bottom);
            bottom.setGuidelinePercent(((Integer)elDef[4]).floatValue()/100);
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
            Log.w(PageBuilder.class.getName(),e);
            return null;
        }
        return input;
    }

    public void create(int style, AlbumViewModel albumVM, ArrayList<Uri> images, ArrayList<String> texts) {
        create(style,albumVM,albumVM.getLdPages().getValue().size(),images,texts);
    }

    /* create one page and import all image/text on the page, take default style
     */
    public void create(AlbumViewModel albumVM, int position, ArrayList<Uri> images, ArrayList<String> texts) {
        Page p = albumVM.createPage(position);
        if (images != null) {
            for (Uri uri: images) {
                ImageElement imageElement = p.createImageElement();
                InputStream input = getInputStreamFromUri(albumVM.getApplication().getContentResolver(), uri);
                String mime = albumVM.getApplication().getContentResolver().getType(uri);
                imageElement.setImage(input, mime);
                Div.NameSize nameSize = getNameAndSizeFromUri(uri,albumVM.getApplication().getContentResolver());
                imageElement.setName(nameSize.name);
                imageElement.setSize(nameSize.size);
                if (albumVM.getDefaultStyle().equals(Album.STYLE_TILE)) {
                    imageElement.setTransformType(ImageElement.ZOOM_OFFSET);
                }
            }
        }
        if (texts != null) {
            for (String text: texts) {
                TextElement textElement = p.createTextElement();
                textElement.setText(text);
            }
        }
        applyDefaultStyle(p);
    }

    /* create as much as wanted pages with style and text/images provided
     */
    public void create(int style, AlbumViewModel albumVM, int position, ArrayList<Uri> images, ArrayList<String> texts) {
        if (texts == null) {
            texts = new ArrayList<String>();
        }
        if (images == null) {
            images = new ArrayList<Uri>();
        }
        int imCursor = 0;
        int txtCursor = 0;
        while (((imCursor < images.size()) && (isConsumingImage(style))) || ((txtCursor < texts.size()) && (isConsumingTxt(style)))) {
            Page p = albumVM.createPage(position);
            position = albumVM.getPosition(p.getId()) + 1;
            for (Object[] elDef: PAGE_STYLE_MAP[style]) {
                switch ((String)elDef[0]) {
                    case "txt":
                        String txt = (txtCursor < texts.size()) ? texts.get(txtCursor) : "";
                        txtCursor += 1;
                        Element e_txt = new TextElement(txt,(int)elDef[1],(int)elDef[2],(int)elDef[3],(int)elDef[4]);
                        p.addElement(e_txt);
                        break;
                    case "img":
                        ImageElement e_img = new ImageElement((int)elDef[1],(int)elDef[2],(int)elDef[3],(int)elDef[4]);
                        p.addElement(e_img);
                        if (imCursor < images.size()) {
                            InputStream input = getInputStreamFromUri(albumVM.getApplication().getContentResolver(), images.get(imCursor));
                            String mime = albumVM.getApplication().getContentResolver().getType(images.get(imCursor));
                            imCursor += 1;
                            if (input != null) {
                                e_img.setImage(input, mime);
                            }
                        }
                }
            }
        }
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

    /*
    Apply style in place in page
     */
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
        while (((imCursor < imageElementArrayList.size()) && (isConsumingImage(style))) || ((txtCursor < textElementArrayList.size()) && (isConsumingTxt(style)))) {
            for (Object[] elDef: PAGE_STYLE_MAP[style]) {
                switch ((String)elDef[0]) {
                    case "txt":
                        TextElement e_txt = (txtCursor < textElementArrayList.size()) ? textElementArrayList.get(txtCursor) : null;
                        if (e_txt != null) {
                            e_txt.setTop((int)elDef[2]);
                            e_txt.setBottom((int)elDef[4]);
                            e_txt.setLeft((int)elDef[1]);
                            e_txt.setRight((int)elDef[3]);
                            txtCursor += 1;
                        }
                        break;
                    case "img":
                        ImageElement e_img = (imCursor < imageElementArrayList.size()) ? imageElementArrayList.get(imCursor) : null;
                        if (e_img != null) {
                            e_img.setTop((int)elDef[2]);
                            e_img.setBottom((int)elDef[4]);
                            e_img.setLeft((int)elDef[1]);
                            e_img.setRight((int)elDef[3]);
                            imCursor += 1;
                        }
                        break;
                }
            }
        }
    }

    /*
    return -1 if no default style found
     */
    public int getDefaultStyle(Page page) {
        int defStyle=-1;
        for (int i=0;i<PAGE_STYLE_MAP.length;i++) {
            if (getNbTxt(i) == page.getNbTxt()) {
                if (getNbImages(i) == page.getNbImage()) {
                    defStyle = i;
                    break;
                }
            }
        }
        return defStyle;
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

    public boolean isStyleFitted(int style, int imageNb, int textNb) {
        if ((imageNb != -1) && (getNbImages(style) != imageNb)) {
            return false;
        }
        if ((textNb != -1) && (getNbTxt(style) != textNb)) {
            return false;
        }
        return true;
    }
}
