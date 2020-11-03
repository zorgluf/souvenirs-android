package fr.nuage.souvenirs.model.nc;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.UnknownElement;

public abstract class ElementNC {

    private MutableLiveData<Integer> ldLeft = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldRight = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldTop = new MutableLiveData<Integer>();
    private MutableLiveData<Integer> ldBottom = new MutableLiveData<Integer>();
    private MutableLiveData<UUID> ldId = new MutableLiveData<UUID>();
    private Integer left;
    private Integer right;
    private Integer top;
    private Integer bottom;
    protected PageNC pageParent;
    private UUID id;

    public ElementNC() {
        this(0,0,100,100);
    }

    public ElementNC(int left, int top, int right, int bottom) {
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

    public void setPageParent(PageNC p) {
        pageParent = p;
    }

    abstract public JSONObject completeToJSON(JSONObject json) throws JSONException;

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("class",this.getClass().getSimpleName().replaceAll("^(.+)NC","$1"));
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

    public static ElementNC fromJSON(JSONObject jsonObject, PageNC page) throws JSONException {
        String elementClass = jsonObject.getString("class").replaceAll("^(.+\\.)?([^\\.]+)$","$2NC");
        ElementNC e;
        try {
            Class<?> clazz = Class.forName(ElementNC.class.getPackage().getName()+"."+elementClass);
            e = (ElementNC) clazz.newInstance();
        } catch (Exception ex) {
            e = new UnknownElementNC();
        }
        e.setPageParent(page);
        if (jsonObject.has("id")) {
            e.setId(UUID.fromString(jsonObject.getString("id")));
        }
        e.setLeft(jsonObject.getInt("left"));
        e.setTop(jsonObject.getInt("top"));
        e.setRight(jsonObject.getInt("right"));
        e.setBottom(jsonObject.getInt("bottom"));
        e.completeFromJSON(jsonObject);
        return e;
    }


    public MutableLiveData<Integer> getLiveDataLeft() { return ldLeft; }

    public void setLeft(int left) {
        this.left = left;
        this.ldLeft.postValue(this.left);
        onChange();
    }


    public MutableLiveData<Integer> getLiveDataRight() { return ldRight; }

    public void setRight(int right) {
        this.right = right;
        this.ldRight.postValue(this.right);
        onChange();
    }


    public MutableLiveData<Integer> getLiveDataTop() { return ldTop; }

    public void setTop(int top) {
        this.top = top;
        this.ldTop.postValue(this.top);
        onChange();
    }

    public MutableLiveData<Integer> getLiveDataBottom() { return ldBottom; }

    public void setBottom(int bottom) {
        this.bottom = bottom;
        this.ldBottom.postValue(this.bottom);
        onChange();
    }

    public MutableLiveData<UUID> getLiveDataId() { return ldId; }

    public void setId(UUID id) {
        this.id = id;
        this.ldId.postValue(this.id);
        onChange();
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

}
