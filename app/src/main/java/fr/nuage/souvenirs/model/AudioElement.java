package fr.nuage.souvenirs.model;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

public class AudioElement extends Element {

    private String audioPath;
    private boolean stop;
    private MutableLiveData<String> ldAudioPath = new MutableLiveData<>();
    private MutableLiveData<Boolean> ldStop = new MutableLiveData<>();

    public AudioElement(String audioPath, boolean stop) {
        super();
        setAudioPath(audioPath,false);
        setStop(stop,false);
    }

    public AudioElement() {
        this("",false);
    }

    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json.put("audio",Utils.getRelativePath(pageParent.getAlbum().getAlbumPath(),audioPath));
        json.put("stop",stop);
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("audio")) {
            setAudioPath(new File(pageParent.getAlbum().getAlbumPath(),jsonObject.getString("audio")).getPath(),false);
        }
        if (jsonObject.has("stop")) {
            setStop(jsonObject.getBoolean("stop"),false);
        }
    }

    public void setAudioPath(String audioPath, boolean save) {
        this.audioPath = audioPath;
        ldAudioPath.postValue(audioPath);
        if (save) {
            onChange();
        }
    }

    public void setStop(boolean stop, boolean save) {
        this.stop = stop;
        ldStop.postValue(stop);
        if (save) {
            onChange();
        }
    }

    public String getAudioPath() {
        return audioPath;
    }

    public boolean isStop() {
        return stop;
    }

    public MutableLiveData<String> getLdAudioPath() {
        return ldAudioPath;
    }

    public MutableLiveData<Boolean> getLdStop() {
        return ldStop;
    }

    public void setAudio(InputStream input, String mimeType) {
        String audioPath = pageParent.getAlbum().createDataFile(input,mimeType);
        if (audioPath != null) {
            setAudioPath(audioPath,true);
        } else {
            setAudioPath("",true);
        }
    }

    public void setAudio(File audioFile) {
        setAudioPath(audioFile.getPath(),true);
    }

    private void deleteAudioFile() {
        if (getAudioPath() != null) {
            File audioFile = new File(getAudioPath());
            if (audioFile.exists()) {
                audioFile.delete();
            }
        }
    }

    @Override
    public void clear() {
        deleteAudioFile();
    }

    @Override
    public void delete() {
        clear();
        super.delete();
    }
}
