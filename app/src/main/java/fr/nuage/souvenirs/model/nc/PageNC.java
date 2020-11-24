package fr.nuage.souvenirs.model.nc;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.Page;

public class PageNC {

    private MutableLiveData<ArrayList<ElementNC>> ldElementsList = new MutableLiveData<ArrayList<ElementNC>>();
    private ArrayList<ElementNC> elements;
    private UUID id;
    private Date lastEditDate;

    public PageNC() {
        setElements(new ArrayList<>());
    }

    public void setElements(ArrayList<ElementNC> ee) {
        elements = ee;
        ldElementsList.postValue(elements);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id",getId().toString());
            JSONArray elements = new JSONArray();
            for( ElementNC p: this.elements) {
                elements.put(p.toJSON());
            }
            json.put("elements",elements);
            if (lastEditDate != null) {
                json.put("lastEditDate",new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(lastEditDate));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }


    public void fromJSON(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("id")) {
            id = UUID.fromString(jsonObject.getString("id"));
        }
        if (jsonObject.has("elements")) {
            JSONArray jelements = jsonObject.getJSONArray("elements");
            ArrayList<ElementNC> elements = new ArrayList<ElementNC>();
            for (int i = 0; i < jelements.length(); i++) {
                ElementNC e = ElementNC.fromJSON(jelements.getJSONObject(i),this);
                elements.add(e);
            }
            setElements(elements);
        }
        if (jsonObject.has("lastEditDate")) {
            try {
                setLastEditDate(new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(jsonObject.getString("lastEditDate")));
            } catch (Exception e) {
            }
        } else {
            lastEditDate = null;
        }
    }

    public void load(APIProvider.PageResp pageResp) {
        id = pageResp.id;
        setLastEditDate(pageResp.lastEditDate);
        if (pageResp.elements != null) {
            ArrayList<ElementNC> elementNCArrayList = new ArrayList<>();
            for (APIProvider.ElementResp elementResp : pageResp.elements) {
                String elementClass = elementResp.className.replaceAll("^(.+\\.)?([^\\.]+)$","$2NC");
                ElementNC elementNC;
                try {
                    Class<?> clazz = Class.forName(ElementNC.class.getPackage().getName()+"."+elementClass);
                    elementNC = (ElementNC) clazz.newInstance();
                } catch (Exception ex) {
                    elementNC = new UnknownElementNC();
                }
                elementNC.load(elementResp);
                elementNCArrayList.add(elementNC);
            }
            setElements(elementNCArrayList);
        }
    }

    public APIProvider.PageResp generatePageResp() {
        APIProvider.PageResp pageResp = new APIProvider.PageResp();
        pageResp.id = getId();
        pageResp.lastEditDate = getLastEditDate();
        pageResp.elements = new ArrayList<>();
        for (ElementNC elementNC: getElements()) {
            pageResp.elements.add(elementNC.generateElementResp());
        }
        return pageResp;
    }

    public ArrayList<ElementNC> getElements() {
        if (elements == null) {
            setElements(new ArrayList<ElementNC>());
        }
        return elements;
    }

    public MutableLiveData<ArrayList<ElementNC>>  getLiveDataElements() {
        return ldElementsList;
    }

    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    public void clear() {
        //clear all persistent data on page before deletion
        for (ElementNC e : getElements()) {
            e.clear();
        }
    }



    public ElementNC getElement(UUID elementUUID) {
        for (ElementNC e : getElements()) {
            if (e.getId().equals(elementUUID)) {
                return e;
            }
        }
        return null;
    }


    public Date getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(Date date) {
        lastEditDate = date;
    }

    public boolean update(Page page) {
        try {
            fromJSON(page.toJSON());
        } catch (JSONException e) {
            Log.w(getClass().getName(),"Error parsing json.");
            return false;
        }
        return true;
    }


    public boolean pushAssets(String localAlbumPath, AlbumNC  albumNC) {
        //push images
        for (ElementNC e : getElements()) {
            if (e instanceof ImageElementNC) {
                ImageElementNC ime = (ImageElementNC)e;
                if (!albumNC.pushAsset(localAlbumPath,ime.getImagePath())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean pullAssets(String localAlbumPath, AlbumNC  albumNC) {
        //pull images
        for (ElementNC e : getElements()) {
            if (e instanceof ImageElementNC) {
                ImageElementNC ime = (ImageElementNC)e;
                if (!albumNC.pullAsset(localAlbumPath,ime.getImagePath())) {
                    return false;
                }
            }
        }
        return true;
    }
}
