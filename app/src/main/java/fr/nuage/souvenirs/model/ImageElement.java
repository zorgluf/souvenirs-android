package fr.nuage.souvenirs.model;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class ImageElement extends Element {

    public static final int FIT = 0;
    public static final int CENTERCROP = 1;
    public static final int ZOOM_OFFSET = 2;
    public static final String GOOGLE_PANORAMA_360_MIMETYPE = "application/vnd.google.panorama360+jpg";

    private MutableLiveData<String> ldImagePath = new MutableLiveData<String>();
    private MutableLiveData<Integer> ldTransformType = new MutableLiveData<>();
    private MutableLiveData<Integer> ldZoom = new MutableLiveData<>();
    private MutableLiveData<Integer> ldOffsetX = new MutableLiveData<>();
    private MutableLiveData<Integer> ldOffsetY = new MutableLiveData<>();
    private MutableLiveData<Boolean> ldIsPano = new MutableLiveData<>();
    private String imagePath;
    private String mimeType;
    private int zoom = 100;
    private int offsetX = 0;
    private int offsetY = 0;
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
        ldIsPano.postValue(isPhotosphere());
        if (save) {
            onChange();
        }
    }

    private boolean isPhotosphere(String imagePath) {
        File mFile = new File(imagePath);
        try {
            BufferedReader mReader = new BufferedReader(new FileReader(mFile));
            String sCurrentLine;
            while ((sCurrentLine = mReader.readLine()) != null) {
                if(sCurrentLine.contains("GPano:ProjectionType")){
                    mReader.close();
                    return true;
                }
            }
            mReader.close();
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private boolean isPhotosphere() {
        return (mimeType.equals(GOOGLE_PANORAMA_360_MIMETYPE));
    }

    public void setImage(InputStream input, String mimeType) {
        String imPath = pageParent.getAlbum().createDataFile(input,mimeType);
        if (imPath != null) {
            if (isPhotosphere(imPath)) {
                mimeType = GOOGLE_PANORAMA_360_MIMETYPE;
            }
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
        json.put("zoom",zoom);
        json.put("offsetX",offsetX);
        json.put("offsetY",offsetY);
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
        if (jsonObject.has("zoom")) {
            setZoom(jsonObject.getInt("zoom"),false);
        }
        if (jsonObject.has("offsetX")) {
            setOffsetX(jsonObject.getInt("offsetX"),false);
        }
        if (jsonObject.has("offsetY")) {
            setOffsetY(jsonObject.getInt("offsetY"),false);
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

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        setZoom(zoom,true);
    }

    public void setZoom(int zoom, boolean save) {
        this.zoom = zoom;
        ldZoom.postValue(zoom);
        if (save) {
            onChange();
        }
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        setOffsetX(offsetX,true);
    }

    public void setOffsetX(int offsetX, boolean save) {
        this.offsetX = offsetX;
        ldOffsetX.postValue(offsetX);
        if (save) {
            onChange();
        }
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        setOffsetY(offsetY,true);
    }

    public void setOffsetY(int offsetY, boolean save) {
        this.offsetY = offsetY;
        ldOffsetY.postValue(offsetY);
        if (save) {
            onChange();
        }
    }

    public MutableLiveData<Integer> getLdZoom() {
        return ldZoom;
    }

    public MutableLiveData<Integer> getLdOffsetX() {
        return ldOffsetX;
    }

    public MutableLiveData<Integer> getLdOffsetY() {
        return ldOffsetY;
    }

    public MutableLiveData<Boolean> getLdIsPano() {
        return ldIsPano;
    }

    /**
     * get width of image file
     * @return width
     */
    public int getImageWidth() {
        if ((imagePath == null) || (imagePath.equals(""))) {
            return 0;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return options.outHeight;
            }
        } catch (IOException e) {
        }
        return options.outWidth;
    }

    /**
     * get heiqht of image file
     * @return height
     */
    public int getImageHeight() {
        if ((imagePath == null) || (imagePath.equals(""))) {
            return 0;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return options.outWidth;
            }
        } catch (IOException e) {
        }
        return options.outHeight;
    }
}
