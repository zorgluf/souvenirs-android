package fr.nuage.souvenirs.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Size;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class VideoElement extends ImageElement {

    private final MutableLiveData<String> ldVideoPath = new MutableLiveData<>();
    private String videoPath;

    public VideoElement() {
        this("");
    }

    public VideoElement(int left, int top, int right, int bottom) {
        this("",left,top,right,bottom);
    }

    public VideoElement(String imgPath) {
        this(imgPath,0,0,100,100);
    }

    public VideoElement(String imgPath, int left, int top, int right, int bottom) {
        this(imgPath,left,top,right,bottom,"");
    }

    public VideoElement(String imgPath, int left, int top, int right, int bottom, String mimeType) {
        this(imgPath,left,top,right,bottom,mimeType,"");
    }

    public VideoElement(String imgPath, int left, int top, int right, int bottom, String mimeType, String videoPath) {
        super(imgPath,left, top, right, bottom);
        this.videoPath = videoPath;
        ldVideoPath.postValue(videoPath);
    }

    public void setVideoPath(String path) {
        setVideoPath(path,true);
    }

    public void setVideoPath(String path, boolean save) {
        videoPath = path;
        ldVideoPath.postValue(videoPath);
        if (save) {
            onChange();
        }
    }

    public void setVideo(InputStream input, String mimeType) {
        String videoPath = pageParent.getAlbum().createDataFile(input,mimeType);
        if (videoPath != null) {
            setVideoPath(videoPath);
            setMimeType(mimeType);
        } else {
            setVideoPath("");
            setMimeType("");
        }
        //create image from thumbmail
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(getVideoPath(), MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
        setImage(thumb);
    }

    private static Size getThumbSize(String path) {
        MediaMetadataRetriever dataRetriever = new MediaMetadataRetriever();
        dataRetriever.setDataSource(path);
        String width =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        return new Size(Integer.parseInt(width), Integer.parseInt(height));
    }



    /**
    * This method set the image path attribute to element, without deleting the previous one.
     * Path must be local to album, ie already set in album data path
     */
    public void setVideo(String localVideoPath, String mimeType) {
        setImagePath(localVideoPath);
        setMimeType(mimeType);
    }

    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json = super.completeToJSON(json);
        json.put("video",Utils.getRelativePath(pageParent.getAlbum().getAlbumPath(),videoPath));
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        super.completeFromJSON(jsonObject);
        //must be called from UI thread
        if (jsonObject.has("video")) {
                setVideoPath(new File(pageParent.getAlbum().getAlbumPath(),jsonObject.getString("video")).getPath(),false);
        }
    }

    public String getVideoPath() {
        return videoPath;
    }

    private void deleteVideoFile() {
        if (getVideoPath() != null ) {
            File videoFile = new File(getVideoPath());
            if (videoFile.exists()) {
                videoFile.delete();
            }
        }
    }

    @Override
    public void delete() {
        deleteVideoFile();
        super.delete();
    }

    public MutableLiveData<String> getLdVideoPath() {
        return ldVideoPath;
    }
}
