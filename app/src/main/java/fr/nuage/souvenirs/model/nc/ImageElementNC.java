package fr.nuage.souvenirs.model.nc;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.Utils;

public class ImageElementNC extends ElementNC {

    public static final int FIT = 0;
    public static final int CENTERCROP = 1;
    public static final int ZOOM_OFFSET = 2;

    private String imagePath;
    private String mimeType;
    private int zoom = 100;
    private int offsetX = 0;
    private int offsetY = 0;
    private int transformType = FIT;

    public ImageElementNC() {
        this("");
    }

    public ImageElementNC(int left, int top, int right, int bottom) {
        this("",left,top,right,bottom);
    }

    public ImageElementNC(String imgPath) {
        this(imgPath,0,0,100,100);
    }

    public ImageElementNC(String imgPath, int left, int top, int right, int bottom) {
        this(imgPath,left,top,right,bottom,"");
    }

    public ImageElementNC(String imgPath, int left, int top, int right, int bottom, String mimeType) {
        super(left, top, right, bottom);
        imagePath = imgPath;
        this.mimeType = mimeType;
    }

    public void setImagePath(String path) {
        imagePath = path;
        onChange();
    }

    public void setTransformType(int transformType) {
        this.transformType = transformType;
        onChange();
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    public void setImage(String localImagePath, String mimeType) {
        setImagePath(localImagePath);
        setMimeType(mimeType);
    }

    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json.put("image",imagePath);
        json.put("mime",mimeType);
        json.put("transformType",transformType);
        json.put("zoom",zoom);
        json.put("offsetX",offsetX);
        json.put("offsetY",offsetY);
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        //must be called from UI thread
        if (jsonObject.has("image")) {
            String imagePath = jsonObject.getString("image");
            //if start with / assume old absolute format and try to guess relative
            if (imagePath.startsWith("/")) {
                setImagePath(imagePath.replaceAll("^.+/([^/]+/[^/]+)$","$1"));
            } else {
                setImagePath(imagePath);
            }
        }
        if (jsonObject.has("mime")) {
            setMimeType(jsonObject.getString("mime"));
        }
        if (jsonObject.has("transformType")) {
            setTransformType(jsonObject.getInt("transformType"));
        }
        if (jsonObject.has("zoom")) {
            zoom = jsonObject.getInt("zoom");
        }
        if (jsonObject.has("offsetX")) {
            offsetX = jsonObject.getInt("offsetX");
        }
        if (jsonObject.has("offsetY")) {
            offsetY = jsonObject.getInt("offsetY");
        }
    }

    @Override
    public void load(APIProvider.ElementResp elementResp) {
        super.load(elementResp);
        setImagePath(elementResp.imagePath);
        setMimeType(elementResp.mimeType);
        setTransformType(elementResp.transformType);
        zoom = elementResp.zoom;
        offsetX = elementResp.offsetX;
        offsetY = elementResp.offsetY;
    }

    @Override
    public APIProvider.ElementResp generateElementResp() {
        APIProvider.ElementResp elementResp = super.generateElementResp();
        elementResp.imagePath = getImagePath();
        elementResp.mimeType = getMimeType();
        elementResp.transformType = getTransformType();
        elementResp.zoom = zoom;
        elementResp.offsetX = offsetX;
        elementResp.offsetY = offsetY;
        return elementResp;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getTransformType() { return transformType; }


}
