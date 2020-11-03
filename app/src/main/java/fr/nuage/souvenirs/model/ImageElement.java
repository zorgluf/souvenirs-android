package fr.nuage.souvenirs.model;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

public class ImageElement extends Element {

    public static final int FIT = 0;
    public static final int CENTERCROP = 1;

    private MutableLiveData<String> ldImagePath = new MutableLiveData<String>();
    private MutableLiveData<Integer> ldTransformType = new MutableLiveData<>();
    private String imagePath;
    private String mimeType;
    private int transformType = FIT;

    public ImageElement() {
        this("");
    }

    public ImageElement(int left, int top, int right, int bottom) {
        this("",left,top,right,bottom);
    }

    public ImageElement(String imgPath) {
        this(imgPath,0,0,100,100);
    }

    public ImageElement(String imgPath, int left, int top, int right, int bottom) {
        this(imgPath,left,top,right,bottom,"");
    }

    public ImageElement(String imgPath, int left, int top, int right, int bottom, String mimeType) {
        super(left, top, right, bottom);
        imagePath = imgPath;
        ldImagePath.postValue(imagePath);
        this.mimeType = mimeType;
    }

    public void setImagePath(String path) {
        setImagePath(path,true);
    }

    public void setImagePath(String path, boolean save) {
        imagePath = path;
        ldImagePath.postValue(imagePath);
        if (save) {
            onChange();
        }
    }

    public void setTransformType(int transformType) {
        setTransformType(transformType,true);
    }

    public void setTransformType(int transformType, boolean save) {
        this.transformType = transformType;
        ldTransformType.postValue(transformType);
        if (save) {
            onChange();
        }
    }

    public void setMimeType(String mimeType) {
        setMimeType(mimeType,true);
    }

    public void setMimeType(String mimeType, boolean save) {
        this.mimeType = mimeType;
        if (save) {
            onChange();
        }
    }

    public void setImage(InputStream input, String mimeType) {
        String imPath = pageParent.getAlbum().createDataFile(input,mimeType);
        if (imPath != null) {
            setImagePath(imPath);
            setMimeType(mimeType);
        } else {
            setImagePath("");
            setMimeType("");
        }
    }


    /**
    * This method set the image path attribute to element, without deleting the previous one.
     * Path must be local to album, ie already set in album data path
     */
    public void setImage(String localImagePath, String mimeType) {
        setImagePath(localImagePath);
        setMimeType(mimeType);
    }

    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json.put("image",Utils.getRelativePath(pageParent.getAlbum().getAlbumPath(),imagePath));
        json.put("mime",mimeType);
        json.put("transformType",transformType);
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        //must be called from UI thread
        if (jsonObject.has("image")) {
            if (jsonObject.getString("image").startsWith("/")) {
                setImagePath(jsonObject.getString("image"),false);
            } else {
                setImagePath(new File(pageParent.getAlbum().getAlbumPath(),jsonObject.getString("image")).getPath(),false);
            }
        }
        if (jsonObject.has("mime")) {
            setMimeType(jsonObject.getString("mime"),false);
        }
        if (jsonObject.has("transformType")) {
            setTransformType(jsonObject.getInt("transformType"),false);
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getTransformType() { return transformType; }

    public MutableLiveData<String> getLiveDataImagePath() { return ldImagePath; }

    public MutableLiveData<Integer> getLiveDataTransformType() {
        return ldTransformType;
    }

    private void deleteImageFile() {
        if (getImagePath() != null) {
            File imageFile = new File(getImagePath());
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    @Override
    public void clear() {
        deleteImageFile();
    }

    @Override
    public void delete() {
        clear();
        super.delete();
    }

    public void setAsAlbumImage() {
        pageParent.getAlbum().setAlbumImage(getImagePath());
    }
}
