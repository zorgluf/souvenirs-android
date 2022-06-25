package fr.nuage.souvenirs.model.nc;

import org.json.JSONException;
import org.json.JSONObject;

public class VideoElementNC extends ImageElementNC {


    private String videoPath;

    public VideoElementNC() {
        this("");
    }

    public VideoElementNC(int left, int top, int right, int bottom) {
        this("",left,top,right,bottom);
    }

    public VideoElementNC(String imgPath) {
        this(imgPath,0,0,100,100);
    }

    public VideoElementNC(String imgPath, int left, int top, int right, int bottom) {
        this(imgPath,left,top,right,bottom,"");
    }

    public VideoElementNC(String imgPath, int left, int top, int right, int bottom, String mimeType) {
        this(imgPath,left,top,right,bottom,mimeType,"");
    }

    public VideoElementNC(String imgPath, int left, int top, int right, int bottom, String mimeType, String videoPath) {
        super(imgPath,left, top, right, bottom,mimeType);
        this.videoPath = videoPath;
    }

    public void setVideoPath(String path) {
        this.videoPath = path;
        onChange();
    }


    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json = super.completeToJSON(json);
        json.put("video",videoPath);
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        //must be called from UI thread
        if (jsonObject.has("video")) {
            String path = jsonObject.getString("video");
            setVideoPath(path);
        }
        super.completeFromJSON(jsonObject);
    }

    @Override
    public void load(APIProvider.ElementResp elementResp) {
        super.load(elementResp);
        setVideoPath(elementResp.videoPath);
    }

    @Override
    public APIProvider.ElementResp generateElementResp() {
        APIProvider.ElementResp resp = super.generateElementResp();
        resp.videoPath = getVideoPath();
        return resp;
    }

    public String getVideoPath() {
        return videoPath;
    }


}
