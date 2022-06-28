package fr.nuage.souvenirs.model.nc;

import org.json.JSONException;
import org.json.JSONObject;

public class AudioElementNC extends ElementNC {

    private String audioPath;
    private boolean stop;

    public AudioElementNC() {
        this("",false);
    }

    public AudioElementNC(String audioPath, boolean stop) {
        super();
        this.audioPath = audioPath;
        this.stop = stop;
    }

    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        return null;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("audio")) {
            String imagePath = jsonObject.getString("audio");
            setAudioPath(imagePath);
        }
        if (jsonObject.has("stop")) {
            setStop(jsonObject.getBoolean("stop"));
        }
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        onChange();
    }

    public void setStop(boolean stop) {
        this.stop = stop;
        onChange();
    }

    public String getAudioPath() {
        return audioPath;
    }

    public boolean isStop() {
        return stop;
    }

    @Override
    public void load(APIProvider.ElementResp elementResp) {
        super.load(elementResp);
        setAudioPath(elementResp.audioPath);
        setStop(elementResp.stop);
    }

    @Override
    public APIProvider.ElementResp generateElementResp() {
        APIProvider.ElementResp elementResp = super.generateElementResp();
        elementResp.audioPath = getAudioPath();
        elementResp.stop = isStop();
        return elementResp;
    }
}
