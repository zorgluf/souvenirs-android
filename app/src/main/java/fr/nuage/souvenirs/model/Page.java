package fr.nuage.souvenirs.model;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.model.nc.PageNC;

public class Page {

    private final MutableLiveData<ArrayList<Element>> ldElementsList = new MutableLiveData<ArrayList<Element>>();
    private ArrayList<Element> elementsList;
    private Album albumParent;
    private UUID id;
    private Date lastEditDate;


    public Page() {
        setElements(new ArrayList<Element>());
    }

    public void setAlbumParent(Album a) {
        albumParent = a;
    }

    public Album getAlbum() {
        return albumParent;
    }

    public void setElements(ArrayList<Element> ee) {
        setElements(ee,true);
    }

    public void setElements(ArrayList<Element> ee, boolean save) {
        elementsList = ee;
        ldElementsList.postValue(elementsList);
        if (save) {
            onChange();
        }
    }

    /*
    add element to page
    add at the end, except if there is a paint element
     */
    public void addElement(Element e) {
        ArrayList<Element> tmp = getElements();
        e.setPageParent(this);
        if (getPaintElement() == null) {
            tmp.add(e);
        } else {
            tmp.add(tmp.size()-1,e);
        }

        setElements(tmp);
    }

    public void addElement(Element e, int position) {
        ArrayList<Element> tmp = getElements();
        e.setPageParent(this);
        tmp.add(position,e);
        setElements(tmp);
    }

    public void addElements(ArrayList<Element> ee) {
        ArrayList<Element> tmp = getElements();
        for (Element e : ee) {
            e.setPageParent(this);
        }
        tmp.addAll(ee);
        setElements(tmp);
    }

    public void delElement(Element element) {
        if (getElements().contains(element)) {
            ArrayList<Element> tmp = getElements();
            tmp.remove(element);
            setElements(tmp);
        }
        new TilePageBuilder().applyDefaultStyle(this);
    }

    public ImageElement createImageElement() {
        ImageElement imageElement = new ImageElement();
        addElement(imageElement);
        return imageElement;
    }

    public TextElement createTextElement() {
        TextElement textElement = new TextElement();
        addElement(textElement);
        return textElement;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id",getId().toString());
            JSONArray elements = new JSONArray();
            for( Element p: elementsList ) {
                elements.put(p.toJSON());
            }
            json.put("elements",elements);
            json.put("lastEditDate",new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(lastEditDate));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    public static Page fromJSON(JSONObject jsonObject, Album album) throws JSONException {
        Page p = new Page();
        p.setAlbumParent(album);
        p.fromJSON(jsonObject);
        return p;
    }

    public void fromJSON(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("id")) {
            id = UUID.fromString(jsonObject.getString("id"));
        }
        if (jsonObject.has("elements")) {
            JSONArray jelements = jsonObject.getJSONArray("elements");
            ArrayList<Element> elements = new ArrayList<Element>();
            for (int i = 0; i < jelements.length(); i++) {
                Element e = Element.fromJSON(jelements.getJSONObject(i),this);
                elements.add(e);
            }
            setElements(elements,false);
        }
        if (jsonObject.has("lastEditDate")) {
            try {
                setLastEditDate(new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(jsonObject.getString("lastEditDate")));
            } catch (Exception e) {
                setLastEditDate(new Date());
            }
        }
    }

    public ArrayList<Element> getElements() {
        if (elementsList == null) {
            setElements(new ArrayList<Element>());
        }
        return elementsList;
    }

    public MutableLiveData<ArrayList<Element>>  getLiveDataElements() {
        return ldElementsList;
    }

    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    public void delete() {
        albumParent.delPage(this);
    }

    public void clear() {
        //clear all persistent data on page before deletion
        for (Element e : getElements()) {
            e.clear();
        }
    }

    public void onChange() {
        setLastEditDate(new Date());
        if (albumParent != null) {
            albumParent.onPageChange();
        }
    }

    public void moveUp() {
        int index = albumParent.getIndex(this);
        if (index > 0) {
            albumParent.movePage(this, index-1);
        }
    }

    public void moveDown() {
        int index = albumParent.getIndex(this);
        if (index < albumParent.getPages().size()) {
            albumParent.movePage(this, index+2);
        }
    }

    public int getNbTxt() {
        int nb=0;
        for (Element e: getElements()) {
            if (e.getClass().equals(TextElement.class)) {
                nb += 1;
            }
        }
        return nb;
    }

    public int getNbImage() {
        int nb=0;
        for (Element e: getElements()) {
            if (e instanceof ImageElement) {
                nb += 1;
            }
        }
        return nb;
    }


    public Element getElement(UUID elementUUID) {
        for (Element e : getElements()) {
            if (e.getId().equals(elementUUID)) {
                return e;
            }
        }
        return null;
    }

    public void moveToFront(Element element) {
        if (getElements().contains(element)) {
            ArrayList<Element> tmp = getElements();
            tmp.remove(element);
            tmp.add(element);
            setElements(tmp);
        }
    }

    public void moveToBack(Element element) {
        if (getElements().contains(element)) {
            ArrayList<Element> tmp = getElements();
            tmp.remove(element);
            tmp.add(0,element);
            setElements(tmp);
        }
    }

    public Date getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(Date date) {
        lastEditDate = date;
    }

    public boolean update(PageNC pageNC) {
        try {
            fromJSON(pageNC.toJSON());
        } catch (JSONException e) {
            Log.w(getClass().getName(),"Error parsing json.");
            return false;
        }
        return true;
    }

    public boolean hasPaintElement() {
        for (Element e : getElements()) {
            if (e.getClass().equals(PaintElement.class)) {
                return true;
            }
        }
        return false;
    }

    public PaintElement getPaintElement() {
        for (Element e : getElements()) {
            if (e.getClass().equals(PaintElement.class)) {
                return (PaintElement)e;
            }
        }
        return null;
    }

    public PaintElement createPaintElement() {
        PaintElement paintElement = new PaintElement();
        addElement(paintElement);
        return paintElement;
    }

    public AudioElement createAudioElement() {
        AudioElement audioElement = new AudioElement();
        addElement(audioElement);
        return audioElement;
    }

    public void removeAudio() {
        for (Element e : getElements()) {
            if (e.getClass().equals(AudioElement.class)) {
                delElement(e);
                return;
            }
        }
    }

    public VideoElement createVideoElement() {
        VideoElement videoElement = new VideoElement();
        addElement(videoElement);
        return videoElement;
    }
}
