package fr.nuage.souvenirs.model;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public abstract class Element {

    private MutableLiveData<Integer> ldLeft = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldRight = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldTop = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldBottom = new MutableLiveData<Integer>();
    private MutableLiveData<UUID> ldId = new MutableLiveData<UUID>();
    private Integer left;
    private Integer right;
    private Integer top;
    private Integer bottom;
    protected Page pageParent;
    private UUID id;

    public Element() {
        this(0,0,100,100);
    }

    public Element(int left, int top, int right, int bottom) {
        this.left = left;
        this.ldLeft.postValue(this.left);
        this.right = right;
        this.ldRight.postValue(this.right);
        this.top = top;
        this.ldTop.postValue(this.top);
        this.bottom = bottom;
        this.ldBottom.postValue(this.bottom);
        setId(UUID.randomUUID());
    }

    public void setPageParent(Page p) {
        pageParent = p;
    }

    abstract public JSONObject completeToJSON(JSONObject json) throws JSONException;

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("class",this.getClass().getSimpleName());
            json.put("id",getId().toString());
            json.put("left", left);
            json.put("top",top);
            json.put("right",right);
            json.put("bottom",bottom);
            completeToJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    abstract public void completeFromJSON(JSONObject jsonObject) throws JSONException;

    public static Element fromJSON(JSONObject jsonObject, Page page) throws JSONException {
        String elementClass = jsonObject.getString("class").replaceAll("^(.+\\.)?([^\\.]+)$","$2");
        Element e;
        try {
            Class<?> clazz = Class.forName(Element.class.getPackage().getName()+"."+elementClass);
            e = (Element) clazz.newInstance();
        } catch (Exception ex) {
            e = new UnknownElement();
        }
        e.setPageParent(page);
        if (jsonObject.has("id")) {
            e.setId(UUID.fromString(jsonObject.getString("id")),false);
        }
        e.setLeft(jsonObject.getInt("left"),false);
        e.setTop(jsonObject.getInt("top"),false);
        e.setRight(jsonObject.getInt("right"),false);
        e.setBottom(jsonObject.getInt("bottom"),false);
        e.completeFromJSON(jsonObject);
        return e;
    }


    public MutableLiveData<Integer> getLiveDataLeft() { return ldLeft; }

    public void setLeft(int left) {
        setLeft(left,true);
    }

    public void setLeft(int left, boolean save) {
        this.left = left;
        this.ldLeft.postValue(this.left);
        if (save) {
            onChange();
        }
    }


    public MutableLiveData<Integer> getLiveDataRight() { return ldRight; }

    public void setRight(int right) {
        setRight(right,true);
    }

    public void setRight(int right, boolean save) {
        this.right = right;
        this.ldRight.postValue(this.right);
        if (save) {
            onChange();
        }
    }


    public MutableLiveData<Integer> getLiveDataTop() { return ldTop; }

    public void setTop(int top) {
        setTop(top,true);
    }

    public void setTop(int top, boolean save) {
        this.top = top;
        this.ldTop.postValue(this.top);
        if (save) {
            onChange();
        }
    }

    public MutableLiveData<Integer> getLiveDataBottom() { return ldBottom; }

    public void setBottom(int bottom) {
        setBottom(bottom,true);
    }

    public void setBottom(int bottom, boolean save) {
        this.bottom = bottom;
        this.ldBottom.postValue(this.bottom);
        if (save) {
            onChange();
        }
    }

    public MutableLiveData<UUID> getLiveDataId() { return ldId; }

    public void setId(UUID id) {
        setId(id,true);
    }

    public void setId(UUID id, boolean save) {
        this.id = id;
        this.ldId.postValue(this.id);
        if (save) {
            onChange();
        }
    }

    public UUID getId() {
        return id;
    }

    public void clear() {
        //clear element before deletion
        //should be overridden
    }

    public void onChange() {
        if (pageParent != null) {
            pageParent.onChange();
        }
    }

    public Integer getLeft() {
        return left;
    }

    public Integer getRight() {
        return right;
    }

    public Integer getTop() {
        return top;
    }

    public Integer getBottom() {
        return bottom;
    }

    public void delete() {
        pageParent.delElement(this);
    }

    public void moveToPreviousPage() {
        Page actualPage = pageParent;
        Album album = actualPage.getAlbum();
        if (!album.FirstPage(actualPage)) {
            Page previousPage = album.getPage(album.getIndex(actualPage)-1);
            previousPage.addElement(this);
            actualPage.delElement(this);
            TilePageBuilder pageBuilder = new TilePageBuilder();
            pageBuilder.applyDefaultStyle(actualPage);
            pageBuilder.applyDefaultStyle(previousPage);
        }
    }

    public void moveToNextPage() {
        Page actualPage = pageParent;
        Album album = actualPage.getAlbum();
        if (!album.isLastPage(actualPage)) {
            Page nextPage = album.getPage(album.getIndex(actualPage)+1);
            nextPage.addElement(this);
            actualPage.delElement(this);
            TilePageBuilder pageBuilder = new TilePageBuilder();
            pageBuilder.applyDefaultStyle(actualPage);
            pageBuilder.applyDefaultStyle(nextPage);
        }
    }

    public void moveToFront() {
        pageParent.moveToFront(this);
    }

    public void moveToBack() {
        pageParent.moveToBack(this);
    }
}
