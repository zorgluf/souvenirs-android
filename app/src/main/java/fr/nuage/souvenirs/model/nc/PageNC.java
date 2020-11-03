package fr.nuage.souvenirs.model.nc;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.viewmodel.utils.FixedDownloadFileRemoteOperation;
import fr.nuage.souvenirs.viewmodel.utils.NCGetAssetProbe;
import fr.nuage.souvenirs.viewmodel.utils.NCPostPage;

public class PageNC {

    private MutableLiveData<ArrayList<ElementNC>> ldElementsList = new MutableLiveData<ArrayList<ElementNC>>();
    private ArrayList<ElementNC> elementsList;
    private AlbumNC albumParent;
    private UUID id;
    private Date lastEditDate;


    public PageNC() {
        setElements(new ArrayList<>());
    }

    public void setAlbumParent(AlbumNC a) {
        albumParent = a;
    }

    public AlbumNC getAlbum() {
        return albumParent;
    }

    public void setElements(ArrayList<ElementNC> ee) {
        elementsList = ee;
        ldElementsList.postValue(elementsList);
        onChange();
    }

    public void addElement(ElementNC e) {
        ArrayList<ElementNC> tmp = getElements();
        e.setPageParent(this);
        tmp.add(e);
        setElements(tmp);
    }

    public void addElements(ArrayList<ElementNC> ee) {
        ArrayList<ElementNC> tmp = getElements();
        for (ElementNC e : ee) {
            e.setPageParent(this);
        }
        tmp.addAll(ee);
        setElements(tmp);
    }

    public void delElement(ElementNC element) {
        if (getElements().contains(element)) {
            ArrayList<ElementNC> tmp = getElements();
            tmp.remove(element);
            setElements(tmp);
        }
    }

    public ImageElementNC createImageElement() {
        ImageElementNC imageElement = new ImageElementNC();
        addElement(imageElement);
        return imageElement;
    }

    public TextElementNC createTextElement() {
        TextElementNC textElement = new TextElementNC();
        addElement(textElement);
        return textElement;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id",getId().toString());
            JSONArray elements = new JSONArray();
            for( ElementNC p: elementsList ) {
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

    public static PageNC fromJSON(JSONObject jsonObject, AlbumNC album) throws JSONException {
        PageNC p = new PageNC();
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

    public ArrayList<ElementNC> getElements() {
        if (elementsList == null) {
            setElements(new ArrayList<ElementNC>());
        }
        return elementsList;
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

    public void delete() {
        albumParent.delPage(this);
    }

    public void clear() {
        //clear all persistent data on page before deletion
        for (ElementNC e : getElements()) {
            e.clear();
        }
    }

    public void onChange() {
        if (albumParent != null) {
            albumParent.onChange();
        }
        setLastEditDate(new Date());
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

    /*
    !sync method
     */
    public boolean save(String localAlbumPath) {
        if (!pushAssets(localAlbumPath,getAlbum()))  {
            return false;
        }

        //save page content
        RemoteOperationResult postPageResult = new NCPostPage(getAlbum(),this).execute(getAlbum().getNcClient());
        if (postPageResult.isSuccess()) {
            Log.d(getClass().getName(),String.format("Page %1$s uploaded.",getId().toString()));
            return true;
        } else {
            Log.i(getClass().getName(),String.format("Error in page %1$s upload.",getId().toString()));
            return false;
        }
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
